package com.wifichat.ui;

import com.wifichat.model.ChatMessage;
import com.wifichat.util.TextUtils;
import com.wifichat.util.TimeUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<ChatMessage> {
    private final JLabel metaLabel;
    private final JLabel replyLabel;
    private final JLabel contentLabel;
    private final String localUserId;

    public MessageCellRenderer(String localUserId) {
        this.localUserId = localUserId;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        setOpaque(true);

        metaLabel = new JLabel();
        metaLabel.setFont(AppTheme.body(java.awt.Font.BOLD, 12));

        replyLabel = new JLabel();
        replyLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 12));
        replyLabel.setForeground(new Color(82, 96, 113));

        contentLabel = new JLabel();
        contentLabel.setFont(AppTheme.body(java.awt.Font.PLAIN, 14));

        add(metaLabel);
        add(replyLabel);
        add(contentLabel);
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
            metaLabel.setText("");
            replyLabel.setText("");
            contentLabel.setText("");
            return this;
        }

        boolean mine = value.senderId() != null && value.senderId().equals(localUserId);
        String sender = TextUtils.htmlEscape(value.senderName());
        String content = TextUtils.htmlEscape(value.content());
        String time = TimeUtils.formatTime(value.timestamp());

        metaLabel.setText("[" + time + "] " + sender + (mine ? " (you)" : ""));
        metaLabel.setForeground(mine ? new Color(17, 77, 141) : new Color(66, 51, 124));

        if (value.replyPreview() != null && !value.replyPreview().isBlank()) {
            replyLabel.setVisible(true);
            replyLabel.setText("Reply: " + TextUtils.htmlEscape(value.replyPreview()));
        } else {
            replyLabel.setVisible(false);
            replyLabel.setText("");
        }

        contentLabel.setText("<html><body style='width: 420px; margin-top: 2px;'>" + content + "</body></html>");

        Color mineColor = new Color(219, 238, 255);
        Color otherColor = new Color(244, 234, 255);
        Color selectedColor = new Color(255, 232, 198);

        setBackground(isSelected ? selectedColor : (mine ? mineColor : otherColor));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(5, 10, 5, 10),
                new RoundedBorder(new Color(206, 214, 226), 16, 1)
        ));

        return this;
    }
}
