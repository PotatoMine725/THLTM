package com.wifichat.auth;

import com.wifichat.shared.dto.AuthResponse;
import com.wifichat.tcp.TcpChatClient;
import com.wifichat.tcp.TcpClientException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

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
        setTitle("Login / Register");
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(new Dimension(420, 280));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Login", buildLoginPanel());
        tabs.add("Register", buildRegisterPanel());

        add(tabs, BorderLayout.CENTER);
    }

    public static AuthSession showDialog(TcpChatClient tcpClient) {
        LoginDialog dialog = new LoginDialog(tcpClient);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        loginUser = new JTextField();
        loginPassword = new JPasswordField();

        form.add(new JLabel("Username"));
        form.add(loginUser);
        form.add(new JLabel("Password"));
        form.add(loginPassword);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> doLogin());

        panel.add(form, BorderLayout.CENTER);
        panel.add(loginButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        registerUser = new JTextField();
        registerPassword = new JPasswordField();
        registerDisplay = new JTextField();

        form.add(new JLabel("Username"));
        form.add(registerUser);
        form.add(new JLabel("Password"));
        form.add(registerPassword);
        form.add(new JLabel("Display Name"));
        form.add(registerDisplay);

        JButton registerButton = new JButton("Create Account");
        registerButton.addActionListener(e -> doRegister());

        panel.add(form, BorderLayout.CENTER);
        panel.add(registerButton, BorderLayout.SOUTH);
        return panel;
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
