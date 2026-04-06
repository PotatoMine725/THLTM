package com.wifichat;

import com.wifichat.auth.AuthSession;
import com.wifichat.auth.LoginDialog;
import com.wifichat.auth.SessionStore;
import com.wifichat.config.AppConfig;
import com.wifichat.network.ChatNode;
import com.wifichat.shared.TransportMode;
import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.tcp.TcpChatClient;
import com.wifichat.tcp.TcpClientException;
import com.wifichat.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromArgs(args);
        if (config.transportMode() == TransportMode.HYBRID) {
            runHybrid(config);
            return;
        }
        runUdpOnly(config);
    }

    private static void runUdpOnly(AppConfig config) {
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

    private static void runHybrid(AppConfig config) {
        SessionStore sessionStore = new SessionStore(config.profileName());
        launchHybridSession(config, sessionStore, false);
    }

    private static void launchHybridSession(AppConfig config, SessionStore sessionStore, boolean forceLogin) {
        Thread launcher = new Thread(() -> {
            TcpChatClient tcpClient = new TcpChatClient(config.serverHost(), config.serverPort());
            try {
                tcpClient.connect();
            } catch (Exception e) {
                System.err.println("Cannot connect TCP server " + config.serverHost() + ":" + config.serverPort() + " -> " + e.getMessage());
                JOptionPane.showMessageDialog(null,
                        "Cannot connect TCP server " + config.serverHost() + ":" + config.serverPort() + "\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            AuthSession session = forceLogin ? null : tryResume(sessionStore.load(), tcpClient);
            if (session == null) {
                session = promptLogin(tcpClient);
                if (session == null) {
                    tcpClient.close();
                    return;
                }
            }

            sessionStore.save(session);
            final AuthSession finalSession = session;

            SwingUtilities.invokeLater(() -> {
                ChatNode node = new ChatNode(config, finalSession.userId, finalSession.displayName, finalSession.sessionToken, tcpClient);
                final MainFrame[] frameRef = new MainFrame[1];

                Runnable logoutAction = () -> {
                    sessionStore.clear();
                    node.stop();
                    MainFrame current = frameRef[0];
                    if (current != null) {
                        current.dispose();
                    }

                    Thread relaunch = new Thread(() -> launchHybridSession(config, sessionStore, true), "hybrid-relogin");
                    relaunch.start();
                };

                MainFrame frame = new MainFrame(config, node, logoutAction);
                frameRef[0] = frame;
                node.setListener(frame);
                frame.startNodeSafely();
                frame.setVisible(true);
            });
        }, "hybrid-launch");
        launcher.start();
    }

    private static AuthSession tryResume(AuthSession cached, TcpChatClient tcpClient) {
        if (cached == null || cached.sessionToken == null || cached.sessionToken.isBlank() || cached.isExpired()) {
            return null;
        }

        try {
            AuthResponse response = tcpClient.resumeSession(cached.sessionToken);
            AuthSession session = new AuthSession();
            session.sessionToken = response.sessionToken;
            session.userId = response.userId;
            session.username = response.username;
            session.displayName = response.displayName;
            session.expiresAt = response.expiresAt;
            return session;
        } catch (TcpClientException e) {
            return null;
        }
    }

    private static AuthSession promptLogin(TcpChatClient tcpClient) {
        final AuthSession[] holder = new AuthSession[1];
        try {
            SwingUtilities.invokeAndWait(() -> holder[0] = LoginDialog.showDialog(tcpClient));
        } catch (Exception e) {
            System.err.println("Login dialog failed: " + e.getMessage());
            return null;
        }
        return holder[0];
    }
}
