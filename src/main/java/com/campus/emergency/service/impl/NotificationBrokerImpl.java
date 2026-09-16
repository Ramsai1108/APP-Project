package com.campus.emergency.service.impl;

import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.dao.INotificationDao;
import com.campus.emergency.dao.IUserDao;
import com.campus.emergency.dao.impl.NotificationDaoImpl;
import com.campus.emergency.dao.impl.UserDaoImpl;
import com.campus.emergency.logging.AuditLogger;
import com.campus.emergency.model.*;
import com.campus.emergency.service.INotificationService;
import com.campus.emergency.service.NotificationBroker;
import com.campus.emergency.service.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of NotificationBroker and INotificationService.
 * Controls multi-channel alert routing, priority rules, recipient determination, async task execution,
 * and graceful channel failure isolation.
 */
public class NotificationBrokerImpl implements NotificationBroker, INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationBrokerImpl.class);

    private final INotificationDao notificationDao;
    private final IUserDao userDao;
    private final Map<NotificationChannelType, NotificationChannel> channels;

    public NotificationBrokerImpl(INotificationDao notificationDao, IUserDao userDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;

        // Register polymorphic notification channels
        this.channels = new ConcurrentHashMap<>();
        this.channels.put(NotificationChannelType.IN_APP, new InAppNotificationChannel(notificationDao));
        this.channels.put(NotificationChannelType.EMAIL, new EmailNotificationChannel());
        this.channels.put(NotificationChannelType.SMS, new SmsNotificationChannel());
        this.channels.put(NotificationChannelType.PUSH, new PushNotificationChannel());
    }

    public NotificationBrokerImpl(INotificationDao notificationDao) {
        this(notificationDao, new UserDaoImpl());
    }

    public NotificationBrokerImpl() {
        this(new NotificationDaoImpl(), new UserDaoImpl());
    }

    @Override
    public void dispatchNotification(Notification notification, String priorityLevel) {
        if (notification == null || notification.getRecipientUserId() == null) {
            logger.warn("Cannot dispatch null notification or notification without recipient ID");
            return;
        }

        String priority = (priorityLevel != null && !priorityLevel.trim().isEmpty())
                ? priorityLevel.trim().toUpperCase()
                : (notification.getPriority() != null ? notification.getPriority().toUpperCase() : "MEDIUM");

        notification.setPriority(priority);
        Set<NotificationChannelType> targetChannels = determineChannelsForPriority(priority);

        // Async execution to avoid blocking caller thread / ED Thread
        TaskExecutorManager.submitAsyncTask(() -> {
            for (NotificationChannelType channelType : targetChannels) {
                NotificationChannel channel = channels.get(channelType);
                if (channel == null || !channel.isAvailable()) {
                    logger.warn("Channel {} is not registered or unavailable", channelType);
                    continue;
                }

                try {
                    // Create distinct channel notification record
                    Notification channelNotif = new Notification(
                            notification.getRecipientUserId(),
                            notification.getIncidentId(),
                            notification.getNotificationType(),
                            channelType,
                            notification.getTitle(),
                            notification.getMessage(),
                            priority
                    );

                    // Persist notification record first
                    try {
                        notificationDao.create(channelNotif);
                    } catch (Exception ex) {
                        logger.warn("Database fallback for notification persistence: {}", ex.getMessage());
                    }

                    // Execute channel send strategy
                    channel.send(channelNotif);

                    AuditLogger.log(notification.getRecipientUserId(), "SEND_NOTIFICATION", "NOTIFICATION",
                            "Channel: " + channelType + " | Title: " + notification.getTitle() + " | Incident ID: #" + notification.getIncidentId());

                } catch (Exception e) {
                    logger.error("Failed to send notification via channel {} to user {}", channelType, notification.getRecipientUserId(), e);
                    AuditLogger.log(notification.getRecipientUserId(), "NOTIFICATION_FAILED", "NOTIFICATION",
                            "Channel: " + channelType + " | Error: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public Set<NotificationChannelType> determineChannelsForPriority(String priorityLevel) {
        Set<NotificationChannelType> channelSet = EnumSet.of(NotificationChannelType.IN_APP);

        if (priorityLevel == null) return channelSet;

        switch (priorityLevel.toUpperCase()) {
            case "CRITICAL":
                channelSet.add(NotificationChannelType.EMAIL);
                channelSet.add(NotificationChannelType.SMS);
                channelSet.add(NotificationChannelType.PUSH);
                break;
            case "HIGH":
                channelSet.add(NotificationChannelType.EMAIL);
                channelSet.add(NotificationChannelType.SMS);
                break;
            case "MEDIUM":
            case "LOW":
            default:
                // IN_APP only
                break;
        }

        return channelSet;
    }

    @Override
    public void notifyEmergencyCreated(Long incidentId, Long reporterId, String category, String location, double priorityScore) {
        String priorityLevel = priorityScore >= 80.0 ? "CRITICAL" : (priorityScore >= 60.0 ? "HIGH" : (priorityScore >= 40.0 ? "MEDIUM" : "LOW"));
        String title = (priorityScore >= 80.0 ? "🔴 " : (priorityScore >= 60.0 ? "🟠 " : "🟡 ")) + "New Emergency #" + incidentId;
        String message = String.format("Emergency report #%d submitted. Category: %s | Location: %s | Priority Score: %.1f",
                incidentId, category, location, priorityScore);

        // 1. Notify Dispatchers / Control Room (Default user ID 2)
        Notification dispatcherNotif = new Notification(2L, incidentId, NotificationType.EMERGENCY_CREATED,
                NotificationChannelType.IN_APP, title, message, priorityLevel);
        dispatchNotification(dispatcherNotif, priorityLevel);

        // 2. Notify Reporter Confirmation
        if (reporterId != null) {
            String reporterTitle = "Emergency Submitted - Incident #" + incidentId;
            String reporterMessage = "Your emergency report at " + location + " has been received and prioritized (" + priorityLevel + ").";
            Notification reporterNotif = new Notification(reporterId, incidentId, NotificationType.EMERGENCY_CREATED,
                    NotificationChannelType.IN_APP, reporterTitle, reporterMessage, priorityLevel);
            dispatchNotification(reporterNotif, priorityLevel);
        }
    }

    @Override
    public void notifyResourceDispatched(Long incidentId, Long reporterId, Long responderUserId, Long dispatcherUserId, String details) {
        String priorityLevel = "HIGH";
        String title = "🚨 Emergency Incident Dispatched - #" + incidentId;
        String message = "Responders and resources have been dispatched for Incident #" + incidentId + ". Details: " + details;

        // 1. Notify Assigned Responder
        if (responderUserId != null) {
            Notification responderNotif = new Notification(responderUserId, incidentId, NotificationType.RESOURCE_DISPATCHED,
                    NotificationChannelType.IN_APP, title, "You have been dispatched to Incident #" + incidentId + ". Details: " + details, priorityLevel);
            dispatchNotification(responderNotif, priorityLevel);
        }

        // 2. Notify Reporter
        if (reporterId != null) {
            Notification reporterNotif = new Notification(reporterId, incidentId, NotificationType.RESOURCE_DISPATCHED,
                    NotificationChannelType.IN_APP, "Help is on the way! Incident #" + incidentId, "Emergency responders are en route to your location. " + details, priorityLevel);
            dispatchNotification(reporterNotif, priorityLevel);
        }

        // 3. Notify Dispatcher Confirmation
        if (dispatcherUserId != null) {
            Notification dispatcherNotif = new Notification(dispatcherUserId, incidentId, NotificationType.RESOURCE_DISPATCHED,
                    NotificationChannelType.IN_APP, "Dispatch Confirmation - #" + incidentId, "Successful dispatch for Incident #" + incidentId + ". " + details, "MEDIUM");
            dispatchNotification(dispatcherNotif, "MEDIUM");
        }
    }

    @Override
    public void notifyIncidentStatusChanged(Long incidentId, Long reporterId, Long responderUserId, String oldStatus, String newStatus) {
        String priorityLevel = "RESOLVED".equalsIgnoreCase(newStatus) ? "MEDIUM" : "HIGH";
        String title = "Status Update: Incident #" + incidentId + " [" + newStatus + "]";
        String message = String.format("Incident #%d status changed from %s to %s.", incidentId, oldStatus, newStatus);

        NotificationType notifType = "RESOLVED".equalsIgnoreCase(newStatus)
                ? NotificationType.INCIDENT_RESOLVED
                : NotificationType.INCIDENT_STATUS_CHANGED;

        // 1. Notify Reporter
        if (reporterId != null) {
            Notification reporterNotif = new Notification(reporterId, incidentId, notifType,
                    NotificationChannelType.IN_APP, title, message, priorityLevel);
            dispatchNotification(reporterNotif, priorityLevel);
        }

        // 2. Notify Responder if assigned
        if (responderUserId != null) {
            Notification responderNotif = new Notification(responderUserId, incidentId, notifType,
                    NotificationChannelType.IN_APP, title, message, priorityLevel);
            dispatchNotification(responderNotif, priorityLevel);
        }
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId) {
        if (userId == null) return Collections.emptyList();
        return notificationDao.findByRecipientUserId(userId);
    }

    @Override
    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        if (userId == null) return Collections.emptyList();
        return notificationDao.findUnreadByRecipientUserId(userId);
    }

    @Override
    public boolean markAsRead(Long notificationId, Long requestingUserId) {
        if (notificationId == null || requestingUserId == null) return false;

        Optional<Notification> opt = notificationDao.findById(notificationId);
        if (opt.isPresent() && !opt.get().getRecipientUserId().equals(requestingUserId)) {
            logger.warn("Security Violation: User {} attempted to mark notification {} belonging to user {}",
                    requestingUserId, notificationId, opt.get().getRecipientUserId());
            AuditLogger.log(requestingUserId, "UNAUTHORIZED_NOTIFICATION_ACCESS", "NOTIFICATION", "Notification ID: #" + notificationId);
            return false;
        }

        boolean success = notificationDao.markAsRead(notificationId);
        if (success) {
            AuditLogger.log(requestingUserId, "NOTIFICATION_READ", "NOTIFICATION", "Marked notification #" + notificationId + " as read");
        }
        return success;
    }

    @Override
    public boolean markAllAsRead(Long userId) {
        if (userId == null) return false;
        boolean success = notificationDao.markAllAsRead(userId);
        if (success) {
            AuditLogger.log(userId, "NOTIFICATION_ALL_READ", "NOTIFICATION", "Marked all notifications as read");
        }
        return success;
    }

    @Override
    public int countUnreadForUser(Long userId) {
        if (userId == null) return 0;
        return notificationDao.countUnreadByRecipientUserId(userId);
    }

    // =========================================================================
    // INotificationService Legacy Compatibility Methods
    // =========================================================================

    @Override
    public void broadcastCampusAlert(String title, String message, String alertLevel) {
        logger.info("BROADCAST ALERT [{}]: {} - {}", alertLevel, title, message);
        // Notify control room (user 2) and system admin (user 1)
        Notification notif1 = new Notification(2L, null, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, title, message, alertLevel);
        Notification notif2 = new Notification(1L, null, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, title, message, alertLevel);

        dispatchNotification(notif1, alertLevel);
        dispatchNotification(notif2, alertLevel);
    }

    @Override
    public void sendTargetedAlert(Long userId, String title, String message) {
        logger.info("TARGETED ALERT [User {}]: {} - {}", userId, title, message);
        Notification notification = new Notification(userId, null, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, title, message, "MEDIUM");
        dispatchNotification(notification, "MEDIUM");
    }
}
