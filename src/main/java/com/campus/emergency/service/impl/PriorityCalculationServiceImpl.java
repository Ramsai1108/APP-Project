package com.campus.emergency.service.impl;

import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.TimeSensitivity;
import com.campus.emergency.service.IPriorityCalculationService;
import com.campus.emergency.service.PriorityEngine;
import com.campus.emergency.service.PriorityResult;

/**
 * Priority Calculation service delegating to PriorityEngine.
 */
public class PriorityCalculationServiceImpl implements IPriorityCalculationService {

    private final PriorityEngine priorityEngine;

    public PriorityCalculationServiceImpl() {
        this.priorityEngine = new PriorityEngineImpl();
    }

    public PriorityCalculationServiceImpl(PriorityEngine priorityEngine) {
        this.priorityEngine = priorityEngine;
    }

    @Override
    public double calculatePriorityScore(EmergencyReport report, double buildingVulnerabilityScore) {
        PriorityResult result = priorityEngine.calculatePriority(report, buildingVulnerabilityScore, TimeSensitivity.NORMAL);
        return result.numericScore();
    }
}
