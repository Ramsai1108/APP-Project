package com.campus.emergency.util;

/**
 * Common input validation utility methods.
 */
public class ValidationUtil {

    private ValidationUtil() {}

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^[+]?[0-9\\-\\s()]{7,20}$");
    }
}
