package com.campus.emergency.service;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;

import java.util.List;
import java.util.Set;

/**
 * Service interface for Notification Broker & Emergency Alert System.
 * Manages notification creation, routing, multi-channel dispatch, recipient resolution, and status tracking.
 */
public interface NotificationBroker {

    /**
     * Dispatch notification to a specific recipient over designated channels based on priority level.
     */
    void dispatchNotification(Notification notification, String priorityLevel);

    /**
     * Helper method to notify dispatchers/admins of a new emergency report.
     */
    void notifyEmergencyCreated(Long incidentId, Long reporterId, String category, String location, double priorityScore);

    /**
     * Helper method to notify reporter, responder, and dispatcher when resources/responders are dispatched.
     */
    void notifyResourceDispatched(Long incidentId, Long reporterId, Long responderUserId, Long dispatcherUserId, String details);

    /**
     * Helper method to notify relevant parties when an incident status changes.
     */
    void notifyIncidentStatusChanged(Long incidentId, Long reporterId, Long responderUserId, String oldStatus, String newStatus);

    /**
     * Determine supported channels for a given priority level.
     */
    Set<NotificationChannelType> determineChannelsForPriority(String priorityLevel);

    /**
     * Get notification history for a user.
     */
    List<Notification> getNotificationsForUser(Long userId);

    /**
     * Get unread notifications for a user.
     */
    List<Notification> getUnreadNotificationsForUser(Long userId);

    /**
     * Mark a notification as read.
     */
    boolean markAsRead(Long notificationId, Long requestingUserId);

    /**
     * Mark all notifications as read for a user.
     */
    boolean markAllAsRead(Long userId);

    /**
     * Count unread notifications for a user.
     */
    int countUnreadForUser(Long userId);
}
