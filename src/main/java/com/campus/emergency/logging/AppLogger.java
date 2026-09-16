package com.campus.emergency.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System Logger wrapper.
 */
public class AppLogger {

    private AppLogger() {}

    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
