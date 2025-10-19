package com.eduactivity.model;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private String userId;
    private String title;
    private String content;
    private NotificationType type;
    private boolean read;
    private Integer relatedActivityId;
    private Integer relatedRegistrationId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime readAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Integer getRelatedActivityId() { return relatedActivityId; }
    public void setRelatedActivityId(Integer relatedActivityId) { this.relatedActivityId = relatedActivityId; }
    public Integer getRelatedRegistrationId() { return relatedRegistrationId; }
    public void setRelatedRegistrationId(Integer relatedRegistrationId) { this.relatedRegistrationId = relatedRegistrationId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}

