package com.campus.emergency.service.channel;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Simulated SMS Notification Channel.
 * Logs simulated SMS dispatch without requiring external Twilio or cellular API connections.
 */
public class SmsNotificationChannel implements NotificationChannel {

    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationChannel.class);

    @Override
    public void send(Notification notification) {
        String maskedPhone = "+91XXXXXX" + String.format("%04d", notification.getRecipientUserId() % 10000);

        logger.info("\n======================================================\n" +
                        "[SIMULATED SMS]\n" +
                        "Recipient: {}\n" +
                        "Message: [{}] {}\n" +
                        "Status: SIMULATED_SUCCESS\n" +
                        "=======================================================",
                maskedPhone, notification.getPriority(), notification.getMessage());

        notification.setStatus(NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
    }

    @Override
    public NotificationChannelType getChannelType() {
        return NotificationChannelType.SMS;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
