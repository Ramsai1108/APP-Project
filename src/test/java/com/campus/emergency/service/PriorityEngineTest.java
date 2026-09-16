package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.PriorityLevel;
import com.campus.emergency.model.TimeSensitivity;
import com.campus.emergency.service.impl.PriorityEngineImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PriorityEngineTest {

    private PriorityEngine priorityEngine;

    @BeforeEach
    public void setUp() {
        priorityEngine = new PriorityEngineImpl();
    }

    // 1. FIRE + Severity 5 = CRITICAL
    @Test
    public void testFireSeverity5ReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("FIRE", 5, "Science Complex", 2, TimeSensitivity.IMMEDIATE);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 80.0);
        assertTrue(result.categoryRule().contains("FIRE"));
    }

    // 2. MEDICAL + Severity 5 = CRITICAL
    @Test
    public void testMedicalSeverity5ReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("MEDICAL", 5, "Library", 1, TimeSensitivity.IMMEDIATE);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 80.0);
        assertTrue(result.categoryRule().contains("MEDICAL"));
    }

    // 3. SECURITY + Severity 4 = HIGH
    @Test
    public void testSecuritySeverity4ReturnsHigh() {
        PriorityResult result = priorityEngine.calculatePriority("SECURITY", 4, "Engineering A", 0, TimeSensitivity.URGENT);
        assertTrue(result.priorityLevel() == PriorityLevel.HIGH || result.priorityLevel() == PriorityLevel.CRITICAL);
        assertTrue(result.numericScore() >= 70.0);
        assertTrue(result.categoryRule().contains("SECURITY"));
    }

    // 4. SECURITY + Severity 5 = CRITICAL
    @Test
    public void testSecuritySeverity5ReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("SECURITY", 5, "Admin Building", 5, TimeSensitivity.IMMEDIATE);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 90.0);
    }

    // 5. ACCIDENT + Severity 3 = MEDIUM
    @Test
    public void testAccidentSeverity3ReturnsMedium() {
        PriorityResult result = priorityEngine.calculatePriority("ACCIDENT", 3, "Parking Lot B", 1, TimeSensitivity.NORMAL);
        assertEquals(PriorityLevel.MEDIUM, result.priorityLevel());
        assertTrue(result.numericScore() >= 45.0 && result.numericScore() <= 59.0);
    }

    // 6. NATURAL DISASTER + Severity 4 = CRITICAL
    @Test
    public void testNaturalDisasterSeverity4ReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("NATURAL_DISASTER", 4, "Dorm Alpha", 10, TimeSensitivity.URGENT);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 80.0);
    }

    // 7. NATURAL DISASTER + Severity 5 = CRITICAL
    @Test
    public void testNaturalDisasterSeverity5ReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("NATURAL_DISASTER", 5, "Entire Campus", 50, TimeSensitivity.IMMEDIATE);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertEquals(100.0, result.numericScore());
    }

    // 8. FIRE + Severity 1 + Low Sensitivity = LOW
    @Test
    public void testFireSeverity1LowSensitivityReturnsLow() {
        PriorityResult result = priorityEngine.calculatePriority("FIRE", 1, "Outdoor Bench", 0, TimeSensitivity.LOW);
        assertEquals(PriorityLevel.LOW, result.priorityLevel());
        assertTrue(result.numericScore() < 40.0);
    }

    // 9. OTHER + Severity 1 = LOW
    @Test
    public void testOtherSeverity1ReturnsLow() {
        PriorityResult result = priorityEngine.calculatePriority("OTHER", 1, "Cafeteria", 0, TimeSensitivity.NORMAL);
        assertEquals(PriorityLevel.LOW, result.priorityLevel());
        assertTrue(result.numericScore() < 40.0);
    }

    // 10. MEDICAL + Severity 2 + Normal Sensitivity = MEDIUM
    @Test
    public void testMedicalSeverity2ReturnsMedium() {
        PriorityResult result = priorityEngine.calculatePriority("MEDICAL", 2, "Health Center", 1, TimeSensitivity.NORMAL);
        assertEquals(PriorityLevel.MEDIUM, result.priorityLevel());
        assertTrue(result.numericScore() >= 40.0 && result.numericScore() < 60.0);
    }

    // 11. ACCIDENT + Severity 1 = LOW
    @Test
    public void testAccidentSeverity1ReturnsLow() {
        PriorityResult result = priorityEngine.calculatePriority("ACCIDENT", 1, "Walkway", 0, TimeSensitivity.LOW);
        assertEquals(PriorityLevel.LOW, result.priorityLevel());
        assertTrue(result.numericScore() < 40.0);
    }

    // 12. FIRE + Severity 3 + High Affected + Immediate = HIGH / CRITICAL
    @Test
    public void testFireSeverity3HighAffectedReturnsHighOrCritical() {
        PriorityResult result = priorityEngine.calculatePriority("FIRE", 3, "Chemistry Lab", 15, TimeSensitivity.IMMEDIATE);
        assertTrue(result.priorityLevel() == PriorityLevel.HIGH || result.priorityLevel() == PriorityLevel.CRITICAL);
        assertTrue(result.numericScore() >= 75.0);
    }

    // 13. ACCIDENT + Severity 5 + Immediate = CRITICAL
    @Test
    public void testAccidentSeverity5ImmediateReturnsCritical() {
        PriorityResult result = priorityEngine.calculatePriority("ACCIDENT", 5, "Main Highway Gate", 4, TimeSensitivity.IMMEDIATE);
        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 80.0);
    }

    // 14. OTHER + Severity 5 + Urgent
    @Test
    public void testOtherSeverity5UrgentReturnsHigh() {
        PriorityResult result = priorityEngine.calculatePriority("OTHER", 5, "Server Room", 2, TimeSensitivity.URGENT);
        assertTrue(result.priorityLevel() == PriorityLevel.HIGH || result.priorityLevel() == PriorityLevel.MEDIUM);
        assertTrue(result.numericScore() >= 50.0);
    }

    // 15. EmergencyReport Object Integration Test
    @Test
    public void testEmergencyReportObjectCalculation() {
        EmergencyReport report = new EmergencyReport(
                6L, "Active Shooter Report", "Spotted armed suspect", "SECURITY", 5, "Engineering B", 0, null
        );
        PriorityResult result = priorityEngine.calculatePriority(report, 7.5, TimeSensitivity.IMMEDIATE);

        assertEquals(PriorityLevel.CRITICAL, result.priorityLevel());
        assertTrue(result.numericScore() >= 85.0);
        assertNotNull(result.explanation());
    }

    // 16. Null Report Handling Test
    @Test
    public void testNullReportHandling() {
        PriorityResult result = priorityEngine.calculatePriority(null, 5.0, TimeSensitivity.NORMAL);
        assertEquals(PriorityLevel.LOW, result.priorityLevel());
        assertEquals(0.0, result.numericScore());
    }
}
