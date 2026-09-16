package com.campus.emergency.dao;

import com.campus.emergency.model.EmergencyIncident;
import java.util.List;

/**
 * EmergencyIncident DAO interface.
 */
public interface IIncidentDao extends IGenericDao<EmergencyIncident, Long> {
    List<EmergencyIncident> findByStatus(String status);
    List<EmergencyIncident> findActiveIncidentsSortedByPriority();
}
