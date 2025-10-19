package com.eduactivity.model;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String fullName;
    private String email;
    private String password; // demo only
    private UserRole role;
    private String className; // for students
    private String department; // for teachers

    private final List<Integer> createdActivityIds = new ArrayList<>();

    public User(String id, String fullName, String email, String password, UserRole role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<Integer> getCreatedActivityIds() { return createdActivityIds; }
}

