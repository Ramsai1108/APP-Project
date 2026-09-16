package com.campus.emergency.controller;

import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.service.IIncidentService;
import java.util.List;

/**
 * Controller mediating emergency reporting and incident management actions.
 */
public class IncidentController {

    private final IIncidentService incidentService;

    public IncidentController(IIncidentService incidentService) {
        this.incidentService = incidentService;
    }

    public EmergencyIncident submitReport(EmergencyReport report) {
        return incidentService.reportAndCreateIncident(report);
    }

    public boolean updateStatus(Long incidentId, String status, Long userId) {
        return incidentService.updateIncidentStatus(incidentId, status, userId);
    }

    public List<EmergencyIncident> getActiveIncidents() {
        return incidentService.getActiveIncidentsSortedByPriority();
    }
}
