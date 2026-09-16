package com.campus.emergency.dao;

import com.campus.emergency.model.IncidentTimeline;

import java.util.List;

/**
 * Data Access Object interface for Incident Timeline event tracking.
 */
public interface IIncidentTimelineDao extends IGenericDao<IncidentTimeline, Long> {
    List<IncidentTimeline> findByIncidentId(Long incidentId);
}
