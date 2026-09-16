package com.campus.emergency.service.channel;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Simulated Mobile/Web Push Notification Channel.
 * Logs simulated push notification payload without requiring Firebase / APNs.
 */
public class PushNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationChannel.class);

    @Override
    public void send(Notification notification) {
        logger.info("\n======================================================\n" +
                        "[SIMULATED PUSH]\n" +
                        "Recipient: User #{}\n" +
                        "Title: {}\n" +
                        "Message: {}\n" +
                        "Status: SIMULATED_SUCCESS\n" +
                        "=======================================================",
                notification.getRecipientUserId(), notification.getTitle(), notification.getMessage());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.PUSH;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
