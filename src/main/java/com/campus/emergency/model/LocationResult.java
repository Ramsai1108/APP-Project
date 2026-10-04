package com.campus.emergency.model;

public class LocationResult {
    private double latitude;
    private double longitude;
    private String displayName;
    private Double accuracy; // In meters, nullable if not available
    private boolean success;
    private String errorMessage;

    public LocationResult() {}

    public LocationResult(double latitude, double longitude, String displayName, Double accuracy, boolean success, String errorMessage) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.displayName = displayName;
        this.accuracy = accuracy;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static LocationResult ok(double latitude, double longitude, String displayName, Double accuracy) {
        return new LocationResult(latitude, longitude, displayName, accuracy, true, null);
    }

    public static LocationResult fail(String errorMessage) {
        return new LocationResult(0.0, 0.0, null, null, false, errorMessage);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("LocationResult{lat=%.6f, lon=%.6f, display='%s', accuracy=%.1fm}",
                    latitude, longitude, displayName, accuracy != null ? accuracy : 0.0);
        } else {
            return "LocationResult{failed: '" + errorMessage + "'}";
        }
    }
}
