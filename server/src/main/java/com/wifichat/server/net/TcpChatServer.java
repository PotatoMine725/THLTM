package com.wifichat.server.net;

import com.wifichat.server.auth.PasswordService;
import com.wifichat.server.db.ChatRepository;
import com.wifichat.server.model.AuthContext;
import com.wifichat.server.model.SessionInfo;
import com.wifichat.server.model.UserAccount;
import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.shared.dto.ConversationsResponse;
import com.wifichat.shared.dto.FetchHistoryRequest;
import com.wifichat.shared.dto.HeartbeatRequest;
import com.wifichat.shared.dto.HistoryBatchResponse;
import com.wifichat.shared.dto.ListConversationsRequest;
import com.wifichat.shared.dto.LoginRequest;
import com.wifichat.shared.dto.MessagePayload;
import com.wifichat.shared.dto.MessageRecord;
import com.wifichat.shared.dto.RegisterRequest;
import com.wifichat.shared.dto.ResumeSessionRequest;
import com.wifichat.shared.dto.SendMessageRequest;
import com.wifichat.shared.dto.SubscribeConversationRequest;
import com.wifichat.shared.protocol.ErrorCodes;
import com.wifichat.shared.protocol.PacketTypes;
import com.wifichat.shared.protocol.WireCodec;
import com.wifichat.shared.protocol.WireEnvelope;
import com.wifichat.shared.util.ConversationKeys;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpChatServer {
    private final int port;
    private final ChatRepository repository;
    private final PasswordService passwordService;
    private final ExecutorService workerPool;
    private final Map<String, Set<ClientHandler>> subscriptions;

    private volatile boolean running;

    public TcpChatServer(int port, ChatRepository repository) {
        this.port = port;
        this.repository = repository;
        this.passwordService = new PasswordService();
        this.workerPool = Executors.newCachedThreadPool();
        this.subscriptions = new ConcurrentHashMap<>();
    }

    public void start() throws IOException {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP server listening on port " + port);
            while (running) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                workerPool.submit(handler);
            }
        }
    }

    public void stop() {
        running = false;
        workerPool.shutdownNow();
    }

    private void subscribe(String conversationKey, ClientHandler handler) {
        subscriptions.computeIfAbsent(conversationKey, ignored -> ConcurrentHashMap.newKeySet()).add(handler);
    }

    private void unsubscribeAll(ClientHandler handler) {
        for (Set<ClientHandler> set : subscriptions.values()) {
            set.remove(handler);
        }
    }

    private void broadcast(String conversationKey, WireEnvelope event) {
        Set<ClientHandler> handlers = subscriptions.getOrDefault(conversationKey, Collections.emptySet());
        for (ClientHandler handler : handlers) {
            handler.send(event);
        }
    }


    private final class ClientHandler implements Runnable {
        private final Socket socket;
        private final WireCodec codec;
        private final Object writeLock;

        private volatile boolean connected;

        private ClientHandler(Socket socket) {
            this.socket = socket;
            this.codec = new WireCodec();
            this.writeLock = new Object();
            this.connected = true;
        }

        @Override
        public void run() {
            try (socket) {
                while (connected) {
                    WireEnvelope request = codec.read(socket.getInputStream());
                    handleRequest(request);
                }
            } catch (Exception e) {
                System.err.println("Client disconnected: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            } finally {
                connected = false;
                unsubscribeAll(this);
            }
        }

        private void handleRequest(WireEnvelope request) {
            if (request == null || request.type == null) {
                return;
            }

            try {
                switch (request.type) {
                    case PacketTypes.REGISTER -> handleRegister(request);
                    case PacketTypes.LOGIN -> handleLogin(request);
                    case PacketTypes.RESUME_SESSION -> handleResumeSession(request);
                    case PacketTypes.SEND_MESSAGE -> handleSendMessage(request);
                    case PacketTypes.FETCH_HISTORY -> handleFetchHistory(request);
                    case PacketTypes.SUBSCRIBE_CONVERSATION -> handleSubscribe(request);
                    case PacketTypes.HEARTBEAT -> handleHeartbeat(request);
                    case PacketTypes.LIST_CONVERSATIONS -> handleListConversations(request);
                    default -> send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "Unsupported request type"));
                }
            } catch (SQLException e) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.INTERNAL_ERROR, "Database error: " + e.getMessage()));
            } catch (Exception e) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.INTERNAL_ERROR, "Server error: " + e.getMessage()));
            }
        }

        private void handleRegister(WireEnvelope request) throws SQLException {
            RegisterRequest payload = parse(request.payload, RegisterRequest.class);
            if (payload == null || isBlank(payload.username) || isBlank(payload.password) || isBlank(payload.displayName)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "username/password/displayName are required"));
                return;
            }

            String username = payload.username.trim().toLowerCase();
            String displayName = payload.displayName.trim();
            if (username.length() < 3 || payload.password.length() < 4) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "Username or password too short"));
                return;
            }

            String salt = passwordService.newSaltBase64();
            String hash = passwordService.hash(payload.password, salt);

            UserAccount user = repository.createUser(username, displayName, hash, salt);
            if (user == null) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.USERNAME_TAKEN, "Username already taken"));
                return;
            }

            SessionInfo session = repository.createSession(user.id);
            sendOkAuth(request.requestId, user, session);
        }

        private void handleLogin(WireEnvelope request) throws SQLException {
            LoginRequest payload = parse(request.payload, LoginRequest.class);
            if (payload == null || isBlank(payload.username) || isBlank(payload.password)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "username/password are required"));
                return;
            }

            UserAccount user = repository.findUserByUsername(payload.username.trim().toLowerCase());
            if (user == null || !passwordService.verify(payload.password, user.salt, user.passwordHash)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.AUTH_INVALID, "Invalid credentials"));
                return;
            }

            SessionInfo session = repository.createSession(user.id);
            sendOkAuth(request.requestId, user, session);
        }

        private void handleResumeSession(WireEnvelope request) throws SQLException {
            ResumeSessionRequest payload = parse(request.payload, ResumeSessionRequest.class);
            if (payload == null || isBlank(payload.sessionToken)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "sessionToken is required"));
                return;
            }

            AuthContext context = repository.authBySession(payload.sessionToken);
            if (context == null) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.SESSION_EXPIRED, "Session invalid or expired"));
                return;
            }

            AuthResponse response = new AuthResponse();
            response.sessionToken = context.sessionToken;
            response.userId = context.userId;
            response.username = context.username;
            response.displayName = context.displayName;
            response.expiresAt = context.expiresAt;
            send(ok(request.requestId, response));
        }

        private void handleSendMessage(WireEnvelope request) throws SQLException {
            SendMessageRequest payload = parse(request.payload, SendMessageRequest.class);
            if (payload == null || payload.message == null) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "message payload is required"));
                return;
            }

            AuthContext context = requireSession(payload.sessionToken, request.requestId);
            if (context == null) {
                return;
            }

            MessageRecord message = payload.message;
            if (isBlank(message.content)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "message content is required"));
                return;
            }

            if (isBlank(message.scope)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "message scope is required"));
                return;
            }

            String scope = message.scope.trim().toUpperCase();
            message.scope = scope;
            message.senderUserId = context.userId;
            message.senderName = context.displayName;
            message.timestamp = System.currentTimeMillis();
            if (isBlank(message.messageId)) {
                message.messageId = UUID.randomUUID().toString();
            }

            if ("GROUP".equals(scope)) {
                if (isBlank(message.roomName)) {
                    send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "roomName required for GROUP"));
                    return;
                }
                message.conversationKey = ConversationKeys.room(message.roomName);
                repository.addConversationMember(message.conversationKey, context.userId);
            } else if ("PRIVATE".equals(scope)) {
                if (isBlank(message.targetUserId)) {
                    send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "targetUserId required for PRIVATE"));
                    return;
                }
                message.conversationKey = ConversationKeys.pm(context.userId, message.targetUserId);
                repository.addConversationMember(message.conversationKey, context.userId);
                repository.addConversationMember(message.conversationKey, message.targetUserId);
            } else {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "Unknown scope"));
                return;
            }

            repository.saveMessage(message);

            MessagePayload eventPayload = new MessagePayload();
            eventPayload.message = message;

            send(WireEnvelope.of(PacketTypes.MESSAGE_SAVED, request.requestId, eventPayload));
            broadcast(message.conversationKey, WireEnvelope.of(PacketTypes.MESSAGE_EVENT, null, eventPayload));
        }

        private void handleFetchHistory(WireEnvelope request) throws SQLException {
            FetchHistoryRequest payload = parse(request.payload, FetchHistoryRequest.class);
            if (payload == null || isBlank(payload.conversationKey)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "conversationKey is required"));
                return;
            }
            AuthContext context = requireSession(payload.sessionToken, request.requestId);
            if (context == null) {
                return;
            }

            if (!repository.isConversationMember(payload.conversationKey, context.userId)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.FORBIDDEN, "Not a member of this conversation"));
                return;
            }

            int limit = payload.limit <= 0 ? 150 : Math.min(payload.limit, 500);
            List<MessageRecord> records = repository.fetchHistory(payload.conversationKey, limit);

            HistoryBatchResponse response = new HistoryBatchResponse();
            response.conversationKey = payload.conversationKey;
            response.messages = records;
            send(WireEnvelope.of(PacketTypes.HISTORY_BATCH, request.requestId, response));
        }

        private void handleSubscribe(WireEnvelope request) throws SQLException {
            SubscribeConversationRequest payload = parse(request.payload, SubscribeConversationRequest.class);
            if (payload == null || isBlank(payload.conversationKey)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.VALIDATION_ERROR, "conversationKey is required"));
                return;
            }

            AuthContext context = requireSession(payload.sessionToken, request.requestId);
            if (context == null) {
                return;
            }

            if (ConversationKeys.isPm(payload.conversationKey) && !payload.conversationKey.contains(context.userId)) {
                send(WireEnvelope.error(request.requestId, ErrorCodes.FORBIDDEN, "Forbidden PM conversation"));
                return;
            }

            if (ConversationKeys.isPm(payload.conversationKey)) {
                repository.addConversationMember(payload.conversationKey, context.userId);
            }

            subscribe(payload.conversationKey, this);
            send(WireEnvelope.of(PacketTypes.OK, request.requestId, null));
        }

        private void handleHeartbeat(WireEnvelope request) throws SQLException {
            HeartbeatRequest payload = parse(request.payload, HeartbeatRequest.class);
            AuthContext context = requireSession(payload == null ? null : payload.sessionToken, request.requestId);
            if (context == null) {
                return;
            }
            send(WireEnvelope.of(PacketTypes.OK, request.requestId, null));
        }

        private void handleListConversations(WireEnvelope request) throws SQLException {
            ListConversationsRequest payload = parse(request.payload, ListConversationsRequest.class);
            AuthContext context = requireSession(payload == null ? null : payload.sessionToken, request.requestId);
            if (context == null) {
                return;
            }

            int limit = payload.limit <= 0 ? 30 : Math.min(payload.limit, 300);
            ConversationsResponse response = new ConversationsResponse();
            response.conversationKeys = repository.listConversationsForUser(context.userId, limit);
            send(WireEnvelope.of(PacketTypes.CONVERSATIONS, request.requestId, response));
        }

        private AuthContext requireSession(String token, String requestId) throws SQLException {
            AuthContext context = repository.authBySession(token);
            if (context == null) {
                send(WireEnvelope.error(requestId, ErrorCodes.SESSION_EXPIRED, "Session invalid or expired"));
                return null;
            }
            return context;
        }

        private void sendOkAuth(String requestId, UserAccount user, SessionInfo session) {
            AuthResponse response = new AuthResponse();
            response.sessionToken = session.token;
            response.userId = user.id;
            response.username = user.username;
            response.displayName = user.displayName;
            response.expiresAt = session.expiresAt;
            send(ok(requestId, response));
        }

        private WireEnvelope ok(String requestId, Object payload) {
            return WireEnvelope.of(PacketTypes.OK, requestId, payload);
        }

        private <T> T parse(Object payload, Class<T> type) {
            if (payload == null) {
                return null;
            }
            if (!type.isInstance(payload)) {
                return null;
            }
            return type.cast(payload);
        }

        private void send(WireEnvelope envelope) {
            if (!connected) {
                return;
            }
            try {
                synchronized (writeLock) {
                    codec.write(socket.getOutputStream(), envelope);
                }
            } catch (Exception e) {
                connected = false;
            }
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }
    }
}


