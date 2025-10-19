package com.eduactivity.dao;

import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.Activity;
import com.eduactivity.model.ActivityStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ActivityDao {
    public List<Activity> listAll() throws SQLException {
        String sql = "SELECT Id, Title, Description, StartTime, EndTime, Location, MaxParticipants, CurrentParticipants, Status, RequireApproval, Category, CreatorId, CreatedAt, UpdatedAt FROM Activities ORDER BY StartTime";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<Activity> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    public List<Activity> listByCreator(String creatorId) throws SQLException {
        String sql = "SELECT Id, Title, Description, StartTime, EndTime, Location, MaxParticipants, CurrentParticipants, Status, RequireApproval, Category, CreatorId, CreatedAt, UpdatedAt FROM Activities WHERE CreatorId=? ORDER BY CreatedAt DESC";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, creatorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Activity> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public Optional<Activity> getById(int id) throws SQLException {
        String sql = "SELECT Id, Title, Description, StartTime, EndTime, Location, MaxParticipants, CurrentParticipants, Status, RequireApproval, Category, CreatorId, CreatedAt, UpdatedAt FROM Activities WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        }
    }

    public Activity insert(Activity a) throws SQLException {
        String sql = "INSERT INTO Activities (Title, Description, StartTime, EndTime, Location, MaxParticipants, CurrentParticipants, Status, RequireApproval, Category, CreatorId, CreatedAt, UpdatedAt) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?); SELECT SCOPE_IDENTITY();";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(a.getEndTime()));
            ps.setString(5, a.getLocation());
            ps.setInt(6, a.getMaxParticipants());
            ps.setInt(7, a.getCurrentParticipants());
            ps.setInt(8, a.getStatus().ordinal()+1); // EF enum starts at 1
            ps.setBoolean(9, a.isRequireApproval());
            ps.setString(10, a.getCategory());
            ps.setString(11, a.getCreatorId());
            ps.setTimestamp(12, Timestamp.valueOf(a.getCreatedAt()));
            ps.setTimestamp(13, Timestamp.valueOf(a.getUpdatedAt()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) a.setId(rs.getInt(1));
            }
            return a;
        }
    }

    public void cancel(int id) throws SQLException {
        String sql = "UPDATE Activities SET Status=?, UpdatedAt=? WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ActivityStatus.Cancelled.ordinal()+1);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public int incrementParticipantsIfAvailable(int activityId) throws SQLException {
        String sql = "UPDATE Activities SET CurrentParticipants = CurrentParticipants + 1, UpdatedAt=SYSDATETIME(), Status = CASE WHEN CurrentParticipants + 1 >= MaxParticipants THEN ? ELSE Status END WHERE Id=? AND CurrentParticipants < MaxParticipants";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ActivityStatus.Full.ordinal()+1);
            ps.setInt(2, activityId);
            return ps.executeUpdate();
        }
    }

    public int decrementParticipants(int activityId) throws SQLException {
        String sql = "UPDATE Activities SET CurrentParticipants = CASE WHEN CurrentParticipants>0 THEN CurrentParticipants-1 ELSE 0 END, UpdatedAt=SYSDATETIME(), Status = CASE WHEN CurrentParticipants - 1 < MaxParticipants AND Status=? THEN ? ELSE Status END WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ActivityStatus.Full.ordinal()+1);
            ps.setInt(2, ActivityStatus.Open.ordinal()+1);
            ps.setInt(3, activityId);
            return ps.executeUpdate();
        }
    }

    public int update(Activity a) throws SQLException {
        String sql = "UPDATE Activities SET Title=?, Description=?, StartTime=?, EndTime=?, Location=?, MaxParticipants=?, RequireApproval=?, Category=?, UpdatedAt=SYSDATETIME() WHERE Id=? AND CurrentParticipants <= ?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, a.getTitle());
            ps.setString(2, a.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(a.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(a.getEndTime()));
            ps.setString(5, a.getLocation());
            ps.setInt(6, a.getMaxParticipants());
            ps.setBoolean(7, a.isRequireApproval());
            ps.setString(8, a.getCategory());
            ps.setInt(9, a.getId());
            ps.setInt(10, a.getMaxParticipants());
            return ps.executeUpdate();
        }
    }

    public void deleteActivityAndRegistrations(int activityId) throws SQLException {
        try (Connection c = ConnectionManager.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps1 = c.prepareStatement("DELETE FROM Registrations WHERE ActivityId=?");
                 PreparedStatement ps2 = c.prepareStatement("DELETE FROM Activities WHERE Id=?")) {
                ps1.setInt(1, activityId); ps1.executeUpdate();
                ps2.setInt(1, activityId); ps2.executeUpdate();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private Activity map(ResultSet rs) throws SQLException {
        Activity a = new Activity();
        a.setId(rs.getInt("Id"));
        a.setTitle(rs.getString("Title"));
        a.setDescription(rs.getString("Description"));
        a.setStartTime(rs.getTimestamp("StartTime").toLocalDateTime());
        a.setEndTime(rs.getTimestamp("EndTime").toLocalDateTime());
        a.setLocation(rs.getString("Location"));
        a.setMaxParticipants(rs.getInt("MaxParticipants"));
        a.setCurrentParticipants(rs.getInt("CurrentParticipants"));
        int status = rs.getInt("Status");
        a.setStatus(ActivityStatus.values()[status-1]);
        a.setRequireApproval(rs.getBoolean("RequireApproval"));
        a.setCategory(rs.getString("Category"));
        a.setCreatorId(rs.getString("CreatorId"));
        Timestamp ca = rs.getTimestamp("CreatedAt");
        Timestamp ua = rs.getTimestamp("UpdatedAt");
        if (ca != null) a.getCreatedAt(); // keep default
        if (ua != null) a.touchUpdatedAt();
        return a;
    }
}
