package com.campus.emergency.exception;

/**
 * Base Runtime Exception for application domain errors.
 */
public class CampusEmergencyException extends RuntimeException {

    public CampusEmergencyException(String message) {
        super(message);
    }

    public CampusEmergencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
