package com.campus.emergency;

import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.security.PasswordHasher;
import com.campus.emergency.service.PriorityEngine;
import com.campus.emergency.util.ValidationUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest {

    @Test
    @DisplayName("Password Hashing Verification")
    public void testPasswordHashing() {
        String rawPassword = "securePassword123";
        String hash1 = PasswordHasher.hashPassword(rawPassword);
        String hash2 = PasswordHasher.hashPassword(rawPassword);

        assertNotNull(hash1);
        assertNotEquals(hash1, hash2, "Unique salts per hash should generate distinct hashes");
        assertTrue(PasswordHasher.verifyPassword(rawPassword, hash1), "Password verification should pass for hash1");
        assertTrue(PasswordHasher.verifyPassword(rawPassword, hash2), "Password verification should pass for hash2");
        assertFalse(PasswordHasher.verifyPassword("wrongPassword", hash1), "Password verification should fail for invalid password");
    }

    @Test
    @DisplayName("Priority Engine Rule Calculation")
    public void testPriorityEngine() {
        PriorityEngine priorityEngine = new PriorityEngine();

        assertEquals("CRITICAL", priorityEngine.calculatePriority("FIRE", "Fire in Science Hall", 1));
        assertEquals("HIGH", priorityEngine.calculatePriority("MEDICAL", "Injury in gym", 1));
        assertEquals("CRITICAL", priorityEngine.calculatePriority("MEDICAL", "Mass casualty", 5));
        assertEquals("LOW", priorityEngine.calculatePriority("THEFT", "Stolen bike", 1));
        assertEquals("MEDIUM", priorityEngine.calculatePriority("OTHER", "Water leak", 1));
    }

    @Test
    @DisplayName("Role Parsing")
    public void testRoleParsing() {
        assertEquals(Role.STUDENT, Role.fromString("student"));
        assertEquals(Role.ADMIN, Role.fromString("ADMIN"));
        assertEquals(Role.DISPATCHER, Role.fromString("dispatcher"));
        assertEquals(Role.STAFF, Role.fromString("STAFF"));
        assertEquals(Role.USER, Role.fromString("UNKNOWN"));
    }

    @Test
    @DisplayName("Validation Utilities")
    public void testValidationUtils() {
        assertTrue(ValidationUtils.isValidEmail("user@campus.edu"));
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertTrue(ValidationUtils.isNotNullOrEmpty("test"));
        assertFalse(ValidationUtils.isNotNullOrEmpty("   "));
        assertFalse(ValidationUtils.isNotNullOrEmpty(null));
    }

    @Test
    @DisplayName("User Model Instantiation")
    public void testUserModel() {
        User user = new User(1, "student1", "hash", "Student One", "student1@campus.edu", "555-1234", Role.STUDENT);
        assertEquals(1, user.getId());
        assertEquals("student1", user.getUsername());
        assertEquals(Role.STUDENT, user.getRole());
    }
}
