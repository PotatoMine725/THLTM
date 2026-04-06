package com.wifichat.server.model;

public class UserAccount {
    public String id;
    public String username;
    public String displayName;
    public String passwordHash;
    public String salt;
    public long createdAt;

    public UserAccount() {
    }
}
