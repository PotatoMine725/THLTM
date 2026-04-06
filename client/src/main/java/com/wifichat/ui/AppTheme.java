package com.wifichat.ui;

import java.awt.Color;
import java.awt.Font;

public final class AppTheme {
    public static final Color WINDOW_BG = new Color(244, 248, 252);
    public static final Color SIDEBAR_BG = new Color(231, 241, 250);
    public static final Color CARD_BG = new Color(255, 255, 255);

    public static final Color ROOM_ACCENT = new Color(53, 116, 193);
    public static final Color USER_ACCENT = new Color(43, 148, 108);
    public static final Color CHAT_ACCENT = new Color(226, 126, 73);

    public static final Color PRIMARY_BUTTON = new Color(48, 123, 208);
    public static final Color SUCCESS_BUTTON = new Color(43, 148, 108);
    public static final Color NEUTRAL_BUTTON = new Color(96, 109, 124);

    public static final Color SOFT_TEXT = new Color(95, 109, 126);
    public static final Color STRONG_TEXT = new Color(35, 43, 53);

    private AppTheme() {
    }

    public static Font heading(int size) {
        return new Font("Bahnschrift", Font.BOLD, size);
    }

    public static Font body(int style, int size) {
        return new Font("Segoe UI", style, size);
    }
}
