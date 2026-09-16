package com.campus.emergency.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility for BCrypt password hashing and validation.
 */
public class PasswordUtil {

    private PasswordUtil() {}

    /**
     * Hash a plain-text password using BCrypt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        return BCrypt.withDefaults().hashToString(10, plainPassword.toCharArray());
    }

    /**
     * Verify a plain-text password against a BCrypt hash.
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified;
    }
}
