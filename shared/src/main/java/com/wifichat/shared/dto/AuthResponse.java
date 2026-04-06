package com.wifichat.shared.dto;

public class AuthResponse implements java.io.Serializable {
    public String sessionToken;
    public String userId;
    public String username;
    public String displayName;
    public long expiresAt;

    public AuthResponse() {
    }
}

