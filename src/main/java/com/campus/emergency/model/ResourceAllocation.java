package com.campus.emergency.model;

import java.time.LocalDateTime;

/**
 * ResourceAllocation entity representing resource & responder assignments to incidents.
 */
public class ResourceAllocation extends BaseEntity {
    private Long incidentId;
    private Long resourceId;
    private Long responderId;
    private Long allocatedByUserId;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private String status;

    public ResourceAllocation() {}

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getResponderId() {
        return responderId;
    }

    public void setResponderId(Long responderId) {
        this.responderId = responderId;
    }

    public Long getAllocatedByUserId() {
        return allocatedByUserId;
    }

    public void setAllocatedByUserId(Long allocatedByUserId) {
        this.allocatedByUserId = allocatedByUserId;
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
