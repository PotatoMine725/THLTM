import os

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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<ChatMessage> {
    private static final DateTimeFormatter DATE_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JLabel dayLabel;
    private final JPanel messageRow;
    private final JLabel avatarLabel;
    private final BubblePanel bubblePanel;
    private final JLabel metaLabel;
    private final JLabel replyLabel;
    private final JLabel contentLabel;
    private final String localUserId;

    // Custom rounded panel that respects opacity and shrinks wrapping
    private static class BubblePanel extends JPanel {
        private int radius = 22;
        public BubblePanel() {
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

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
        messageRow.setLayout(new BorderLayout()); 
        messageRow.setOpaque(true);
        messageRow.setBorder(new EmptyBorder(4, 16, 4, 16));

        avatarLabel = new JLabel(" ", SwingConstants.CENTER);
        avatarLabel.setOpaque(true);
        avatarLabel.setFont(AppTheme.body(java.awt.Font.BOLD, 12));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setPreferredSize(new Dimension(32, 32));
        avatarLabel.setMaximumSize(new Dimension(32, 32));
        avatarLabel.putClientProperty("FlatLaf.styleClass", "h1");
        
        avatarLabel.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_STRONG, 16, 1),
                new EmptyBorder(0, 0, 0, 0)
        ));

        bubblePanel = new BubblePanel();
        bubblePanel.setLayout(new BoxLayout(bubblePanel, BoxLayout.Y_AXIS));
        bubblePanel.setBorder(new EmptyBorder(10, 14, 10, 14)); 

        metaLabel = new JLabel();
        metaLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 11));
        
        replyLabel = new JLabel();
        replyLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 12));
        replyLabel.setOpaque(false);
        
        contentLabel = new JLabel();
        contentLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 15));

        // Let the content text wrap naturally but pad
        JPanel textContainer = new JPanel(new BorderLayout());
        textContainer.setOpaque(false);
        textContainer.add(contentLabel, BorderLayout.CENTER);

        bubblePanel.add(replyLabel);
        bubblePanel.add(textContainer);

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
            contentLabel.setText("<html><div style='max-width: " + contentWidth + "px; margin: 0;'>" + content + "</div></html>");
            bubblePanel.setBackground(AppTheme.MINE_BUBBLE);
            
            if (value.replyPreview() != null && !value.replyPreview().isBlank()) {
                replyLabel.setVisible(true);
                replyLabel.setText("<html>&#x2514; <span style='color:#E0E0E0'>" + TextUtils.htmlEscape(value.replyPreview()) + "</span></html>");
                replyLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            } else {
                replyLabel.setVisible(false);
            }

            metaLabel.setText(time);
            metaLabel.setForeground(AppTheme.TEXT_MUTED);

            JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bubbleWrapper.setOpaque(false);
            bubbleWrapper.add(bubblePanel);

            JPanel metaWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            metaWrapper.setOpaque(false);
            metaWrapper.setBorder(new EmptyBorder(4, 0, 0, 8)); 
            metaWrapper.add(metaLabel);

            JPanel verticalStack = new JPanel();
            verticalStack.setLayout(new BoxLayout(verticalStack, BoxLayout.Y_AXIS));
            verticalStack.setOpaque(false);
            verticalStack.add(bubbleWrapper);
            if (!groupedWithPrevious) {
                verticalStack.add(metaWrapper);
            }

            messageRow.add(verticalStack, BorderLayout.EAST);
        } else {
            contentLabel.setForeground(AppTheme.TEXT_PRIMARY);
            contentLabel.setText("<html><div style='max-width: " + contentWidth + "px; margin: 0;'>" + content + "</div></html>");
            bubblePanel.setBackground(AppTheme.ITEM_BG);

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

            JPanel bubbleWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            bubbleWrapper.setOpaque(false);
            bubbleWrapper.add(bubblePanel);

            JPanel metaWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            metaWrapper.setOpaque(false);
            metaWrapper.setBorder(new EmptyBorder(4, 12, 0, 0));
            metaWrapper.add(metaLabel);

            JPanel verticalStack = new JPanel();
            verticalStack.setLayout(new BoxLayout(verticalStack, BoxLayout.Y_AXIS));
            verticalStack.setOpaque(false);
            
            verticalStack.add(bubbleWrapper);
            if (!groupedWithPrevious) {
                verticalStack.add(metaWrapper);
            }

            JPanel combined = new JPanel(new BorderLayout(8, 0));
            combined.setOpaque(false);
            combined.add(avatarWrapper, BorderLayout.WEST);
            combined.add(verticalStack, BorderLayout.CENTER);

            messageRow.add(combined, BorderLayout.WEST);
        }

        if (groupedWithPrevious) {
            messageRow.setBorder(new EmptyBorder(2, 16, 2, 16));
        } else {
            messageRow.setBorder(new EmptyBorder(8, 16, 2, 16));
        }

        Color listBackground = AppTheme.WINDOW_BG;
        setBackground(listBackground);
        
        if (isSelected) {
            messageRow.setBackground(AppTheme.SIDEBAR_BG); 
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
