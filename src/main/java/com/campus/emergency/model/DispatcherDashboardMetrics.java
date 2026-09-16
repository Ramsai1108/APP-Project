package com.campus.emergency.model;

import java.io.Serializable;

/**
 * Record DTO containing statistical metrics for the Dispatcher Control Dashboard.
 */
public record DispatcherDashboardMetrics(
        int activeIncidentCount,
        int criticalIncidentCount,
        int highPriorityIncidentCount,
        int availableResponderCount,
        int busyResponderCount,
        int availableResourceCount,
        int inUseResourceCount
) implements Serializable {}
