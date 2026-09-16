package com.campus.emergency.model;

/**
 * Emergency Priority Levels.
 */
public enum PriorityLevel {
    CRITICAL("CRITICAL", "Immediate Life/Structural Danger"),
    HIGH("HIGH", "Urgent Response Needed"),
    MEDIUM("MEDIUM", "Moderate Concern"),
    LOW("LOW", "Standard Operational Concern");

    private final String code;
    private final String description;

    PriorityLevel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code;
    }
}
