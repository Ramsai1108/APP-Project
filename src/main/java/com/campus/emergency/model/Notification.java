package com.campus.emergency.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model representing an emergency notification entity.
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long recipientUserId;
    private Long incidentId;
    private NotificationType notificationType;
    private NotificationChannelType channel;
    private String title;
    private String message;
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public Notification() {
        this.status = NotificationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Notification(Long recipientUserId, Long incidentId, NotificationType notificationType,
                        NotificationChannelType channel, String title, String message, String priority) {
        this();
        this.recipientUserId = recipientUserId;
        this.incidentId = incidentId;
        this.notificationType = notificationType;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public NotificationChannelType getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannelType channel) {
        this.channel = channel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", recipientUserId=" + recipientUserId +
                ", incidentId=" + incidentId +
                ", notificationType=" + notificationType +
                ", channel=" + channel +
                ", title='" + title + '\'' +
                ", priority='" + priority + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
