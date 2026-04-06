package com.wifichat;

import com.wifichat.config.AppConfig;
import com.wifichat.network.ChatNode;
import com.wifichat.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromArgs(args);
        String userName = config.userName();
        if (userName == null || userName.isBlank()) {
            userName = JOptionPane.showInputDialog(null, "Enter your display name:", "WiFi Chat", JOptionPane.PLAIN_MESSAGE);
        }

        if (userName == null || userName.isBlank()) {
            System.err.println("Display name is required.");
            return;
        }

        final String finalUserName = userName.trim();
        SwingUtilities.invokeLater(() -> {
            ChatNode node = new ChatNode(config, finalUserName);
            MainFrame frame = new MainFrame(config, node);
            node.setListener(frame);
            frame.startNodeSafely();
            frame.setVisible(true);
        });
    }
}

