package com.campus.emergency.model;

/**
 * Lifecycle status of Emergency Reports.
 */
public enum EmergencyStatus {
    SUBMITTED("Submitted"),
    OPEN("Open / Triaged"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String displayName;

    EmergencyStatus(String displayName) {
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
