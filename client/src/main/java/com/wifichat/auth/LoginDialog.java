package com.wifichat.auth;

import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.shared.ui.AppTheme;
import com.wifichat.shared.ui.RoundedBorder;
import com.wifichat.shared.ui.UIHelper;
import com.wifichat.tcp.TcpChatClient;
import com.wifichat.tcp.TcpClientException;

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
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

public class LoginDialog extends JDialog {
    private final TcpChatClient tcpClient;
    private AuthSession result;

    private JTextField loginUser;
    private JPasswordField loginPassword;

    private JTextField registerUser;
    private JPasswordField registerPassword;
    private JTextField registerDisplay;

    public LoginDialog(TcpChatClient tcpClient) {
        this.tcpClient = tcpClient;
        setTitle("WiFi Chat Login");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
        getContentPane().setBackground(AppTheme.WINDOW_BG);
        add(buildRoot(), BorderLayout.CENTER);
        pack();
        setMinimumSize(new Dimension(500, 420));
        setLocationRelativeTo(null);
    }

    public static AuthSession showDialog(TcpChatClient tcpClient) {
        LoginDialog dialog = new LoginDialog(tcpClient);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(AppTheme.WINDOW_BG);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("WiFi Chat");
        title.setFont(AppTheme.heading(24));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in or create an account");
        subtitle.setFont(AppTheme.body(Font.PLAIN, 12));
        subtitle.setForeground(AppTheme.TEXT_MUTED);
        subtitle.setAlignmentX(JLabel.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(AppTheme.WINDOW_BG);
        tabs.setForeground(AppTheme.TEXT_PRIMARY);
        tabs.putClientProperty("JTabbedPane.tabAreaInsets", new Insets(6, 6, 0, 6));
        tabs.putClientProperty("JTabbedPane.tabHeight", 34);
        tabs.putClientProperty("JTabbedPane.showTabSeparators", true);

        tabs.add("Login", buildLoginPanel());
        tabs.add("Register", buildRegisterPanel());
        return tabs;
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.WINDOW_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(fieldLabel("Username"));
        panel.add(Box.createVerticalStrut(4));
        loginUser = new JTextField();
        styleInput(loginUser);
        panel.add(loginUser);
        panel.add(Box.createVerticalStrut(10));

        panel.add(fieldLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        loginPassword = new JPasswordField();
        styleInput(loginPassword);
        panel.add(loginPassword);
        panel.add(Box.createVerticalStrut(14));

        JButton loginButton = new JButton("Login");
        UIHelper.styleButton(loginButton, AppTheme.PRIMARY_BUTTON, AppTheme.PRIMARY_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 9, 16);
        loginButton.setFont(AppTheme.body(Font.BOLD, 14));
        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginButton.addActionListener(e -> doLogin());
        panel.add(loginButton);
        panel.add(Box.createVerticalGlue());

        return wrapScrollable(panel);
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(AppTheme.WINDOW_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(fieldLabel("Username"));
        panel.add(Box.createVerticalStrut(4));
        registerUser = new JTextField();
        styleInput(registerUser);
        panel.add(registerUser);
        panel.add(Box.createVerticalStrut(10));

        panel.add(fieldLabel("Password"));
        panel.add(Box.createVerticalStrut(4));
        registerPassword = new JPasswordField();
        styleInput(registerPassword);
        panel.add(registerPassword);
        panel.add(Box.createVerticalStrut(10));

        panel.add(fieldLabel("Display Name"));
        panel.add(Box.createVerticalStrut(4));
        registerDisplay = new JTextField();
        styleInput(registerDisplay);
        panel.add(registerDisplay);
        panel.add(Box.createVerticalStrut(14));

        JButton registerButton = new JButton("Create Account");
        UIHelper.styleButton(registerButton, AppTheme.NEUTRAL_BUTTON, AppTheme.BORDER_STRONG, AppTheme.TEXT_PRIMARY, 10, 9, 16);
        registerButton.setFont(AppTheme.body(Font.BOLD, 14));
        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        registerButton.addActionListener(e -> doRegister());
        panel.add(registerButton);
        panel.add(Box.createVerticalGlue());

        return wrapScrollable(panel);
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(AppTheme.body(Font.BOLD, 12));
        label.setForeground(AppTheme.TEXT_SECONDARY);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
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

    private JPanel wrapScrollable(JPanel content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(AppTheme.WINDOW_BG);
        scrollPane.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private void doLogin() {
        try {
            AuthResponse auth = tcpClient.login(loginUser.getText(), new String(loginPassword.getPassword()));
            result = toSession(auth);
            dispose();
        } catch (TcpClientException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        try {
            AuthResponse auth = tcpClient.register(
                    registerUser.getText(),
                    new String(registerPassword.getPassword()),
                    registerDisplay.getText()
            );
            result = toSession(auth);
            dispose();
        } catch (TcpClientException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Register Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private AuthSession toSession(AuthResponse auth) {
        AuthSession session = new AuthSession();
        session.sessionToken = auth.sessionToken;
        session.userId = auth.userId;
        session.username = auth.username;
        session.displayName = auth.displayName;
        session.expiresAt = auth.expiresAt;
        return session;
    }
}
