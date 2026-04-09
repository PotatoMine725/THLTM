package com.wifichat.server.db;

import com.wifichat.server.model.AuthContext;
import com.wifichat.server.model.SessionInfo;
import com.wifichat.server.model.UserAccount;
import com.wifichat.shared.dto.AdminUserInfo;
import com.wifichat.shared.dto.MessageRecord;
import com.wifichat.shared.util.ConversationKeys;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatRepository {
    private static final long SESSION_TTL_MILLIS = 1000L * 60 * 60 * 24 * 7;
    private static final long SESSION_TOUCH_INTERVAL_MILLIS = 10_000L;

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final String jdbcUrl;

    public ChatRepository(String dbPath) {
        this.jdbcUrl = "jdbc:sqlite:" + dbPath;
    }

    public void init() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY,
                        username TEXT NOT NULL UNIQUE,
                        display_name TEXT NOT NULL,
                        role TEXT NOT NULL DEFAULT 'USER',
                        password_hash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sessions (
                        token TEXT PRIMARY KEY,
                        user_id TEXT NOT NULL,
                        expires_at INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        last_seen_at INTEGER NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id TEXT PRIMARY KEY,
                        conversation_key TEXT NOT NULL,
                        sender_user_id TEXT NOT NULL,
                        sender_name TEXT NOT NULL,
                        scope TEXT NOT NULL,
                        room_name TEXT,
                        target_user_id TEXT,
                        target_user_name TEXT,
                        content TEXT NOT NULL,
                        reply_to_message_id TEXT,
                        reply_preview TEXT,
                        created_at INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS conversation_members (
                        conversation_key TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        PRIMARY KEY (conversation_key, user_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS muted_users (
                        user_id TEXT PRIMARY KEY,
                        muted_at INTEGER NOT NULL,
                        FOREIGN KEY(user_id) REFERENCES users(id)
                    )
                    """);

            ensureRoleColumn(statement);
        }
    }

    private void ensureRoleColumn(Statement statement) throws SQLException {
        boolean hasRole = false;
        try (ResultSet rs = statement.executeQuery("PRAGMA table_info(users)")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if ("role".equalsIgnoreCase(name)) {
                    hasRole = true;
                    break;
                }
            }
        }
        if (!hasRole) {
            statement.executeUpdate("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'");
        }
    }

    public synchronized UserAccount createUser(String username, String displayName, String passwordHash, String salt) throws SQLException {
        return createUserWithRole(username, displayName, passwordHash, salt, ROLE_USER);
    }

    public synchronized UserAccount createUserWithRole(String username, String displayName, String passwordHash, String salt, String role) throws SQLException {
        if (findUserByUsername(username) != null) {
            return null;
        }

        UserAccount account = new UserAccount();
        account.id = UUID.randomUUID().toString();
        account.username = username;
        account.displayName = displayName;
        account.role = normalizeRole(role);
        account.passwordHash = passwordHash;
        account.salt = salt;
        account.createdAt = System.currentTimeMillis();

        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO users (id, username, display_name, role, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, account.id);
            ps.setString(2, account.username);
            ps.setString(3, account.displayName);
            ps.setString(4, account.role);
            ps.setString(5, account.passwordHash);
            ps.setString(6, account.salt);
            ps.setLong(7, account.createdAt);
            ps.executeUpdate();
        }

        return account;
    }

    public synchronized UserAccount findUserByUsername(String username) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "SELECT id, username, display_name, role, password_hash, salt, created_at FROM users WHERE username = ?"
        )) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapUser(rs);
            }
        }
    }

    public synchronized SessionInfo createSession(String userId) throws SQLException {
        long now = System.currentTimeMillis();
        SessionInfo session = new SessionInfo();
        session.token = UUID.randomUUID().toString();
        session.userId = userId;
        session.expiresAt = now + SESSION_TTL_MILLIS;

        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO sessions (token, user_id, expires_at, created_at, last_seen_at) VALUES (?, ?, ?, ?, ?)"
        )) {
            ps.setString(1, session.token);
            ps.setString(2, session.userId);
            ps.setLong(3, session.expiresAt);
            ps.setLong(4, now);
            ps.setLong(5, now);
            ps.executeUpdate();
        }

        return session;
    }

    public synchronized AuthContext authBySession(String token) throws SQLException {
        if (token == null || token.isBlank()) {
            return null;
        }

        String sql = """
                SELECT s.token, s.expires_at, s.last_seen_at,
                       u.id AS user_id, u.username, u.display_name, u.role
                FROM sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.token = ?
                """;
        long now = System.currentTimeMillis();
        AuthContext context = null;
        long expiresAt = 0L;
        long lastSeenAt = 0L;
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                expiresAt = rs.getLong("expires_at");
                lastSeenAt = rs.getLong("last_seen_at");
                context = new AuthContext();
                context.sessionToken = rs.getString("token");
                context.expiresAt = expiresAt;
                context.userId = rs.getString("user_id");
                context.username = rs.getString("username");
                context.displayName = rs.getString("display_name");
                context.role = normalizeRole(rs.getString("role"));
            }
        }

        if (context == null) {
            return null;
        }
        if (expiresAt < now) {
            deleteSession(token);
            return null;
        }
        if (now - lastSeenAt >= SESSION_TOUCH_INTERVAL_MILLIS) {
            touchSession(token, now);
        }
        return context;
    }

    public synchronized void deleteSession(String token) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement("DELETE FROM sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public synchronized void touchSession(String token) throws SQLException {
        touchSession(token, System.currentTimeMillis());
    }

    private synchronized void touchSession(String token, long timestamp) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "UPDATE sessions SET last_seen_at = ? WHERE token = ?"
        )) {
            ps.setLong(1, timestamp);
            ps.setString(2, token);
            ps.executeUpdate();
        }
    }

    public synchronized boolean isAdmin(String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "SELECT role FROM users WHERE id = ?"
        )) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                return ROLE_ADMIN.equalsIgnoreCase(normalizeRole(rs.getString("role")));
            }
        }
    }

    public synchronized boolean isUserMuted(String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM muted_users WHERE user_id = ?"
        )) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public synchronized void setUserMuted(String userId, boolean muted) throws SQLException {
        if (userId == null || userId.isBlank()) {
            return;
        }

        try (Connection connection = open()) {
            if (muted) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT OR REPLACE INTO muted_users (user_id, muted_at) VALUES (?, ?)"
                )) {
                    ps.setString(1, userId);
                    ps.setLong(2, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = connection.prepareStatement("DELETE FROM muted_users WHERE user_id = ?")) {
                    ps.setString(1, userId);
                    ps.executeUpdate();
                }
            }
        }
    }

    public synchronized List<AdminUserInfo> listAllUsers() throws SQLException {
        List<AdminUserInfo> users = new ArrayList<>();
        String sql = """
                SELECT u.id, u.username, u.display_name, u.role,
                       CASE WHEN m.user_id IS NULL THEN 0 ELSE 1 END AS muted
                FROM users u
                LEFT JOIN muted_users m ON m.user_id = u.id
                ORDER BY u.created_at ASC
                """;
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AdminUserInfo info = new AdminUserInfo();
                info.userId = rs.getString("id");
                info.username = rs.getString("username");
                info.displayName = rs.getString("display_name");
                info.role = normalizeRole(rs.getString("role"));
                info.muted = rs.getInt("muted") == 1;
                users.add(info);
            }
        }
        return users;
    }

    public synchronized void addConversationMember(String conversationKey, String userId) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO conversation_members (conversation_key, user_id) VALUES (?, ?)"
        )) {
            ps.setString(1, conversationKey);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public synchronized boolean isConversationMember(String conversationKey, String userId) throws SQLException {
        if (ConversationKeys.isRoom(conversationKey)) {
            return true;
        }
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM conversation_members WHERE conversation_key = ? AND user_id = ?"
        )) {
            ps.setString(1, conversationKey);
            ps.setString(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public synchronized void saveMessage(MessageRecord message) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(
                """
                        INSERT INTO messages (
                            id, conversation_key, sender_user_id, sender_name, scope, room_name,
                            target_user_id, target_user_name, content, reply_to_message_id, reply_preview, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """
        )) {
            ps.setString(1, message.messageId);
            ps.setString(2, message.conversationKey);
            ps.setString(3, message.senderUserId);
            ps.setString(4, message.senderName);
            ps.setString(5, message.scope);
            ps.setString(6, message.roomName);
            ps.setString(7, message.targetUserId);
            ps.setString(8, message.targetUserName);
            ps.setString(9, message.content);
            ps.setString(10, message.replyToMessageId);
            ps.setString(11, message.replyPreview);
            ps.setLong(12, message.timestamp);
            ps.executeUpdate();
        }
    }

    public synchronized void deleteMessage(String messageId) throws SQLException {
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement("DELETE FROM messages WHERE id = ?")) {
            ps.setString(1, messageId);
            ps.executeUpdate();
        }
    }

    public synchronized void deleteConversation(String conversationKey) throws SQLException {
        try (Connection connection = open()) {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM messages WHERE conversation_key = ?")) {
                ps.setString(1, conversationKey);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM conversation_members WHERE conversation_key = ?")) {
                ps.setString(1, conversationKey);
                ps.executeUpdate();
            }
        }
    }

    public synchronized List<MessageRecord> fetchHistory(String conversationKey, int limit) throws SQLException {
        List<MessageRecord> records = new ArrayList<>();
        String sql = """
                SELECT id, conversation_key, sender_user_id, sender_name, scope, room_name,
                       target_user_id, target_user_name, content, reply_to_message_id, reply_preview, created_at
                FROM messages
                WHERE conversation_key = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, conversationKey);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(mapMessage(rs));
                }
            }
        }

        records.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        return records;
    }

    public synchronized List<String> listConversationsForUser(String userId, int limit) throws SQLException {
        List<String> keys = new ArrayList<>();
        String sql = """
                SELECT conversation_key, MAX(created_at) AS latest
                FROM messages
                WHERE sender_user_id = ?
                   OR conversation_key IN (
                       SELECT conversation_key FROM conversation_members WHERE user_id = ?
                   )
                GROUP BY conversation_key
                ORDER BY latest DESC
                LIMIT ?
                """;
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    keys.add(rs.getString("conversation_key"));
                }
            }
        }
        return keys;
    }

    public synchronized List<String> listAllConversations(int limit) throws SQLException {
        List<String> keys = new ArrayList<>();
        String sql = """
                SELECT conversation_key, MAX(created_at) AS latest
                FROM messages
                GROUP BY conversation_key
                ORDER BY latest DESC
                LIMIT ?
                """;
        try (Connection connection = open(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    keys.add(rs.getString("conversation_key"));
                }
            }
        }
        return keys;
    }

    private UserAccount mapUser(ResultSet rs) throws SQLException {
        UserAccount user = new UserAccount();
        user.id = rs.getString("id");
        user.username = rs.getString("username");
        user.displayName = rs.getString("display_name");
        user.role = normalizeRole(rs.getString("role"));
        user.passwordHash = rs.getString("password_hash");
        user.salt = rs.getString("salt");
        user.createdAt = rs.getLong("created_at");
        return user;
    }

    private MessageRecord mapMessage(ResultSet rs) throws SQLException {
        MessageRecord message = new MessageRecord();
        message.messageId = rs.getString("id");
        message.conversationKey = rs.getString("conversation_key");
        message.senderUserId = rs.getString("sender_user_id");
        message.senderName = rs.getString("sender_name");
        message.scope = rs.getString("scope");
        message.roomName = rs.getString("room_name");
        message.targetUserId = rs.getString("target_user_id");
        message.targetUserName = rs.getString("target_user_name");
        message.content = rs.getString("content");
        message.replyToMessageId = rs.getString("reply_to_message_id");
        message.replyPreview = rs.getString("reply_preview");
        message.timestamp = rs.getLong("created_at");
        return message;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        String normalized = role.trim().toUpperCase();
        return ROLE_ADMIN.equals(normalized) ? ROLE_ADMIN : ROLE_USER;
    }

    private Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }
}
