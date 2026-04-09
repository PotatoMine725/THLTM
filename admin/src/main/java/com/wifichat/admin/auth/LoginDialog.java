package com.wifichat.admin.auth;

import com.wifichat.admin.tcp.AdminTcpClient;
import com.wifichat.admin.tcp.AdminTcpException;
import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.shared.ui.AppTheme;
import com.wifichat.shared.ui.RoundedBorder;
import com.wifichat.shared.ui.UIHelper;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

public class LoginDialog extends JDialog {
    private final AdminTcpClient tcpClient;
    private AuthSession result;

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginDialog(AdminTcpClient tcpClient) {
        this.tcpClient = tcpClient;
        setTitle("Admin Login");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        getContentPane().setBackground(AppTheme.WINDOW_BG);

        add(buildPanel(), BorderLayout.CENTER);
        pack();
        setMinimumSize(new Dimension(480, 390));
        setLocationRelativeTo(null);
    }

    public static AuthSession showDialog(AdminTcpClient tcpClient) {
        LoginDialog dialog = new LoginDialog(tcpClient);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(AppTheme.WINDOW_BG);
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // ── Header with admin badge ──
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("WiFi Chat Admin");
        titleLabel.setFont(AppTheme.heading(22));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel badgeLabel = new JLabel("ADMINISTRATOR ACCESS", SwingConstants.CENTER);
        badgeLabel.setFont(AppTheme.body(Font.BOLD, 11));
        badgeLabel.setForeground(AppTheme.ADMIN_ACCENT);
        badgeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel hintLabel = new JLabel("Sign in with an admin account to manage the server");
        hintLabel.setFont(AppTheme.body(Font.PLAIN, 12));
        hintLabel.setForeground(AppTheme.TEXT_MUTED);
        hintLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(badgeLabel);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(hintLabel);

        // ── Form ──
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(AppTheme.body(Font.BOLD, 12));
        userLabel.setForeground(AppTheme.TEXT_SECONDARY);
        userLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(userLabel);
        form.add(Box.createVerticalStrut(4));

        usernameField = new JTextField();
        styleInput(usernameField);
        form.add(usernameField);
        form.add(Box.createVerticalStrut(10));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(AppTheme.body(Font.BOLD, 12));
        passLabel.setForeground(AppTheme.TEXT_SECONDARY);
        passLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(passLabel);
        form.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField();
        styleInput(passwordField);
        form.add(passwordField);

        // ── Login Button ──
        JButton loginButton = new JButton("Sign In");
        UIHelper.styleButton(loginButton, AppTheme.ADMIN_ACCENT, AppTheme.ADMIN_ACCENT.brighter(), AppTheme.TEXT_PRIMARY, 10, 10, 20);
        loginButton.setFont(AppTheme.body(Font.BOLD, 14));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        loginButton.addActionListener(e -> doLogin());

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        buttonPanel.add(loginButton, BorderLayout.CENTER);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getViewport().setOpaque(false);
        formScroll.getViewport().setBackground(AppTheme.WINDOW_BG);
        formScroll.setOpaque(false);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(formScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void styleInput(JTextField field) {
        field.setFont(AppTheme.body(Font.PLAIN, 14));
        field.setBackground(AppTheme.ITEM_BG);
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setCaretColor(AppTheme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(AppTheme.BORDER_SUBTLE, 8, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        field.setColumns(20);
        field.setPreferredSize(new Dimension(320, 44));
        field.setMinimumSize(new Dimension(220, 44));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        field.setAlignmentX(LEFT_ALIGNMENT);
    }

    private void doLogin() {
        try {
            AuthResponse auth = tcpClient.login(usernameField.getText(), new String(passwordField.getPassword()));
            if (auth == null || auth.role == null || !"ADMIN".equalsIgnoreCase(auth.role)) {
                JOptionPane.showMessageDialog(this, "This account is not ADMIN.", "Access denied", JOptionPane.ERROR_MESSAGE);
                return;
            }

            AuthSession session = new AuthSession();
            session.sessionToken = auth.sessionToken;
            session.userId = auth.userId;
            session.username = auth.username;
            session.displayName = auth.displayName;
            session.role = auth.role;
            session.expiresAt = auth.expiresAt;
            result = session;
            dispose();
        } catch (AdminTcpException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}

