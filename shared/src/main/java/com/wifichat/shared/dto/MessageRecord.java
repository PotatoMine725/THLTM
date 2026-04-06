package com.wifichat.shared.dto;

public class MessageRecord implements java.io.Serializable {
    public String messageId;
    public String conversationKey;
    public String scope;
    public String senderUserId;
    public String senderName;
    public String roomName;
    public String targetUserId;
    public String targetUserName;
    public long timestamp;
    public String content;
    public String replyToMessageId;
    public String replyPreview;

    public MessageRecord() {
    }
}

