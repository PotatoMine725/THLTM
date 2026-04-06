package com.wifichat.network;

import com.wifichat.config.AppConfig;
import com.wifichat.model.ChatMessage;
import com.wifichat.model.NetworkPacket;
import com.wifichat.model.PacketType;
import com.wifichat.model.PeerAnnouncement;
import com.wifichat.model.PeerInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatNode {
    private static final int MAX_PACKET_SIZE = 65507;

    private final AppConfig config;
    private final String userId;
    private final String userName;
    private final PacketCodec codec;
    private final Map<String, PeerInfo> peers;
    private final AtomicBoolean running;

    private volatile ChatNodeListener listener;
    private volatile boolean started;

    private MulticastSocket multicastSocket;
    private DatagramSocket privateSocket;
    private NetworkInterface selectedInterface;

    private ExecutorService receiverPool;
    private ScheduledExecutorService scheduler;

    public ChatNode(AppConfig config, String userName) {
        this.config = Objects.requireNonNull(config);
        this.userName = Objects.requireNonNull(userName);
        this.userId = UUID.randomUUID().toString();
        this.codec = new PacketCodec();
        this.peers = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(false);
    }

    public void setListener(ChatNodeListener listener) {
        this.listener = listener;
    }

    public String userId() {
        return userId;
    }

    public String userName() {
        return userName;
    }

    public synchronized void start() throws IOException {
        if (started) {
            return;
        }
        started = true;
        running.set(true);

        selectedInterface = NetworkInterfaceSelector.choose(config.interfaceName());
        multicastSocket = createMulticastSocket();
        privateSocket = createPrivateSocket();

        receiverPool = Executors.newFixedThreadPool(2);
        scheduler = Executors.newScheduledThreadPool(2);

        receiverPool.submit(this::listenMulticast);
        receiverPool.submit(this::listenPrivate);

        scheduler.scheduleAtFixedRate(this::safeHeartbeat, 0, config.heartbeatSeconds(), TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::safePeerPrune, config.heartbeatSeconds(), config.heartbeatSeconds(), TimeUnit.SECONDS);

        sendHello();
        announceRoom(config.defaultRoom());
    }

    private MulticastSocket createMulticastSocket() throws IOException {
        MulticastSocket socket = new MulticastSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(config.multicastPort()));
        socket.setTimeToLive(16);
        socket.setSoTimeout(1200);

        if (selectedInterface != null) {
            socket.setNetworkInterface(selectedInterface);
            socket.joinGroup(new InetSocketAddress(config.multicastGroup(), config.multicastPort()), selectedInterface);
        } else {
            socket.joinGroup(config.multicastGroup());
        }
        return socket;
    }

    private DatagramSocket createPrivateSocket() throws SocketException {
        if (config.privatePort() != null) {
            return new DatagramSocket(config.privatePort());
        }
        return new DatagramSocket(0);
    }

    public synchronized void stop() {
        if (!started) {
            return;
        }
        started = false;

        try {
            sendGoodbye();
        } catch (Exception ignored) {
        }

        running.set(false);

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        if (receiverPool != null) {
            receiverPool.shutdownNow();
            receiverPool = null;
        }

        if (multicastSocket != null && !multicastSocket.isClosed()) {
            try {
                if (selectedInterface != null) {
                    multicastSocket.leaveGroup(new InetSocketAddress(config.multicastGroup(), config.multicastPort()), selectedInterface);
                } else {
                    multicastSocket.leaveGroup(config.multicastGroup());
                }
            } catch (Exception ignored) {
            }
            multicastSocket.close();
            multicastSocket = null;
        }

        if (privateSocket != null && !privateSocket.isClosed()) {
            privateSocket.close();
            privateSocket = null;
        }

        peers.clear();
        notifyPeers();
    }

    public void announceRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return;
        }
        NetworkPacket packet = NetworkPacket.roomAnnounce(userId, userName, roomName.trim());
        sendMulticast(packet);
    }

    public void sendGroupMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        NetworkPacket packet = NetworkPacket.groupMessage(userId, userName, message);
        sendMulticast(packet);
    }

    public void sendPrivateMessage(ChatMessage message, PeerInfo recipient) {
        if (message == null || recipient == null) {
            return;
        }
        NetworkPacket packet = NetworkPacket.privateMessage(userId, userName, message);
        sendUnicast(packet, recipient.address(), recipient.privatePort());
    }

    private void listenMulticast() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (running.get()) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                multicastSocket.receive(datagram);
                NetworkPacket packet = codec.decode(datagram.getData(), datagram.getLength());
                handlePacket(packet, datagram.getAddress());
            } catch (SocketTimeoutException ignored) {
            } catch (SocketException e) {
                if (running.get()) {
                    notice("Multicast socket closed unexpectedly: " + e.getMessage());
                }
                return;
            } catch (Exception e) {
                notice("Failed to process multicast packet: " + e.getMessage());
            }
        }
    }

    private void listenPrivate() {
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        while (running.get()) {
            DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
            try {
                privateSocket.receive(datagram);
                NetworkPacket packet = codec.decode(datagram.getData(), datagram.getLength());
                handlePacket(packet, datagram.getAddress());
            } catch (SocketException e) {
                if (running.get()) {
                    notice("Private socket closed unexpectedly: " + e.getMessage());
                }
                return;
            } catch (Exception e) {
                notice("Failed to process private packet: " + e.getMessage());
            }
        }
    }

    private void handlePacket(NetworkPacket packet, InetAddress sourceAddress) {
        if (packet == null || packet.senderId() == null) {
            return;
        }
        if (packet.senderId().equals(userId)) {
            return;
        }

        switch (packet.type()) {
            case HELLO, HEARTBEAT -> handlePresence(packet, sourceAddress);
            case GOODBYE -> removePeer(packet.senderId());
            case ROOM_ANNOUNCE -> {
                if (packet.roomName() != null) {
                    ChatNodeListener current = listener;
                    if (current != null) {
                        current.onRoomDiscovered(packet.roomName());
                    }
                }
            }
            case GROUP_CHAT -> {
                if (packet.chatMessage() != null) {
                    ChatNodeListener current = listener;
                    if (current != null) {
                        current.onGroupMessage(packet.chatMessage());
                    }
                }
            }
            case PRIVATE_CHAT -> {
                if (packet.chatMessage() != null) {
                    ChatNodeListener current = listener;
                    if (current != null) {
                        current.onPrivateMessage(packet.chatMessage());
                    }
                }
            }
        }
    }

    private void handlePresence(NetworkPacket packet, InetAddress sourceAddress) {
        PeerAnnouncement announcement = packet.peerAnnouncement();
        if (announcement == null) {
            return;
        }

        peers.compute(packet.senderId(), (peerId, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null) {
                return new PeerInfo(packet.senderId(), packet.senderName(), sourceAddress, announcement.privatePort(), now);
            }
            existing.touch(now);
            return new PeerInfo(packet.senderId(), packet.senderName(), sourceAddress, announcement.privatePort(), existing.lastSeenMillis());
        });
        notifyPeers();

        if (packet.type() == PacketType.HELLO) {
            sendHeartbeat();
        }
    }

    private void removePeer(String peerId) {
        if (peerId == null) {
            return;
        }
        if (peers.remove(peerId) != null) {
            notifyPeers();
        }
    }

    private void safeHeartbeat() {
        try {
            sendHeartbeat();
        } catch (Exception e) {
            notice("Heartbeat send failed: " + e.getMessage());
        }
    }

    private void safePeerPrune() {
        try {
            prunePeers();
        } catch (Exception e) {
            notice("Peer cleanup failed: " + e.getMessage());
        }
    }

    private void sendHello() {
        NetworkPacket packet = NetworkPacket.presence(PacketType.HELLO, userId, userName, privateSocket.getLocalPort());
        sendMulticast(packet);
    }

    private void sendHeartbeat() {
        if (!running.get()) {
            return;
        }
        NetworkPacket packet = NetworkPacket.presence(PacketType.HEARTBEAT, userId, userName, privateSocket.getLocalPort());
        sendMulticast(packet);
    }

    private void sendGoodbye() {
        if (privateSocket == null) {
            return;
        }
        NetworkPacket packet = NetworkPacket.presence(PacketType.GOODBYE, userId, userName, privateSocket.getLocalPort());
        sendMulticast(packet);
    }

    private void sendMulticast(NetworkPacket packet) {
        if (multicastSocket == null || multicastSocket.isClosed()) {
            return;
        }
        try {
            byte[] payload = codec.encode(packet);
            DatagramPacket datagram = new DatagramPacket(payload, payload.length, config.multicastGroup(), config.multicastPort());
            multicastSocket.send(datagram);
        } catch (Exception e) {
            notice("Unable to send multicast packet: " + e.getMessage());
        }
    }

    private void sendUnicast(NetworkPacket packet, InetAddress targetAddress, int targetPort) {
        if (privateSocket == null || privateSocket.isClosed()) {
            return;
        }
        try {
            byte[] payload = codec.encode(packet);
            DatagramPacket datagram = new DatagramPacket(payload, payload.length, targetAddress, targetPort);
            privateSocket.send(datagram);
        } catch (Exception e) {
            notice("Unable to send private message: " + e.getMessage());
        }
    }

    private void prunePeers() {
        long now = System.currentTimeMillis();
        long maxIdleMillis = TimeUnit.SECONDS.toMillis(config.peerTimeoutSeconds());
        boolean changed = false;

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, PeerInfo> entry : peers.entrySet()) {
            if ((now - entry.getValue().lastSeenMillis()) > maxIdleMillis) {
                toRemove.add(entry.getKey());
            }
        }

        for (String peerId : toRemove) {
            peers.remove(peerId);
            changed = true;
        }

        if (changed) {
            notifyPeers();
        }
    }

    private void notifyPeers() {
        ChatNodeListener current = listener;
        if (current == null) {
            return;
        }
        List<PeerInfo> snapshot = new ArrayList<>(peers.values());
        snapshot.sort(Comparator.comparing(PeerInfo::displayName, String.CASE_INSENSITIVE_ORDER));
        current.onPeerListUpdated(snapshot);
    }

    private void notice(String message) {
        ChatNodeListener current = listener;
        if (current != null) {
            current.onSystemNotice(message);
        }
    }
}

