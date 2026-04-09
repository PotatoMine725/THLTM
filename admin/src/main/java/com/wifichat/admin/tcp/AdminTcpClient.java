package com.wifichat.admin.tcp;

import com.wifichat.shared.dto.AdminDeleteConversationRequest;
import com.wifichat.shared.dto.AdminDeleteMessageRequest;
import com.wifichat.shared.dto.AdminListConversationsRequest;
import com.wifichat.shared.dto.AdminListUsersRequest;
import com.wifichat.shared.dto.AdminSetUserMutedRequest;
import com.wifichat.shared.dto.AdminUserInfo;
import com.wifichat.shared.dto.AdminUsersResponse;
import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.shared.dto.ConversationsResponse;
import com.wifichat.shared.dto.FetchHistoryRequest;
import com.wifichat.shared.dto.HistoryBatchResponse;
import com.wifichat.shared.dto.LoginRequest;
import com.wifichat.shared.dto.MessageRecord;
import com.wifichat.shared.dto.ResumeSessionRequest;
import com.wifichat.shared.protocol.PacketTypes;
import com.wifichat.shared.protocol.WireCodec;
import com.wifichat.shared.protocol.WireEnvelope;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AdminTcpClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    private final String host;
    private final int port;
    private final WireCodec codec;
    private final ConcurrentHashMap<String, CompletableFuture<WireEnvelope>> pending;

    private volatile Socket socket;
    private volatile Thread readerThread;
    private volatile boolean running;

    public AdminTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.codec = new WireCodec();
        this.pending = new ConcurrentHashMap<>();
    }

    public synchronized void connect() throws IOException {
        if (running) {
            return;
        }

        Socket newSocket = new Socket();
        newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
        newSocket.setTcpNoDelay(true);

        socket = newSocket;
        running = true;
        readerThread = new Thread(this::readLoop, "admin-tcp-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void close() {
        running = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }

        for (CompletableFuture<WireEnvelope> future : pending.values()) {
            future.completeExceptionally(new AdminTcpException("Connection closed"));
        }
        pending.clear();
    }

    public AuthResponse login(String username, String password) throws AdminTcpException {
        LoginRequest payload = new LoginRequest();
        payload.username = username;
        payload.password = password;
        WireEnvelope response = request(PacketTypes.LOGIN, payload, Set.of(PacketTypes.OK));
        return expectPayload(response, AuthResponse.class);
    }

    public AuthResponse resumeSession(String token) throws AdminTcpException {
        ResumeSessionRequest payload = new ResumeSessionRequest();
        payload.sessionToken = token;
        WireEnvelope response = request(PacketTypes.RESUME_SESSION, payload, Set.of(PacketTypes.OK));
        return expectPayload(response, AuthResponse.class);
    }

    public List<String> listAllConversations(String sessionToken, int limit) throws AdminTcpException {
        AdminListConversationsRequest payload = new AdminListConversationsRequest();
        payload.sessionToken = sessionToken;
        payload.limit = limit;
        WireEnvelope response = request(PacketTypes.ADMIN_LIST_CONVERSATIONS, payload, Set.of(PacketTypes.CONVERSATIONS));
        ConversationsResponse body = expectPayload(response, ConversationsResponse.class);
        return body == null || body.conversationKeys == null ? List.of() : body.conversationKeys;
    }

    public List<MessageRecord> fetchHistory(String sessionToken, String conversationKey, int limit) throws AdminTcpException {
        FetchHistoryRequest payload = new FetchHistoryRequest();
        payload.sessionToken = sessionToken;
        payload.conversationKey = conversationKey;
        payload.limit = limit;
        WireEnvelope response = request(PacketTypes.FETCH_HISTORY, payload, Set.of(PacketTypes.HISTORY_BATCH));
        HistoryBatchResponse body = expectPayload(response, HistoryBatchResponse.class);
        return body == null || body.messages == null ? List.of() : body.messages;
    }

    public List<AdminUserInfo> listUsers(String sessionToken) throws AdminTcpException {
        AdminListUsersRequest payload = new AdminListUsersRequest();
        payload.sessionToken = sessionToken;
        WireEnvelope response = request(PacketTypes.ADMIN_LIST_USERS, payload, Set.of(PacketTypes.ADMIN_USERS));
        AdminUsersResponse body = expectPayload(response, AdminUsersResponse.class);
        return body == null || body.users == null ? List.of() : body.users;
    }

    public void deleteMessage(String sessionToken, String messageId) throws AdminTcpException {
        AdminDeleteMessageRequest payload = new AdminDeleteMessageRequest();
        payload.sessionToken = sessionToken;
        payload.messageId = messageId;
        request(PacketTypes.ADMIN_DELETE_MESSAGE, payload, Set.of(PacketTypes.OK));
    }

    public void deleteConversation(String sessionToken, String conversationKey) throws AdminTcpException {
        AdminDeleteConversationRequest payload = new AdminDeleteConversationRequest();
        payload.sessionToken = sessionToken;
        payload.conversationKey = conversationKey;
        request(PacketTypes.ADMIN_DELETE_CONVERSATION, payload, Set.of(PacketTypes.OK));
    }

    public void setUserMuted(String sessionToken, String userId, boolean muted) throws AdminTcpException {
        AdminSetUserMutedRequest payload = new AdminSetUserMutedRequest();
        payload.sessionToken = sessionToken;
        payload.userId = userId;
        payload.muted = muted;
        request(PacketTypes.ADMIN_SET_USER_MUTED, payload, Set.of(PacketTypes.OK));
    }

    private void readLoop() {
        try {
            while (running) {
                WireEnvelope envelope = codec.read(socket.getInputStream());
                if (envelope.requestId != null) {
                    CompletableFuture<WireEnvelope> future = pending.remove(envelope.requestId);
                    if (future != null) {
                        future.complete(envelope);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            close();
        }
    }

    private WireEnvelope request(String type, Object payload, Set<String> acceptedTypes) throws AdminTcpException {
        if (!running || socket == null || socket.isClosed()) {
            throw new AdminTcpException("TCP client is not connected");
        }

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<WireEnvelope> future = new CompletableFuture<>();
        pending.put(requestId, future);

        try {
            synchronized (this) {
                codec.write(socket.getOutputStream(), WireEnvelope.of(type, requestId, payload));
            }

            WireEnvelope response = future.get(12, TimeUnit.SECONDS);
            if (PacketTypes.ERROR.equals(response.type)) {
                throw new AdminTcpException((response.errorCode == null ? "ERROR" : response.errorCode) + ": " + response.errorMessage);
            }
            if (!acceptedTypes.contains(response.type)) {
                throw new AdminTcpException("Unexpected response type: " + response.type);
            }
            return response;
        } catch (AdminTcpException e) {
            throw e;
        } catch (Exception e) {
            throw new AdminTcpException("TCP request failed: " + e.getMessage(), e);
        } finally {
            pending.remove(requestId);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T expectPayload(WireEnvelope envelope, Class<T> type) throws AdminTcpException {
        if (envelope == null || envelope.payload == null) {
            return null;
        }
        if (!type.isInstance(envelope.payload)) {
            throw new AdminTcpException("Unexpected payload type: " + envelope.payload.getClass().getName());
        }
        return (T) envelope.payload;
    }
}
