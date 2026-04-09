package com.wifichat.admin.auth;

import java.io.Serializable;

public class AuthSession implements Serializable {
    public String sessionToken;
    public String userId;
    public String username;
    public String displayName;
    public String role;
    public long expiresAt;

    public boolean isExpired() {
        return expiresAt > 0 && System.currentTimeMillis() >= expiresAt;
    }
}
