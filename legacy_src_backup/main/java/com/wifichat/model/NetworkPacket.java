package com.wifichat.model;

import java.io.Serial;
import java.io.Serializable;

public class NetworkPacket implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final PacketType type;
    private final String senderId;
    private final String senderName;
    private final long timestamp;
    private final PeerAnnouncement peerAnnouncement;
    private final String roomName;
    private final ChatMessage chatMessage;

    public NetworkPacket(
            PacketType type,
            String senderId,
            String senderName,
            long timestamp,
            PeerAnnouncement peerAnnouncement,
            String roomName,
            ChatMessage chatMessage
    ) {
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = timestamp;
        this.peerAnnouncement = peerAnnouncement;
        this.roomName = roomName;
        this.chatMessage = chatMessage;
    }

    public static NetworkPacket presence(PacketType type, String senderId, String senderName, int privatePort) {
        return new NetworkPacket(type, senderId, senderName, System.currentTimeMillis(), new PeerAnnouncement(privatePort), null, null);
    }

    public static NetworkPacket roomAnnounce(String senderId, String senderName, String roomName) {
        return new NetworkPacket(PacketType.ROOM_ANNOUNCE, senderId, senderName, System.currentTimeMillis(), null, roomName, null);
    }

    public static NetworkPacket groupMessage(String senderId, String senderName, ChatMessage message) {
        return new NetworkPacket(PacketType.GROUP_CHAT, senderId, senderName, System.currentTimeMillis(), null, message.roomName(), message);
    }

    public static NetworkPacket privateMessage(String senderId, String senderName, ChatMessage message) {
        return new NetworkPacket(PacketType.PRIVATE_CHAT, senderId, senderName, System.currentTimeMillis(), null, null, message);
    }

    public PacketType type() {
        return type;
    }

    public String senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public long timestamp() {
        return timestamp;
    }

    public PeerAnnouncement peerAnnouncement() {
        return peerAnnouncement;
    }

    public String roomName() {
        return roomName;
    }

    public ChatMessage chatMessage() {
        return chatMessage;
    }
}

