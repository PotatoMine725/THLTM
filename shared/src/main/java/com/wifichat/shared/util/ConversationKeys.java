package com.wifichat.shared.util;

public final class ConversationKeys {
    private ConversationKeys() {
    }

    public static String room(String roomName) {
        return "room:" + roomName.trim().toLowerCase();
    }

    public static String pm(String userId1, String userId2) {
        if (userId1.compareTo(userId2) <= 0) {
            return "pm:" + userId1 + ":" + userId2;
        }
        return "pm:" + userId2 + ":" + userId1;
    }

    public static boolean isRoom(String conversationKey) {
        return conversationKey != null && conversationKey.startsWith("room:");
    }

    public static boolean isPm(String conversationKey) {
        return conversationKey != null && conversationKey.startsWith("pm:");
    }
}
