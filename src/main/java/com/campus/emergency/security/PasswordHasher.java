package com.campus.emergency.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

public class PasswordHasher {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_LENGTH = 16; // bytes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String hashPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        byte[] salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        String saltHex = HexFormat.of().formatHex(salt);
        String hashHex = HexFormat.of().formatHex(hash);

        return String.format("PBKDF2:%d:%s:%s", ITERATIONS, saltHex, hashHex);
    }

    public static boolean verifyPassword(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // Support PBKDF2 format: PBKDF2:iterations:saltHex:hashHex
        if (storedHash.startsWith("PBKDF2:")) {
            String[] parts = storedHash.split(":");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = HexFormat.of().parseHex(parts[2]);
                byte[] expectedHash = HexFormat.of().parseHex(parts[3]);

                byte[] actualHash = pbkdf2(rawPassword.toCharArray(), salt, iterations, expectedHash.length * 8);
                return MessageDigest.isEqual(expectedHash, actualHash);
            } catch (Exception e) {
                return false;
            }
        }

        // Legacy SHA-256 fallback for 64-char hex string hashes
        if (storedHash.length() == 64) {
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] legacyHashBytes = sha256.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                byte[] expectedLegacyBytes = HexFormat.of().parseHex(storedHash);
                return MessageDigest.isEqual(expectedLegacyBytes, legacyHashBytes);
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password using " + ALGORITHM, e);
        }
    }
}
