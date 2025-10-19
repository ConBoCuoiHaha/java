package com.eduactivity.ui;

import com.eduactivity.model.User;
import com.eduactivity.service.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField emailField = new JTextField("student@socketir.com");
    private final JPasswordField passwordField = new JPasswordField("Student123!");
    private final JButton loginBtn = new JButton("Đăng nhập");
    private final JLabel statusLabel = new JLabel(" ");
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        setTitle("EduActivity - Đăng nhập");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 240);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("EduActivity");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; panel.add(title, c);

        c.gridwidth = 1;
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Email"), c);
        c.gridx = 1; panel.add(emailField, c);

        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Mật khẩu"), c);
        c.gridx = 1; panel.add(passwordField, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; panel.add(statusLabel, c);
        statusLabel.setForeground(Color.DARK_GRAY);

        c.gridy = 4; panel.add(loginBtn, c);
        loginBtn.addActionListener(e -> doLogin());

        setContentPane(panel);
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String pwd = new String(passwordField.getPassword());
        authService.login(email, pwd).ifPresentOrElse(this::openMain, () -> {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Sai email hoặc mật khẩu");
        });
    }

    private void openMain(User u) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame(u).setVisible(true);
            dispose();
        });
    }
}

