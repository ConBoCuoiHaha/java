package com.eduactivity.service;

import com.eduactivity.dao.ActivityDao;
import com.eduactivity.dao.RegistrationDao;
import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.*;
import com.eduactivity.store.DataStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegistrationService {
    private final RegistrationDao dao = new RegistrationDao();
    private final ActivityDao activityDao = new ActivityDao();
    public Registration register(String studentId, int activityId, String notes) {
        boolean dbConfigured = ConnectionManager.isConfigured();
        Activity activity = null;
        try { if (dbConfigured) activity = activityDao.getById(activityId).orElse(null); } catch (Exception ignored) {}
        boolean usingDb = dbConfigured && activity != null;
        if (activity == null) activity = DataStore.activities.get(activityId);
        if (activity == null) throw new IllegalArgumentException("Activity not found");
        if (activity.getStatus() != ActivityStatus.Open) throw new IllegalStateException("Activity not open");

        boolean already;
        if (usingDb) {
            try { already = new RegistrationDao().existsForStudentActivity(studentId, activityId); }
            catch (Exception ex) { already = false; }
        } else {
            already = DataStore.registrations.values().stream().anyMatch(r -> r.getActivityId() == activityId && r.getStudentId().equals(studentId));
        }
        if (already) throw new IllegalStateException("Already registered");

        if (!activity.isRequireApproval()) {
            if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) throw new IllegalStateException("Full");
        }

        Registration r = usingDb ? new Registration() : DataStore.createRegistration(studentId, activityId, !activity.isRequireApproval());
        r.setStudentId(studentId);
        r.setActivityId(activityId);
        r.setStatus(activity.isRequireApproval() ? RegistrationStatus.Pending : RegistrationStatus.Approved);
        r.setNotes(notes);

        if (usingDb) {
            try {
                dao.insert(r);
                if (r.getStatus() == RegistrationStatus.Approved) {
                    int affected = activityDao.incrementParticipantsIfAvailable(activityId);
                    if (affected == 0) throw new IllegalStateException("Activity full");
                }
            } catch (RuntimeException ex) { throw ex; }
            catch (Exception ignored) {}
        } else {
            if (!activity.isRequireApproval()) {
                activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
                if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
                    activity.setStatus(ActivityStatus.Full);
                }
                activity.touchUpdatedAt();
            }
        }
        return r;
    }

    public void cancel(String userId, int registrationId) {
        Registration r;
        if (ConnectionManager.isConfigured()) {
            try { r = dao.getById(registrationId); } catch (Exception e) { r = null; }
        } else {
            r = DataStore.registrations.get(registrationId);
        }
        if (r == null) throw new IllegalArgumentException("Registration not found");
        if (!r.getStudentId().equals(userId)) throw new SecurityException("Cannot cancel others' registration");
        Activity a;
        if (ConnectionManager.isConfigured()) { try { a = activityDao.getById(r.getActivityId()).orElse(null);} catch (Exception e) { a=null; } }
        else { a = DataStore.activities.get(r.getActivityId()); }
        if (a != null && r.getStatus() == RegistrationStatus.Approved && a.getStartTime().isAfter(LocalDateTime.now())) {
            if (ConnectionManager.isConfigured()) {
                try { activityDao.decrementParticipants(a.getId()); } catch (Exception ignored) {}
            } else {
                a.setCurrentParticipants(Math.max(0, a.getCurrentParticipants() - 1));
                if (a.getStatus() == ActivityStatus.Full && a.getCurrentParticipants() < a.getMaxParticipants()) {
                    a.setStatus(ActivityStatus.Open);
                }
                a.touchUpdatedAt();
            }
        }
        r.setStatus(RegistrationStatus.Cancelled);
        if (ConnectionManager.isConfigured()) {
            try { dao.cancel(registrationId); } catch (Exception ignored) {}
        }
    }

    public void processApproval(String approverId, int registrationId, boolean approve) {
        Registration r;
        if (ConnectionManager.isConfigured()) {
            try { r = dao.getById(registrationId); } catch (Exception e) { r = null; }
        } else {
            r = DataStore.registrations.get(registrationId);
        }
        if (r == null) throw new IllegalArgumentException("Registration not found");
        Activity a;
        if (ConnectionManager.isConfigured()) { try { a = activityDao.getById(r.getActivityId()).orElse(null);} catch (Exception e) { a=null; } }
        else { a = DataStore.activities.get(r.getActivityId()); }
        if (a == null) throw new IllegalStateException("Activity missing");
        if (!(a.getCreatorId().equals(approverId))) throw new SecurityException("No permission");
        if (r.getStatus() != RegistrationStatus.Pending) throw new IllegalStateException("Already processed");

        if (approve) {
            if (a.getCurrentParticipants() >= a.getMaxParticipants()) throw new IllegalStateException("Activity full");
            r.setStatus(RegistrationStatus.Approved);
            r.setApprovalTime(LocalDateTime.now());
            r.setApprovedById(approverId);
            if (ConnectionManager.isConfigured()) {
                try {
                    dao.updateStatusApprove(registrationId, approverId, true);
                    int affected = activityDao.incrementParticipantsIfAvailable(a.getId());
                    if (affected == 0) throw new IllegalStateException("Activity full");
                } catch (RuntimeException ex) { throw ex; }
                catch (Exception ignored) {}
            } else {
                a.setCurrentParticipants(a.getCurrentParticipants() + 1);
                if (a.getCurrentParticipants() >= a.getMaxParticipants()) a.setStatus(ActivityStatus.Full);
                a.touchUpdatedAt();
            }
        } else {
            r.setStatus(RegistrationStatus.Rejected);
            r.setApprovalTime(LocalDateTime.now());
            r.setApprovedById(approverId);
            if (ConnectionManager.isConfigured()) { try { dao.updateStatusApprove(registrationId, approverId, false); } catch (Exception ignored) {} }
        }
    }

    public List<Registration> myRegistrations(String studentId) {
        if (!ConnectionManager.isConfigured()) return List.of();
        try { return dao.byStudent(studentId); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Registration> pendingForCreator(String creatorId) {
        if (!ConnectionManager.isConfigured()) return List.of();
        try { return dao.pendingForCreator(creatorId); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Registration> byActivity(int activityId) {
        if (!ConnectionManager.isConfigured()) return List.of();
        try { return dao.byActivity(activityId); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
