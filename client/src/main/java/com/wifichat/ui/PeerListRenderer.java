package com.wifichat.ui;

import com.wifichat.model.PeerInfo;
import com.wifichat.shared.ui.UIHelper;

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
 * Renders online peer entries in the sidebar peers list.
 * Shows: avatar circle | display name + IP | unread badge.
 */
final class PeerListRenderer extends DefaultListCellRenderer {
    private final JPanel panel;
    private final JLabel avatarLabel;
    private final JLabel nameLabel;
    private final JLabel ipLabel;
    private final JLabel badgeLabel;
    private final Function<String, Color> avatarColorProvider;
    private final Function<String, Integer> unreadCountProvider;

    PeerListRenderer(Function<String, Color> avatarColorProvider, Function<String, Integer> unreadCountProvider) {
        this.avatarColorProvider = avatarColorProvider;
        this.unreadCountProvider = unreadCountProvider;

        panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        avatarLabel = new JLabel("", SwingConstants.CENTER);
        avatarLabel.setFont(AppTheme.body(Font.BOLD, 11));
        avatarLabel.setForeground(AppTheme.TEXT_PRIMARY);
        avatarLabel.setOpaque(true);
        avatarLabel.setPreferredSize(new Dimension(26, 26));
        avatarLabel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 13, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        nameLabel = new JLabel();
        nameLabel.setFont(AppTheme.body(Font.BOLD, 13));

        ipLabel = new JLabel();
        ipLabel.setFont(AppTheme.body(Font.PLAIN, 11));
        ipLabel.setForeground(AppTheme.TEXT_MUTED);

        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(1));
        textPanel.add(ipLabel);

        badgeLabel = new JLabel("", SwingConstants.CENTER);
        badgeLabel.setFont(AppTheme.body(Font.BOLD, 11));
        badgeLabel.setOpaque(true);
        badgeLabel.setForeground(AppTheme.BADGE_TEXT);
        badgeLabel.setBackground(AppTheme.BADGE_BG);
        badgeLabel.setPreferredSize(new Dimension(20, 18));
        badgeLabel.setBorder(BorderFactory.createLineBorder(AppTheme.BADGE_BG.darker(), 1, true));

        panel.add(avatarLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(badgeLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        PeerInfo peer = value instanceof PeerInfo ? (PeerInfo) value : null;
        if (peer == null) {
            nameLabel.setText("");
            ipLabel.setText("");
            badgeLabel.setVisible(false);
            return panel;
        }

        avatarLabel.setText(UIHelper.initials(peer.displayName()));
        avatarLabel.setBackground(avatarColorProvider.apply(peer.userId()));
        nameLabel.setText(peer.displayName());
        nameLabel.setForeground(isSelected ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
        ipLabel.setText(peer.address().getHostAddress());

        int unread = unreadCountProvider.apply(peer.userId());
        badgeLabel.setVisible(unread > 0);
        badgeLabel.setText(unread > 99 ? "99+" : String.valueOf(unread));

        panel.setBackground(isSelected ? AppTheme.SIDEBAR_ITEM_ACTIVE_BG : AppTheme.SIDEBAR_ITEM_BG);
        return panel;
    }
}
