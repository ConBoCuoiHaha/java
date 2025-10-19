package com.eduactivity.ui;

import com.eduactivity.dao.UserDao;
import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.Activity;
import com.eduactivity.model.ActivityStatus;
import com.eduactivity.model.User;
import com.eduactivity.store.DataStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPanel extends JPanel {
    private final DefaultTableModel usersModel = new DefaultTableModel(new Object[]{"ID","Họ tên","Email","Vai trò"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable usersTable = new JTable(usersModel);

    private final DefaultTableModel activityModel = new DefaultTableModel(new Object[]{"ID","Tiêu đề","SL","Trạng thái"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable activityTable = new JTable(activityModel);
    private final JComboBox<String> actStatusFilter = new JComboBox<>(new String[]{"All","Open","Full","Closed","Cancelled","Completed"});

    private final UserDao userDao = new UserDao();
    // Stats labels
    private final JLabel lblTotalUsers = new JLabel("0");
    private final JLabel lblStudents = new JLabel("0");
    private final JLabel lblTeachers = new JLabel("0");
    private final JLabel lblAdmins = new JLabel("0");
    private final JLabel lblTotalActivities = new JLabel("0");
    private final JLabel lblActiveActivities = new JLabel("0");
    private final JLabel lblTotalRegs = new JLabel("0");
    private final JLabel lblPendingRegs = new JLabel("0");

    public AdminPanel() {
        setLayout(new BorderLayout());
        // Top stats
        JPanel stats = new JPanel(new GridLayout(0,4,12,6));
        stats.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        stats.add(new JLabel("Tổng người dùng:")); stats.add(lblTotalUsers);
        stats.add(new JLabel("Học sinh:")); stats.add(lblStudents);
        stats.add(new JLabel("Giáo viên:")); stats.add(lblTeachers);
        stats.add(new JLabel("Admin:")); stats.add(lblAdmins);
        stats.add(new JLabel("Tổng hoạt động:")); stats.add(lblTotalActivities);
        stats.add(new JLabel("Đang mở:")); stats.add(lblActiveActivities);
        stats.add(new JLabel("Tổng đăng ký:")); stats.add(lblTotalRegs);
        stats.add(new JLabel("Chờ duyệt:")); stats.add(lblPendingRegs);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);

        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.add(new JLabel("Người dùng"), BorderLayout.NORTH);
        usersPanel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        // Context menu / double-click to edit user
        JPopupMenu userMenu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Sua nguoi dung");
        miEdit.addActionListener(e -> onEditUser());
        userMenu.add(miEdit);
        usersTable.setComponentPopupMenu(userMenu);
        usersTable.addMouseListener(new java.awt.event.MouseAdapter(){
            public void mouseClicked(java.awt.event.MouseEvent e){
                if (e.getClickCount()==2 && SwingUtilities.isLeftMouseButton(e)) onEditUser();
            }
        });

        JPanel actPanel = new JPanel(new BorderLayout());
        actPanel.add(new JLabel("Hoạt động"), BorderLayout.NORTH);
        JPanel actCenter = new JPanel(new BorderLayout());
        JPanel actFilters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actFilters.add(new JLabel("Trang thai:")); actFilters.add(actStatusFilter);
        JButton applyActFilter = new JButton("Loc"); applyActFilter.addActionListener(e -> refresh());
        actFilters.add(applyActFilter);
        actCenter.add(actFilters, BorderLayout.NORTH);
        actCenter.add(new JScrollPane(activityTable), BorderLayout.CENTER);
        actPanel.add(actCenter, BorderLayout.CENTER);
        JButton deleteAct = new JButton("Xóa hoạt động");
        deleteAct.addActionListener(e -> onDeleteActivity());
        actPanel.add(deleteAct, BorderLayout.SOUTH);

        split.setTopComponent(usersPanel);
        split.setBottomComponent(actPanel);
        add(stats, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton createUserBtn = new JButton("Tạo người dùng");
        createUserBtn.addActionListener(e -> onCreateUser());
        JButton reload = new JButton("Tải lại");
        reload.addActionListener(e -> refresh());
        JButton toggleLock = new JButton("Khoá/Mở khoá");
        toggleLock.addActionListener(e -> onToggleLock());
        bottomBar.add(createUserBtn);
        bottomBar.add(toggleLock);
        bottomBar.add(reload);
        add(bottomBar, BorderLayout.SOUTH);

        refresh();
    }

    private void refresh() {
        usersModel.setRowCount(0);
        List<User> users;
        if (ConnectionManager.isConfigured()) {
            try { users = userDao.listAll(); }
            catch (Exception ex) { users = List.of(); }
        } else {
            users = DataStore.users.values().stream().sorted((a,b)->a.getFullName().compareToIgnoreCase(b.getFullName())).collect(Collectors.toList());
        }
        for (User u: users) {
            usersModel.addRow(new Object[]{u.getId(), u.getFullName(), u.getEmail(), u.getRole().name()});
        }

        activityModel.setRowCount(0);
        var actService = new com.eduactivity.service.ActivityService();
        List<Activity> acts = actService.listAll();
        String st = String.valueOf(actStatusFilter.getSelectedItem());
        if (!"All".equals(st)) {
            acts = acts.stream().filter(a -> a.getStatus().name().equalsIgnoreCase(st)).toList();
        }
        for (Activity a: acts) {
            activityModel.addRow(new Object[]{a.getId(), a.getTitle(), a.getCurrentParticipants()+"/"+a.getMaxParticipants(), a.getStatus().name()});
        }

        // Update stats labels
        lblTotalUsers.setText(String.valueOf(users.size()));
        lblStudents.setText(String.valueOf(users.stream().filter(u->u.getRole().name().equals("Student")).count()));
        lblTeachers.setText(String.valueOf(users.stream().filter(u->u.getRole().name().equals("Teacher")).count()));
        lblAdmins.setText(String.valueOf(users.stream().filter(u->u.getRole().name().equals("Admin")).count()));
        lblTotalActivities.setText(String.valueOf(acts.size()));
        lblActiveActivities.setText(String.valueOf(acts.stream().filter(a->a.getStatus()==ActivityStatus.Open).count()));
        if (ConnectionManager.isConfigured()) {
            try { lblTotalRegs.setText(String.valueOf(new com.eduactivity.dao.RegistrationDao().countAll())); } catch (Exception ignored) { lblTotalRegs.setText("-"); }
            try { lblPendingRegs.setText(String.valueOf(new com.eduactivity.dao.RegistrationDao().countPending())); } catch (Exception ignored) { lblPendingRegs.setText("-"); }
        } else {
            lblTotalRegs.setText("-");
            lblPendingRegs.setText("-");
        }
    }

    private void onToggleLock() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn người dùng"); return; }
        String id = String.valueOf(usersModel.getValueAt(row, 0));
        if (ConnectionManager.isConfigured()) {
            try { userDao.toggleLock(id); JOptionPane.showMessageDialog(this, "Đã cập nhật trạng thái khoá"); refresh(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
        } else {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ với DB SQL Server");
        }
    }

    private void onDeleteActivity() {
        int row = activityTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn hoạt động"); return; }
        int id = (Integer) activityModel.getValueAt(row, 0);
        if (ConnectionManager.isConfigured()) {
            try {
                new com.eduactivity.dao.ActivityDao().deleteActivityAndRegistrations(id);
                JOptionPane.showMessageDialog(this, "Đã xóa hoạt động #"+id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ với DB SQL Server");
        }
    }

    private void onCreateUser() {
        JTextField fullName = new JTextField();
        JTextField email = new JTextField();
        JPasswordField password = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[]{"Student","Teacher","Admin"});
        JTextField className = new JTextField();
        JTextField department = new JTextField();

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Họ tên")); p.add(fullName);
        p.add(new JLabel("Email")); p.add(email);
        p.add(new JLabel("Mật khẩu")); p.add(password);
        p.add(new JLabel("Vai trò")); p.add(role);
        p.add(new JLabel("Lớp (Student)")); p.add(className);
        p.add(new JLabel("Bộ môn (Teacher)")); p.add(department);

        int opt = JOptionPane.showConfirmDialog(this, p, "Tạo người dùng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            if (!com.eduactivity.db.ConnectionManager.isConfigured()) {
                JOptionPane.showMessageDialog(this, "Chỉ hỗ trợ khi kết nối SQL Server");
                return;
            }
            try {
                String id = userDao.createUser(fullName.getText().trim(), email.getText().trim(), new String(password.getPassword()), (String) role.getSelectedItem(), className.getText().trim(), department.getText().trim());
                JOptionPane.showMessageDialog(this, "Đã tạo người dùng #"+id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditUser() {
        int row = usersTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Ch?n ngu?i d�ng"); return; }
        String id = String.valueOf(usersModel.getValueAt(row, 0));

        String fullName = String.valueOf(usersModel.getValueAt(row, 1));
        String email = String.valueOf(usersModel.getValueAt(row, 2));
        String role = String.valueOf(usersModel.getValueAt(row, 3));
        String className = "";
        String department = "";
        if (ConnectionManager.isConfigured()) {
            try {
                var u = userDao.getById(id);
                if (u != null) {
                    if (u.getFullName()!=null) fullName = u.getFullName();
                    if (u.getEmail()!=null) email = u.getEmail();
                    if (u.getRole()!=null) role = u.getRole().name();
                    if (u.getClassName()!=null) className = u.getClassName();
                    if (u.getDepartment()!=null) department = u.getDepartment();
                }
            } catch (Exception ignored) {}
        }

        JTextField txtFullName = new JTextField(fullName);
        JTextField txtEmail = new JTextField(email);
        JComboBox<String> cbRole = new JComboBox<>(new String[]{"Student","Teacher","Admin"});
        cbRole.setSelectedItem(role);
        JTextField txtClass = new JTextField(className);
        JTextField txtDept = new JTextField(department);

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("H? t�n")); p.add(txtFullName);
        p.add(new JLabel("Email")); p.add(txtEmail);
        p.add(new JLabel("Vai tr�")); p.add(cbRole);
        p.add(new JLabel("L?p (Student)")); p.add(txtClass);
        p.add(new JLabel("B? m�n (Teacher)")); p.add(txtDept);

        int opt = JOptionPane.showConfirmDialog(this, p, "S?a ngu?i d�ng", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            if (!ConnectionManager.isConfigured()) {
                JOptionPane.showMessageDialog(this, "Ch? h? tr? khi k?t n?i SQL Server");
                return;
            }
            try {
                userDao.updateUser(id, txtFullName.getText().trim(), txtEmail.getText().trim(), (String) cbRole.getSelectedItem(), txtClass.getText().trim(), txtDept.getText().trim());
                JOptionPane.showMessageDialog(this, "Da c?p nh?t ngu?i d�ng");
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "L?i", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
