package com.wifichat.ui;

import com.wifichat.config.AppConfig;
import com.wifichat.model.ChatMessage;
import com.wifichat.model.ChatScope;
import com.wifichat.model.PeerInfo;
import com.wifichat.network.ChatNode;
import com.wifichat.network.ChatNodeListener;
import com.wifichat.network.MessageFactory;
import com.wifichat.shared.util.ConversationKeys;
import com.wifichat.util.TextUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class MainFrame extends JFrame implements ChatNodeListener {
    private final AppConfig config;
    private final ChatNode node;
    private final MessageFactory messageFactory;
    private final Runnable onLogout;

    private final Map<String, DefaultListModel<ChatMessage>> conversationModels;
    private final Map<String, String> conversationTitles;
    private final Map<String, PeerInfo> peersById;
    private final Map<String, Set<String>> conversationMessageIds;
    private final Map<String, String> pmTargetByConversation;

    private final Set<String> joinedRooms;
    private final Set<String> discoveredRooms;

    private final DefaultListModel<String> roomListModel;
    private final JList<String> roomList;
    private final DefaultListModel<PeerInfo> peerListModel;
    private final JList<PeerInfo> peerList;

    private final JList<ChatMessage> messageList;
    private final JLabel titleLabel;
    private final JLabel statusLabel;
    private final JTextArea composer;
    private final JLabel replyLabel;
    private final JPanel replyPanel;

    private String currentConversationKey;
    private ChatMessage replyTarget;

    public MainFrame(AppConfig config, ChatNode node) {
        this(config, node, null);
    }

    public MainFrame(AppConfig config, ChatNode node, Runnable onLogout) {
        this.config = config;
        this.node = node;
        this.onLogout = onLogout;
        this.messageFactory = new MessageFactory(node.userId(), node.userName());

        this.conversationModels = new LinkedHashMap<>();
        this.conversationTitles = new LinkedHashMap<>();
        this.peersById = new LinkedHashMap<>();
        this.conversationMessageIds = new LinkedHashMap<>();
        this.pmTargetByConversation = new LinkedHashMap<>();

        this.joinedRooms = new LinkedHashSet<>();
        this.discoveredRooms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        this.roomListModel = new DefaultListModel<>();
        this.roomList = new JList<>(roomListModel);
        this.peerListModel = new DefaultListModel<>();
        this.peerList = new JList<>(peerListModel);

        this.messageList = new JList<>(new DefaultListModel<>());
        this.titleLabel = new JLabel("Pick a room or start a PM");
        this.statusLabel = new JLabel("Ready", SwingConstants.LEFT);
        this.composer = new JTextArea(3, 30);
        this.replyLabel = new JLabel();
        this.replyPanel = new JPanel(new BorderLayout());

        setupWindow();
        setupLayout();
        setupInteractions();

        joinRoom(config.defaultRoom(), true);

    }

    public void startNodeSafely() {
        Thread starter = new Thread(() -> {
            try {
                node.start();
                SwingUtilities.invokeLater(() -> {
                    setStatus("Connected to " + config.multicastGroup().getHostAddress() + ":" + config.multicastPort() +
                            (node.isHybrid() ? " | TCP " + config.serverHost() + ":" + config.serverPort() : ""));
                    preloadConversationFromServerAsync();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Cannot start network node: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Cannot start network node:\n" + e.getMessage(), "Network Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }, "chat-node-starter");
        starter.setDaemon(true);
        starter.start();
    }

    private void preloadConversationFromServerAsync() {
        if (!node.isHybrid()) {
            return;
        }
        Thread loader = new Thread(() -> {
            List<String> keys = node.listConversationKeys(50);
            SwingUtilities.invokeLater(() -> applyPreloadedConversations(keys));
        }, "conversation-preload");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyPreloadedConversations(List<String> keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>());
            conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
            if (ConversationKeys.isRoom(key)) {
                String room = key.substring("room:".length());
                joinRoom(room, false);
            }
        }
    }

    private void setupWindow() {
        setTitle("WiFi Multicast Chat - " + node.userName());
        setSize(1220, 780);
        setMinimumSize(new Dimension(1024, 680));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(AppTheme.WINDOW_BG);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                node.stop();
                dispose();
            }
        });
    }

    private void setupLayout() {
        JPanel sidebar = buildSidebar();
        JPanel center = buildCenterPane();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, center);
        splitPane.setDividerLocation(330);
        splitPane.setResizeWeight(0.28);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        splitPane.setDividerSize(8);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));
        panel.setBackground(AppTheme.SIDEBAR_BG);

        JPanel appCard = createCardPanel();
        appCard.setLayout(new BoxLayout(appCard, BoxLayout.Y_AXIS));
        JLabel appTitle = new JLabel("LAN Chat Hub");
        appTitle.setFont(AppTheme.heading(24));
        appTitle.setForeground(AppTheme.STRONG_TEXT);
        JLabel appSubtitle = new JLabel(node.isHybrid() ? "Hybrid mode (TCP + UDP)" : "UDP legacy mode");
        appSubtitle.setFont(AppTheme.body(Font.PLAIN, 12));
        appSubtitle.setForeground(AppTheme.SOFT_TEXT);
        appCard.add(appTitle);
        appCard.add(Box.createVerticalStrut(4));
        appCard.add(appSubtitle);
        if (onLogout != null) {
            appCard.add(Box.createVerticalStrut(10));
            JButton logoutButton = new JButton("Log Out");
            styleButton(logoutButton, new Color(196, 92, 82));
            logoutButton.addActionListener(e -> handleLogout());
            appCard.add(logoutButton);
        }

        JPanel roomsCard = createSectionCard("Rooms", "Create channel spaces", AppTheme.ROOM_ACCENT);
        JPanel roomButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        roomButtons.setOpaque(false);
        JButton createRoomButton = new JButton("Create");
        JButton joinRoomButton = new JButton("Join");
        styleButton(createRoomButton, AppTheme.PRIMARY_BUTTON);
        styleButton(joinRoomButton, AppTheme.NEUTRAL_BUTTON);
        roomButtons.add(createRoomButton);
        roomButtons.add(joinRoomButton);

        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setBackground(new Color(248, 251, 255));
        roomList.setFont(AppTheme.body(Font.PLAIN, 13));
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText("# " + value);
                label.setBorder(new EmptyBorder(6, 10, 6, 8));
                label.setFont(AppTheme.body(Font.BOLD, 13));
                label.setBackground(isSelected ? new Color(209, 228, 255) : new Color(248, 251, 255));
                label.setForeground(isSelected ? new Color(18, 79, 152) : new Color(49, 63, 81));
                return label;
            }
        });
        JScrollPane roomScroll = wrapScroll(roomList);
        roomScroll.setPreferredSize(new Dimension(280, 220));

        JPanel roomContent = new JPanel(new BorderLayout(0, 8));
        roomContent.setOpaque(false);
        roomContent.add(roomButtons, BorderLayout.NORTH);
        roomContent.add(roomScroll, BorderLayout.CENTER);
        roomsCard.add(roomContent, BorderLayout.CENTER);

        JPanel usersCard = createSectionCard("Online Users", "Double-click to open PM", AppTheme.USER_ACCENT);
        JButton pmButton = new JButton("Private Chat");
        styleButton(pmButton, AppTheme.SUCCESS_BUTTON);

        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerList.setBackground(new Color(247, 255, 252));
        peerList.setFont(AppTheme.body(Font.PLAIN, 13));
        peerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PeerInfo peer) {
                    label.setText("o " + peer.displayName() + "  [" + peer.address().getHostAddress() + "]");
                }
                label.setBorder(new EmptyBorder(6, 10, 6, 8));
                label.setFont(AppTheme.body(Font.PLAIN, 13));
                label.setBackground(isSelected ? new Color(205, 242, 226) : new Color(247, 255, 252));
                label.setForeground(isSelected ? new Color(18, 106, 76) : new Color(42, 70, 58));
                return label;
            }
        });
        JScrollPane userScroll = wrapScroll(peerList);
        userScroll.setPreferredSize(new Dimension(280, 240));

        JPanel userContent = new JPanel(new BorderLayout(0, 8));
        userContent.setOpaque(false);
        userContent.add(pmButton, BorderLayout.NORTH);
        userContent.add(userScroll, BorderLayout.CENTER);
        usersCard.add(userContent, BorderLayout.CENTER);

        createRoomButton.addActionListener(e -> promptJoinRoom());
        joinRoomButton.addActionListener(e -> promptJoinRoom());
        pmButton.addActionListener(e -> openSelectedPrivateChat());

        panel.add(appCard);
        panel.add(Box.createVerticalStrut(10));
        panel.add(roomsCard);
        panel.add(Box.createVerticalStrut(10));
        panel.add(usersCard);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JPanel buildCenterPane() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(10, 0, 10, 10));
        panel.setBackground(AppTheme.WINDOW_BG);

        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint paint = new GradientPaint(
                        0, 0, new Color(255, 198, 138),
                        getWidth(), getHeight(), new Color(236, 128, 108)
                );
                g2d.setPaint(paint);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2d.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(14, 16, 14, 16));

        titleLabel.setFont(AppTheme.heading(22));
        titleLabel.setForeground(new Color(35, 28, 22));
        statusLabel.setFont(AppTheme.body(Font.BOLD, 12));
        statusLabel.setForeground(new Color(62, 56, 52));
        header.add(titleLabel, BorderLayout.NORTH);
        header.add(statusLabel, BorderLayout.SOUTH);

        JPanel messageCard = createCardPanel();
        messageCard.setLayout(new BorderLayout());

        messageList.setCellRenderer(new MessageCellRenderer(node.userId()));
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageList.setBackground(new Color(252, 250, 255));
        JScrollPane messageScroll = wrapScroll(messageList);
        messageCard.add(messageScroll, BorderLayout.CENTER);

        JPanel composerPanel = buildComposerPanel();

        panel.add(header, BorderLayout.NORTH);
        panel.add(messageCard, BorderLayout.CENTER);
        panel.add(composerPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildComposerPanel() {
        JPanel panel = createCardPanel();
        panel.setLayout(new BorderLayout(0, 8));

        replyLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        replyLabel.setForeground(new Color(120, 74, 30));
        replyPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(233, 193, 157), 14, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        replyPanel.setBackground(new Color(255, 244, 231));

        JButton cancelReplyButton = new JButton("Cancel");
        styleButton(cancelReplyButton, AppTheme.NEUTRAL_BUTTON);
        cancelReplyButton.addActionListener(e -> clearReplyContext());

        replyPanel.add(replyLabel, BorderLayout.CENTER);
        replyPanel.add(cancelReplyButton, BorderLayout.EAST);
        replyPanel.setVisible(false);

        composer.setFont(AppTheme.body(Font.PLAIN, 14));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setBackground(new Color(249, 252, 255));
        composer.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(204, 215, 230), 14, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(BorderFactory.createEmptyBorder());

        JButton replyButton = new JButton("Reply Selected");
        JButton sendButton = new JButton("Send Message");
        styleButton(replyButton, AppTheme.NEUTRAL_BUTTON);
        styleButton(sendButton, AppTheme.PRIMARY_BUTTON);

        replyButton.addActionListener(e -> activateReplySelected());
        sendButton.addActionListener(e -> sendCurrentMessage());

        composer.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ctrl ENTER"), "send-message");
        composer.getActionMap().put("send-message", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendCurrentMessage();
            }
        });

        JPanel buttonRow = new JPanel(new GridLayout(2, 1, 8, 8));
        buttonRow.setOpaque(false);
        buttonRow.add(replyButton);
        buttonRow.add(sendButton);

        panel.add(replyPanel, BorderLayout.NORTH);
        panel.add(composerScroll, BorderLayout.CENTER);
        panel.add(buttonRow, BorderLayout.EAST);

        return panel;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(212, 221, 232), 18, 1),
                new EmptyBorder(10, 12, 10, 12)
        ));
        return panel;
    }

    private JPanel createSectionCard(String title, String subtitle, Color accent) {
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.heading(18));
        titleLabel.setForeground(accent);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        subtitleLabel.setForeground(AppTheme.SOFT_TEXT);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitleLabel);

        card.add(header, BorderLayout.NORTH);
        return card;
    }

    private void styleButton(JButton button, Color background) {
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFont(AppTheme.body(Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(background.darker(), 14, 1),
                new EmptyBorder(7, 12, 7, 12)
        ));
    }

    private JScrollPane wrapScroll(JList<?> list) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(211, 220, 232), 14, 1),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        scroll.getViewport().setBackground(list.getBackground());
        return scroll;
    }

    private void setupInteractions() {
        roomList.addListSelectionListener(this::handleRoomSelection);

        peerList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedPrivateChat();
                }
            }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem replyMenu = new JMenuItem("Reply message");
        replyMenu.addActionListener(e -> activateReplySelected());
        popup.add(replyMenu);

        messageList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int index = messageList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    messageList.setSelectedIndex(index);
                    popup.show(messageList, e.getX(), e.getY());
                }
            }
        });
    }

    private void handleRoomSelection(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        String selected = roomList.getSelectedValue();
        if (selected != null) {
            openRoom(selected);
        }
    }

    private void promptJoinRoom() {
        List<String> known = new ArrayList<>(discoveredRooms);
        String suggestion = known.isEmpty() ? config.defaultRoom() : known.get(0);
        String knownRooms = known.isEmpty() ? "(none yet)" : String.join(", ", known);

        String roomName = JOptionPane.showInputDialog(
                this,
                "Enter room name.\nKnown rooms: " + knownRooms,
                suggestion
        );

        roomName = TextUtils.normalizeRoom(roomName);
        if (roomName == null) {
            return;
        }

        joinRoom(roomName, true);
        node.announceRoom(roomName);
    }

    private void joinRoom(String roomName, boolean focusRoom) {
        String normalized = TextUtils.normalizeRoom(roomName);
        if (normalized == null) {
            return;
        }

        discoveredRooms.add(normalized);
        if (joinedRooms.add(normalized)) {
            roomListModel.addElement(normalized);
        }

        String key = roomKey(normalized);
        conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>());
        conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
        conversationTitles.putIfAbsent(key, "#" + normalized);

        if (focusRoom) {
            roomList.setSelectedValue(normalized, true);
            openRoom(normalized);
        }
    }

    private void openRoom(String roomName) {
        String key = roomKey(roomName);
        currentConversationKey = key;
        titleLabel.setText(conversationTitles.getOrDefault(key, "#" + roomName));
        messageList.setModel(conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>()));
        conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
        loadHistoryForConversationAsync(key);
        scrollToBottom();
        clearReplyContext();
    }

    private void openSelectedPrivateChat() {
        PeerInfo selected = peerList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a user first.", "Private Chat", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openPrivateConversation(selected.userId(), selected.displayName());
    }

    private void openPrivateConversation(String peerId, String displayName) {
        String key = pmKey(peerId);
        conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>());
        conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
        conversationTitles.put(key, "@" + displayName);
        pmTargetByConversation.put(key, peerId);
        currentConversationKey = key;
        titleLabel.setText("@" + displayName);
        messageList.setModel(conversationModels.get(key));
        loadHistoryForConversationAsync(key);
        clearReplyContext();
        scrollToBottom();
    }

    private void loadHistoryForConversationAsync(String conversationKey) {
        if (!node.isHybrid() || conversationKey == null) {
            return;
        }
        Thread historyLoader = new Thread(() -> {
            node.subscribeConversation(conversationKey);
            List<ChatMessage> history = node.fetchHistory(conversationKey, 150);
            SwingUtilities.invokeLater(() -> {
                for (ChatMessage message : history) {
                    appendMessage(conversationKey, message);
                }
            });
        }, "history-load-" + Math.abs(conversationKey.hashCode()));
        historyLoader.setDaemon(true);
        historyLoader.start();
    }

    private void sendCurrentMessage() {
        String content = TextUtils.sanitizeMessage(composer.getText());
        if (content == null) {
            return;
        }

        if (currentConversationKey == null) {
            JOptionPane.showMessageDialog(this, "Select a room or private chat first.", "No Conversation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentConversationKey.startsWith("room:")) {
            String roomName = currentConversationKey.substring("room:".length());
            ChatMessage message = messageFactory.group(roomName, content, replyTarget);
            if (!node.isHybrid()) {
                appendMessage(currentConversationKey, message);
            }
            node.sendGroupMessage(message);
        } else if (currentConversationKey.startsWith("pm:")) {
            String targetUserId = pmTargetByConversation.get(currentConversationKey);
            if (targetUserId == null) {
                targetUserId = extractOtherUserFromPmKey(currentConversationKey);
            }
            if (targetUserId == null) {
                JOptionPane.showMessageDialog(this, "Unable to resolve PM target user.", "Private Chat", JOptionPane.WARNING_MESSAGE);
                return;
            }
            PeerInfo peer = peersById.get(targetUserId);
            if (peer == null) {
                if (!node.isHybrid()) {
                    JOptionPane.showMessageDialog(this, "Selected user is currently offline.", "User Offline", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                peer = new PeerInfo(targetUserId, conversationTitles.getOrDefault(currentConversationKey, "Unknown").replace("@", ""),
                        java.net.InetAddress.getLoopbackAddress(), 0, System.currentTimeMillis());
            }

            ChatMessage message = messageFactory.privateMessage(peer, content, replyTarget);
            if (!node.isHybrid()) {
                appendMessage(currentConversationKey, message);
            }
            node.sendPrivateMessage(message, peer);
        }

        composer.setText("");
        clearReplyContext();
    }

    private String extractOtherUserFromPmKey(String key) {
        if (key == null || !key.startsWith("pm:")) {
            return null;
        }
        String[] parts = key.split(":");
        if (parts.length != 3) {
            return null;
        }
        if (parts[1].equals(node.userId())) {
            return parts[2];
        }
        if (parts[2].equals(node.userId())) {
            return parts[1];
        }
        return parts[1];
    }

    private void activateReplySelected() {
        ChatMessage selected = messageList.getSelectedValue();
        if (selected == null) {
            setStatus("Select a message first to reply.");
            return;
        }
        replyTarget = selected;
        replyLabel.setText("Replying to: " + TextUtils.shortPreview(selected.senderName(), selected.content()));
        replyPanel.setVisible(true);
    }

    private void clearReplyContext() {
        replyTarget = null;
        replyLabel.setText("");
        replyPanel.setVisible(false);
    }

    private void appendMessage(String conversationKey, ChatMessage message) {
        if (conversationKey == null || message == null) {
            return;
        }

        Set<String> ids = conversationMessageIds.computeIfAbsent(conversationKey, ignored -> new HashSet<>());
        if (message.messageId() != null && !ids.add(message.messageId())) {
            return;
        }

        DefaultListModel<ChatMessage> model = conversationModels.computeIfAbsent(conversationKey, ignored -> new DefaultListModel<>());
        model.addElement(message);
        if (model.size() > 600) {
            ChatMessage removed = model.getElementAt(0);
            if (removed.messageId() != null) {
                ids.remove(removed.messageId());
            }
            model.remove(0);
        }
        if (conversationKey.equals(currentConversationKey)) {
            scrollToBottom();
        }
    }

    private void scrollToBottom() {
        ListModel<ChatMessage> model = messageList.getModel();
        int last = model.getSize() - 1;
        if (last >= 0) {
            messageList.ensureIndexIsVisible(last);
        }
    }

    private String roomKey(String roomName) {
        return ConversationKeys.room(roomName);
    }

    private String pmKey(String peerId) {
        return ConversationKeys.pm(node.userId(), peerId);
    }

    private void handleLogout() {
        if (onLogout == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Log out and return to login screen?",
                "Log Out",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setStatus("Logging out...");
        onLogout.run();
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    @Override
    public void onPeerListUpdated(List<PeerInfo> peers) {
        SwingUtilities.invokeLater(() -> {
            peersById.clear();
            peerListModel.clear();
            for (PeerInfo peer : peers) {
                peersById.put(peer.userId(), peer);
                peerListModel.addElement(peer);
            }
            setStatus("Online peers: " + peers.size());
        });
    }

    @Override
    public void onRoomDiscovered(String roomName) {
        SwingUtilities.invokeLater(() -> {
            String normalized = TextUtils.normalizeRoom(roomName);
            if (normalized == null) {
                return;
            }
            if (discoveredRooms.add(normalized)) {
                setStatus("Discovered room: " + normalized);
            }
        });
    }

    @Override
    public void onGroupMessage(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            String room = TextUtils.normalizeRoom(message.roomName());
            if (room == null) {
                return;
            }
            discoveredRooms.add(room);

            String key = roomKey(room);
            conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>());
            conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
            conversationTitles.putIfAbsent(key, "#" + room);
            appendMessage(key, message);

            if (joinedRooms.contains(room)) {
                if (key.equals(currentConversationKey)) {
                    setStatus("New message in #" + room);
                }
            } else {
                setStatus("Message arrived in room #" + room + " (join room to chat there)");
            }
        });
    }

    @Override
    public void onPrivateMessage(ChatMessage message) {
        SwingUtilities.invokeLater(() -> {
            if (message.scope() != ChatScope.PRIVATE) {
                return;
            }
            if (message.targetUserId() != null && !message.targetUserId().equals(node.userId()) && !message.senderId().equals(node.userId())) {
                return;
            }

            String otherUserId = message.senderId().equals(node.userId()) ? message.targetUserId() : message.senderId();
            String otherName = message.senderId().equals(node.userId())
                    ? (message.targetUserName() == null ? "Unknown" : message.targetUserName())
                    : (message.senderName() == null ? "Unknown" : message.senderName());

            if (otherUserId == null) {
                return;
            }

            String key = pmKey(otherUserId);
            conversationModels.computeIfAbsent(key, ignored -> new DefaultListModel<>());
            conversationMessageIds.computeIfAbsent(key, ignored -> new HashSet<>());
            pmTargetByConversation.put(key, otherUserId);
            conversationTitles.put(key, "@" + otherName);
            appendMessage(key, message);
            setStatus("Private message from " + otherName);
        });
    }

    @Override
    public void onSystemNotice(String message) {
        SwingUtilities.invokeLater(() -> setStatus(message));
    }
}









