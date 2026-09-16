package com.campus.emergency.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DateTime formatting utility methods.
 */
public class DateFormatterUtil {

    private static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateFormatterUtil() {}

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(STANDARD_FORMATTER) : "N/A";
    }
}
