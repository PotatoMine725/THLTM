package com.wifichat.admin.ui;

import com.wifichat.admin.auth.AuthSession;
import com.wifichat.admin.tcp.AdminTcpClient;
import com.wifichat.admin.tcp.AdminTcpException;
import com.wifichat.shared.dto.AdminUserInfo;
import com.wifichat.shared.dto.MessageRecord;
import com.wifichat.shared.ui.AppTheme;
import com.wifichat.shared.ui.RoundedBorder;
import com.wifichat.shared.ui.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminFrame extends JFrame {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AdminTcpClient tcpClient;
    private final AuthSession session;
    private final Runnable onLogout;

    private final DefaultListModel<String> conversationModel;
    private final DefaultListModel<AdminUserInfo> usersModel;
    private final DefaultListModel<MessageRecord> messageModel;

    private final JList<String> conversations;
    private final JList<AdminUserInfo> users;
    private final JList<MessageRecord> messages;
    private final JLabel statusLabel;
    private final JLabel titleLabel;

    private String selectedConversation;

    public AdminFrame(AdminTcpClient tcpClient, AuthSession session, Runnable onLogout) {
        this.tcpClient = tcpClient;
        this.session = session;
        this.onLogout = onLogout;

        this.conversationModel = new DefaultListModel<>();
        this.usersModel = new DefaultListModel<>();
        this.messageModel = new DefaultListModel<>();

        this.conversations = new JList<>(conversationModel);
        this.users = new JList<>(usersModel);
        this.messages = new JList<>(messageModel);
        this.statusLabel = new JLabel("Ready", SwingConstants.LEFT);
        this.titleLabel = new JLabel("Admin Chat Manager");

        setupWindow();
        setupLayout();
        setupInteractions();

        refreshAll();
    }

    private void setupWindow() {
        setTitle("WiFi Chat Admin - " + session.displayName);
        setSize(1240, 820);
        setMinimumSize(new Dimension(1000, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(AppTheme.WINDOW_BG);
    }

    private void setupLayout() {
        JPanel left = buildSidebar();
        JPanel right = buildMainPane();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(380);
        split.setResizeWeight(0.32);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setDividerSize(3);
        split.setContinuousLayout(true);
        split.setBackground(AppTheme.WINDOW_BG);

        setLayout(new BorderLayout());
        add(split, BorderLayout.CENTER);
    }

    // ── Sidebar ──

    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(AppTheme.SIDEBAR_BG);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        stack.add(buildProfileCard());
        stack.add(buildDivider());
        stack.add(buildConversationSection());
        stack.add(buildDivider());
        stack.add(buildUsersSection());

        JScrollPane sidebarScroll = new JScrollPane(stack);
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setOpaque(false);
        sidebarScroll.getViewport().setOpaque(false);
        sidebarScroll.getViewport().setBackground(AppTheme.SIDEBAR_BG);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(14);

        panel.add(sidebarScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildProfileCard() {
        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        profile.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Avatar
        JLabel avatar = new JLabel(UIHelper.initials(session.displayName), SwingConstants.CENTER);
        avatar.setFont(AppTheme.body(Font.BOLD, 13));
        avatar.setForeground(AppTheme.TEXT_PRIMARY);
        avatar.setOpaque(true);
        avatar.setBackground(AppTheme.ADMIN_ACCENT);
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 17, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Name + role
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(session.displayName);
        name.setFont(AppTheme.body(Font.BOLD, 14));
        name.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel role = new JLabel("Administrator");
        role.setFont(AppTheme.body(Font.PLAIN, 11));
        role.setForeground(AppTheme.ADMIN_ACCENT);

        text.add(name);
        text.add(Box.createVerticalStrut(1));
        text.add(role);

        profile.add(avatar, BorderLayout.WEST);
        profile.add(text, BorderLayout.CENTER);

        JButton logoutButton = new JButton("Log out");
        UIHelper.styleButton(logoutButton, AppTheme.DANGER_BUTTON, AppTheme.DANGER_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 5, 9);
        logoutButton.setFont(AppTheme.body(Font.BOLD, 11));
        logoutButton.addActionListener(e -> onLogout.run());
        profile.add(logoutButton, BorderLayout.EAST);

        return profile;
    }

    private JPanel buildConversationSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        section.add(createSectionHeader("CONVERSATIONS", this::refreshConversations), BorderLayout.NORTH);

        conversations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversations.setBackground(AppTheme.SIDEBAR_ITEM_BG);
        conversations.setForeground(AppTheme.TEXT_SECONDARY);
        conversations.setFont(AppTheme.body(Font.BOLD, 13));
        conversations.setCellRenderer(new ConversationRenderer());

        section.add(wrapSidebarList(conversations, 220), BorderLayout.CENTER);

        JButton deleteConversationBtn = new JButton("Delete Conversation");
        UIHelper.styleButton(deleteConversationBtn, AppTheme.DANGER_BUTTON, AppTheme.DANGER_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 6, 12);
        deleteConversationBtn.addActionListener(e -> deleteSelectedConversation());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        btnPanel.add(deleteConversationBtn, BorderLayout.CENTER);

        section.add(btnPanel, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildUsersSection() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        section.add(createSectionHeader("USERS", this::refreshUsers), BorderLayout.NORTH);

        users.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        users.setBackground(AppTheme.SIDEBAR_ITEM_BG);
        users.setForeground(AppTheme.TEXT_SECONDARY);
        users.setFont(AppTheme.body(Font.PLAIN, 13));
        users.setCellRenderer(new UserRenderer());

        section.add(wrapSidebarList(users, 280), BorderLayout.CENTER);

        JButton muteToggleBtn = new JButton("Mute / Unmute User");
        UIHelper.styleButton(muteToggleBtn, AppTheme.NEUTRAL_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 10, 6, 12);
        muteToggleBtn.addActionListener(e -> toggleMuteSelectedUser());

        JPanel btnPanel = new JPanel(new BorderLayout());
        btnPanel.setOpaque(false);
        btnPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        btnPanel.add(muteToggleBtn, BorderLayout.CENTER);

        section.add(btnPanel, BorderLayout.SOUTH);
        return section;
    }

    private JPanel buildDivider() {
        JPanel divider = new JPanel(new BorderLayout());
        divider.setOpaque(false);
        divider.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel line = new JPanel();
        line.setBackground(AppTheme.SIDEBAR_DIVIDER);
        line.setPreferredSize(new Dimension(10, 1));
        divider.add(line, BorderLayout.CENTER);
        return divider;
    }

    private JPanel createSectionHeader(String title, Runnable refreshAction) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(1, 0, 1, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.body(Font.BOLD, 12));
        titleLabel.setForeground(AppTheme.SIDEBAR_HEADER_TEXT);
        header.add(titleLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JButton refreshBtn = new JButton("R");
        refreshBtn.setFont(AppTheme.body(Font.BOLD, 14));
        refreshBtn.setPreferredSize(new Dimension(22, 22));
        UIHelper.styleButton(refreshBtn, AppTheme.GHOST_BUTTON, AppTheme.SIDEBAR_DIVIDER, AppTheme.SIDEBAR_HEADER_TEXT, 11, 2, 2);
        refreshBtn.addActionListener(e -> refreshAction.run());
        right.add(refreshBtn);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JScrollPane wrapSidebarList(JList<?> list, int preferredHeight) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(AppTheme.SIDEBAR_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setPreferredSize(new Dimension(340, preferredHeight));
        return scroll;
    }

    // ── Main pane ──

    private JPanel buildMainPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(AppTheme.WINDOW_BG);

        panel.add(buildConversationHeader(), BorderLayout.NORTH);
        panel.add(buildMessagePanel(), BorderLayout.CENTER);
        panel.add(buildStatusBar(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildConversationHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(AppTheme.WINDOW_BG);
        header.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        titleLabel.setFont(AppTheme.heading(24));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("Select a conversation to view messages");
        subtitleLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitleLabel);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);

        JButton refreshHistoryBtn = new JButton("Refresh");
        JButton deleteMessageBtn = new JButton("Delete Msg");
        UIHelper.styleButton(refreshHistoryBtn, AppTheme.GHOST_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 12, 7, 14);
        UIHelper.styleButton(deleteMessageBtn, AppTheme.DANGER_BUTTON, AppTheme.DANGER_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 12, 7, 14);

        refreshHistoryBtn.addActionListener(e -> refreshHistory());
        deleteMessageBtn.addActionListener(e -> deleteSelectedMessage());

        actions.add(refreshHistoryBtn);
        actions.add(deleteMessageBtn);

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMessagePanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(AppTheme.WINDOW_BG);
        card.setBorder(new EmptyBorder(0, 12, 8, 12));

        messages.setCellRenderer(new MessageRenderer());
        messages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messages.setBackground(AppTheme.PANEL_BG);

        JScrollPane messageScroll = new JScrollPane(messages);
        messageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messageScroll.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_SUBTLE, 12, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        messageScroll.setBackground(AppTheme.PANEL_BG);
        messageScroll.getViewport().setBackground(AppTheme.PANEL_BG);
        messageScroll.getVerticalScrollBar().setUnitIncrement(14);

        card.add(messageScroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 14, 8, 14));

        statusLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        statusLabel.setForeground(AppTheme.TEXT_MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ── Interactions ──

    private void setupInteractions() {
        conversations.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedConversation = conversations.getSelectedValue();
                refreshHistory();
            }
        });
    }

    public void refreshAll() {
        refreshConversations();
        refreshUsers();
    }

    private void refreshConversations() {
        runAsync(() -> {
            List<String> keys = tcpClient.listAllConversations(session.sessionToken, 300);
            SwingUtilities.invokeLater(() -> {
                conversationModel.clear();
                for (String key : keys) {
                    conversationModel.addElement(key);
                }
                setStatus("Loaded conversations: " + keys.size());
            });
        });
    }

    private void refreshUsers() {
        runAsync(() -> {
            List<AdminUserInfo> allUsers = tcpClient.listUsers(session.sessionToken);
            SwingUtilities.invokeLater(() -> {
                usersModel.clear();
                for (AdminUserInfo info : allUsers) {
                    usersModel.addElement(info);
                }
                setStatus("Loaded users: " + allUsers.size());
            });
        });
    }

    private void refreshHistory() {
        if (selectedConversation == null || selectedConversation.isBlank()) {
            return;
        }

        runAsync(() -> {
            List<MessageRecord> history = tcpClient.fetchHistory(session.sessionToken, selectedConversation, 500);
            SwingUtilities.invokeLater(() -> {
                messageModel.clear();
                for (MessageRecord record : history) {
                    messageModel.addElement(record);
                }
                titleLabel.setText("Conversation: " + selectedConversation);
                setStatus("Messages: " + history.size());
            });
        });
    }

    private void deleteSelectedMessage() {
        MessageRecord selected = messages.getSelectedValue();
        if (selected == null || selected.messageId == null) {
            JOptionPane.showMessageDialog(this, "Select a message first.", "Delete message", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected message?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        runAsync(() -> {
            tcpClient.deleteMessage(session.sessionToken, selected.messageId);
            SwingUtilities.invokeLater(() -> {
                setStatus("Deleted message " + selected.messageId);
                refreshHistory();
            });
        });
    }

    private void deleteSelectedConversation() {
        String key = conversations.getSelectedValue();
        if (key == null || key.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a conversation first.", "Delete conversation", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Delete conversation " + key + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        runAsync(() -> {
            tcpClient.deleteConversation(session.sessionToken, key);
            SwingUtilities.invokeLater(() -> {
                selectedConversation = null;
                messageModel.clear();
                setStatus("Deleted conversation " + key);
                refreshConversations();
            });
        });
    }

    private void toggleMuteSelectedUser() {
        AdminUserInfo selected = users.getSelectedValue();
        if (selected == null || selected.userId == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.", "Mute user", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if ("ADMIN".equalsIgnoreCase(selected.role)) {
            JOptionPane.showMessageDialog(this, "Cannot mute an ADMIN account.", "Mute user", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean targetMuted = !selected.muted;
        runAsync(() -> {
            tcpClient.setUserMuted(session.sessionToken, selected.userId, targetMuted);
            SwingUtilities.invokeLater(() -> {
                setStatus((targetMuted ? "Muted " : "Unmuted ") + selected.displayName);
                refreshUsers();
            });
        });
    }

    private void runAsync(Task task) {
        Thread thread = new Thread(() -> {
            try {
                task.run();
            } catch (AdminTcpException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, e.getMessage(), "Admin error", JOptionPane.ERROR_MESSAGE));
            }
        }, "admin-ui-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void setStatus(String text) {
        statusLabel.setText(text == null ? "" : text);
    }

    private interface Task {
        void run() throws AdminTcpException;
    }

    // ── Custom Renderers ──

    /**
     * Conversation list renderer: shows # prefix for rooms, @ for PMs,
     * with styled Discord-like backgrounds.
     */
    private static final class ConversationRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel prefixLabel;
        private final JLabel nameLabel;

        private ConversationRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(8, 8, 8, 8));

            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

            prefixLabel = new JLabel("#");
            prefixLabel.setFont(AppTheme.body(Font.BOLD, 14));
            prefixLabel.setForeground(AppTheme.SIDEBAR_CHANNEL_HASH);

            nameLabel = new JLabel();
            nameLabel.setFont(AppTheme.body(Font.BOLD, 13));

            left.add(prefixLabel);
            left.add(Box.createHorizontalStrut(8));
            left.add(nameLabel);

            add(left, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                nameLabel.setText("");
                setBackground(AppTheme.SIDEBAR_ITEM_BG);
                return this;
            }

            boolean isRoom = value.startsWith("room:");
            boolean isPm = value.startsWith("pm:");
            String display;

            if (isRoom) {
                display = value.substring("room:".length());
                prefixLabel.setText("#");
                prefixLabel.setForeground(AppTheme.SIDEBAR_CHANNEL_HASH);
            } else if (isPm) {
                display = value.substring("pm:".length());
                prefixLabel.setText("@");
                prefixLabel.setForeground(AppTheme.DM_DOT_BLUE);
            } else {
                display = value;
                prefixLabel.setText(".");
                prefixLabel.setForeground(AppTheme.TEXT_MUTED);
            }

            nameLabel.setText(display);
            nameLabel.setForeground(isSelected ? AppTheme.SIDEBAR_SELECTED_TEXT : AppTheme.TEXT_SECONDARY);
            setBackground(isSelected ? AppTheme.SIDEBAR_ITEM_ACTIVE_BG : AppTheme.SIDEBAR_ITEM_BG);
            return this;
        }
    }

    /**
     * User list renderer: shows avatar initials, name, role badge, mute indicator.
     */
    private static final class UserRenderer extends JPanel implements ListCellRenderer<AdminUserInfo> {
        private final JLabel avatarLabel;
        private final JLabel nameLabel;
        private final JLabel roleLabel;
        private final JLabel muteLabel;

        private static final Color[] AVATAR_COLORS = {
                AppTheme.PEER_AVATAR_BLUE,
                AppTheme.PEER_AVATAR_GREEN,
                AppTheme.PEER_AVATAR_GOLD,
                AppTheme.PEER_AVATAR_PURPLE
        };

        private UserRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(8, 8, 8, 8));

            avatarLabel = new JLabel("", SwingConstants.CENTER);
            avatarLabel.setFont(AppTheme.body(Font.BOLD, 11));
            avatarLabel.setForeground(AppTheme.TEXT_PRIMARY);
            avatarLabel.setOpaque(true);
            avatarLabel.setPreferredSize(new Dimension(28, 28));
            avatarLabel.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppTheme.BORDER_STRONG, 14, 1),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            topRow.setOpaque(false);

            nameLabel = new JLabel();
            nameLabel.setFont(AppTheme.body(Font.BOLD, 13));

            roleLabel = new JLabel();
            roleLabel.setFont(AppTheme.body(Font.BOLD, 10));
            roleLabel.setOpaque(true);
            roleLabel.setBorder(new EmptyBorder(1, 6, 1, 6));

            topRow.add(nameLabel);
            topRow.add(roleLabel);

            muteLabel = new JLabel();
            muteLabel.setFont(AppTheme.body(Font.PLAIN, 11));

            textPanel.add(topRow);
            textPanel.add(Box.createVerticalStrut(1));
            textPanel.add(muteLabel);

            add(avatarLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AdminUserInfo> list, AdminUserInfo value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                nameLabel.setText("");
                roleLabel.setVisible(false);
                muteLabel.setVisible(false);
                setBackground(AppTheme.SIDEBAR_ITEM_BG);
                return this;
            }

            // Avatar
            avatarLabel.setText(UIHelper.initials(value.displayName));
            int colorIndex = Math.abs((value.userId == null ? 0 : value.userId.hashCode())) % AVATAR_COLORS.length;
            avatarLabel.setBackground(AVATAR_COLORS[colorIndex]);

            // Name
            nameLabel.setText(value.displayName + " (@" + value.username + ")");
            nameLabel.setForeground(isSelected ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);

            // Role badge
            boolean isAdmin = "ADMIN".equalsIgnoreCase(value.role);
            roleLabel.setText(value.role);
            roleLabel.setForeground(AppTheme.TEXT_PRIMARY);
            roleLabel.setBackground(isAdmin ? AppTheme.ADMIN_ACCENT : AppTheme.NEUTRAL_BUTTON);
            roleLabel.setVisible(true);

            // Mute indicator
            if (value.muted) {
                muteLabel.setText("[MUTED] Messages blocked");
                muteLabel.setForeground(AppTheme.DANGER_BUTTON);
                muteLabel.setVisible(true);
            } else {
                muteLabel.setText("Active");
                muteLabel.setForeground(AppTheme.TEXT_MUTED);
                muteLabel.setVisible(true);
            }

            // Row background
            if (value.muted && !isSelected) {
                setBackground(AppTheme.MUTED_USER_BG);
            } else {
                setBackground(isSelected ? AppTheme.SIDEBAR_ITEM_ACTIVE_BG : AppTheme.SIDEBAR_ITEM_BG);
            }
            return this;
        }
    }

    /**
     * Message list renderer: shows timestamp, sender name, and content
     * with styled colors matching the client's Discord-inspired design.
     */
    private static final class MessageRenderer extends JPanel implements ListCellRenderer<MessageRecord> {
        private final JLabel contentLabel;

        private MessageRenderer() {
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(6, 10, 6, 10));

            contentLabel = new JLabel();
            contentLabel.setFont(AppTheme.body(Font.PLAIN, 14));

            add(contentLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends MessageRecord> list, MessageRecord value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                contentLabel.setText("");
                setBackground(AppTheme.PANEL_BG);
                return this;
            }

            String time = TIME_FMT.format(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(value.timestamp), ZoneId.systemDefault()));
            String sender = value.senderName != null ? value.senderName : "Unknown";
            String escaped = escapeHtml(value.content != null ? value.content : "");
            contentLabel.setText(
                    "<html><span style='color:" + UIHelper.colorToHex(AppTheme.TEXT_MUTED) + "'>[" + time + "]</span> "
                            + "<span style='color:" + UIHelper.colorToHex(AppTheme.TEXT_SECONDARY) + "'>" + escapeHtml(sender) + ":</span> "
                            + "<span style='color:" + UIHelper.colorToHex(AppTheme.TEXT_PRIMARY) + "'>" + escaped + "</span></html>"
            );

            setToolTipText("messageId=" + value.messageId);

            if (isSelected) {
                setBackground(AppTheme.SIDEBAR_BG);
            } else {
                setBackground(AppTheme.PANEL_BG);
            }
            return this;
        }

        private String escapeHtml(String text) {
            return text.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }
}

