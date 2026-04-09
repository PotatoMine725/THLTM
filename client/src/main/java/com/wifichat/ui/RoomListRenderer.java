package com.wifichat.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Function;

/**
 * Renders room items in the sidebar channel list.
 * Shows: # prefix | room name | unread badge.
 */
final class RoomListRenderer extends DefaultListCellRenderer {
    private final JPanel panel;
    private final JLabel hashLabel;
    private final JLabel nameLabel;
    private final JLabel badgeLabel;
    private final Function<String, Integer> unreadCountProvider;

    RoomListRenderer(Function<String, Integer> unreadCountProvider) {
        this.unreadCountProvider = unreadCountProvider;

        panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        hashLabel = new JLabel("#");
        hashLabel.setFont(AppTheme.body(Font.BOLD, 14));
        hashLabel.setForeground(AppTheme.SIDEBAR_CHANNEL_HASH);

        nameLabel = new JLabel();
        nameLabel.setFont(AppTheme.body(Font.BOLD, 14));

        left.add(hashLabel);
        left.add(Box.createHorizontalStrut(12));
        left.add(nameLabel);

        badgeLabel = new JLabel("", SwingConstants.CENTER);
        badgeLabel.setFont(AppTheme.body(Font.BOLD, 11));
        badgeLabel.setOpaque(true);
        badgeLabel.setForeground(AppTheme.BADGE_TEXT);
        badgeLabel.setBackground(AppTheme.BADGE_BG);
        badgeLabel.setPreferredSize(new Dimension(20, 18));
        badgeLabel.setBorder(BorderFactory.createLineBorder(AppTheme.BADGE_BG.darker(), 1, true));

        panel.add(left, BorderLayout.CENTER);
        panel.add(badgeLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            nameLabel.setText("");
            badgeLabel.setVisible(false);
            return panel;
        }

        String room = String.valueOf(value);
        nameLabel.setText(room.isBlank() ? "(Unnamed)" : room.substring(0, 1).toUpperCase() + room.substring(1));
        nameLabel.setForeground(isSelected ? AppTheme.SIDEBAR_SELECTED_TEXT : AppTheme.TEXT_SECONDARY);

        int unread = unreadCountProvider.apply(room);
        badgeLabel.setVisible(unread > 0);
        badgeLabel.setText(unread > 99 ? "99+" : String.valueOf(unread));

        panel.setBackground(isSelected ? AppTheme.SIDEBAR_ITEM_ACTIVE_BG : AppTheme.SIDEBAR_ITEM_BG);
        return panel;
    }
}
