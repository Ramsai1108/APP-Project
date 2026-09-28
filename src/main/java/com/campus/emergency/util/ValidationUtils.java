package com.campus.emergency.util;

import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
