package com.wifichat.model;

import java.net.InetAddress;

public class PeerInfo {
    private final String userId;
    private final String displayName;
    private final InetAddress address;
    private final int privatePort;
    private volatile long lastSeenMillis;

    public PeerInfo(String userId, String displayName, InetAddress address, int privatePort, long lastSeenMillis) {
        this.userId = userId;
        this.displayName = displayName;
        this.address = address;
        this.privatePort = privatePort;
        this.lastSeenMillis = lastSeenMillis;
    }

    public String userId() {
        return userId;
    }

    public String displayName() {
        return displayName;
    }

    public InetAddress address() {
        return address;
    }

    public int privatePort() {
        return privatePort;
    }

    public long lastSeenMillis() {
        return lastSeenMillis;
    }

    public void touch(long millis) {
        this.lastSeenMillis = millis;
    }

    @Override
    public String toString() {
        return displayName + " (" + address.getHostAddress() + ":" + privatePort + ")";
    }
}

