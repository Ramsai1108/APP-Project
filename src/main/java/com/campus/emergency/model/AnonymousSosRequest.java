package com.campus.emergency.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Model entity representing an unauthenticated Instant Anonymous Emergency SOS submission.
 */
public class AnonymousSosRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String sosReference; // e.g., SOS-2026-000001
    private Long incidentId;
    private String source; // ANONYMOUS_SOS
    private String emergencyCategory; // EMERGENCY_SOS, MEDICAL, FIRE, SECURITY, ACCIDENT, OTHER
    private Double latitude;
    private Double longitude;
    private String locationStatus; // AVAILABLE, UNAVAILABLE
    private String locationDescription;
    private String status; // SUBMITTED, DISPATCHED, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AnonymousSosRequest() {
        this.source = "ANONYMOUS_SOS";
        this.emergencyCategory = "EMERGENCY_SOS";
        this.locationStatus = "UNAVAILABLE";
        this.status = "SUBMITTED";
        this.createdAt = LocalDateTime.now();
    }

    public AnonymousSosRequest(String sosReference, Long incidentId, String emergencyCategory, LocationData locationData) {
        this();
        this.sosReference = sosReference;
        this.incidentId = incidentId;
        if (emergencyCategory != null && !emergencyCategory.trim().isEmpty()) {
            this.emergencyCategory = emergencyCategory.trim().toUpperCase();
        }
        if (locationData != null) {
            this.latitude = locationData.latitude();
            this.longitude = locationData.longitude();
            this.locationStatus = locationData.locationStatus() != null ? locationData.locationStatus() : "UNAVAILABLE";
            this.locationDescription = locationData.locationDescription();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSosReference() {
        return sosReference;
    }

    public void setSosReference(String sosReference) {
        this.sosReference = sosReference;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(Long incidentId) {
        this.incidentId = incidentId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getEmergencyCategory() {
        return emergencyCategory;
    }

    public void setEmergencyCategory(String emergencyCategory) {
        this.emergencyCategory = emergencyCategory;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "AnonymousSosRequest{" +
                "id=" + id +
                ", sosReference='" + sosReference + '\'' +
                ", incidentId=" + incidentId +
                ", source='" + source + '\'' +
                ", emergencyCategory='" + emergencyCategory + '\'' +
                ", locationStatus='" + locationStatus + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
