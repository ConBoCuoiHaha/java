package com.eduactivity.service;

import com.eduactivity.model.Notification;
import com.eduactivity.model.NotificationType;
import com.eduactivity.store.DataStore;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationService {
    public Notification notifyUser(String userId, String title, String content, NotificationType type, Integer activityId, Integer registrationId) {
        return DataStore.createNotification(userId, title, content, type, activityId, registrationId);
    }

    public List<Notification> getUserNotifications(String userId) {
        return DataStore.notifications.values().stream()
                .filter(n -> n.getUserId().equals(userId))
                .sorted((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }
}

