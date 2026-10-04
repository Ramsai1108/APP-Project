package com.campus.emergency.model;

import java.time.LocalDateTime;

public class EmergencyReport {
    private int id;
    private int reporterId;
    private String category;
    private String description;
    private int locationId;
    private String status; // SUBMITTED, PROCESSED, CANCELLED
    private String photoPath;
    private String videoPath;
    private Double latitude;
    private Double longitude;
    private Double locationAccuracy;
    private LocalDateTime locationTimestamp;
    private String locationSource; // "GPS" or "CAMPUS_MANUAL"
    private Integer campusLocationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyReport() {}

    public EmergencyReport(int id, int reporterId, String category, String description, int locationId, String status) {
        this.id = id;
        this.reporterId = reporterId;
        this.category = category;
        this.description = description;
        this.locationId = locationId;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getReporterId() { return reporterId; }
    public void setReporterId(int reporterId) { this.reporterId = reporterId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getLocationId() { return locationId; }
    public void setLocationId(int locationId) { this.locationId = locationId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLocationAccuracy() { return locationAccuracy; }
    public void setLocationAccuracy(Double locationAccuracy) { this.locationAccuracy = locationAccuracy; }

    public LocalDateTime getLocationTimestamp() { return locationTimestamp; }
    public void setLocationTimestamp(LocalDateTime locationTimestamp) { this.locationTimestamp = locationTimestamp; }

    public String getLocationSource() { return locationSource; }
    public void setLocationSource(String locationSource) { this.locationSource = locationSource; }

    public Integer getCampusLocationId() { return campusLocationId; }
    public void setCampusLocationId(Integer campusLocationId) { this.campusLocationId = campusLocationId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
