package com.campus.emergency.model;

/**
 * Emergency Severity Levels (1 to 5).
 */
public enum EmergencySeverity {
    LOW(1, "1 — LOW"),
    MODERATE(2, "2 — MODERATE"),
    SERIOUS(3, "3 — SERIOUS"),
    HIGH(4, "4 — HIGH"),
    CRITICAL(5, "5 — CRITICAL");

    private final int level;
    private final String label;

    EmergencySeverity(int level, String label) {
        this.level = level;
        this.label = label;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public static EmergencySeverity fromLevel(int level) {
        for (EmergencySeverity s : values()) {
            if (s.level == level) {
                return s;
            }
        }
        return LOW;
    }

    @Override
    public String toString() {
        return label;
    }
}
