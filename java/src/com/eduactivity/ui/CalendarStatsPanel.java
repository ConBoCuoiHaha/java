package com.eduactivity.ui;

import com.eduactivity.model.Activity;
import com.eduactivity.model.ActivityStatus;
import com.eduactivity.service.ActivityService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarStatsPanel extends JPanel {
    private final ActivityService activityService = new ActivityService();

    private final DefaultTableModel calendarModel = new DefaultTableModel(new Object[]{"Ngày","Số hoạt động","Tiêu đề"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable calendarTable = new JTable(calendarModel);

    private final JLabel totalLbl = new JLabel();
    private final JLabel openLbl = new JLabel();
    private final JLabel fullLbl = new JLabel();
    private final JLabel cancelledLbl = new JLabel();
    private final JLabel completedLbl = new JLabel();
    // Filters
    private final JComboBox<String> monthCombo = new JComboBox<>(new String[]{"1","2","3","4","5","6","7","8","9","10","11","12"});
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(java.time.LocalDate.now().getYear(), 2000, 2100, 1));
    private final JTextField categoryField = new JTextField(10);

    public CalendarStatsPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Lịch", createCalendarPanel());
        tabs.addTab("Thống kê", createStatsPanel());
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    private JPanel createCalendarPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(new JLabel("Month:")); top.add(monthCombo);
        top.add(new JLabel("Year:")); top.add(yearSpinner);
        top.add(new JLabel("Category:")); top.add(categoryField);
        JButton reload = new JButton("Tải lại");
        reload.addActionListener(e -> refresh());
        top.add(reload);
        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(calendarTable), BorderLayout.CENTER);
        return p;
    }

    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new GridLayout(0,2,8,8));
        p.add(new JLabel("Tổng hoạt động:")); p.add(totalLbl);
        p.add(new JLabel("Mở đăng ký:")); p.add(openLbl);
        p.add(new JLabel("Đầy:")); p.add(fullLbl);
        p.add(new JLabel("Đã huỷ:")); p.add(cancelledLbl);
        p.add(new JLabel("Hoàn thành:")); p.add(completedLbl);
        return p;
    }

    private void refresh() {
        List<Activity> list = activityService.listAll();
        int m = monthCombo.getSelectedIndex() + 1;
        int y = (Integer) yearSpinner.getValue();
        LocalDate start = LocalDate.of(y, m, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String cat = categoryField.getText().trim().toLowerCase();
        Map<LocalDate, List<Activity>> byDate = list.stream()
                .filter(a -> !a.getStartTime().toLocalDate().isBefore(start) && !a.getStartTime().toLocalDate().isAfter(end))
                .filter(a -> cat.isEmpty() || (a.getCategory()!=null && a.getCategory().toLowerCase().contains(cat)))
                .collect(Collectors.groupingBy(a -> a.getStartTime().toLocalDate()));

        calendarModel.setRowCount(0);
        byDate.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            String titles = e.getValue().stream().map(Activity::getTitle).collect(Collectors.joining(", "));
            calendarModel.addRow(new Object[]{df.format(e.getKey()), e.getValue().size(), titles});
        });

        List<Activity> filtered = list.stream()
                .filter(a -> !a.getStartTime().toLocalDate().isBefore(start) && !a.getStartTime().toLocalDate().isAfter(end))
                .filter(a -> cat.isEmpty() || (a.getCategory()!=null && a.getCategory().toLowerCase().contains(cat)))
                .collect(Collectors.toList());
        long total = filtered.size();
        long open = filtered.stream().filter(a -> a.getStatus() == ActivityStatus.Open).count();
        long full = filtered.stream().filter(a -> a.getStatus() == ActivityStatus.Full).count();
        long cancelled = filtered.stream().filter(a -> a.getStatus() == ActivityStatus.Cancelled).count();
        long completed = filtered.stream().filter(a -> a.getStatus() == ActivityStatus.Completed).count();

        totalLbl.setText(String.valueOf(total));
        openLbl.setText(String.valueOf(open));
        fullLbl.setText(String.valueOf(full));
        cancelledLbl.setText(String.valueOf(cancelled));
        completedLbl.setText(String.valueOf(completed));
    }
}
