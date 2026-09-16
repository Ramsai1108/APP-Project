package com.campus.emergency.security;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * BCrypt Password Hashing and Verification Utility.
 */
public class PasswordHasher {

    private PasswordHasher() {}

    public static String hashPassword(String plainPassword) {
        return BCrypt.withDefaults().hashToString(10, plainPassword.toCharArray());
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword).verified;
    }
}
