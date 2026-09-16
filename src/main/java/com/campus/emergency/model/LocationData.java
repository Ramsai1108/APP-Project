package com.campus.emergency.model;

import java.io.Serializable;

/**
 * Record DTO capturing location coordinates and status for emergency requests.
 */
public record LocationData(
        Double latitude,
        Double longitude,
        String locationStatus, // AVAILABLE, UNAVAILABLE
        String locationDescription
) implements Serializable {

    public static LocationData unavailable() {
        return new LocationData(null, null, "UNAVAILABLE", "Location Unavailable (Hardware positioning absent)");
    }

    public static LocationData create(Double lat, Double lng, String desc) {
        return new LocationData(lat, lng, "AVAILABLE", desc != null ? desc : "Campus Location");
    }
}
