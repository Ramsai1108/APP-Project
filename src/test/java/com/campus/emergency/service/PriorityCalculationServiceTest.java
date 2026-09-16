package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.service.impl.PriorityCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PriorityCalculationServiceTest {

    private IPriorityCalculationService priorityCalculationService;

    @BeforeEach
    public void setUp() {
        priorityCalculationService = new PriorityCalculationServiceImpl();
    }

    @Test
    public void testCalculatePriorityScore_ActiveThreat() {
        EmergencyReport report = new EmergencyReport();
        report.setCategory("ACTIVE_THREAT");
        report.setSeverity(5);
        report.setAffectedPeople(2);

        double score = priorityCalculationService.calculatePriorityScore(report, 8.0);
        assertTrue(score >= 80.0, "Active threat should compute high priority >= 80.0");
    }

    @Test
    public void testCalculatePriorityScore_NullReport() {
        double score = priorityCalculationService.calculatePriorityScore(null, 5.0);
        assertEquals(0.0, score, 0.001);
    }
}
