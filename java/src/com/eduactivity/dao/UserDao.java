package com.eduactivity.dao;

import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.User;
import com.eduactivity.model.UserRole;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public List<User> listAll() throws SQLException {
        // Try schema with custom columns (FullName, Role, Class, Department)
        String sql1 = "SELECT Id, FullName, Email, Role, [Class], Department FROM AspNetUsers ORDER BY FullName";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql1)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) {
                    String fullName = rs.getString("FullName");
                    if (fullName == null || fullName.isBlank()) fullName = rs.getString("Email");
                    User u = new User(rs.getString("Id"), fullName, rs.getString("Email"), null, mapRole(rs.getInt("Role")));
                    try { u.setClassName(rs.getString("Class")); } catch (SQLException ignored) {}
                    try { u.setDepartment(rs.getString("Department")); } catch (SQLException ignored) {}
                    list.add(u);
                }
                return list;
            }
        } catch (SQLException primaryEx) {
            // Fallback to default ASP.NET Core Identity schema (roles via join tables)
            String sql2 = "SELECT u.Id, u.UserName, u.Email, r.Name as RoleName " +
                    "FROM AspNetUsers u " +
                    "LEFT JOIN AspNetUserRoles ur ON ur.UserId = u.Id " +
                    "LEFT JOIN AspNetRoles r ON r.Id = ur.RoleId " +
                    "ORDER BY u.UserName";
            try (Connection c2 = ConnectionManager.getConnection(); PreparedStatement ps2 = c2.prepareStatement(sql2); ResultSet rs2 = ps2.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs2.next()) {
                    String fullName = rs2.getString("UserName");
                    String roleName = rs2.getString("RoleName");
                    UserRole role = UserRole.Student;
                    if (roleName != null) {
                        switch (roleName) {
                            case "Admin": role = UserRole.Admin; break;
                            case "Teacher": role = UserRole.Teacher; break;
                            case "Student": role = UserRole.Student; break;
                        }
                    }
                    User u = new User(rs2.getString("Id"), fullName, rs2.getString("Email"), null, role);
                    list.add(u);
                }
                return list;
            }
        }
    }

    public void toggleLock(String userId) throws SQLException {
        // If locked -> unlock (set NULL). If unlocked -> lock for 365 days
        String sql = "UPDATE AspNetUsers SET LockoutEnd = CASE WHEN LockoutEnd IS NULL OR LockoutEnd <= SYSDATETIMEOFFSET() THEN DATEADD(day, 365, SYSDATETIMEOFFSET()) ELSE NULL END WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public String createUser(String fullName, String email, String rawPassword, String role, String className, String department) throws SQLException {
        // Prepare fields required by ASP.NET Identity schema
        String id = java.util.UUID.randomUUID().toString();
        String userName = email;
        String normalized = email.toUpperCase(java.util.Locale.ROOT);
        String passwordHash = com.eduactivity.security.IdentityV3PasswordHasher.hash(rawPassword);
        String securityStamp = java.util.UUID.randomUUID().toString();
        String concurrencyStamp = java.util.UUID.randomUUID().toString();
        int roleVal = mapRoleVal(role);
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        try (Connection c = ConnectionManager.getConnection()) {
            // Detect existing columns to avoid NULL constraint issues on CreatedAt/UpdatedAt
            java.util.Set<String> cols = new java.util.HashSet<>();
            try (ResultSet rs = c.getMetaData().getColumns(null, null, "AspNetUsers", null)) {
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase(java.util.Locale.ROOT));
                }
            }

            StringBuilder col = new StringBuilder();
            StringBuilder val = new StringBuilder();
            java.util.List<java.util.function.Consumer<PreparedStatement>> setters = new java.util.ArrayList<>();

            // Helper to append column + placeholder and record setter in order
            java.util.function.BiConsumer<String, java.util.function.Consumer<PreparedStatement>> add = (name, setter) -> {
                if (col.length() > 0) { col.append(", "); val.append(", "); }
                col.append(name); val.append("?"); setters.add(setter);
            };

            // Always-present Identity columns
            add.accept("Id", ps -> { try { ps.setString(nextIndex(ps), id); } catch (SQLException ignored) {} });
            add.accept("UserName", ps -> { try { ps.setString(nextIndex(ps), userName); } catch (SQLException ignored) {} });
            add.accept("NormalizedUserName", ps -> { try { ps.setString(nextIndex(ps), normalized); } catch (SQLException ignored) {} });
            add.accept("Email", ps -> { try { ps.setString(nextIndex(ps), email); } catch (SQLException ignored) {} });
            add.accept("NormalizedEmail", ps -> { try { ps.setString(nextIndex(ps), normalized); } catch (SQLException ignored) {} });
            add.accept("EmailConfirmed", ps -> { try { ps.setBoolean(nextIndex(ps), true); } catch (SQLException ignored) {} });
            add.accept("PasswordHash", ps -> { try { ps.setString(nextIndex(ps), passwordHash); } catch (SQLException ignored) {} });
            add.accept("SecurityStamp", ps -> { try { ps.setString(nextIndex(ps), securityStamp); } catch (SQLException ignored) {} });
            add.accept("ConcurrencyStamp", ps -> { try { ps.setString(nextIndex(ps), concurrencyStamp); } catch (SQLException ignored) {} });
            add.accept("PhoneNumberConfirmed", ps -> { try { ps.setBoolean(nextIndex(ps), false); } catch (SQLException ignored) {} });
            add.accept("TwoFactorEnabled", ps -> { try { ps.setBoolean(nextIndex(ps), false); } catch (SQLException ignored) {} });
            add.accept("LockoutEnabled", ps -> { try { ps.setBoolean(nextIndex(ps), true); } catch (SQLException ignored) {} });
            add.accept("AccessFailedCount", ps -> { try { ps.setInt(nextIndex(ps), 0); } catch (SQLException ignored) {} });

            // Optional custom columns
            if (cols.contains("fullname")) add.accept("FullName", ps -> { try { ps.setString(nextIndex(ps), fullName); } catch (SQLException ignored) {} });
            if (cols.contains("role")) add.accept("Role", ps -> { try { ps.setInt(nextIndex(ps), roleVal); } catch (SQLException ignored) {} });
            if (cols.contains("class")) add.accept("[Class]", ps -> { try { ps.setString(nextIndex(ps), className); } catch (SQLException ignored) {} });
            if (cols.contains("department")) add.accept("Department", ps -> { try { ps.setString(nextIndex(ps), department); } catch (SQLException ignored) {} });
            if (cols.contains("createdat")) add.accept("CreatedAt", ps -> { try { ps.setTimestamp(nextIndex(ps), now); } catch (SQLException ignored) {} });
            if (cols.contains("updatedat")) add.accept("UpdatedAt", ps -> { try { ps.setTimestamp(nextIndex(ps), now); } catch (SQLException ignored) {} });

            String sql = "INSERT INTO AspNetUsers (" + col + ") VALUES (" + val + ")";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                // We cannot easily know parameter index inside lambda.
                // Use a small trick: maintain index in statement via wrapper method nextIndex.
                paramIndex.set(1);
                for (java.util.function.Consumer<PreparedStatement> fn : setters) fn.accept(ps);
                ps.executeUpdate();
            }
        }
        return id;
    }

    // ThreadLocal to track the next parameter index for PreparedStatement setters above
    private static final ThreadLocal<Integer> paramIndex = ThreadLocal.withInitial(() -> 1);
    private static int nextIndex(PreparedStatement ps) throws SQLException {
        int i = paramIndex.get();
        paramIndex.set(i + 1);
        return i;
    }

    private int mapRoleVal(String role) {
        if (role == null) return 1;
        switch (role.trim()) {
            case "Student": return 1;
            case "Teacher": return 2;
            case "Admin": return 3;
            default: return 1;
        }
    }

    private UserRole mapRole(int roleVal) {
        // EF enum starts at 1: Student=1, Teacher=2, Admin=3
        switch (roleVal) {
            case 1: return UserRole.Student;
            case 2: return UserRole.Teacher;
            case 3: return UserRole.Admin;
            default: return UserRole.Student;
        }
    }

    public void deleteUser(String id) throws SQLException {
        String sql = "DELETE FROM AspNetUsers WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public void updateUser(String id, String fullName, String email, String role, String className, String department) throws SQLException {
        String normalized = email != null ? email.toUpperCase(java.util.Locale.ROOT) : null;
        int roleVal = mapRoleVal(role);
        try (Connection c = ConnectionManager.getConnection()) {
            boolean hasUpdatedAt;
            try (ResultSet rs = c.getMetaData().getColumns(null, null, "AspNetUsers", "UpdatedAt")) {
                hasUpdatedAt = rs.next();
            }
            String sql = hasUpdatedAt
                    ? "UPDATE AspNetUsers SET FullName=?, Email=?, NormalizedEmail=?, Role=?, [Class]=?, Department=?, UpdatedAt=? WHERE Id=?"
                    : "UPDATE AspNetUsers SET FullName=?, Email=?, NormalizedEmail=?, Role=?, [Class]=?, Department=? WHERE Id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, fullName);
                ps.setString(i++, email);
                ps.setString(i++, normalized);
                ps.setInt(i++, roleVal);
                ps.setString(i++, className);
                ps.setString(i++, department);
                if (hasUpdatedAt) ps.setTimestamp(i++, new java.sql.Timestamp(System.currentTimeMillis()));
                ps.setString(i, id);
                ps.executeUpdate();
            }
        }
    }

    public User getById(String id) throws SQLException {
        String sql = "SELECT Id, FullName, Email, Role, [Class], Department FROM AspNetUsers WHERE Id=?";
        try (Connection c = ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(rs.getString("Id"), rs.getString("FullName"), rs.getString("Email"), null, mapRole(rs.getInt("Role")));
                    u.setClassName(rs.getString("Class"));
                    u.setDepartment(rs.getString("Department"));
                    return u;
                }
            }
        }
        return null;
    }
}
