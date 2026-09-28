package com.campus.emergency.model;

import java.time.LocalDateTime;

public class EmergencyIncident {
    private int id;
    private int reportId;
    private String priority; // LOW, MEDIUM, HIGH, CRITICAL
    private String status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    private Integer assignedDispatcherId;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public EmergencyIncident() {}

    public EmergencyIncident(int id, int reportId, String priority, String status, Integer assignedDispatcherId) {
        this.id = id;
        this.reportId = reportId;
        this.priority = priority;
        this.status = status;
        this.assignedDispatcherId = assignedDispatcherId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReportId() { return reportId; }
    public void setReportId(int reportId) { this.reportId = reportId; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getAssignedDispatcherId() { return assignedDispatcherId; }
    public void setAssignedDispatcherId(Integer assignedDispatcherId) { this.assignedDispatcherId = assignedDispatcherId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
