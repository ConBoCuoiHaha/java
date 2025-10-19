package com.eduactivity.dao;

import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.Registration;
import com.eduactivity.model.RegistrationStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDao {
    public List<Registration> byStudent(String studentId) throws SQLException {
        String sql = "SELECT Id, ActivityId, StudentId, Status, AttendanceStatus, RegistrationTime, ApprovalTime, ApprovedById, Notes FROM Registrations WHERE StudentId=? ORDER BY RegistrationTime DESC";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Registration> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<Registration> pendingForCreator(String creatorId) throws SQLException {
        String sql = "SELECT r.Id, r.ActivityId, r.StudentId, r.Status, r.AttendanceStatus, r.RegistrationTime, r.ApprovalTime, r.ApprovedById, r.Notes FROM Registrations r JOIN Activities a ON a.Id=r.ActivityId WHERE r.Status=? AND a.CreatorId=? ORDER BY r.RegistrationTime DESC";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, RegistrationStatus.Pending.ordinal()+1);
            ps.setString(2, creatorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Registration> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Registration insert(Registration r) throws SQLException {
        String sql = "INSERT INTO Registrations (ActivityId, StudentId, Status, AttendanceStatus, RegistrationTime, Notes) VALUES (?,?,?,?,?,?); SELECT SCOPE_IDENTITY();";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, r.getActivityId());
            ps.setString(2, r.getStudentId());
            ps.setInt(3, r.getStatus().ordinal()+1);
            ps.setInt(4, r.getAttendanceStatus().ordinal());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(6, r.getNotes());
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) r.setId(rs.getInt(1)); }
            return r;
        }
    }

    public void updateStatusApprove(int id, String approverId, boolean approve) throws SQLException {
        String sql = "UPDATE Registrations SET Status=?, ApprovalTime=?, ApprovedById=? WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, approve ? RegistrationStatus.Approved.ordinal()+1 : RegistrationStatus.Rejected.ordinal()+1);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, approverId);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void cancel(int id) throws SQLException {
        String sql = "UPDATE Registrations SET Status=? WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, RegistrationStatus.Cancelled.ordinal()+1);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public boolean existsForStudentActivity(String studentId, int activityId) throws SQLException {
        String sql = "SELECT 1 FROM Registrations WHERE StudentId=? AND ActivityId=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, studentId); ps.setInt(2, activityId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public Registration getById(int id) throws SQLException {
        String sql = "SELECT Id, ActivityId, StudentId, Status, AttendanceStatus, RegistrationTime, ApprovalTime, ApprovedById, Notes FROM Registrations WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    public List<Registration> byActivity(int activityId) throws SQLException {
        String sql = "SELECT Id, ActivityId, StudentId, Status, AttendanceStatus, RegistrationTime, ApprovalTime, ApprovedById, Notes FROM Registrations WHERE ActivityId=? ORDER BY RegistrationTime";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, activityId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Registration> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registrations";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
            return 0;
        }
    }

    public int countPending() throws SQLException {
        String sql = "SELECT COUNT(*) FROM Registrations WHERE Status=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, com.eduactivity.model.RegistrationStatus.Pending.ordinal()+1);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
            return 0;
        }
    }

    private Registration map(ResultSet rs) throws SQLException {
        Registration r = new Registration();
        r.setId(rs.getInt("Id"));
        r.setActivityId(rs.getInt("ActivityId"));
        r.setStudentId(rs.getString("StudentId"));
        int status = rs.getInt("Status");
        r.setStatus(RegistrationStatus.values()[status-1]);
        r.setRegistrationTime(rs.getTimestamp("RegistrationTime").toLocalDateTime());
        Timestamp appr = rs.getTimestamp("ApprovalTime");
        if (appr != null) r.setApprovalTime(appr.toLocalDateTime());
        r.setApprovedById(rs.getString("ApprovedById"));
        r.setNotes(rs.getString("Notes"));
        return r;
    }
}
