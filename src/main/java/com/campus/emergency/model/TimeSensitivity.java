package com.campus.emergency.model;

/**
 * Time Sensitivity factors influencing emergency priority.
 */
public enum TimeSensitivity {
    IMMEDIATE(1.30, "Immediate Action Required"),
    URGENT(1.15, "Urgent Action Required"),
    NORMAL(1.00, "Normal Response Time"),
    LOW(0.85, "Non-Urgent Concern");

    private final double multiplier;
    private final String label;

    TimeSensitivity(double multiplier, String label) {
        this.multiplier = multiplier;
        this.label = label;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
