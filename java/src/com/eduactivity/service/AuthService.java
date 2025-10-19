package com.eduactivity.service;

import com.eduactivity.dao.AuthDao;
import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.User;
import com.eduactivity.model.UserRole;
import com.eduactivity.security.IdentityV3PasswordVerifier;
import com.eduactivity.store.DataStore;

import java.time.OffsetDateTime;
import java.util.Optional;

public class AuthService {
    private final AuthDao authDao = new AuthDao();

    public Optional<User> login(String email, String password) {
        if (ConnectionManager.isConfigured()) {
            try {
                var dbu = authDao.findByEmail(email);
                if (dbu != null) {
                    // Check lockout
                    if (dbu.lockoutEnd != null && dbu.lockoutEnd.isAfter(OffsetDateTime.now())) {
                        return Optional.empty();
                    }
                    // Verify password (ASP.NET Identity v3)
                    if (IdentityV3PasswordVerifier.verify(dbu.passwordHash, password)) {
                        User u = new User(dbu.id, dbu.fullName != null ? dbu.fullName : email, dbu.email, null, AuthDao.toRole(dbu.roleVal));
                        u.setClassName(dbu.className);
                        u.setDepartment(dbu.department);
                        return Optional.of(u);
                    }
                    return Optional.empty();
                }
            } catch (Exception ignored) { }
        }
        // Fallback: in-memory demo users
        return DataStore.users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email) && u.getPassword().equals(password))
                .findFirst();
    }
}
