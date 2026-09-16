package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyReport;

/**
 * Emergency Priority Calculation engine service interface.
 */
public interface IPriorityCalculationService {
    double calculatePriorityScore(EmergencyReport report, double buildingVulnerabilityScore);
}
