package com.wifichat.shared.dto;

public class AdminSetUserMutedRequest implements java.io.Serializable {
    public String sessionToken;
    public String userId;
    public boolean muted;

    public AdminSetUserMutedRequest() {
    }
}
