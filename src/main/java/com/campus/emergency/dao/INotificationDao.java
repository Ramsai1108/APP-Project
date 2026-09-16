package com.campus.emergency.dao;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationStatus;

import java.util.List;

/**
 * Data Access Object interface for Notification entity operations.
 */
public interface INotificationDao extends IGenericDao<Notification, Long> {
    List<Notification> findByRecipientUserId(Long recipientUserId);
    List<Notification> findUnreadByRecipientUserId(Long recipientUserId);
    boolean markAsRead(Long notificationId);
    boolean markAllAsRead(Long recipientUserId);
    boolean updateStatus(Long notificationId, NotificationStatus status);
    List<Notification> findByIncidentId(Long incidentId);
    int countUnreadByRecipientUserId(Long recipientUserId);
}
