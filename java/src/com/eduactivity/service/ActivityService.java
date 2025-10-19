package com.eduactivity.service;

import com.eduactivity.dao.ActivityDao;
import com.eduactivity.db.ConnectionManager;
import com.eduactivity.model.*;
import com.eduactivity.store.DataStore;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ActivityService {
    private final ActivityDao dao = new ActivityDao();

    public List<Activity> listAll() {
        if (!ConnectionManager.isConfigured()) return List.of();
        try { return dao.listAll(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public List<Activity> listByCreator(String creatorId) {
        if (!ConnectionManager.isConfigured()) return List.of();
        try { return dao.listByCreator(creatorId); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Optional<Activity> getById(int id) {
        if (!ConnectionManager.isConfigured()) return Optional.empty();
        try { return dao.getById(id); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public Activity create(User creator, String title, String description, LocalDateTime start, LocalDateTime end,
                           String location, int maxParticipants, boolean requireApproval, String category) {
        if (!start.isBefore(end)) throw new IllegalArgumentException("Start must be before end");
        if (!ConnectionManager.isConfigured()) throw new IllegalStateException("DB not configured");
        Activity a = new Activity();
        a.setTitle(title); a.setDescription(description);
        a.setStartTime(start); a.setEndTime(end);
        a.setLocation(location);
        a.setMaxParticipants(maxParticipants);
        a.setCurrentParticipants(0);
        a.setRequireApproval(requireApproval);
        a.setCategory(category);
        a.setCreatorId(creator.getId());
        try { return dao.insert(a); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void cancel(User current, int activityId) {
        Activity a = getById(activityId).orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        if (!(current.getRole() == UserRole.Admin || a.getCreatorId().equals(current.getId()))) {
            throw new SecurityException("No permission to cancel");
        }
        if (!ConnectionManager.isConfigured()) throw new IllegalStateException("DB not configured");
        try { dao.cancel(activityId); } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void update(User current, Activity patch) {
        Activity a = getById(patch.getId()).orElseThrow(() -> new IllegalArgumentException("Activity not found"));
        if (!(current.getRole() == UserRole.Admin || a.getCreatorId().equals(current.getId()))) {
            throw new SecurityException("No permission to update");
        }
        LocalDateTime start = patch.getStartTime() != null ? patch.getStartTime() : a.getStartTime();
        LocalDateTime end = patch.getEndTime() != null ? patch.getEndTime() : a.getEndTime();
        if (!start.isBefore(end)) throw new IllegalArgumentException("Start must be before end");
        int max = patch.getMaxParticipants() > 0 ? patch.getMaxParticipants() : a.getMaxParticipants();
        if (max < a.getCurrentParticipants()) throw new IllegalArgumentException("Max < current participants");

        a.setTitle(patch.getTitle() != null ? patch.getTitle() : a.getTitle());
        a.setDescription(patch.getDescription() != null ? patch.getDescription() : a.getDescription());
        a.setStartTime(start);
        a.setEndTime(end);
        a.setLocation(patch.getLocation() != null ? patch.getLocation() : a.getLocation());
        a.setMaxParticipants(max);
        a.setRequireApproval(patch.isRequireApproval());
        a.setCategory(patch.getCategory() != null ? patch.getCategory() : a.getCategory());
        a.touchUpdatedAt();

        if (!ConnectionManager.isConfigured()) throw new IllegalStateException("DB not configured");
        try { dao.update(a); } catch (Exception e) { throw new RuntimeException(e); }
    }
}
