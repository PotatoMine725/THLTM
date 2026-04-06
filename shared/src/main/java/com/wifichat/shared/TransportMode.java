package com.wifichat.shared;

public enum TransportMode {
    HYBRID,
    UDP_ONLY;

    public static TransportMode fromCli(String value) {
        if (value == null || value.isBlank()) {
            return HYBRID;
        }
        return switch (value.trim().toLowerCase()) {
            case "udp", "udp-only", "udponly" -> UDP_ONLY;
            default -> HYBRID;
        };
    }
}
