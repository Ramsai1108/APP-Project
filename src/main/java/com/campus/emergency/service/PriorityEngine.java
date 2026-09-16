package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.TimeSensitivity;

/**
 * Service interface for dynamic rule-based emergency priority calculation.
 */
public interface PriorityEngine {

    PriorityResult calculatePriority(String category, int severity, String location, int affectedPeople, TimeSensitivity timeSensitivity);

    PriorityResult calculatePriority(EmergencyReport report, double buildingVulnerabilityScore, TimeSensitivity timeSensitivity);
}
