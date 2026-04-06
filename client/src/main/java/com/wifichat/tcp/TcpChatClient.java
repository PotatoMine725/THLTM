package com.wifichat.tcp;

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
import com.wifichat.shared.protocol.PacketTypes;
import com.wifichat.shared.protocol.WireCodec;
import com.wifichat.shared.protocol.WireEnvelope;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public class TcpChatClient {
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    private final String host;
    private final int port;
    private final WireCodec codec;

    private final Map<String, CompletableFuture<WireEnvelope>> pending;
    private final List<TcpEventListener> listeners;

    private volatile Socket socket;
    private volatile Thread readerThread;
    private volatile boolean running;

    public TcpChatClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.codec = new WireCodec();
        this.pending = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public synchronized void connect() throws IOException {
        if (running) {
            return;
        }

        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            newSocket.setTcpNoDelay(true);
        } catch (IOException e) {
            try {
                newSocket.close();
            } catch (IOException ignored) {
            }
            throw new IOException("Cannot connect to " + host + ":" + port + " within " + CONNECT_TIMEOUT_MILLIS + "ms", e);
        }

        socket = newSocket;
        running = true;
        readerThread = new Thread(this::readLoop, "tcp-chat-reader");
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
            future.completeExceptionally(new TcpClientException("Connection closed"));
        }
        pending.clear();
    }

    public boolean isConnected() {
        Socket current = socket;
        return running && current != null && current.isConnected() && !current.isClosed();
    }

    public void addListener(TcpEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TcpEventListener listener) {
        listeners.remove(listener);
    }

    public AuthResponse register(String username, String password, String displayName) throws TcpClientException {
        RegisterRequest payload = new RegisterRequest();
        payload.username = username;
        payload.password = password;
        payload.displayName = displayName;
        WireEnvelope response = request(PacketTypes.REGISTER, payload);
        return expectPayload(response, AuthResponse.class);
    }

    public AuthResponse login(String username, String password) throws TcpClientException {
        LoginRequest payload = new LoginRequest();
        payload.username = username;
        payload.password = password;
        WireEnvelope response = request(PacketTypes.LOGIN, payload);
        return expectPayload(response, AuthResponse.class);
    }

    public AuthResponse resumeSession(String token) throws TcpClientException {
        ResumeSessionRequest payload = new ResumeSessionRequest();
        payload.sessionToken = token;
        WireEnvelope response = request(PacketTypes.RESUME_SESSION, payload);
        return expectPayload(response, AuthResponse.class);
    }

    public MessageRecord sendMessage(String sessionToken, MessageRecord message) throws TcpClientException {
        SendMessageRequest payload = new SendMessageRequest();
        payload.sessionToken = sessionToken;
        payload.message = message;

        WireEnvelope response = request(PacketTypes.SEND_MESSAGE, payload, Set.of(PacketTypes.MESSAGE_SAVED));
        MessagePayload savedPayload = expectPayload(response, MessagePayload.class);
        return savedPayload == null ? null : savedPayload.message;
    }

    public List<MessageRecord> fetchHistory(String sessionToken, String conversationKey, int limit) throws TcpClientException {
        FetchHistoryRequest payload = new FetchHistoryRequest();
        payload.sessionToken = sessionToken;
        payload.conversationKey = conversationKey;
        payload.limit = limit;

        WireEnvelope response = request(PacketTypes.FETCH_HISTORY, payload, Set.of(PacketTypes.HISTORY_BATCH));
        HistoryBatchResponse historyPayload = expectPayload(response, HistoryBatchResponse.class);
        return historyPayload == null ? List.of() : historyPayload.messages;
    }

    public void subscribeConversation(String sessionToken, String conversationKey) throws TcpClientException {
        SubscribeConversationRequest payload = new SubscribeConversationRequest();
        payload.sessionToken = sessionToken;
        payload.conversationKey = conversationKey;
        request(PacketTypes.SUBSCRIBE_CONVERSATION, payload, Set.of(PacketTypes.OK));
    }

    public void heartbeat(String sessionToken) throws TcpClientException {
        HeartbeatRequest payload = new HeartbeatRequest();
        payload.sessionToken = sessionToken;
        request(PacketTypes.HEARTBEAT, payload, Set.of(PacketTypes.OK));
    }

    public List<String> listConversations(String sessionToken, int limit) throws TcpClientException {
        ListConversationsRequest payload = new ListConversationsRequest();
        payload.sessionToken = sessionToken;
        payload.limit = limit;
        WireEnvelope response = request(PacketTypes.LIST_CONVERSATIONS, payload, Set.of(PacketTypes.CONVERSATIONS));
        ConversationsResponse conversationsPayload = expectPayload(response, ConversationsResponse.class);
        return conversationsPayload == null ? List.of() : conversationsPayload.conversationKeys;
    }

    private WireEnvelope request(String type, Object payload) throws TcpClientException {
        return request(type, payload, Set.of(PacketTypes.OK));
    }

    private WireEnvelope request(String type, Object payload, Set<String> acceptedTypes) throws TcpClientException {
        ensureConnected();

        String requestId = UUID.randomUUID().toString();
        CompletableFuture<WireEnvelope> future = new CompletableFuture<>();
        pending.put(requestId, future);

        WireEnvelope envelope = WireEnvelope.of(type, requestId, payload);

        try {
            synchronized (this) {
                codec.write(socket.getOutputStream(), envelope);
            }
            WireEnvelope response = future.get(12, TimeUnit.SECONDS);

            if (PacketTypes.ERROR.equals(response.type)) {
                throw new TcpClientException((response.errorCode == null ? "ERROR" : response.errorCode) + ": " + response.errorMessage);
            }
            if (!acceptedTypes.contains(response.type)) {
                throw new TcpClientException("Unexpected response type: " + response.type);
            }
            return response;
        } catch (TcpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new TcpClientException("TCP request failed: " + e.getMessage(), e);
        } finally {
            pending.remove(requestId);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T expectPayload(WireEnvelope envelope, Class<T> type) throws TcpClientException {
        if (envelope == null || envelope.payload == null) {
            return null;
        }
        if (!type.isInstance(envelope.payload)) {
            throw new TcpClientException("Unexpected payload type: " + envelope.payload.getClass().getName());
        }
        return (T) envelope.payload;
    }

    private void readLoop() {
        try {
            while (running) {
                WireEnvelope envelope = codec.read(socket.getInputStream());
                if (envelope.requestId != null) {
                    CompletableFuture<WireEnvelope> future = pending.remove(envelope.requestId);
                    if (future != null) {
                        future.complete(envelope);
                        continue;
                    }
                }

                if (PacketTypes.MESSAGE_EVENT.equals(envelope.type) && envelope.payload instanceof MessagePayload payload) {
                    if (payload.message != null) {
                        for (TcpEventListener listener : listeners) {
                            listener.onMessageEvent(payload.message);
                        }
                    }
                }
            }
        } catch (EOFException ignored) {
            notifyDisconnected("Server closed connection");
        } catch (Exception e) {
            notifyDisconnected("TCP read error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void notifyDisconnected(String reason) {
        for (TcpEventListener listener : listeners) {
            listener.onDisconnected(reason);
        }
    }

    private void ensureConnected() throws TcpClientException {
        if (!running || socket == null || socket.isClosed()) {
            throw new TcpClientException("TCP client is not connected");
        }
    }
}
