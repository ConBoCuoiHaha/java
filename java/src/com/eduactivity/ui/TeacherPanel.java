package com.eduactivity.ui;

import com.eduactivity.model.Activity;
import com.eduactivity.model.Registration;
import com.eduactivity.model.User;
import com.eduactivity.dao.UserDao;
import com.eduactivity.service.ActivityService;
import com.eduactivity.service.RegistrationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TeacherPanel extends JPanel {
    private final User currentUser;
    private final ActivityService activityService = new ActivityService();
    private final RegistrationService registrationService = new RegistrationService();
    private final UserDao userDao = new UserDao();

    private final DefaultTableModel myActModel = new DefaultTableModel(new Object[]{"ID","Tiêu đề","Bắt đầu","SL","Trạng thái"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable myActTable = new JTable(myActModel);

    private final DefaultTableModel pendingModel = new DefaultTableModel(new Object[]{"Mã","Hoạt động","Học sinh","Trạng thái"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable pendingTable = new JTable(pendingModel);
    // Filters fields
    private final JTextField myActSearch = new JTextField(14);
    private final JTextField pendSearch = new JTextField(14);
    private final JComboBox<String> myActStatusFilter = new JComboBox<>(new String[]{"All","Open","Full","Closed","Cancelled","Completed"});

    public TeacherPanel(User user) {
        this.currentUser = user;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addBtn = new JButton("Tạo hoạt động");
        addBtn.addActionListener(e -> onCreateActivity());
        JButton editBtn = new JButton("Chỉnh sửa");
        editBtn.addActionListener(e -> onEditActivity());
        JButton refreshBtn = new JButton("Tải lại");
        refreshBtn.addActionListener(e -> refresh());
        top.add(addBtn); top.add(editBtn); top.add(refreshBtn);
        add(top, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(0.5);

        JPanel myPanel = new JPanel(new BorderLayout());
        myPanel.add(new JLabel("Hoạt động của tôi"), BorderLayout.NORTH);
        JPanel myCenter = new JPanel(new BorderLayout());
        JPanel myFilters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        myActSearch.setToolTipText("Tim theo tieu de");
        JButton myApply = new JButton("Loc"); myApply.addActionListener(e -> loadMyActivities());
        myFilters.add(new JLabel("Tim:")); myFilters.add(myActSearch);
        myFilters.add(new JLabel("Trang thai:")); myFilters.add(myActStatusFilter);
        myFilters.add(myApply);
        myCenter.add(myFilters, BorderLayout.NORTH);
        myCenter.add(new JScrollPane(myActTable), BorderLayout.CENTER);
        myPanel.add(myCenter, BorderLayout.CENTER);
        JButton cancelBtn = new JButton("Hủy hoạt động");
        cancelBtn.addActionListener(e -> onCancelActivity());
        JButton viewRegsBtn = new JButton("Xem DS đăng ký");
        viewRegsBtn.addActionListener(e -> onViewRegsByActivity());
        JPanel myBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton detailsBtn = new JButton("Chi tiết");
        detailsBtn.addActionListener(e -> onOpenDetails());
        myBottom.add(detailsBtn); myBottom.add(viewRegsBtn); myBottom.add(cancelBtn);
        myPanel.add(myBottom, BorderLayout.SOUTH);

        JPanel pendPanel = new JPanel(new BorderLayout());
        pendPanel.add(new JLabel("Đăng ký chờ duyệt"), BorderLayout.NORTH);
        JPanel pendCenter = new JPanel(new BorderLayout());
        JPanel pendFilters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pendSearch.setToolTipText("Tim theo tieu de hoac studentId");
        JButton pendApply = new JButton("Loc"); pendApply.addActionListener(e -> loadPending());
        pendFilters.add(new JLabel("Tim:")); pendFilters.add(pendSearch); pendFilters.add(pendApply);
        pendCenter.add(pendFilters, BorderLayout.NORTH);
        pendCenter.add(new JScrollPane(pendingTable), BorderLayout.CENTER);
        pendPanel.add(pendCenter, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton approveBtn = new JButton("Duyệt");
        JButton rejectBtn = new JButton("Từ chối");
        approveBtn.addActionListener(e -> onApprove(true));
        rejectBtn.addActionListener(e -> onApprove(false));
        actions.add(approveBtn); actions.add(rejectBtn);
        pendPanel.add(actions, BorderLayout.SOUTH);

        split.setTopComponent(myPanel);
        split.setBottomComponent(pendPanel);
        add(split, BorderLayout.CENTER);

        refresh();
    }

    private void refresh() {
        loadMyActivities();
        loadPending();
    }

    private void loadMyActivities() {
        myActModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Activity> list = activityService.listByCreator(currentUser.getId());
        String q1 = myActSearch.getText().trim().toLowerCase();
        String st = String.valueOf(myActStatusFilter.getSelectedItem());
        for (Activity a : list) {
            if (!q1.isEmpty()) {
                String t = a.getTitle() == null ? "" : a.getTitle().toLowerCase();
                if (!t.contains(q1)) continue;
            }
            if (!"All".equals(st) && !a.getStatus().name().equalsIgnoreCase(st)) continue;
            myActModel.addRow(new Object[]{a.getId(), a.getTitle(), fmt.format(a.getStartTime()), a.getCurrentParticipants()+"/"+a.getMaxParticipants(), a.getStatus().name()});
        }
    }

    private void loadPending() {
        pendingModel.setRowCount(0);
        List<Registration> regs = registrationService.pendingForCreator(currentUser.getId());
        String q2 = pendSearch.getText().trim().toLowerCase();
        for (Registration r : regs) {
            Activity a = activityService.getById(r.getActivityId()).orElse(null);
            String title = a == null ? "?" : a.getTitle();
            String studentDisplay = r.getStudentId();
            try {
                var stu = userDao.getById(r.getStudentId());
                if (stu != null && stu.getFullName() != null && !stu.getFullName().isBlank()) studentDisplay = stu.getFullName();
            } catch (Exception ignored) {}
            if (!q2.isEmpty()) {
                String ti = title == null ? "" : title.toLowerCase();
                String sname = studentDisplay == null ? "" : studentDisplay.toLowerCase();
                if (!(ti.contains(q2) || sname.contains(q2))) continue;
            }
            pendingModel.addRow(new Object[]{r.getId(), title, studentDisplay, r.getStatus().name()});
        }
    }

    private void onCreateActivity() {
        JTextField title = new JTextField();
        JTextField location = new JTextField();
        JTextField category = new JTextField();
        JTextField start = new JTextField("2025-12-01T09:00");
        JTextField end = new JTextField("2025-12-01T11:00");
        JSpinner max = new JSpinner(new SpinnerNumberModel(20,1,1000,1));
        JCheckBox needApprove = new JCheckBox("Cần phê duyệt");
        JTextArea desc = new JTextArea(4, 20);
        JScrollPane descScroll = new JScrollPane(desc);

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Tiêu đề")); panel.add(title);
        panel.add(new JLabel("Địa điểm")); panel.add(location);
        panel.add(new JLabel("Danh mục")); panel.add(category);
        panel.add(new JLabel("Bắt đầu (yyyy-MM-ddTHH:mm)")); panel.add(start);
        panel.add(new JLabel("Kết thúc (yyyy-MM-ddTHH:mm)")); panel.add(end);
        panel.add(new JLabel("Tối đa")); panel.add(max);
        panel.add(new JLabel("")); panel.add(needApprove);
        panel.add(new JLabel("Mô tả")); panel.add(descScroll);

        int opt = JOptionPane.showConfirmDialog(this, panel, "Tạo hoạt động", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                Activity a = activityService.create(currentUser, title.getText().trim(), desc.getText().trim(),
                        LocalDateTime.parse(start.getText().trim()), LocalDateTime.parse(end.getText().trim()),
                        location.getText().trim(), (Integer) max.getValue(), needApprove.isSelected(), category.getText().trim());
                JOptionPane.showMessageDialog(this, "Đã tạo hoạt động #"+a.getId());
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onEditActivity() {
        int row = myActTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn hoạt động"); return; }
        int id = (Integer) myActModel.getValueAt(row, 0);
        var optA = activityService.getById(id);
        if (optA.isEmpty()) { JOptionPane.showMessageDialog(this, "Không tìm thấy hoạt động"); return; }
        var cur = optA.get();

        JTextField title = new JTextField(cur.getTitle());
        JTextField location = new JTextField(cur.getLocation());
        JTextField category = new JTextField(cur.getCategory());
        JTextField start = new JTextField(cur.getStartTime().toString());
        JTextField end = new JTextField(cur.getEndTime().toString());
        JSpinner max = new JSpinner(new SpinnerNumberModel(cur.getMaxParticipants(),1,1000,1));
        JCheckBox needApprove = new JCheckBox("Cần phê duyệt", cur.isRequireApproval());
        JTextArea desc = new JTextArea(cur.getDescription(), 4, 20);
        JScrollPane descScroll = new JScrollPane(desc);

        JPanel panel = new JPanel(new GridLayout(0,2,6,6));
        panel.add(new JLabel("Tiêu đề")); panel.add(title);
        panel.add(new JLabel("Địa điểm")); panel.add(location);
        panel.add(new JLabel("Danh mục")); panel.add(category);
        panel.add(new JLabel("Bắt đầu (yyyy-MM-ddTHH:mm)")); panel.add(start);
        panel.add(new JLabel("Kết thúc (yyyy-MM-ddTHH:mm)")); panel.add(end);
        panel.add(new JLabel("Tối đa")); panel.add(max);
        panel.add(new JLabel("")); panel.add(needApprove);
        panel.add(new JLabel("Mô tả")); panel.add(descScroll);

        int opt = JOptionPane.showConfirmDialog(this, panel, "Chỉnh sửa hoạt động", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            try {
                cur.setTitle(title.getText().trim());
                cur.setDescription(desc.getText().trim());
                cur.setStartTime(java.time.LocalDateTime.parse(start.getText().trim()));
                cur.setEndTime(java.time.LocalDateTime.parse(end.getText().trim()));
                cur.setLocation(location.getText().trim());
                cur.setMaxParticipants((Integer) max.getValue());
                cur.setRequireApproval(needApprove.isSelected());
                cur.setCategory(category.getText().trim());
                activityService.update(currentUser, cur);
                JOptionPane.showMessageDialog(this, "Đã cập nhật");
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onCancelActivity() {
        int row = myActTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn hoạt động"); return; }
        int id = (Integer) myActModel.getValueAt(row, 0);
        try {
            activityService.cancel(currentUser, id);
            JOptionPane.showMessageDialog(this, "Đã hủy hoạt động #"+id);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onApprove(boolean approve) {
        int row = pendingTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn đăng ký"); return; }
        int regId = (Integer) pendingModel.getValueAt(row, 0);
        try {
            registrationService.processApproval(currentUser.getId(), regId, approve);
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onOpenDetails() {
        int row = myActTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn hoạt động"); return; }
        int id = (Integer) myActModel.getValueAt(row, 0);
        var act = activityService.getById(id).orElse(null);
        if (act == null) { JOptionPane.showMessageDialog(this, "Không tìm thấy hoạt động"); return; }
        Window w = SwingUtilities.getWindowAncestor(this);
        new ActivityDetailsDialog(w, currentUser, act, activityService, registrationService).setVisible(true);
        refresh();
    }

    private void onViewRegsByActivity() {
        int row = myActTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Chọn hoạt động"); return; }
        int id = (Integer) myActModel.getValueAt(row, 0);
        var regs = registrationService.byActivity(id);
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Mã","Học sinh","Trạng thái","Ghi chú"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (var r : regs) {
            String studentDisplay = r.getStudentId();
            try {
                var stu = userDao.getById(r.getStudentId());
                if (stu != null && stu.getFullName() != null && !stu.getFullName().isBlank()) studentDisplay = stu.getFullName();
            } catch (Exception ignored) {}
            m.addRow(new Object[]{r.getId(), studentDisplay, r.getStatus().name(), r.getNotes()});
        }
        JTable table = new JTable(m);
        JScrollPane sp = new JScrollPane(table);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(sp, BorderLayout.CENTER);
        JButton approveBtn = new JButton("Duyệt");
        JButton rejectBtn = new JButton("Từ chối");
        approveBtn.addActionListener(e -> {
            int rr = table.getSelectedRow(); if (rr<0) return; int regId = (Integer) m.getValueAt(rr,0);
            try { registrationService.processApproval(currentUser.getId(), regId, true); m.setValueAt("Approved", rr, 2); refresh(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(panel, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });
        rejectBtn.addActionListener(e -> {
            int rr = table.getSelectedRow(); if (rr<0) return; int regId = (Integer) m.getValueAt(rr,0);
            try { registrationService.processApproval(currentUser.getId(), regId, false); m.setValueAt("Rejected", rr, 2); refresh(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(panel, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT)); south.add(approveBtn); south.add(rejectBtn);
        panel.add(south, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, "Đăng ký của hoạt động #"+id, JOptionPane.PLAIN_MESSAGE);
    }
}
