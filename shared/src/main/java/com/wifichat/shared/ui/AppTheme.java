package com.wifichat.shared.ui;

import java.awt.Color;
import java.awt.Font;

/**
 * Centralized theme constants for all WiFi Chat modules (client + admin).
 * All UI code must use these constants — never raw Color/Font constructors.
 */
public class AppTheme {
    // ── Spacing tokens ──
    public static final int SPACE_4 = 4;
    public static final int SPACE_8 = 8;
    public static final int SPACE_12 = 12;
    public static final int SPACE_16 = 16;
    public static final int SPACE_20 = 20;

    // ── Surface colors ──
    public static final Color WINDOW_BG = new Color(49, 51, 56);           // #313338
    public static final Color SIDEBAR_BG = new Color(43, 45, 49);          // #2B2D31
    public static final Color SIDEBAR_GRADIENT_TOP = SIDEBAR_BG;
    public static final Color SIDEBAR_GRADIENT_BOTTOM = SIDEBAR_BG;
    public static final Color SIDEBAR_CARD_BG = new Color(30, 31, 34);     // #1E1F22
    public static final Color SIDEBAR_HEADER_TEXT = new Color(148, 155, 164); // #949BA4
    public static final Color SIDEBAR_DIVIDER = new Color(63, 65, 71);     // #3F4147
    public static final Color SIDEBAR_CHANNEL_HASH = new Color(128, 132, 142); // #80848E
    public static final Color SIDEBAR_SELECTED_TEXT = new Color(242, 243, 245); // #F2F3F5
    public static final Color CARD_BG = new Color(49, 51, 56);             // #313338
    public static final Color PANEL_BG = new Color(49, 51, 56);            // #313338

    // ── Border colors ──
    public static final Color BORDER_SUBTLE = new Color(63, 65, 71);       // #3F4147
    public static final Color BORDER_STRONG = new Color(30, 31, 34);       // #1E1F22

    // ── Text colors ──
    public static final Color TEXT_PRIMARY = new Color(242, 243, 245);      // #F2F3F5
    public static final Color TEXT_SECONDARY = new Color(219, 222, 225);    // #DBDEE1
    public static final Color TEXT_MUTED = new Color(148, 155, 164);        // #949BA4

    // ── Accent colors ──
    public static final Color ROOM_ACCENT = new Color(88, 101, 242);       // Blurple
    public static final Color USER_ACCENT = new Color(35, 165, 89);        // Green
    public static final Color CHAT_ACCENT = new Color(242, 243, 245);

    // ── Button colors ──
    public static final Color PRIMARY_BUTTON = new Color(88, 101, 242);
    public static final Color SUCCESS_BUTTON = new Color(35, 165, 89);
    public static final Color NEUTRAL_BUTTON = new Color(78, 80, 88);
    public static final Color GHOST_BUTTON = new Color(43, 45, 49);
    public static final Color DANGER_BUTTON = new Color(218, 55, 60);
    public static final Color INPUT_FOCUS_BORDER = new Color(0, 0, 0, 0);

    // ── Item colors ──
    public static final Color SIDEBAR_ITEM_BG = new Color(43, 45, 49);
    public static final Color SIDEBAR_ITEM_ACTIVE_BG = new Color(64, 66, 73);
    public static final Color ITEM_BG = new Color(56, 58, 64);
    public static final Color ITEM_ACTIVE_BG = new Color(64, 66, 73);
    public static final Color ITEM_HOVER_BG = new Color(49, 51, 56);

    // ── Badge colors ──
    public static final Color BADGE_BG = new Color(218, 55, 60);
    public static final Color BADGE_TEXT = new Color(255, 255, 255);

    // ── Message bubble colors ──
    public static final Color MINE_BUBBLE = new Color(0, 132, 255);
    public static final Color OTHER_BUBBLE = new Color(58, 59, 60);
    public static final Color REPLY_BG = new Color(43, 45, 49);
    public static final Color AVATAR_MINE = new Color(88, 101, 242);
    public static final Color AVATAR_OTHER = new Color(35, 165, 89);
    public static final Color ONLINE_DOT = new Color(35, 165, 89);
    public static final Color MESSAGE_META_MINE = TEXT_PRIMARY;

    // ── DM dot colors ──
    public static final Color DM_DOT_GREEN = new Color(35, 165, 89);
    public static final Color DM_DOT_ORANGE = new Color(240, 178, 50);
    public static final Color DM_DOT_BLUE = new Color(88, 101, 242);
    public static final Color DM_DOT_PINK = new Color(235, 69, 158);

    // ── Peer avatar colors ──
    public static final Color PEER_AVATAR_BLUE = new Color(88, 101, 242);
    public static final Color PEER_AVATAR_GREEN = new Color(35, 165, 89);
    public static final Color PEER_AVATAR_GOLD = new Color(240, 178, 50);
    public static final Color PEER_AVATAR_PURPLE = new Color(155, 89, 182);

    // ── Aliases ──
    public static final Color SOFT_TEXT = TEXT_SECONDARY;
    public static final Color STRONG_TEXT = TEXT_PRIMARY;

    // ── Admin-specific colors ──
    public static final Color ADMIN_ACCENT = new Color(237, 66, 69);       // Admin red
    public static final Color ADMIN_HEADER_BG = new Color(35, 36, 40);     // Darker header
    public static final Color MUTED_USER_BG = new Color(60, 45, 45);       // Subtle red tint for muted users

    // ── Font family names (with cross-platform fallback) ──
    private static final String HEADING_FAMILY = selectAvailableFont(
            "Bahnschrift", "SF Pro Display", "Helvetica Neue", "Segoe UI", "SansSerif"
    );
    private static final String BODY_FAMILY = selectAvailableFont(
            "Segoe UI", "SF Pro Text", ".SF NS Text", "Helvetica Neue", "SansSerif"
    );

    protected AppTheme() {
    }

    public static Font heading(int size) {
        return new Font(HEADING_FAMILY, Font.BOLD, size);
    }

    public static Font body(int style, int size) {
        return new Font(BODY_FAMILY, style, size);
    }

    /**
     * Blend two colors by a given ratio (0.0 = pure start, 1.0 = pure end).
     */
    public static Color blend(Color start, Color end, float ratio) {
        float clamped = Math.max(0f, Math.min(1f, ratio));
        int red = (int) (start.getRed() + (end.getRed() - start.getRed()) * clamped);
        int green = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * clamped);
        int blue = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * clamped);
        return new Color(red, green, blue);
    }

    /**
     * Select the first available font from a list of candidates.
     */
    private static String selectAvailableFont(String... candidates) {
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>(java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String candidate : candidates) {
            if (available.contains(candidate)) {
                return candidate;
            }
        }
        return candidates[candidates.length - 1]; // fallback to last (generic) name
    }
}
