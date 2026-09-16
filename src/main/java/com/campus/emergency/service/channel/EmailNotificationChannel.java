package com.campus.emergency.service.channel;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Simulated Email Notification Channel.
 * Logs simulated email output without requiring external SMTP or provider connection.
 */
public class EmailNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationChannel.class);

    @Override
    public void send(Notification notification) {
        String recipientEmail = "user_" + notification.getRecipientUserId() + "@campus.local";

        logger.info("\n======================================================\n" +
                        "[SIMULATED EMAIL]\n" +
                        "To: {}\n" +
                        "Subject: {}\n" +
                        "Message: {}\n" +
                        "Priority: {}\n" +
                        "Status: SIMULATED_SUCCESS\n" +
                        "=======================================================",
                recipientEmail, notification.getTitle(), notification.getMessage(), notification.getPriority());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
