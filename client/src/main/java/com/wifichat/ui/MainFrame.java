package com.wifichat.ui;

import com.wifichat.config.AppConfig;
import com.wifichat.model.ChatMessage;
import com.wifichat.model.ChatScope;
import com.wifichat.model.PeerInfo;
import com.wifichat.network.ChatNode;
import com.wifichat.network.ChatNodeListener;
import com.wifichat.network.MessageFactory;
import com.wifichat.shared.ui.UIHelper;
import com.wifichat.shared.util.ConversationKeys;
import com.wifichat.util.TextUtils;

import javax.swing.AbstractAction;
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
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
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
    private final Map<String, Integer> unreadByConversation;
    private final Map<String, DirectMessageEntry> dmEntriesByPeer;

    private final Set<String> joinedRooms;
    private final Set<String> discoveredRooms;

    private final DefaultListModel<String> roomListModel;
    private final JList<String> roomList;
    private final DefaultListModel<DirectMessageEntry> dmListModel;
    private final JList<DirectMessageEntry> dmList;
    private final DefaultListModel<PeerInfo> peerListModel;
    private final JList<PeerInfo> peerList;

    private final JList<ChatMessage> messageList;
    private final JLabel titleLabel;
    private final JLabel statusLabel;
    private final JTextArea composer;
    private final JLabel replyLabel;
    private final JPanel replyPanel;
    private final JLabel typingLabel;
    private final JPanel typingPanel;
    private final JLabel channelsUnreadLabel;
    private final JLabel directUnreadLabel;
    private final JLabel friendsUnreadLabel;

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
        this.unreadByConversation = new LinkedHashMap<>();
        this.dmEntriesByPeer = new LinkedHashMap<>();

        this.joinedRooms = new LinkedHashSet<>();
        this.discoveredRooms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        this.roomListModel = new DefaultListModel<>();
        this.roomList = new JList<>(roomListModel);
        this.dmListModel = new DefaultListModel<>();
        this.dmList = new JList<>(dmListModel);
        this.peerListModel = new DefaultListModel<>();
        this.peerList = new JList<>(peerListModel);

        this.messageList = new JList<>(new DefaultListModel<>());
        this.titleLabel = new JLabel("Pick a room or start a PM");
        this.statusLabel = new JLabel("Ready", SwingConstants.LEFT);
        this.composer = new JTextArea(3, 30);
        this.replyLabel = new JLabel();
        this.replyPanel = new JPanel(new BorderLayout(8, 0));
        this.typingLabel = new JLabel();
        this.typingPanel = new JPanel(new BorderLayout());
        
        this.channelsUnreadLabel = createUnreadHeaderBadge();
        this.directUnreadLabel = createUnreadHeaderBadge();
        this.friendsUnreadLabel = createUnreadHeaderBadge();

        setupWindow();
        setupLayout();
        setupInteractions();
        updateSidebarUnreadBadges();

        joinRoom(config.defaultRoom(), true);
    }

    public void startNodeSafely() {
        Thread starter = new Thread(() -> {
            try {
                node.start();
                SwingUtilities.invokeLater(() -> {
                    setStatus("Connected to " + config.multicastGroup().getHostAddress() + ":" + config.multicastPort()
                            + (node.isHybrid() ? " | TCP " + config.serverHost() + ":" + config.serverPort() : ""));
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
        setSize(1240, 820);
        setMinimumSize(new Dimension(1040, 700));
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
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.20);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(3);
        splitPane.setContinuousLayout(true);
        splitPane.setBackground(AppTheme.WINDOW_BG);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        return new SidebarPanel(
                node.userName(),
                node.isHybrid(),
                onLogout == null ? null : this::handleLogout,
                this::promptJoinRoom,
                this::openSelectedPrivateChat,
                roomList,
                dmList,
                peerList,
                channelsUnreadLabel,
                directUnreadLabel,
                friendsUnreadLabel,
                room -> unreadCount(roomKey(room)),
                this::directMessageDotColor,
                peerId -> unreadCount(pmKey(peerId)),
                this::peerAvatarColor,
                peerId -> unreadCount(pmKey(peerId))
        );
    }

    private JPanel buildCenterPane() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 0));
        panel.setBackground(AppTheme.WINDOW_BG);

        panel.add(buildConversationHeader(), BorderLayout.NORTH);
        panel.add(buildMessageCard(), BorderLayout.CENTER);
        panel.add(buildComposerPanel(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildConversationHeader() {
        JPanel header = createInnerPanel(14);
        header.setLayout(new BorderLayout(8, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        titleLabel.setFont(AppTheme.heading(30));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        statusLabel.setFont(AppTheme.body(Font.PLAIN, 13));
        statusLabel.setForeground(AppTheme.TEXT_SECONDARY);

        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(statusLabel);

        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);

        JButton historyButton = new JButton("History");
        JButton membersButton = new JButton("Members");
        UIHelper.styleButton(historyButton, AppTheme.GHOST_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 12, 7, 14);
        UIHelper.styleButton(membersButton, AppTheme.GHOST_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 12, 7, 14);

        historyButton.addActionListener(e -> setStatus("History panel ready (UI skeleton). Data source remains unchanged."));
        membersButton.addActionListener(e -> setStatus("Members panel ready (UI skeleton) - online peers: " + peerListModel.size()));

        actions.add(historyButton);
        actions.add(membersButton);

        header.add(titleBlock, BorderLayout.CENTER);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildMessageCard() {
        JPanel messageCard = createInnerPanel(14);
        messageCard.setLayout(new BorderLayout(0, 4));

        messageList.setCellRenderer(new MessageCellRenderer(node.userId()));
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        messageList.setBackground(AppTheme.PANEL_BG);

        JScrollPane messageScroll = wrapScroll(messageList);
        messageScroll.getViewport().setBackground(AppTheme.PANEL_BG);
        messageCard.add(messageScroll, BorderLayout.CENTER);

        typingLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        typingLabel.setForeground(AppTheme.TEXT_MUTED);
        typingLabel.setBorder(new EmptyBorder(4, 10, 3, 0));

        typingPanel.setOpaque(false);
        typingPanel.add(typingLabel, BorderLayout.WEST);
        typingPanel.setVisible(false);

        messageCard.add(typingPanel, BorderLayout.SOUTH);
        return messageCard;
    }

    private JPanel buildComposerPanel() {
        JPanel panel = createInnerPanel(14);
        panel.setLayout(new BorderLayout(0, 8));

        replyLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        replyLabel.setForeground(AppTheme.TEXT_SECONDARY);

        replyPanel.setOpaque(true);
        replyPanel.setBackground(AppTheme.REPLY_BG);
        replyPanel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 10, 1),
                new EmptyBorder(5, 8, 5, 8)
        ));

        JButton cancelReplyButton = new JButton("x");
        UIHelper.styleButton(cancelReplyButton, AppTheme.GHOST_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_SECONDARY, 10, 3, 9);
        cancelReplyButton.addActionListener(e -> clearReplyContext());

        replyPanel.add(replyLabel, BorderLayout.CENTER);
        replyPanel.add(cancelReplyButton, BorderLayout.EAST);
        replyPanel.setVisible(false);

        composer.setFont(AppTheme.body(Font.PLAIN, 15));
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setBackground(AppTheme.ITEM_BG);
        composer.setForeground(AppTheme.TEXT_PRIMARY);
        composer.setCaretColor(AppTheme.TEXT_PRIMARY);
        composer.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane composerScroll = new JScrollPane(composer);
        composerScroll.setBorder(BorderFactory.createEmptyBorder());
        composerScroll.getVerticalScrollBar().setUnitIncrement(14);
        composerScroll.putClientProperty("JComponent.roundRect", true);
        composerScroll.setBackground(AppTheme.WINDOW_BG);
        composerScroll.getViewport().setBackground(AppTheme.WINDOW_BG);



        JLabel hintLabel = new JLabel("Ctrl+Enter to send | Right-click a message to reply");
        hintLabel.setForeground(AppTheme.TEXT_MUTED);
        hintLabel.setFont(AppTheme.body(Font.PLAIN, 12));

        JPanel editorArea = new JPanel(new BorderLayout(0, 5));
        editorArea.setOpaque(false);
        editorArea.add(composerScroll, BorderLayout.CENTER);
        editorArea.add(hintLabel, BorderLayout.SOUTH);

        JButton replyButton = new JButton("Reply Selected");
        JButton sendButton = new JButton("Send");
        UIHelper.styleButton(replyButton, AppTheme.GHOST_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 10, 8, 12);
        UIHelper.styleButton(sendButton, AppTheme.PRIMARY_BUTTON, AppTheme.PRIMARY_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 8, 12);

        replyButton.addActionListener(e -> activateReplySelected());
        sendButton.addActionListener(e -> sendCurrentMessage());

        JPanel buttonColumn = new JPanel(new GridLayout(2, 1, 8, 8));
        buttonColumn.setOpaque(false);
        buttonColumn.setPreferredSize(new Dimension(148, 0));
        buttonColumn.add(replyButton);
        buttonColumn.add(sendButton);

        composer.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "send-message");
        composer.getActionMap().put("send-message", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendCurrentMessage();
            }
        });

        panel.add(replyPanel, BorderLayout.NORTH);
        panel.add(editorArea, BorderLayout.CENTER);
        panel.add(buttonColumn, BorderLayout.EAST);
        return panel;
    }



    private JPanel createInnerPanel(int radius) {
        return createInnerPanel(radius, AppTheme.WINDOW_BG);
    }

    private JPanel createInnerPanel(int radius, Color background) {
        JPanel panel = new JPanel();
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));
        return panel;
    }

    private JLabel createUnreadHeaderBadge() {
        JLabel badge = new JLabel("", SwingConstants.CENTER);
        badge.setFont(AppTheme.body(Font.BOLD, 10));
        badge.setOpaque(true);
        badge.setForeground(AppTheme.BADGE_TEXT);
        badge.setBackground(AppTheme.BADGE_BG);
        badge.setBorder(BorderFactory.createLineBorder(AppTheme.BADGE_BG.darker(), 1, true));
        badge.setVisible(false);
        return badge;
    }

    private JScrollPane wrapScroll(JList<?> list) {
        return wrapScroll(list, AppTheme.ITEM_BG);
    }

    private JScrollPane wrapScroll(JList<?> list, Color background) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_SUBTLE, 12, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        scroll.setBackground(background);
        scroll.getViewport().setBackground(list.getBackground());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        return scroll;
    }

    private void setupInteractions() {
        roomList.addListSelectionListener(this::handleRoomSelection);

        dmList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DirectMessageEntry selected = dmList.getSelectedValue();
                    if (selected != null) {
                        openPrivateConversation(selected.peerId(), selected.displayName());
                    }
                }
            }
        });

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

        composer.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateTypingIndicator();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateTypingIndicator();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateTypingIndicator();
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
        clearUnread(key);
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

        DirectMessageEntry entry = ensureDirectMessageEntry(peerId, displayName);
        if (entry != null) {
            dmList.setSelectedValue(entry, true);
        }

        currentConversationKey = key;
        titleLabel.setText("@" + displayName);
        messageList.setModel(conversationModels.get(key));
        clearUnread(key);
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
                peer = new PeerInfo(
                        targetUserId,
                        conversationTitles.getOrDefault(currentConversationKey, "Unknown").replace("@", ""),
                        InetAddress.getLoopbackAddress(),
                        0,
                        System.currentTimeMillis()
                );
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
        replyLabel.setText("Reply to " + TextUtils.shortPreview(selected.senderName(), selected.content()));
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

    private void updateTypingIndicator() {
        String content = composer.getText();
        boolean typing = content != null && !content.trim().isEmpty();
        typingPanel.setVisible(typing);
        typingLabel.setText(typing ? node.userName() + " is typing..." : "");
    }

    private void incrementUnread(String conversationKey) {
        if (conversationKey == null) {
            return;
        }
        unreadByConversation.merge(conversationKey, 1, Integer::sum);
        roomList.repaint();
        dmList.repaint();
        peerList.repaint();
        updateSidebarUnreadBadges();
    }

    private void clearUnread(String conversationKey) {
        if (conversationKey == null) {
            return;
        }
        if (unreadByConversation.remove(conversationKey) != null) {
            roomList.repaint();
            dmList.repaint();
            peerList.repaint();
            updateSidebarUnreadBadges();
        }
    }

    private int unreadCount(String conversationKey) {
        if (conversationKey == null) {
            return 0;
        }
        return unreadByConversation.getOrDefault(conversationKey, 0);
    }

    private void updateSidebarUnreadBadges() {
        int channelsUnread = 0;
        int directUnread = 0;
        int friendsUnread = 0;

        for (Map.Entry<String, Integer> entry : unreadByConversation.entrySet()) {
            String key = entry.getKey();
            int unread = entry.getValue() == null ? 0 : entry.getValue();
            if (unread <= 0 || key == null) {
                continue;
            }

            if (ConversationKeys.isRoom(key)) {
                channelsUnread += unread;
                continue;
            }

            if (ConversationKeys.isPm(key)) {
                directUnread += unread;
                String peerId = extractOtherUserFromPmKey(key);
                if (peerId != null && peersById.containsKey(peerId)) {
                    friendsUnread += unread;
                }
            }
        }

        updateUnreadBadge(channelsUnreadLabel, channelsUnread);
        updateUnreadBadge(directUnreadLabel, directUnread);
        updateUnreadBadge(friendsUnreadLabel, friendsUnread);
    }

    private void updateUnreadBadge(JLabel badge, int count) {
        if (badge == null) {
            return;
        }
        if (count <= 0) {
            badge.setVisible(false);
            badge.setText("");
            return;
        }
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisible(true);
    }

    private Color colorFromPalette(String key, Color[] palette) {
        if (palette == null || palette.length == 0) {
            return AppTheme.TEXT_MUTED;
        }
        int hash = key == null ? 0 : key.hashCode();
        int index = Math.abs(hash) % palette.length;
        return palette[index];
    }

    private Color directMessageDotColor(String peerId) {
        Color[] palette = new Color[]{
                AppTheme.DM_DOT_GREEN,
                AppTheme.DM_DOT_ORANGE,
                AppTheme.DM_DOT_BLUE,
                AppTheme.DM_DOT_PINK
        };
        return colorFromPalette(peerId, palette);
    }

    private Color peerAvatarColor(String peerId) {
        Color[] palette = new Color[]{
                AppTheme.PEER_AVATAR_BLUE,
                AppTheme.PEER_AVATAR_GREEN,
                AppTheme.PEER_AVATAR_GOLD,
                AppTheme.PEER_AVATAR_PURPLE
        };
        return colorFromPalette(peerId, palette);
    }

    private DirectMessageEntry ensureDirectMessageEntry(String peerId, String displayName) {
        if (peerId == null) {
            return null;
        }

        String safeName = (displayName == null || displayName.isBlank()) ? "Unknown" : displayName.trim();
        DirectMessageEntry existing = dmEntriesByPeer.get(peerId);
        if (existing == null) {
            existing = new DirectMessageEntry(peerId, safeName);
            dmEntriesByPeer.put(peerId, existing);
            dmListModel.addElement(existing);
            return existing;
        }

        if (!existing.displayName().equals(safeName)) {
            existing.setDisplayName(safeName);
            dmList.repaint();
        }
        return existing;
    }

    @Override
    public void onPeerListUpdated(List<PeerInfo> peers) {
        SwingUtilities.invokeLater(() -> {
            peersById.clear();
            peerListModel.clear();
            for (PeerInfo peer : peers) {
                peersById.put(peer.userId(), peer);
                peerListModel.addElement(peer);

                DirectMessageEntry entry = dmEntriesByPeer.get(peer.userId());
                if (entry != null && !entry.displayName().equals(peer.displayName())) {
                    entry.setDisplayName(peer.displayName());
                }
            }
            dmList.repaint();
            peerList.repaint();
            updateSidebarUnreadBadges();
            setStatus("Online peers: " + peers.size() + " - " + (node.isHybrid() ? "hybrid mode" : "udp mode"));
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

            boolean mine = message.senderId() != null && message.senderId().equals(node.userId());
            if (!key.equals(currentConversationKey) && !mine) {
                incrementUnread(key);
            }

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
            ensureDirectMessageEntry(otherUserId, otherName);
            appendMessage(key, message);

            boolean mine = message.senderId() != null && message.senderId().equals(node.userId());
            if (!key.equals(currentConversationKey) && !mine) {
                incrementUnread(key);
            }

            setStatus("Private message from " + otherName);
        });
    }

    @Override
    public void onSystemNotice(String message) {
        SwingUtilities.invokeLater(() -> setStatus(message));
    }
}






































