package com.wifichat.ui;

/**
 * Represents a direct-message contact in the sidebar DM list.
 */
public final class DirectMessageEntry {
    final String peerId;
    String displayName;

    public DirectMessageEntry(String peerId, String displayName) {
        this.peerId = peerId;
        this.displayName = displayName;
    }

    public String peerId() {
        return peerId;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
