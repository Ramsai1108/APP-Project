package com.campus.emergency.exception;

/**
 * Authentication failure exception.
 */
public class AuthenticationException extends CampusEmergencyException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
