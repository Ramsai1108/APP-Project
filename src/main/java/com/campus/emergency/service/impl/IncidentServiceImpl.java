package com.campus.emergency.service.impl;

import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.service.IIncidentService;
import com.campus.emergency.service.IPriorityCalculationService;
import java.util.ArrayList;
import java.util.List;

/**
 * Incident service implementation stub.
 */
public class IncidentServiceImpl implements IIncidentService {

    private final IIncidentDao incidentDao;
    private final IPriorityCalculationService priorityCalculationService;

    public IncidentServiceImpl(IIncidentDao incidentDao, IPriorityCalculationService priorityCalculationService) {
        this.incidentDao = incidentDao;
        this.priorityCalculationService = priorityCalculationService;
    }

    @Override
    public EmergencyIncident reportAndCreateIncident(EmergencyReport report) {
        return new EmergencyIncident();
    }

    @Override
    public boolean updateIncidentStatus(Long incidentId, String newStatus, Long updatedByUserId) {
        return true;
    }

    @Override
    public boolean overridePriority(Long incidentId, double newPriority, Long dispatcherId) {
        return true;
    }

    @Override
    public List<EmergencyIncident> getActiveIncidentsSortedByPriority() {
        return new ArrayList<>();
    }
}
