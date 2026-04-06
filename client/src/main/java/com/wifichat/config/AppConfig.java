package com.wifichat.config;

import com.wifichat.shared.TransportMode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public record AppConfig(
        String userName,
        InetAddress multicastGroup,
        int multicastPort,
        String interfaceName,
        Integer privatePort,
        int heartbeatSeconds,
        int peerTimeoutSeconds,
        String defaultRoom,
        TransportMode transportMode,
        String serverHost,
        int serverPort,
        String profileName
) {
    public static final String DEFAULT_GROUP = "239.255.50.10";
    public static final int DEFAULT_PORT = 50000;
    public static final int DEFAULT_HEARTBEAT_SECONDS = 4;
    public static final int DEFAULT_PEER_TIMEOUT_SECONDS = 15;
    public static final String DEFAULT_ROOM = "General";
    public static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 61000;
    public static final String DEFAULT_PROFILE = "default";

    public static AppConfig fromArgs(String[] args) {
        Map<String, String> parsed = parseArgs(args);

        String group = parsed.getOrDefault("--group", DEFAULT_GROUP);
        int port = parseInt(parsed.get("--port"), DEFAULT_PORT);
        Integer privatePort = parseNullableInt(parsed.get("--private-port"));
        String user = parsed.get("--name");
        String iface = parsed.get("--iface");
        String defaultRoom = normalizeRoom(parsed.getOrDefault("--room", DEFAULT_ROOM));

        TransportMode mode = TransportMode.fromCli(parsed.getOrDefault("--mode", "hybrid"));
        String serverHost = parsed.getOrDefault("--server-host", DEFAULT_SERVER_HOST);
        int serverPort = parseInt(parsed.get("--server-port"), DEFAULT_SERVER_PORT);
        String profileName = normalizeProfile(parsed.getOrDefault("--profile", DEFAULT_PROFILE));

        try {
            InetAddress groupAddress = InetAddress.getByName(group);
            return new AppConfig(
                    user,
                    groupAddress,
                    port,
                    iface,
                    privatePort,
                    DEFAULT_HEARTBEAT_SECONDS,
                    DEFAULT_PEER_TIMEOUT_SECONDS,
                    defaultRoom,
                    mode,
                    serverHost,
                    serverPort,
                    profileName
            );
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid multicast group: " + group, e);
        }
    }

    private static String normalizeRoom(String roomName) {
        if (roomName == null || roomName.isBlank()) {
            return DEFAULT_ROOM;
        }
        return roomName.trim();
    }

    private static String normalizeProfile(String profileName) {
        if (profileName == null || profileName.isBlank()) {
            return DEFAULT_PROFILE;
        }
        String normalized = profileName.trim().toLowerCase();
        if (normalized.isBlank()) {
            return DEFAULT_PROFILE;
        }
        return normalized;
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Integer parseNullableInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String current = args[i];
            if (!current.startsWith("--")) {
                continue;
            }
            String value = "true";
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                value = args[i + 1];
                i++;
            }
            values.put(current, value);
        }
        return values;
    }
}
