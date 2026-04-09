package com.wifichat.server.model;

public class AuthContext {
    public String userId;
    public String username;
    public String displayName;
    public String role;
    public String sessionToken;
    public long expiresAt;

    public AuthContext() {
    }
}
