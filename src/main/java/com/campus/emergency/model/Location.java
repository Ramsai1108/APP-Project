package com.campus.emergency.model;

import java.time.LocalDateTime;

public class Location {
    private int id;
    private String buildingName;
    private String floor;
    private String roomNumber;
    private double latitude;
    private double longitude;
    private String description;
    private LocalDateTime createdAt;

    public Location() {}

    public Location(int id, String buildingName, String floor, String roomNumber, double latitude, double longitude, String description) {
        this.id = id;
        this.buildingName = buildingName;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder(buildingName);
        if (floor != null && !floor.isEmpty()) sb.append(", Floor ").append(floor);
        if (roomNumber != null && !roomNumber.isEmpty()) sb.append(", Room ").append(roomNumber);
        return sb.toString();
    }
}
