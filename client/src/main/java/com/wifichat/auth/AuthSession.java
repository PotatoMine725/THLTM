package com.wifichat.auth;

public class AuthSession implements java.io.Serializable {
    public String sessionToken;
    public String userId;
    public String username;
    public String displayName;
    public long expiresAt;

    public AuthSession() {
    }

    public boolean isExpired() {
        return expiresAt > 0 && expiresAt < System.currentTimeMillis();
    }
}
