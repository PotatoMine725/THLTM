import os

# Update AppTheme.java
theme_path = r"d:\Code\Java\test\client\src\main\java\com\wifichat\ui\AppTheme.java"
with open(theme_path, 'r', encoding='utf-8') as f:
    theme_content = f.read()

theme_content = theme_content.replace(
    "public static final Color MINE_BUBBLE = new Color(49, 51, 56);",
    "public static final Color MINE_BUBBLE = new Color(0, 132, 255);"
)
theme_content = theme_content.replace(
    "public static final Color OTHER_BUBBLE = new Color(49, 51, 56);",
    "public static final Color OTHER_BUBBLE = new Color(58, 59, 60);"
)

with open(theme_path, 'w', encoding='utf-8') as f:
    f.write(theme_content)

# Update MessageCellRenderer.java
renderer_path = r"d:\Code\Java\test\client\src\main\java\com\wifichat\ui\MessageCellRenderer.java"

new_renderer_content = """package com.wifichat.ui;

import com.wifichat.model.ChatMessage;
import com.wifichat.util.TextUtils;
import com.wifichat.util.TimeUtils;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<ChatMessage> {
    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JLabel dayLabel;
    private final JPanel messageRow;
    private final JLabel avatarLabel;
    private final JPanel bubblePanel;
    private final JLabel metaLabel;
    private final JLabel replyLabel;
    private final JLabel contentLabel;
    private final String localUserId;

    public MessageCellRenderer(String localUserId) {
        this.localUserId = localUserId;
        setOpaque(true);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(0, 0, 0, 0));

        dayLabel = new JLabel();
        dayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dayLabel.setFont(AppTheme.body(java.awt.Font.BOLD, 12));
        dayLabel.setForeground(AppTheme.TEXT_MUTED);
        dayLabel.setBorder(new EmptyBorder(16, 0, 8, 0));
        dayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        messageRow = new JPanel();
        messageRow.setLayout(new BoxLayout(messageRow, BoxLayout.X_AXIS));
        messageRow.setOpaque(true);
        messageRow.setBorder(new EmptyBorder(4, 16, 4, 16));

        avatarLabel = new JLabel(" ", SwingConstants.CENTER);
        avatarLabel.setOpaque(true);
        avatarLabel.setFont(AppTheme.body(java.awt.Font.BOLD, 12));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setPreferredSize(new Dimension(32, 32));
        avatarLabel.setMaximumSize(new Dimension(32, 32));
        avatarLabel.putClientProperty("FlatLaf.styleClass", "h1");
        // Soft round for avatar
        avatarLabel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 16, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));

        bubblePanel = new JPanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setOpaque(true);

        metaLabel = new JLabel();
        metaLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 11));
        
        replyLabel = new JLabel();
        replyLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 12));
        replyLabel.setOpaque(false);
        
        contentLabel = new JLabel();
        contentLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 15));

        bubblePanel.add(replyLabel);
        bubblePanel.add(contentLabel);

        add(dayLabel);
        add(messageRow);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ChatMessage> list,
            ChatMessage value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
        if (value == null) {
            dayLabel.setVisible(false);
            messageRow.setVisible(false);
            return this;
        }
        messageRow.setVisible(true);

        ChatMessage previous = index > 0 ? list.getModel().getElementAt(index - 1) : null;

        boolean mine = value.senderId() != null && value.senderId().equals(localUserId);
        boolean sameDayAsPrevious = previous != null && isSameDay(value.timestamp(), previous.timestamp());
        boolean groupedWithPrevious = previous != null
                && sameDayAsPrevious
                && safeEquals(previous.senderId(), value.senderId())
                && Math.abs(value.timestamp() - previous.timestamp()) <= 7 * 60 * 1000L;

        dayLabel.setVisible(!sameDayAsPrevious);
        if (!sameDayAsPrevious) {
            dayLabel.setText(dayLabelText(value.timestamp()));
        }

        String sender = TextUtils.htmlEscape(value.senderName());
        String content = TextUtils.htmlEscape(value.content());
        String time = TimeUtils.formatTime(value.timestamp());
        String initials = initials(value.senderName());

        messageRow.removeAll();

        int listWidth = list.getWidth();
        int contentWidth = Math.max(200, Math.min(500, (listWidth > 0 ? listWidth : 860) - 200));

        if (mine) {
            contentLabel.setForeground(Color.WHITE);
            contentLabel.setText("<html><body style='width: " + contentWidth + "px; margin: 0;'>" + content + "</body></html>");
            bubblePanel.setBackground(AppTheme.MINE_BUBBLE);
            
            // Soft rounded Messenger style borders
            bubblePanel.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppTheme.MINE_BUBBLE, 22, 1),
                    new EmptyBorder(8, 14, 8, 14)
            ));

            if (value.replyPreview() != null && !value.replyPreview().isBlank()) {
                replyLabel.setVisible(true);
                replyLabel.setText("<html>&#x2514; <span style='color:#E0E0E0'>" + TextUtils.htmlEscape(value.replyPreview()) + "</span></html>");
                replyLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            } else {
                replyLabel.setVisible(false);
            }

            metaLabel.setText(time);
            metaLabel.setForeground(AppTheme.TEXT_MUTED);

            JPanel textWrapper = new JPanel(new BorderLayout());
            textWrapper.setOpaque(false);
            textWrapper.add(bubblePanel, BorderLayout.EAST);
            
            JPanel metaWrapper = new JPanel(new BorderLayout());
            metaWrapper.setOpaque(false);
            metaWrapper.setBorder(new EmptyBorder(4, 0, 0, 8));
            metaWrapper.add(metaLabel, BorderLayout.EAST);

            JPanel verticalStack = new JPanel();
            verticalStack.setLayout(new BoxLayout(verticalStack, BoxLayout.Y_AXIS));
            verticalStack.setOpaque(false);
            verticalStack.add(textWrapper);
            if (!groupedWithPrevious) { // Only show time on last message or something? No, let's just show time if not grouped
                 // FB messenger hides time unless hovered, but we can just show it beneath
            }
            // For now, always show time beneath if it's the last in group? Simple approach: show time always for simplicity or only on not grouped. We'll show metadata below the bubble if not grouped.
            if (!groupedWithPrevious) {
                verticalStack.add(metaWrapper);
            }

            messageRow.add(Box.createHorizontalGlue());
            messageRow.add(verticalStack);
        } else {
            contentLabel.setForeground(AppTheme.TEXT_PRIMARY);
            contentLabel.setText("<html><body style='width: " + contentWidth + "px; margin: 0;'>" + content + "</body></html>");
            bubblePanel.setBackground(AppTheme.OTHER_BUBBLE);
            
            bubblePanel.setBorder(BorderFactory.createCompoundBorder(
                    new RoundedBorder(AppTheme.OTHER_BUBBLE, 22, 1),
                    new EmptyBorder(8, 14, 8, 14)
            ));

            if (value.replyPreview() != null && !value.replyPreview().isBlank()) {
                replyLabel.setVisible(true);
                replyLabel.setText("<html>&#x2514; <span style='color:" + hex(AppTheme.TEXT_SECONDARY) + "'>" + TextUtils.htmlEscape(value.replyPreview()) + "</span></html>");
                replyLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            } else {
                replyLabel.setVisible(false);
            }

            metaLabel.setText(sender + " • " + time);
            metaLabel.setForeground(AppTheme.TEXT_MUTED);

            avatarLabel.setText(initials);
            avatarLabel.setBackground(AppTheme.AVATAR_OTHER);
            
            JPanel avatarWrapper = new JPanel(new BorderLayout());
            avatarWrapper.setOpaque(false);
            if (!groupedWithPrevious) {
                avatarWrapper.add(avatarLabel, BorderLayout.SOUTH);
            } else {
                avatarWrapper.add(Box.createRigidArea(new Dimension(32, 32)), BorderLayout.SOUTH);
            }

            JPanel textWrapper = new JPanel(new BorderLayout());
            textWrapper.setOpaque(false);
            textWrapper.add(bubblePanel, BorderLayout.WEST);

            JPanel metaWrapper = new JPanel(new BorderLayout());
            metaWrapper.setOpaque(false);
            metaWrapper.setBorder(new EmptyBorder(4, 12, 0, 0));
            metaWrapper.add(metaLabel, BorderLayout.WEST);

            JPanel verticalStack = new JPanel();
            verticalStack.setLayout(new BoxLayout(verticalStack, BoxLayout.Y_AXIS));
            verticalStack.setOpaque(false);
            
            if (!groupedWithPrevious) {
                verticalStack.add(metaWrapper);
            }
            verticalStack.add(textWrapper);

            messageRow.add(avatarWrapper);
            messageRow.add(Box.createHorizontalStrut(8));
            messageRow.add(verticalStack);
            messageRow.add(Box.createHorizontalGlue());
        }

        if (groupedWithPrevious) {
            messageRow.setBorder(new EmptyBorder(1, 16, 1, 16));
        } else {
            messageRow.setBorder(new EmptyBorder(8, 16, 2, 16));
        }

        Color listBackground = AppTheme.WINDOW_BG;
        setBackground(listBackground);
        
        if (isSelected) {
            messageRow.setBackground(AppTheme.ITEM_BG);
        } else {
            messageRow.setBackground(listBackground);
        }

        return this;
    }

    private String hex(Color c) {
        if (c == null) return "#fff";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private boolean isSameDay(long firstMillis, long secondMillis) {
        LocalDate first = Instant.ofEpochMilli(firstMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate second = Instant.ofEpochMilli(secondMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        return first.equals(second);
    }

    private String dayLabelText(long epochMillis) {
        LocalDate target = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        if (target.equals(today)) {
            return "Today";
        }
        return DATE_LABEL_FORMAT.format(target);
    }

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private String initials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String normalized = displayName.trim();
        String[] parts = normalized.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return normalized.substring(0, 1).toUpperCase();
    }
}
"""

with open(renderer_path, 'w', encoding='utf-8') as f:
    f.write(new_renderer_content)
