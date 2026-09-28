package com.campus.emergency.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DEFAULT_FORMATTER);
    }
}
