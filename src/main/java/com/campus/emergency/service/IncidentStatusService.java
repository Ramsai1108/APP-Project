package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyIncident;

/**
 * Service interface for incident status lifecycle management and state transitions.
 */
public interface IncidentStatusService {

    /**
     * Check if a proposed status transition is valid according to state machine rules.
     */
    boolean isValidTransition(String currentStatus, String targetStatus);

    /**
     * Execute a status update for an emergency incident with validation, audit logging, timeline entry, and notifications.
     */
    EmergencyIncident updateIncidentStatus(Long incidentId, String targetStatus, Long actingUserId, String reason);
}
