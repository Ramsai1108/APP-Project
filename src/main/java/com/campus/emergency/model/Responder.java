package com.campus.emergency.model;

public class Responder {
    private int id;
    private int userId;
    private String badgeNumber;
    private String specialization;
    private String status; // AVAILABLE, DISPATCHED, ON_BREAK, OFFLINE
    private Integer currentLocationId;

    public Responder() {}

    public Responder(int id, int userId, String badgeNumber, String specialization, String status, Integer currentLocationId) {
        this.id = id;
        this.userId = userId;
        this.badgeNumber = badgeNumber;
        this.specialization = specialization;
        this.status = status;
        this.currentLocationId = currentLocationId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getBadgeNumber() { return badgeNumber; }
    public void setBadgeNumber(String badgeNumber) { this.badgeNumber = badgeNumber; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getCurrentLocationId() { return currentLocationId; }
    public void setCurrentLocationId(Integer currentLocationId) { this.currentLocationId = currentLocationId; }
}
