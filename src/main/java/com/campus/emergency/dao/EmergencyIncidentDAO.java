package com.campus.emergency.dao;

import com.campus.emergency.model.EmergencyIncident;
import java.util.List;
import java.util.Optional;

public interface EmergencyIncidentDAO {
    EmergencyIncident create(EmergencyIncident incident);
    Optional<EmergencyIncident> findById(int id);
    Optional<EmergencyIncident> findByReportId(int reportId);
    List<EmergencyIncident> findAllActive();
    List<EmergencyIncident> findAll();
    boolean updatePriority(int id, String priority);
    boolean updateStatus(int id, String status);
    boolean assignDispatcher(int incidentId, int dispatcherId);
}
