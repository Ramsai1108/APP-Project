package com.campus.emergency.exception;

/**
 * Database operation exception.
 */
public class DatabaseAccessException extends CampusEmergencyException {

    public DatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
