package com.eduactivity.model;

import java.time.LocalDateTime;

public class Registration {
    private int id;
    private int activityId;
    private String studentId;
    private RegistrationStatus status = RegistrationStatus.Pending;
    private AttendanceStatus attendanceStatus = AttendanceStatus.NotSet;
    private LocalDateTime registrationTime = LocalDateTime.now();
    private LocalDateTime approvalTime;
    private String approvedById;
    private String notes;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getActivityId() { return activityId; }
    public void setActivityId(int activityId) { this.activityId = activityId; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) { this.attendanceStatus = attendanceStatus; }
    public LocalDateTime getRegistrationTime() { return registrationTime; }
    public void setRegistrationTime(LocalDateTime registrationTime) { this.registrationTime = registrationTime; }
    public LocalDateTime getApprovalTime() { return approvalTime; }
    public void setApprovalTime(LocalDateTime approvalTime) { this.approvalTime = approvalTime; }
    public String getApprovedById() { return approvedById; }
    public void setApprovedById(String approvedById) { this.approvedById = approvedById; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
