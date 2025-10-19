package com.eduactivity.store;

import com.eduactivity.model.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DataStore {
    public static final Map<String, User> users = new HashMap<>();
    public static final Map<Integer, Activity> activities = new HashMap<>();
    public static final Map<Integer, Registration> registrations = new HashMap<>();
    public static final Map<Integer, Notification> notifications = new HashMap<>();

    private static final AtomicInteger activityIdSeq = new AtomicInteger(1);
    private static final AtomicInteger registrationIdSeq = new AtomicInteger(1);
    private static final AtomicInteger notificationIdSeq = new AtomicInteger(1);

    static {
        seed();
    }

    public static void seed() {
        users.clear(); activities.clear(); registrations.clear(); notifications.clear();
        activityIdSeq.set(1); registrationIdSeq.set(1); notificationIdSeq.set(1);

        // Admin, Teacher, Student with same emails/passwords as C# seed
        User admin = new User(UUID.randomUUID().toString(), "System Administrator", "admin@socketir.com", "Admin123!", UserRole.Admin);
        User teacher = new User(UUID.randomUUID().toString(), "Nguyen Van Giao", "teacher@socketir.com", "Teacher123!", UserRole.Teacher);
        teacher.setDepartment("CNTT");
        User student = new User(UUID.randomUUID().toString(), "Tran Thi Hoc Sinh", "student@socketir.com", "Student123!", UserRole.Student);
        student.setClassName("IT2021");

        users.put(admin.getId(), admin);
        users.put(teacher.getId(), teacher);
        users.put(student.getId(), student);

        // Quick lookup by email map (optional) not needed; services will search.

        // Seed a few activities by teacher
        createActivity(teacher, "CLB Bóng đá", "Giao lưu bóng đá chiều thứ 7", LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(3).plusHours(2), "Sân A", 20, false, "Thể thao");
        createActivity(teacher, "Seminar Java", "Chia sẻ về Java cơ bản", LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(5).plusHours(2), "Phòng 101", 50, true, "Học thuật");
    }

    public static Activity createActivity(User creator, String title, String desc, LocalDateTime start, LocalDateTime end, String loc, int max, boolean requireApproval, String category) {
        Activity a = new Activity();
        a.setId(activityIdSeq.getAndIncrement());
        a.setTitle(title);
        a.setDescription(desc);
        a.setStartTime(start);
        a.setEndTime(end);
        a.setLocation(loc);
        a.setMaxParticipants(max);
        a.setCurrentParticipants(0);
        a.setRequireApproval(requireApproval);
        a.setCategory(category);
        a.setCreatorId(creator.getId());
        activities.put(a.getId(), a);
        creator.getCreatedActivityIds().add(a.getId());
        return a;
    }

    public static Registration createRegistration(String studentId, int activityId, boolean approved) {
        Registration r = new Registration();
        r.setId(registrationIdSeq.getAndIncrement());
        r.setStudentId(studentId);
        r.setActivityId(activityId);
        r.setStatus(approved ? RegistrationStatus.Approved : RegistrationStatus.Pending);
        registrations.put(r.getId(), r);
        return r;
    }

    public static Notification createNotification(String userId, String title, String content, NotificationType type, Integer activityId, Integer registrationId) {
        Notification n = new Notification();
        n.setId(notificationIdSeq.getAndIncrement());
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setRelatedActivityId(activityId);
        n.setRelatedRegistrationId(registrationId);
        notifications.put(n.getId(), n);
        return n;
    }
}

