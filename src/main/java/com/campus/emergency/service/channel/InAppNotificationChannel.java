package com.campus.emergency.service.channel;

import com.campus.emergency.dao.INotificationDao;
import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Functional In-App Notification Channel.
 * Persists and updates notification status in MySQL database for Swing UI retrieval.
 */
public class InAppNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(InAppNotificationChannel.class);

    private final INotificationDao notificationDao;

    public InAppNotificationChannel(INotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public void send(Notification notification) {
        logger.info("[IN_APP NOTIFICATION] Recipient User ID: {} | Title: '{}' | Message: '{}'",
                notification.getRecipientUserId(), notification.getTitle(), notification.getMessage());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());

        if (notification.getId() != null) {
            notificationDao.updateStatus(notification.getId(), NotificationStatus.SENT);
        } else {
            notificationDao.create(notification);
        }
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.IN_APP;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
