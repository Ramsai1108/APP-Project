package com.campus.emergency.service.impl;

import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.PriorityLevel;
import com.campus.emergency.model.TimeSensitivity;
import com.campus.emergency.service.PriorityEngine;
import com.campus.emergency.service.PriorityResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transparent rule-based implementation of PriorityEngine.
 */
public class PriorityEngineImpl implements PriorityEngine {

    private static final Logger logger = LoggerFactory.getLogger(PriorityEngineImpl.class);

    @Override
    public PriorityResult calculatePriority(String category, int severity, String location, int affectedPeople, TimeSensitivity timeSensitivity) {
        return calculatePriority(category, severity, location, 1.0, affectedPeople, timeSensitivity);
    }

    @Override
    public PriorityResult calculatePriority(EmergencyReport report, double buildingVulnerabilityScore, TimeSensitivity timeSensitivity) {
        if (report == null) {
            return new PriorityResult(0.0, PriorityLevel.LOW, "NULL_REPORT", "Report data was null");
        }
        return calculatePriority(
                report.getCategory(),
                report.getSeverity(),
                report.getLocation(),
                buildingVulnerabilityScore,
                report.getAffectedPeople(),
                timeSensitivity
        );
    }

    private PriorityResult calculatePriority(String category, int severity, String location, double vulnerabilityScore, int affectedPeople, TimeSensitivity timeSensitivity) {
        String cleanCategory = category != null ? category.trim().toUpperCase() : "OTHER";
        int cleanSeverity = Math.min(Math.max(severity, 1), 5);
        int cleanAffected = Math.max(affectedPeople, 0);
        TimeSensitivity sensitivity = timeSensitivity != null ? timeSensitivity : TimeSensitivity.NORMAL;

        // Base Category Weight
        double categoryWeight = switch (cleanCategory) {
            case "SECURITY", "ACTIVE_THREAT" -> 40.0;
            case "FIRE" -> 35.0;
            case "NATURAL_DISASTER" -> 35.0;
            case "MEDICAL" -> 30.0;
            case "ACCIDENT" -> 20.0;
            default -> 10.0;
        };

        // Component Weights
        double severityWeight = cleanSeverity * 6.0; // 6 to 30
        double affectedFactor = Math.min(cleanAffected * 4.0, 20.0); // 0 to 20
        double locationFactor = Math.min(Math.max(vulnerabilityScore, 1.0) * 1.5, 15.0); // 1.5 to 15

        // Raw Base Score
        double baseScore = categoryWeight + severityWeight + affectedFactor + locationFactor;

        // Apply Time Sensitivity Multiplier
        double calculatedScore = baseScore * sensitivity.getMultiplier();

        String ruleApplied = "STANDARD_SCORING_RULE";

        // =========================================================================
        // Explicit Business Rule Overrides & Floors
        // =========================================================================
        if ("FIRE".equals(cleanCategory) && cleanSeverity == 5) {
            calculatedScore = Math.max(calculatedScore, 85.0);
            ruleApplied = "RULE_FIRE_CRITICAL_SEVERITY_5";
        } else if ("MEDICAL".equals(cleanCategory) && cleanSeverity == 5) {
            calculatedScore = Math.max(calculatedScore, 85.0);
            ruleApplied = "RULE_MEDICAL_CRITICAL_SEVERITY_5";
        } else if ("SECURITY".equals(cleanCategory) && cleanSeverity == 5) {
            calculatedScore = Math.max(calculatedScore, 90.0);
            ruleApplied = "RULE_SECURITY_CRITICAL_SEVERITY_5";
        } else if ("SECURITY".equals(cleanCategory) && cleanSeverity == 4) {
            calculatedScore = Math.max(calculatedScore, 70.0);
            ruleApplied = "RULE_SECURITY_HIGH_SEVERITY_4";
        } else if ("ACCIDENT".equals(cleanCategory) && cleanSeverity == 3) {
            calculatedScore = Math.min(Math.max(calculatedScore, 45.0), 59.0);
            ruleApplied = "RULE_ACCIDENT_MEDIUM_SEVERITY_3";
        } else if ("NATURAL_DISASTER".equals(cleanCategory) && cleanSeverity >= 4) {
            calculatedScore = Math.max(calculatedScore, 80.0);
            ruleApplied = "RULE_NATURAL_DISASTER_CRITICAL";
        }

        // Final score capping between 0.0 and 100.0
        final double finalScore = Math.min(Math.max(calculatedScore, 0.0), 100.0);

        // Map to PriorityLevel Enum
        PriorityLevel level;
        if (finalScore >= 80.0) {
            level = PriorityLevel.CRITICAL;
        } else if (finalScore >= 60.0) {
            level = PriorityLevel.HIGH;
        } else if (finalScore >= 40.0) {
            level = PriorityLevel.MEDIUM;
        } else {
            level = PriorityLevel.LOW;
        }

        String explanation = String.format("Category: %s | Severity: %d | Affected: %d | TimeSensitivity: %s | Rule: %s -> Score: %.1f [%s]",
                cleanCategory, cleanSeverity, cleanAffected, sensitivity.name(), ruleApplied, finalScore, level.name());

        logger.info("Priority Engine Computed: {}", explanation);

        return new PriorityResult(finalScore, level, ruleApplied, explanation);
    }
}
