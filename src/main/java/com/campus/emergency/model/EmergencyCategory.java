package com.campus.emergency.model;

/**
 * Emergency Categories supported by Module 2.
 */
public enum EmergencyCategory {
    MEDICAL("Medical Emergency"),
    FIRE("Fire Emergency"),
    SECURITY("Security Threat"),
    ACCIDENT("Accident / Injury"),
    NATURAL_DISASTER("Natural Disaster / Weather"),
    OTHER("Other Emergency");

    private final String displayName;

    EmergencyCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
