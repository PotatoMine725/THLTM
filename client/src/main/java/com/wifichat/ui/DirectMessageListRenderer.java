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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Function;

/**
 * Renders direct-message entries in the sidebar DM list.
 * Shows: colored dot | display name | unread badge.
 */
final class DirectMessageListRenderer extends DefaultListCellRenderer {
    private final JPanel panel;
    private final JLabel dotLabel;
    private final JLabel nameLabel;
    private final JLabel badgeLabel;
    private final Function<String, Color> dotColorProvider;
    private final Function<String, Integer> unreadCountProvider;

    DirectMessageListRenderer(Function<String, Color> dotColorProvider, Function<String, Integer> unreadCountProvider) {
        this.dotColorProvider = dotColorProvider;
        this.unreadCountProvider = unreadCountProvider;

        panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));

        dotLabel = new JLabel("o");
        dotLabel.setFont(AppTheme.body(Font.BOLD, 11));

        nameLabel = new JLabel();
        nameLabel.setFont(AppTheme.body(Font.BOLD, 14));

        left.add(dotLabel);
        left.add(Box.createHorizontalStrut(10));
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
        DirectMessageEntry entry = value instanceof DirectMessageEntry ? (DirectMessageEntry) value : null;
        if (entry == null) {
            nameLabel.setText("");
            badgeLabel.setVisible(false);
            return panel;
        }

        dotLabel.setForeground(dotColorProvider.apply(entry.peerId()));
        nameLabel.setText(entry.displayName());
        nameLabel.setForeground(isSelected ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);

        int unread = unreadCountProvider.apply(entry.peerId());
        badgeLabel.setVisible(unread > 0);
        badgeLabel.setText(unread > 99 ? "99+" : String.valueOf(unread));

        panel.setBackground(isSelected ? AppTheme.SIDEBAR_ITEM_ACTIVE_BG : AppTheme.SIDEBAR_ITEM_BG);
        return panel;
    }
}
