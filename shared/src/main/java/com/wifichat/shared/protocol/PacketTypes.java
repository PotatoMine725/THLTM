package com.wifichat.shared.protocol;

public final class PacketTypes {
    public static final String REGISTER = "register";
    public static final String LOGIN = "login";
    public static final String RESUME_SESSION = "resume_session";
    public static final String SEND_MESSAGE = "send_message";
    public static final String FETCH_HISTORY = "fetch_history";
    public static final String SUBSCRIBE_CONVERSATION = "subscribe_conversation";
    public static final String HEARTBEAT = "heartbeat";
    public static final String LIST_CONVERSATIONS = "list_conversations";

    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String MESSAGE_SAVED = "message_saved";
    public static final String HISTORY_BATCH = "history_batch";
    public static final String MESSAGE_EVENT = "message_event";
    public static final String CONVERSATIONS = "conversations";

    private PacketTypes() {
    }
}
