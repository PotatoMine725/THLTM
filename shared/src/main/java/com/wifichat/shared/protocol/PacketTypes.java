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

    public static final String ADMIN_LIST_CONVERSATIONS = "admin_list_conversations";
    public static final String ADMIN_LIST_USERS = "admin_list_users";
    public static final String ADMIN_DELETE_MESSAGE = "admin_delete_message";
    public static final String ADMIN_DELETE_CONVERSATION = "admin_delete_conversation";
    public static final String ADMIN_SET_USER_MUTED = "admin_set_user_muted";

    public static final String OK = "ok";
    public static final String ERROR = "error";
    public static final String MESSAGE_SAVED = "message_saved";
    public static final String HISTORY_BATCH = "history_batch";
    public static final String MESSAGE_EVENT = "message_event";
    public static final String CONVERSATIONS = "conversations";
    public static final String ADMIN_USERS = "admin_users";

    private PacketTypes() {
    }
}
