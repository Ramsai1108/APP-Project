package com.campus.emergency.exception;

/**
 * Resource allocation conflict exception.
 */
public class ResourceUnavailableException extends CampusEmergencyException {

    public ResourceUnavailableException(String message) {
        super(message);
    }
}
