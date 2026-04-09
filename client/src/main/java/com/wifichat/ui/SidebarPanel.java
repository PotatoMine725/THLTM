package com.wifichat.ui;

import com.wifichat.model.PeerInfo;
import com.wifichat.shared.ui.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Function;

/**
 * Sidebar panel containing profile card, channel list, DM list, and peers list.
 * Extracted from MainFrame — pure UI layout, no business logic.
 */
final class SidebarPanel extends JPanel {

    SidebarPanel(
            String userName,
            boolean isHybrid,
            Runnable onLogout,
            Runnable onJoinRoom,
            Runnable onOpenPrivateChat,
            JList<String> roomList,
            JList<DirectMessageEntry> dmList,
            JList<PeerInfo> peerList,
            JLabel channelsUnreadLabel,
            JLabel directUnreadLabel,
            JLabel friendsUnreadLabel,
            Function<String, Integer> roomUnreadProvider,
            Function<String, Color> dmDotColorProvider,
            Function<String, Integer> dmUnreadProvider,
            Function<String, Color> peerAvatarColorProvider,
            Function<String, Integer> peerUnreadProvider
    ) {
        setBackground(AppTheme.SIDEBAR_BG);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        stack.add(buildProfileCard(userName, isHybrid, onLogout));
        stack.add(buildDivider());
        stack.add(buildChannelSection(roomList, channelsUnreadLabel, onJoinRoom, roomUnreadProvider));
        stack.add(buildDivider());
        stack.add(buildDirectMessageSection(dmList, directUnreadLabel, onOpenPrivateChat, dmDotColorProvider, dmUnreadProvider));
        stack.add(buildDivider());
        stack.add(buildOnlinePeersSection(peerList, friendsUnreadLabel, peerAvatarColorProvider, peerUnreadProvider));

        JScrollPane sidebarScroll = new JScrollPane(stack);
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setOpaque(false);
        sidebarScroll.getViewport().setOpaque(false);
        sidebarScroll.getViewport().setBackground(AppTheme.SIDEBAR_BG);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(14);

        add(sidebarScroll, BorderLayout.CENTER);
    }

    private JPanel buildProfileCard(String userName, boolean isHybrid, Runnable onLogout) {
        JPanel profile = new JPanel(new BorderLayout(10, 0));
        profile.setOpaque(false);
        profile.setBorder(new EmptyBorder(4, 4, 4, 4));

        JLabel avatar = new JLabel(UIHelper.initials(userName), SwingConstants.CENTER);
        avatar.setFont(AppTheme.body(Font.BOLD, 13));
        avatar.setForeground(AppTheme.TEXT_PRIMARY);
        avatar.setOpaque(true);
        avatar.setBackground(AppTheme.PEER_AVATAR_BLUE);
        avatar.setPreferredSize(new Dimension(34, 34));
        avatar.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 17, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(userName);
        name.setFont(AppTheme.body(Font.BOLD, 14));
        name.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel mode = new JLabel(isHybrid ? "Hybrid mode" : "Guest mode");
        mode.setFont(AppTheme.body(Font.PLAIN, 11));
        mode.setForeground(AppTheme.SIDEBAR_HEADER_TEXT);

        text.add(name);
        text.add(Box.createVerticalStrut(1));
        text.add(mode);

        profile.add(avatar, BorderLayout.WEST);
        profile.add(text, BorderLayout.CENTER);

        if (onLogout != null) {
            JButton logout = new JButton("Log out");
            UIHelper.styleButton(logout, AppTheme.DANGER_BUTTON, AppTheme.DANGER_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 5, 9);
            logout.setFont(AppTheme.body(Font.BOLD, 11));
            logout.addActionListener(e -> onLogout.run());
            profile.add(logout, BorderLayout.EAST);
        }

        return profile;
    }

    private JPanel buildChannelSection(JList<String> roomList, JLabel channelsUnreadLabel, Runnable onJoinRoom, Function<String, Integer> unreadProvider) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        section.add(createSectionHeader("CHANNELS", channelsUnreadLabel, onJoinRoom), BorderLayout.NORTH);

        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setBackground(AppTheme.SIDEBAR_ITEM_BG);
        roomList.setForeground(AppTheme.TEXT_SECONDARY);
        roomList.setFont(AppTheme.body(Font.BOLD, 14));
        roomList.setCellRenderer(new RoomListRenderer(unreadProvider));

        section.add(wrapSidebarList(roomList, 184), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildDirectMessageSection(JList<DirectMessageEntry> dmList, JLabel directUnreadLabel, Runnable onOpenPrivateChat, Function<String, Color> dotColorProvider, Function<String, Integer> unreadProvider) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        section.add(createSectionHeader("DIRECT MESSAGES", directUnreadLabel, onOpenPrivateChat), BorderLayout.NORTH);

        dmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dmList.setBackground(AppTheme.SIDEBAR_ITEM_BG);
        dmList.setForeground(AppTheme.TEXT_SECONDARY);
        dmList.setFont(AppTheme.body(Font.BOLD, 14));
        dmList.setCellRenderer(new DirectMessageListRenderer(dotColorProvider, unreadProvider));

        section.add(wrapSidebarList(dmList, 118), BorderLayout.CENTER);
        return section;
    }

    private JPanel buildOnlinePeersSection(JList<PeerInfo> peerList, JLabel friendsUnreadLabel, Function<String, Color> avatarColorProvider, Function<String, Integer> unreadProvider) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);

        section.add(createSectionHeader("ONLINE PEERS", friendsUnreadLabel, null), BorderLayout.NORTH);

        peerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peerList.setBackground(AppTheme.SIDEBAR_ITEM_BG);
        peerList.setFont(AppTheme.body(Font.PLAIN, 13));
        peerList.setCellRenderer(new PeerListRenderer(avatarColorProvider, unreadProvider));

        section.add(wrapSidebarList(peerList, 240), BorderLayout.CENTER);
        return section;
    }

    private static JPanel buildDivider() {
        JPanel divider = new JPanel(new BorderLayout());
        divider.setOpaque(false);
        divider.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel line = new JPanel();
        line.setBackground(AppTheme.SIDEBAR_DIVIDER);
        line.setPreferredSize(new Dimension(10, 1));
        divider.add(line, BorderLayout.CENTER);
        return divider;
    }

    private static JScrollPane wrapSidebarList(JList<?> list, int preferredHeight) {
        JScrollPane scroll = new JScrollPane(list);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getViewport().setBackground(AppTheme.SIDEBAR_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.setPreferredSize(new Dimension(282, preferredHeight));
        return scroll;
    }

    private static JPanel createSectionHeader(String title, JLabel unreadBadge, Runnable action) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(1, 0, 1, 0));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.body(Font.BOLD, 12));
        titleLabel.setForeground(AppTheme.SIDEBAR_HEADER_TEXT);
        header.add(titleLabel, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        if (unreadBadge != null) {
            right.add(unreadBadge);
        }

        if (action != null) {
            JButton actionButton = new JButton("+");
            actionButton.setFont(AppTheme.body(Font.BOLD, 12));
            actionButton.setPreferredSize(new Dimension(22, 22));
            UIHelper.styleButton(actionButton, AppTheme.GHOST_BUTTON, AppTheme.SIDEBAR_DIVIDER, AppTheme.SIDEBAR_HEADER_TEXT, 11, 2, 2);
            actionButton.addActionListener(e -> action.run());
            right.add(actionButton);
        }

        header.add(right, BorderLayout.EAST);
        return header;
    }
}
