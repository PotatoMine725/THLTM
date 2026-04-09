package com.wifichat.admin.config;

public record AdminConfig(String serverHost, int serverPort, String profileName) {
    public static AdminConfig fromArgs(String[] args) {
        String host = "127.0.0.1";
        int port = 61000;
        String profile = "admin";

        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if ("--server-host".equals(key) && i + 1 < args.length) {
                host = args[++i];
            } else if ("--server-port".equals(key) && i + 1 < args.length) {
                port = parseInt(args[++i], port);
            } else if ("--profile".equals(key) && i + 1 < args.length) {
                profile = args[++i];
            }
        }

        return new AdminConfig(host, port, profile);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
