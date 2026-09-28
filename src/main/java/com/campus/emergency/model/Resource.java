package com.campus.emergency.model;

public class Resource {
    private int id;
    private String name;
    private String type; // AMBULANCE, FIRE_EXTINGUISHER, FIRST_AID_KIT, SECURITY_VEHICLE
    private String status; // AVAILABLE, IN_USE, MAINTENANCE
    private Integer locationId;
    private Integer assignedIncidentId;

    public Resource() {}

    public Resource(int id, String name, String type, String status, Integer locationId, Integer assignedIncidentId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.locationId = locationId;
        this.assignedIncidentId = assignedIncidentId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getLocationId() { return locationId; }
    public void setLocationId(Integer locationId) { this.locationId = locationId; }

    public Integer getAssignedIncidentId() { return assignedIncidentId; }
    public void setAssignedIncidentId(Integer assignedIncidentId) { this.assignedIncidentId = assignedIncidentId; }
}
