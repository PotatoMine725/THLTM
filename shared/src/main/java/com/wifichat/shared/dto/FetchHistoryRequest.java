package com.wifichat.shared.dto;

public class FetchHistoryRequest implements java.io.Serializable {
    public String sessionToken;
    public String conversationKey;
    public int limit;

    public FetchHistoryRequest() {
    }
}

