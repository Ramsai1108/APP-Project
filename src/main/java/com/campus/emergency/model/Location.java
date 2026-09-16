package com.campus.emergency.model;

/**
 * Location entity representing campus buildings, rooms, and vulnerability scores.
 */
public class Location extends BaseEntity {
    private String buildingName;
    private String buildingCode;
    private int floorLevel;
    private String roomNumber;
    private double vulnerabilityScore;

    public Location() {}

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getBuildingCode() {
        return buildingCode;
    }

    public void setBuildingCode(String buildingCode) {
        this.buildingCode = buildingCode;
    }

    public int getFloorLevel() {
        return floorLevel;
    }

    public void setFloorLevel(int floorLevel) {
        this.floorLevel = floorLevel;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public double getVulnerabilityScore() {
        return vulnerabilityScore;
    }

    public void setVulnerabilityScore(double vulnerabilityScore) {
        this.vulnerabilityScore = vulnerabilityScore;
    }
}
