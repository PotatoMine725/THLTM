package com.wifichat.admin;

import com.wifichat.admin.auth.AuthSession;
import com.wifichat.admin.auth.LoginDialog;
import com.wifichat.admin.auth.SessionStore;
import com.wifichat.admin.config.AdminConfig;
import com.wifichat.admin.tcp.AdminTcpClient;
import com.wifichat.admin.tcp.AdminTcpException;
import com.wifichat.admin.ui.AdminFrame;
import com.wifichat.shared.dto.AuthResponse;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class AdminMain {
    private AdminMain() {
    }

    public static void main(String[] args) {
        com.wifichat.shared.ui.FlatLafBootstrap.setup();

        AdminConfig config = AdminConfig.fromArgs(args);
        SessionStore store = new SessionStore(config.profileName());
        launch(config, store, false);
    }

    private static void launch(AdminConfig config, SessionStore store, boolean forceLogin) {
        Thread launcher = new Thread(() -> {
            AdminTcpClient tcpClient = new AdminTcpClient(config.serverHost(), config.serverPort());
            try {
                tcpClient.connect();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Cannot connect TCP server " + config.serverHost() + ":" + config.serverPort() + "\n" + e.getMessage(),
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            AuthSession session = forceLogin ? null : tryResume(store.load(), tcpClient);
            if (session == null) {
                session = promptLogin(tcpClient);
                if (session == null) {
                    tcpClient.close();
                    return;
                }
            }

            store.save(session);
            AuthSession finalSession = session;

            SwingUtilities.invokeLater(() -> {
                final AdminFrame[] frameRef = new AdminFrame[1];

                Runnable logout = () -> {
                    store.clear();
                    tcpClient.close();
                    if (frameRef[0] != null) {
                        frameRef[0].dispose();
                    }
                    Thread relaunch = new Thread(() -> launch(config, store, true), "admin-relogin");
                    relaunch.start();
                };

                AdminFrame frame = new AdminFrame(tcpClient, finalSession, logout);
                frameRef[0] = frame;
                frame.setVisible(true);
            });
        }, "admin-launch");
        launcher.start();
    }

    private static AuthSession tryResume(AuthSession cached, AdminTcpClient tcpClient) {
        if (cached == null || cached.sessionToken == null || cached.sessionToken.isBlank() || cached.isExpired()) {
            return null;
        }

        try {
            AuthResponse response = tcpClient.resumeSession(cached.sessionToken);
            if (response == null || response.role == null || !"ADMIN".equalsIgnoreCase(response.role)) {
                return null;
            }

            AuthSession session = new AuthSession();
            session.sessionToken = response.sessionToken;
            session.userId = response.userId;
            session.username = response.username;
            session.displayName = response.displayName;
            session.role = response.role;
            session.expiresAt = response.expiresAt;
            return session;
        } catch (AdminTcpException e) {
            return null;
        }
    }

    private static AuthSession promptLogin(AdminTcpClient tcpClient) {
        final AuthSession[] holder = new AuthSession[1];
        try {
            SwingUtilities.invokeAndWait(() -> holder[0] = LoginDialog.showDialog(tcpClient));
        } catch (Exception e) {
            return null;
        }
        return holder[0];
    }
}

