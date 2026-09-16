package com.campus.emergency.model;

import java.time.LocalDateTime;

/**
 * EmergencyIncident entity representing triaged and managed emergency cases.
 */
public class EmergencyIncident extends BaseEntity {
    private Long reportId;
    private double calculatedPriority;
    private Double overridePriority;
    private double effectivePriority;
    private String status;
    private Long triagedByUserId;
    private LocalDateTime triagedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    public EmergencyIncident() {}

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public double getCalculatedPriority() {
        return calculatedPriority;
    }

    public void setCalculatedPriority(double calculatedPriority) {
        this.calculatedPriority = calculatedPriority;
    }

    public Double getOverridePriority() {
        return overridePriority;
    }

    public void setOverridePriority(Double overridePriority) {
        this.overridePriority = overridePriority;
    }

    public double getEffectivePriority() {
        if (overridePriority != null) return overridePriority;
        if (effectivePriority > 0) return effectivePriority;
        return calculatedPriority;
    }

    public void setEffectivePriority(double effectivePriority) {
        this.effectivePriority = effectivePriority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getTriagedByUserId() {
        return triagedByUserId;
    }

    public void setTriagedByUserId(Long triagedByUserId) {
        this.triagedByUserId = triagedByUserId;
    }

    public LocalDateTime getTriagedAt() {
        return triagedAt;
    }

    public void setTriagedAt(LocalDateTime triagedAt) {
        this.triagedAt = triagedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
