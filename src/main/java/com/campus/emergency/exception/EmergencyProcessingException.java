package com.campus.emergency.exception;

public class EmergencyProcessingException extends Exception {
    public EmergencyProcessingException(String message) {
        super(message);
    }

    public EmergencyProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
