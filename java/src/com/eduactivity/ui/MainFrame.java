package com.eduactivity.ui;

import com.eduactivity.model.User;
import com.eduactivity.model.UserRole;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final User currentUser;

    public MainFrame(User user) {
        this.currentUser = user;
        setTitle("EduActivity - " + user.getFullName() + " (" + user.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Thông tin", createProfilePanel());

        switch (user.getRole()) {
            case Student -> {
                tabs.addTab("Hoạt động", new StudentPanel(user));
                tabs.addTab("Lịch/Thống kê", new CalendarStatsPanel());
            }
            case Teacher -> {
                tabs.addTab("Quản lý hoạt động", new TeacherPanel(user));
                tabs.addTab("Lịch/Thống kê", new CalendarStatsPanel());
            }
            case Admin -> {
                tabs.addTab("Quản trị", new AdminPanel());
                tabs.addTab("Lịch/Thống kê", new CalendarStatsPanel());
            }
        }

        // Top bar with Logout
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("EduActivity");
        title.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        JButton logoutBtn = new JButton("Đăng xuất");
        logoutBtn.addActionListener(e -> onLogout());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.add(new JLabel(user.getFullName() + " (" + user.getRole() + ")"));
        right.add(logoutBtn);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(right, BorderLayout.EAST);

        JPanel root = new JPanel(new BorderLayout());
        root.add(topBar, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel createProfilePanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.anchor = GridBagConstraints.WEST;
        int row = 0;

        p.add(new JLabel("Họ tên:"), gbc(c,0,row));
        p.add(new JLabel(currentUser.getFullName()), gbc(c,1,row++));
        p.add(new JLabel("Email:"), gbc(c,0,row));
        p.add(new JLabel(currentUser.getEmail()), gbc(c,1,row++));
        p.add(new JLabel("Vai trò:"), gbc(c,0,row));
        p.add(new JLabel(currentUser.getRole().name()), gbc(c,1,row++));
        if (currentUser.getRole() == UserRole.Student) {
            p.add(new JLabel("Lớp:"), gbc(c,0,row));
            p.add(new JLabel(currentUser.getClassName() == null ? "-" : currentUser.getClassName()), gbc(c,1,row++));
        }
        if (currentUser.getRole() == UserRole.Teacher) {
            p.add(new JLabel("Bộ môn:"), gbc(c,0,row));
            p.add(new JLabel(currentUser.getDepartment() == null ? "-" : currentUser.getDepartment()), gbc(c,1,row++));
        }

        return p;
    }

    private GridBagConstraints gbc(GridBagConstraints base, int x, int y) {
        GridBagConstraints c = (GridBagConstraints) base.clone();
        c.gridx = x; c.gridy = y; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = x==1?1:0;
        return c;
    }

    private void onLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn đăng xuất?", "Xác nhận", JOptionPane.OK_CANCEL_OPTION);
        if (confirm == JOptionPane.OK_OPTION) {
            SwingUtilities.invokeLater(() -> {
                new LoginFrame().setVisible(true);
                dispose();
            });
        }
    }
}
