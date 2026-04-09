package com.wifichat.shared.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared UI styling utilities for buttons and common components.
 * Used by both client and admin modules to ensure visual consistency.
 */
public final class UIHelper {

    private UIHelper() {
    }

    /**
     * Apply the standard styled-button look: background, foreground, hover/press effects,
     * and a rounded border.
     */
    public static void styleButton(
            JButton button,
            Color background,
            Color border,
            Color text,
            int radius,
            int verticalPadding,
            int horizontalPadding
    ) {
        Color hover = AppTheme.blend(background, Color.WHITE, 0.12f);
        Color pressed = AppTheme.blend(background, Color.BLACK, 0.12f);

        button.setBackground(background);
        button.setForeground(text);
        button.setFont(AppTheme.body(Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(border, radius, 1),
                new EmptyBorder(verticalPadding, horizontalPadding, verticalPadding, horizontalPadding)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(background);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hover : background);
            }
        });
    }

    /**
     * Get initials from a display name (first letter of first two words, or first letter if single word).
     */
    public static String initials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }
        String[] parts = displayName.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }

    /**
     * Format a Color object as a CSS hex string like "#rrggbb".
     */
    public static String colorToHex(Color c) {
        if (c == null) return "#fff";
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
