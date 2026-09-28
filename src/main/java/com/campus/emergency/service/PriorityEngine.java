package com.campus.emergency.service;

public class PriorityEngine {

    public String calculatePriority(String category, String description, int peopleAffected) {
        if (category == null || category.trim().isEmpty()) {
            return "MEDIUM";
        }

        String catUpper = category.trim().toUpperCase();
        String descUpper = (description != null) ? description.trim().toUpperCase() : "";

        // Rule 1: Immediate life-safety emergencies are CRITICAL
        if (catUpper.contains("FIRE") || catUpper.contains("ACTIVE_SHOOTER") ||
            catUpper.contains("EXPLOSION") || catUpper.contains("HAZMAT") ||
            descUpper.contains("BOMB") || descUpper.contains("SHOOTER")) {
            return "CRITICAL";
        }

        // Rule 2: Medical emergencies are CRITICAL if multiple people affected, otherwise HIGH
        if (catUpper.contains("MEDICAL") || catUpper.contains("INJURY")) {
            return peopleAffected > 2 ? "CRITICAL" : "HIGH";
        }

        // Rule 3: Property crimes and minor disruptions are LOW priority
        if (catUpper.contains("THEFT") || catUpper.contains("VANDALISM") ||
            catUpper.contains("NOISE") || catUpper.contains("LOST_PROPERTY")) {
            return "LOW";
        }

        // Rule 4: Severe weather or facility failures with multiple people affected
        if (catUpper.contains("FACILITY") || catUpper.contains("WEATHER")) {
            return peopleAffected > 5 ? "HIGH" : "MEDIUM";
        }

        // Default Rule: Medium priority for general unclassified emergencies
        return "MEDIUM";
    }

    public String calculatePriority(String category, String description) {
        return calculatePriority(category, description, 1);
    }
}
