package com.wifichat.model;

import java.io.Serial;
import java.io.Serializable;

public class ChatMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final ChatScope scope;
    private final String senderId;
    private final String senderName;
    private final String roomName;
    private final String targetUserId;
    private final String targetUserName;
    private final long timestamp;
    private final String content;
    private final String replyToMessageId;
    private final String replyPreview;

    public ChatMessage(
            String messageId,
            ChatScope scope,
            String senderId,
            String senderName,
            String roomName,
            String targetUserId,
            String targetUserName,
            long timestamp,
            String content,
            String replyToMessageId,
            String replyPreview
    ) {
        this.messageId = messageId;
        this.scope = scope;
        this.senderId = senderId;
        this.senderName = senderName;
        this.roomName = roomName;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.timestamp = timestamp;
        this.content = content;
        this.replyToMessageId = replyToMessageId;
        this.replyPreview = replyPreview;
    }

    public String messageId() {
        return messageId;
    }

    public ChatScope scope() {
        return scope;
    }

    public String senderId() {
        return senderId;
    }

    public String senderName() {
        return senderName;
    }

    public String roomName() {
        return roomName;
    }

    public String targetUserId() {
        return targetUserId;
    }

    public String targetUserName() {
        return targetUserName;
    }

    public long timestamp() {
        return timestamp;
    }

    public String content() {
        return content;
    }

    public String replyToMessageId() {
        return replyToMessageId;
    }

    public String replyPreview() {
        return replyPreview;
    }
}

