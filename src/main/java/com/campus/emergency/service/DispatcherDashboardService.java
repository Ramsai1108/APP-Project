package com.campus.emergency.service;

import com.campus.emergency.model.DispatcherDashboardMetrics;
import com.campus.emergency.model.EmergencyIncident;

import java.util.List;

/**
 * Service interface for Dispatcher Control Room dashboard operations,
 * incident monitoring, critical alerts, search/filtering, and statistical aggregation.
 */
public interface DispatcherDashboardService {

    /**
     * Compute real-time operational statistics for the dispatcher dashboard.
     */
    DispatcherDashboardMetrics getDashboardMetrics(Long dispatcherUserId);

    /**
     * Retrieve all active emergency incidents sorted by priority.
     */
    List<EmergencyIncident> getActiveIncidents(Long dispatcherUserId);

    /**
     * Retrieve critical and high-priority incidents sorted by priority score.
     */
    List<EmergencyIncident> getCriticalIncidents(Long dispatcherUserId);

    /**
     * Search, filter, and retrieve emergency incidents.
     */
    List<EmergencyIncident> searchAndFilterIncidents(Long dispatcherUserId, String query, String category, String priorityLevel, String status);

    /**
     * Sort incident list by specified criteria.
     */
    List<EmergencyIncident> sortIncidents(List<EmergencyIncident> incidents, String sortBy);

    /**
     * Validate dispatcher/admin role authorization.
     */
    void validateDispatcherAuthorization(Long userId);
}
