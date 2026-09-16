package com.campus.emergency.service;

import com.campus.emergency.model.PriorityLevel;

/**
 * Result object produced by PriorityEngine containing numeric score, priority level, and rule audit trace.
 */
public record PriorityResult(
        double numericScore,
        PriorityLevel priorityLevel,
        String categoryRule,
        String explanation
) {
    public PriorityResult(double numericScore, PriorityLevel priorityLevel, String categoryRule, String explanation) {
        this.numericScore = Math.min(Math.max(numericScore, 0.0), 100.0);
        this.priorityLevel = priorityLevel;
        this.categoryRule = categoryRule;
        this.explanation = explanation;
    }
}
