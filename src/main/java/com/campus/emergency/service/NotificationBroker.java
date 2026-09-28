package com.campus.emergency.service;

import com.campus.emergency.infrastructure.LoggingService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationBroker {

    public enum NotificationType {
        PUSH, SMS, EMAIL, IN_APP
    }

    public enum EventType {
        EMERGENCY_REPORTED,
        INCIDENT_ACKNOWLEDGED,
        RESOURCE_DISPATCHED,
        STATUS_CHANGED,
        INCIDENT_RESOLVED,
        RESPONDER_ASSIGNED,
        RESOURCE_ASSIGNED,
        DISPATCH_UPDATED,
        RESOURCE_RELEASED,
        LOCATION_UPDATED
    }

    public static class NotificationMessage {
        private final EventType eventType;
        private final String recipient;
        private final String content;
        private final NotificationType type;
        private final LocalDateTime timestamp;

        public NotificationMessage(EventType eventType, String recipient, String content, NotificationType type) {
            this.eventType = eventType;
            this.recipient = recipient;
            this.content = content;
            this.type = type;
            this.timestamp = LocalDateTime.now();
        }

        public EventType getEventType() { return eventType; }
        public String getRecipient() { return recipient; }
        public String getContent() { return content; }
        public NotificationType getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    private final List<NotificationMessage> sentNotifications = Collections.synchronizedList(new ArrayList<>());

    public void notifyEvent(EventType eventType, String recipient, String message, NotificationType type) {
        NotificationMessage notification = new NotificationMessage(eventType, recipient, message, type);
        sentNotifications.add(notification);

        LoggingService.info(String.format("[NOTIFICATION - %s - %s] To: %s | Message: %s",
                eventType, type, recipient, message));
    }

    public void sendNotification(String recipient, String message, NotificationType type) {
        notifyEvent(EventType.STATUS_CHANGED, recipient, message, type);
    }

    public List<NotificationMessage> getSentNotifications() {
        return new ArrayList<>(sentNotifications);
    }

    public List<NotificationMessage> getNotificationsForUser(com.campus.emergency.model.User user) throws com.campus.emergency.exception.EmergencyProcessingException {
        if (user == null) {
            throw new com.campus.emergency.exception.EmergencyProcessingException("Authentication required to retrieve notifications");
        }

        String userIdStr = String.valueOf(user.getId());
        String usernameStr = user.getUsername() != null ? user.getUsername() : "";
        com.campus.emergency.model.Role role = user.getRole();

        List<NotificationMessage> userNotifications = new ArrayList<>();
        synchronized (sentNotifications) {
            for (NotificationMessage msg : sentNotifications) {
                String r = msg.getRecipient();
                if (r == null) continue;

                if (r.equalsIgnoreCase("ALL") || r.equalsIgnoreCase("ALL_STAKEHOLDERS") ||
                        r.equalsIgnoreCase(userIdStr) || r.equalsIgnoreCase(usernameStr)) {
                    userNotifications.add(msg);
                } else if ((role == com.campus.emergency.model.Role.DISPATCHER || role == com.campus.emergency.model.Role.ADMIN) &&
                        r.equalsIgnoreCase("DISPATCH_CENTER")) {
                    userNotifications.add(msg);
                } else if (role == com.campus.emergency.model.Role.STAFF && r.equalsIgnoreCase("STAFF")) {
                    userNotifications.add(msg);
                } else if (role == com.campus.emergency.model.Role.STUDENT && r.equalsIgnoreCase("STUDENT")) {
                    userNotifications.add(msg);
                }
            }
        }
        return userNotifications;
    }

    public void clearNotifications() {
        sentNotifications.clear();
    }
}
