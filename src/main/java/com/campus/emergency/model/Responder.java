package com.campus.emergency.model;

/**
 * Responder entity representing personnel dispatched during emergencies.
 */
public class Responder extends BaseEntity {
    private Long userId;
    private String fullName;
    private String specialization;
    private String phone;
    private String dutyStatus;
    private Long assignedLocationId;

    public Responder() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(String dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public Long getAssignedLocationId() {
        return assignedLocationId;
    }

    public void setAssignedLocationId(Long assignedLocationId) {
        this.assignedLocationId = assignedLocationId;
    }
}
