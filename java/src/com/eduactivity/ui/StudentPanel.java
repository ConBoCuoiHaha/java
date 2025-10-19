package com.eduactivity.ui;

import com.eduactivity.model.Activity;
import com.eduactivity.model.ActivityStatus;
import com.eduactivity.model.Registration;
import com.eduactivity.model.User;
import com.eduactivity.service.ActivityService;
import com.eduactivity.service.RegistrationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StudentPanel extends JPanel {
    private final User currentUser;
    private final ActivityService activityService = new ActivityService();
    private final RegistrationService registrationService = new RegistrationService();

    private final DefaultTableModel activitiesModel = new DefaultTableModel(new Object[]{"ID","Tiêu đề","Bắt đầu","Địa điểm","SL"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable activitiesTable = new JTable(activitiesModel);

    private final DefaultTableModel myRegsModel = new DefaultTableModel(new Object[]{"Mã","Hoạt động","Trạng thái","Thời gian"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable myRegsTable = new JTable(myRegsModel);

    // Filters for activities
    private final JTextField searchField = new JTextField(16);
    private final JTextField categoryField = new JTextField(10);
    private final JCheckBox onlyAvailableCheck = new JCheckBox("Loc con cho");
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All","Open","Full","Closed","Cancelled","Completed"});

    public StudentPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.add(new JLabel("Danh sách hoạt động sắp tới"), BorderLayout.WEST);
        JButton refreshBtn = new JButton("Tải lại");
        refreshBtn.addActionListener(e -> refresh());
        top.add(refreshBtn, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.6);

        JPanel activitiesPanel = new JPanel(new BorderLayout());
        // Filters panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchField.setToolTipText("Tim kiem tieu de/mo ta");
        categoryField.setToolTipText("Danh muc");
        JButton applyFilterBtn = new JButton("Loc");
        applyFilterBtn.addActionListener(e -> loadActivities());
        filterPanel.add(new JLabel("Tim:")); filterPanel.add(searchField);
        filterPanel.add(new JLabel("Danh muc:")); filterPanel.add(categoryField);
        filterPanel.add(new JLabel("Trang thai:")); filterPanel.add(statusFilter);
        filterPanel.add(onlyAvailableCheck);
        filterPanel.add(applyFilterBtn);
        activitiesPanel.add(filterPanel, BorderLayout.NORTH);
        activitiesPanel.add(new JScrollPane(activitiesTable), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton detailBtn = new JButton("Chi tiết");
        detailBtn.addActionListener(e -> onDetails());
        JButton registerBtn = new JButton("Đăng ký");
        registerBtn.addActionListener(e -> onRegister());
        actions.add(detailBtn); actions.add(registerBtn);
        JButton detail2Btn = new JButton("Detail+");
        detail2Btn.addActionListener(e -> onDetailsDialog());
        actions.add(detail2Btn);
        activitiesPanel.add(actions, BorderLayout.SOUTH);

        JPanel regsPanel = new JPanel(new BorderLayout());
        regsPanel.add(new JLabel("Đăng ký của tôi"), BorderLayout.NORTH);
        regsPanel.add(new JScrollPane(myRegsTable), BorderLayout.CENTER);
        JButton cancelRegBtn = new JButton("Hủy đăng ký");
        cancelRegBtn.addActionListener(e -> onCancelRegistration());
        regsPanel.add(cancelRegBtn, BorderLayout.SOUTH);

        split.setTopComponent(activitiesPanel);
        split.setBottomComponent(regsPanel);
        add(split, BorderLayout.CENTER);

        refresh();
    }

    private void refresh() {
        loadActivities();
        loadMyRegistrations();
    }

    private void loadActivities() {
        activitiesModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Activity> list = activityService.listAll();
        String q = searchField.getText().trim().toLowerCase();
        String cat = categoryField.getText().trim().toLowerCase();
        boolean onlyAvail = onlyAvailableCheck.isSelected();
        String st = String.valueOf(statusFilter.getSelectedItem());
        for (Activity a : list) {
            boolean ok = true;
            if (!q.isEmpty()) ok &= (a.getTitle()!=null && a.getTitle().toLowerCase().contains(q)) || (a.getDescription()!=null && a.getDescription().toLowerCase().contains(q));
            if (!cat.isEmpty()) ok &= (a.getCategory()!=null && a.getCategory().toLowerCase().contains(cat));
            if (onlyAvail) ok &= a.getCurrentParticipants() < a.getMaxParticipants();
            if (!"All".equals(st)) ok &= a.getStatus().name().equalsIgnoreCase(st);
            if (ok) activitiesModel.addRow(new Object[]{a.getId(), a.getTitle(), fmt.format(a.getStartTime()), a.getLocation(), a.getCurrentParticipants()+"/"+a.getMaxParticipants()});
        }
    }

    private void loadMyRegistrations() {
        myRegsModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Registration> regs = registrationService.myRegistrations(currentUser.getId());
        for (Registration r : regs) {
            Activity a = activityService.getById(r.getActivityId()).orElse(null);
            String title = a == null ? "?" : a.getTitle();
            myRegsModel.addRow(new Object[]{r.getId(), title, r.getStatus().name(), fmt.format(r.getRegistrationTime())});
        }
    }

    private void onRegister() {
        int row = activitiesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một hoạt động", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) activitiesModel.getValueAt(row, 0);
        try {
            registrationService.register(currentUser.getId(), id, null);
            JOptionPane.showMessageDialog(this, "Đăng ký thành công", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelRegistration() {
        int row = myRegsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn đăng ký"); return; }
        int regId = (Integer) myRegsModel.getValueAt(row, 0);
        try {
            registrationService.cancel(currentUser.getId(), regId);
            JOptionPane.showMessageDialog(this, "Đã hủy đăng ký #"+regId);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDetails() {
        int row = activitiesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một hoạt động", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) activitiesModel.getValueAt(row, 0);
        activityService.getById(id).ifPresentOrElse(a -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            JPanel p = new JPanel(new GridLayout(0,1,4,4));
            p.add(new JLabel("Tiêu đề: " + a.getTitle()));
            p.add(new JLabel("Mô tả: " + (a.getDescription()==null?"":a.getDescription())));
            p.add(new JLabel("Bắt đầu: " + fmt.format(a.getStartTime())));
            p.add(new JLabel("Kết thúc: " + fmt.format(a.getEndTime())));
            p.add(new JLabel("Địa điểm: " + a.getLocation()));
            p.add(new JLabel("Danh mục: " + (a.getCategory()==null?"":a.getCategory())));
            p.add(new JLabel("Số lượng: " + a.getCurrentParticipants()+"/"+a.getMaxParticipants()));
            p.add(new JLabel("Yêu cầu phê duyệt: " + (a.isRequireApproval()?"Có":"Không")));
            JOptionPane.showMessageDialog(this, p, "Chi tiết hoạt động #"+id, JOptionPane.INFORMATION_MESSAGE);
        }, () -> JOptionPane.showMessageDialog(this, "Không tìm thấy hoạt động", "Lỗi", JOptionPane.ERROR_MESSAGE));
    }

    // Enhanced details dialog with register/cancel actions
    private void onDetailsDialog() {
        int row = activitiesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an activity", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) activitiesModel.getValueAt(row, 0);
        activityService.getById(id).ifPresentOrElse(a -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            JPanel p = new JPanel(new BorderLayout(8,8));
            JPanel info = new JPanel(new GridLayout(0,1,4,4));
            info.add(new JLabel("Title: " + a.getTitle()));
            info.add(new JLabel("Description: " + (a.getDescription()==null?"":a.getDescription())));
            info.add(new JLabel("Start: " + fmt.format(a.getStartTime())));
            info.add(new JLabel("End: " + fmt.format(a.getEndTime())));
            info.add(new JLabel("Location: " + a.getLocation()));
            info.add(new JLabel("Category: " + (a.getCategory()==null?"":a.getCategory())));
            info.add(new JLabel("Status: " + a.getStatus().name()));
            info.add(new JLabel("Participants: " + a.getCurrentParticipants()+"/"+a.getMaxParticipants()));
            info.add(new JLabel("Require approval: " + (a.isRequireApproval()?"Yes":"No")));
            p.add(info, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnRegister = new JButton("Register");
            JButton btnCancel = new JButton("Cancel registration");
            actions.add(btnCancel); actions.add(btnRegister);
            p.add(actions, BorderLayout.SOUTH);

            boolean isRegistered = registrationService.myRegistrations(currentUser.getId()).stream().anyMatch(r -> r.getActivityId() == a.getId() && r.getStatus() != com.eduactivity.model.RegistrationStatus.Cancelled);
            boolean canRegister = a.getStatus() == ActivityStatus.Open && a.getCurrentParticipants() < a.getMaxParticipants() && !isRegistered;
            btnRegister.setEnabled(canRegister);
            btnCancel.setEnabled(isRegistered);

            btnRegister.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Confirm register?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (confirm == JOptionPane.OK_OPTION) {
                    try { registrationService.register(currentUser.getId(), a.getId(), null); loadActivities(); loadMyRegistrations(); SwingUtilities.getWindowAncestor(p).dispose(); }
                    catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
                }
            });
            btnCancel.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this, "Cancel your registration?", "Confirm", JOptionPane.OK_CANCEL_OPTION);
                if (confirm == JOptionPane.OK_OPTION) {
                    try {
                        var reg = registrationService.myRegistrations(currentUser.getId()).stream().filter(r -> r.getActivityId() == a.getId()).findFirst();
                        if (reg.isPresent()) { registrationService.cancel(currentUser.getId(), reg.get().getId()); loadActivities(); loadMyRegistrations(); SwingUtilities.getWindowAncestor(p).dispose(); }
                    } catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
                }
            });

            JOptionPane.showMessageDialog(this, p, "Activity details #"+id, JOptionPane.PLAIN_MESSAGE);
        }, () -> JOptionPane.showMessageDialog(this, "Activity not found", "Error", JOptionPane.ERROR_MESSAGE));
    }
}
