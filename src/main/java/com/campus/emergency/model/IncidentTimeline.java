package com.campus.emergency.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an audit event on the incident timeline.
 */
public class IncidentTimeline implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long incidentId;
    private Long actionByUserId;
    private String actionType; // CREATED, PRIORITIZED, DISPATCHED, RESPONDER_ASSIGNED, RESOURCE_ALLOCATED, STATUS_CHANGED, RESOLVED, CLOSED
    private String description;
    private LocalDateTime createdAt;

    public IncidentTimeline() {
        this.createdAt = LocalDateTime.now();
    }

    public IncidentTimeline(Long incidentId, Long actionByUserId, String actionType, String description) {
        this();
        this.incidentId = incidentId;
        this.actionByUserId = actionByUserId;
        this.actionType = actionType;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public Long getActionByUserId() {
        return actionByUserId;
    }

    public void setActionByUserId(Long actionByUserId) {
        this.actionByUserId = actionByUserId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "IncidentTimeline{" +
                "id=" + id +
                ", incidentId=" + incidentId +
                ", actionByUserId=" + actionByUserId +
                ", actionType='" + actionType + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
