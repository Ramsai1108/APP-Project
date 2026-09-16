package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import java.util.List;

/**
 * Incident service interface.
 */
public interface IIncidentService {
    EmergencyIncident reportAndCreateIncident(EmergencyReport report);
    boolean updateIncidentStatus(Long incidentId, String newStatus, Long updatedByUserId);
    boolean overridePriority(Long incidentId, double newPriority, Long dispatcherId);
    List<EmergencyIncident> getActiveIncidentsSortedByPriority();
}
