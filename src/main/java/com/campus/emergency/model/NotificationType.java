package com.campus.emergency.model;

/**
 * Categorization of notification trigger events.
 */
public enum NotificationType {
    EMERGENCY_CREATED,
    EMERGENCY_PRIORITIZED,
    RESOURCE_DISPATCHED,
    INCIDENT_STATUS_CHANGED,
    INCIDENT_RESOLVED,
    SYSTEM_ALERT
}
