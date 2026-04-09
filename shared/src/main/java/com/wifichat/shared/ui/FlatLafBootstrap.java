package com.wifichat.shared.ui;

import java.awt.Color;

/**
 * Centralized FlatLaf Dark Look-and-Feel bootstrap.
 * Call {@link #setup()} once at application startup before any Swing component is created.
 */
public final class FlatLafBootstrap {

    private FlatLafBootstrap() {
    }

    /**
     * Initialize FlatLaf Dark theme with custom UI defaults shared across
     * client and admin modules.
     */
    public static void setup() {
        try {
            com.formdev.flatlaf.FlatDarkLaf.setup();
            javax.swing.UIManager.put("Button.arc", 6);
            javax.swing.UIManager.put("Component.arc", 6);
            javax.swing.UIManager.put("CheckBox.arc", 6);
            javax.swing.UIManager.put("ProgressBar.arc", 6);
            javax.swing.UIManager.put("TextComponent.arc", 8);
            javax.swing.UIManager.put("ScrollBar.showButtons", false);
            javax.swing.UIManager.put("ScrollBar.width", 8);
            javax.swing.UIManager.put("ScrollBar.thumbArc", 8);
            javax.swing.UIManager.put("Component.focusWidth", 0);
            javax.swing.UIManager.put("Component.innerFocusWidth", 0);
            javax.swing.UIManager.put("TabbedPane.showTabSeparators", true);
            javax.swing.UIManager.put("SplitPane.dividerSize", 3);
            javax.swing.UIManager.put("SplitPaneDivider.draggingColor", new Color(63, 65, 71));
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf: " + e.getMessage());
        }
    }
}
