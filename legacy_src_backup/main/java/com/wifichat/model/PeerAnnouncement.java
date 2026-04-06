package com.wifichat.model;

import java.io.Serial;
import java.io.Serializable;

public class PeerAnnouncement implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int privatePort;

    public PeerAnnouncement(int privatePort) {
        this.privatePort = privatePort;
    }

    public int privatePort() {
        return privatePort;
    }
}

