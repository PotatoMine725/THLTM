package com.wifichat.util;

public final class TextUtils {
    private TextUtils() {
    }

    public static String normalizeRoom(String roomName) {
        if (roomName == null) {
            return null;
        }
        String value = roomName.trim();
        return value.isBlank() ? null : value;
    }

    public static String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String value = message.trim();
        return value.isBlank() ? null : value;
    }

    public static String shortPreview(String sender, String content) {
        String safeSender = sender == null ? "Unknown" : sender;
        String safeContent = content == null ? "" : content.trim();
        if (safeContent.length() > 64) {
            safeContent = safeContent.substring(0, 64) + "...";
        }
        return safeSender + ": " + safeContent;
    }

    public static String htmlEscape(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
