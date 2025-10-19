package com.eduactivity.ui;

import com.eduactivity.dao.UserDao;
import com.eduactivity.model.Activity;
import com.eduactivity.model.Registration;
import com.eduactivity.model.User;
import com.eduactivity.model.RegistrationStatus;
import com.eduactivity.service.ActivityService;
import com.eduactivity.service.RegistrationService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityDetailsDialog extends JDialog {
    private final User currentUser;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final UserDao userDao = new UserDao();

    private Activity activity;

    // Editable fields
    private final JTextField txtTitle = new JTextField();
    private final JTextArea txtDescription = new JTextArea(3, 40);
    private final JTextField txtStart = new JTextField();
    private final JTextField txtEnd = new JTextField();
    private final JTextField txtLocation = new JTextField();
    private final JTextField txtCategory = new JTextField();
    private final JSpinner spMax = new JSpinner(new SpinnerNumberModel(10, 1, 10000, 1));
    private final JCheckBox chkRequireApproval = new JCheckBox("Yêu cầu phê duyệt");
    private final JLabel lblCounts = new JLabel();
    private final JLabel lblStatus = new JLabel();

    private final DefaultTableModel regsModel = new DefaultTableModel(new Object[]{"Mã","Học sinh","Trạng thái","Thời gian","Ghi chú"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable regsTable = new JTable(regsModel);
    private final JComboBox<String> cbStatusFilter = new JComboBox<>(new String[]{"All","Pending","Approved","Rejected","Cancelled"});
    private final JTextField txtSearch = new JTextField(16);

    public ActivityDetailsDialog(Window owner, User user, Activity activity,
                                 ActivityService activityService,
                                 RegistrationService registrationService) {
        super(owner, "Chi tiết hoạt động", ModalityType.APPLICATION_MODAL);
        this.currentUser = user;
        this.activity = activity;
        this.activityService = activityService;
        this.registrationService = registrationService;
        buildUI();
        loadActivity();
        loadRegistrations();
        setSize(900, 600);
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel info = new JPanel(new GridLayout(0,2,6,6));
        info.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        info.add(new JLabel("Tiêu đề:")); info.add(txtTitle);
        info.add(new JLabel("Mô tả:")); info.add(new JScrollPane(txtDescription));
        info.add(new JLabel("Bắt đầu (yyyy-MM-ddTHH:mm):")); info.add(txtStart);
        info.add(new JLabel("Kết thúc (yyyy-MM-ddTHH:mm):")); info.add(txtEnd);
        info.add(new JLabel("Địa điểm:")); info.add(txtLocation);
        info.add(new JLabel("Danh mục:")); info.add(txtCategory);
        info.add(new JLabel("Tối đa:")); info.add(spMax);
        info.add(new JLabel("Phê duyệt:")); info.add(chkRequireApproval);
        info.add(new JLabel("Số lượng:")); info.add(lblCounts);
        info.add(new JLabel("Trạng thái:")); info.add(lblStatus);

        JButton btnRefresh = new JButton("Tải lại");
        btnRefresh.addActionListener(e -> { loadActivity(); loadRegistrations(); });
        JButton btnApprove = new JButton("Duyệt chọn");
        btnApprove.addActionListener(e -> onBulkApprove(true));
        JButton btnReject = new JButton("Từ chối chọn");
        btnReject.addActionListener(e -> onBulkApprove(false));
        JButton btnSave = new JButton("Lưu chỉnh sửa");
        btnSave.addActionListener(e -> onSave());
        JButton btnCancelAct = new JButton("Hủy hoạt động");
        btnCancelAct.addActionListener(e -> onCancelActivity());
        JButton btnClose = new JButton("Đóng");
        btnClose.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(btnRefresh);
        actions.add(btnSave);
        actions.add(btnCancelAct);
        actions.add(btnApprove);
        actions.add(btnReject);
        actions.add(btnClose);

        JPanel top = new JPanel(new BorderLayout());
        top.add(info, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnApply = new JButton("Lọc");
        btnApply.addActionListener(e -> loadRegistrations());
        filters.add(new JLabel("Trạng thái:")); filters.add(cbStatusFilter);
        filters.add(new JLabel("Tìm:")); filters.add(txtSearch);
        filters.add(btnApply);
        center.add(filters, BorderLayout.NORTH);
        regsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        center.add(new JScrollPane(regsTable), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void loadActivity() {
        this.activity = activityService.getById(activity.getId()).orElse(this.activity);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        txtTitle.setText(activity.getTitle());
        txtDescription.setText(activity.getDescription());
        txtStart.setText(activity.getStartTime().toString());
        txtEnd.setText(activity.getEndTime().toString());
        txtLocation.setText(activity.getLocation());
        txtCategory.setText(activity.getCategory());
        spMax.setValue(activity.getMaxParticipants());
        chkRequireApproval.setSelected(activity.isRequireApproval());
        lblCounts.setText(activity.getCurrentParticipants() + "/" + activity.getMaxParticipants());
        lblStatus.setText(activity.getStatus().name());
    }

    private void loadRegistrations() {
        regsModel.setRowCount(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Registration> regs = registrationService.byActivity(activity.getId());
        String st = String.valueOf(cbStatusFilter.getSelectedItem());
        String q = txtSearch.getText().trim().toLowerCase();
        for (Registration r : regs) {
            String student = r.getStudentId();
            try {
                var stu = userDao.getById(r.getStudentId());
                if (stu != null && stu.getFullName() != null && !stu.getFullName().isBlank()) student = stu.getFullName();
            } catch (Exception ignored) {}
            if (!"All".equals(st) && !r.getStatus().name().equalsIgnoreCase(st)) continue;
            if (!q.isEmpty()) {
                String sname = student == null ? "" : student.toLowerCase();
                if (!sname.contains(q)) continue;
            }
            regsModel.addRow(new Object[]{r.getId(), student, r.getStatus().name(), fmt.format(r.getRegistrationTime()), r.getNotes()});
        }
    }

    private void onBulkApprove(boolean approve) {
        int[] rows = regsTable.getSelectedRows();
        if (rows.length == 0) { JOptionPane.showMessageDialog(this, "Chọn ít nhất một đăng ký"); return; }
        int ok = 0; int fail = 0;
        for (int r : rows) {
            int regId = (Integer) regsModel.getValueAt(r, 0);
            try { registrationService.processApproval(currentUser.getId(), regId, approve); ok++; }
            catch (Exception ex) { fail++; }
        }
        if (fail > 0) JOptionPane.showMessageDialog(this, "Thành công: "+ok+", thất bại: "+fail);
        loadActivity();
        loadRegistrations();
    }

    private void onSave() {
        try {
            var patch = new Activity();
            patch.setId(activity.getId());
            patch.setTitle(txtTitle.getText().trim());
            patch.setDescription(txtDescription.getText().trim());
            patch.setStartTime(java.time.LocalDateTime.parse(txtStart.getText().trim()));
            patch.setEndTime(java.time.LocalDateTime.parse(txtEnd.getText().trim()));
            patch.setLocation(txtLocation.getText().trim());
            patch.setMaxParticipants((Integer) spMax.getValue());
            patch.setRequireApproval(chkRequireApproval.isSelected());
            patch.setCategory(txtCategory.getText().trim());
            activityService.update(currentUser, patch);
            JOptionPane.showMessageDialog(this, "Đã lưu cập nhật");
            loadActivity();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancelActivity() {
        try {
            int confirm = JOptionPane.showConfirmDialog(this, "Hủy hoạt động này?", "Xác nhận", JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION) return;
            activityService.cancel(currentUser, activity.getId());
            JOptionPane.showMessageDialog(this, "Đã hủy hoạt động");
            loadActivity();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
