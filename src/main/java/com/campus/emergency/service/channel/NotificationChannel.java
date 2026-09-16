package com.campus.emergency.service.channel;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;

/**
 * Polymorphic strategy interface for notification delivery channels.
 */
public interface NotificationChannel {
    
    /**
     * Deliver the notification through this channel.
     * 
     * @param notification The notification entity to deliver
     */
    void send(Notification notification);

    /**
     * Get the channel type supported by this implementation.
     * 
     * @return NotificationChannelType enum value
     */
    NotificationChannelType getChannelType();

    /**
     * Check if the channel is available for delivery.
     * 
     * @return true if operational
     */
    boolean isAvailable();
}
