package com.wifichat.shared.dto;

public class SendMessageRequest implements java.io.Serializable {
    public String sessionToken;
    public MessageRecord message;

    public SendMessageRequest() {
    }
}

