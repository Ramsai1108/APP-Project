package com.campus.emergency.model;

import java.time.LocalDateTime;

public class SystemLog {
    private int id;
    private String eventType;
    private Integer userId;
    private String details;
    private LocalDateTime createdAt;

    public SystemLog() {}

    public SystemLog(int id, String eventType, Integer userId, String details, LocalDateTime createdAt) {
        this.id = id;
        this.eventType = eventType;
        this.userId = userId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
