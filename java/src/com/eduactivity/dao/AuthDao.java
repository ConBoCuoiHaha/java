package com.eduactivity.dao;

import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.User;
import com.eduactivity.model.UserRole;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Locale;

public class AuthDao {
    public static class DbUser {
        public String id;
        public String email;
        public String fullName;
        public int roleVal;
        public String className;
        public String department;
        public boolean emailConfirmed;
        public OffsetDateTime lockoutEnd;
        public String passwordHash;
    }

    public DbUser findByEmail(String email) throws SQLException {
        String sqlNorm = "SELECT Id, Email, PasswordHash, FullName, Role, [Class], Department, EmailConfirmed, LockoutEnd FROM AspNetUsers WHERE NormalizedEmail = ?";
        String sqlEmail = "SELECT Id, Email, PasswordHash, FullName, Role, [Class], Department, EmailConfirmed, LockoutEnd FROM AspNetUsers WHERE Email = ?";
        try (Connection c = ConnectionManager.getConnection()) {
            DbUser user = null;
            try (PreparedStatement ps = c.prepareStatement(sqlNorm)) {
                ps.setString(1, email.toUpperCase(Locale.ROOT));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) user = map(rs);
                }
            }
            if (user == null) {
                try (PreparedStatement ps = c.prepareStatement(sqlEmail)) {
                    ps.setString(1, email);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) user = map(rs);
                    }
                }
            }
            return user;
        }
    }

    private DbUser map(ResultSet rs) throws SQLException {
        DbUser u = new DbUser();
        u.id = rs.getString("Id");
        u.email = rs.getString("Email");
        u.passwordHash = rs.getString("PasswordHash");
        u.fullName = rs.getString("FullName");
        try { u.roleVal = rs.getInt("Role"); } catch (SQLException ignored) { u.roleVal = 1; }
        try { u.className = rs.getString("Class"); } catch (SQLException ignored) { }
        try { u.department = rs.getString("Department"); } catch (SQLException ignored) { }
        try { u.emailConfirmed = rs.getBoolean("EmailConfirmed"); } catch (SQLException ignored) { }
        try { u.lockoutEnd = rs.getObject("LockoutEnd", OffsetDateTime.class); } catch (SQLException ignored) { }
        return u;
    }

    public static UserRole toRole(int val) {
        switch (val) {
            case 1: return UserRole.Student;
            case 2: return UserRole.Teacher;
            case 3: return UserRole.Admin;
            default: return UserRole.Student;
        }
    }
}

