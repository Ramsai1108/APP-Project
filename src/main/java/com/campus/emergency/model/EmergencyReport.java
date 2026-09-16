package com.campus.emergency.model;

import java.time.LocalDateTime;

/**
 * EmergencyReport entity representing emergency reports submitted by campus users.
 */
public class EmergencyReport extends BaseEntity {
    private Long reporterId;
    private String title;
    private String description;
    private String category;
    private int severity; // 1 to 5
    private String location;
    private Long locationId;
    private int affectedPeople;
    private String mediaPath;
    private String status = "SUBMITTED";
    private LocalDateTime reportedAt;

    public EmergencyReport() {
        this.reportedAt = LocalDateTime.now();
    }

    public EmergencyReport(Long reporterId, String title, String description, String category,
                           int severity, String location, int affectedPeople, String mediaPath) {
        this();
        this.reporterId = reporterId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.severity = severity;
        this.location = location;
        this.affectedPeople = affectedPeople;
        this.mediaPath = mediaPath;
        this.status = "SUBMITTED";
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Alias for backwards compatibility
    public String getIncidentType() {
        return category;
    }

    public void setIncidentType(String incidentType) {
        this.category = incidentType;
    }

    public int getSeverity() {
        return severity;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    // Alias for backwards compatibility
    public int getHazardLevel() {
        return severity;
    }

    public void setHazardLevel(int hazardLevel) {
        this.severity = hazardLevel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public int getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(int affectedPeople) {
        this.affectedPeople = affectedPeople;
    }

    // Alias for backwards compatibility
    public int getCasualtyCount() {
        return affectedPeople;
    }

    public void setCasualtyCount(int casualtyCount) {
        this.affectedPeople = casualtyCount;
    }

    public String getMediaPath() {
        return mediaPath;
    }

    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
