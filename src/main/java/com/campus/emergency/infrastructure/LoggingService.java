package com.campus.emergency.infrastructure;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggingService {
    private static final Logger LOGGER = Logger.getLogger(LoggingService.class.getName());

    public static void info(String message) {
        LOGGER.log(Level.INFO, message);
    }

    public static void warn(String message) {
        LOGGER.log(Level.WARNING, message);
    }

    public static void debug(String message) {
        LOGGER.log(Level.FINE, message);
    }

    public static void error(String message) {
        LOGGER.log(Level.SEVERE, message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    public static void logEvent(String category, String event) {
        LOGGER.info(String.format("[%s] %s", category.toUpperCase(), event));
    }
}
