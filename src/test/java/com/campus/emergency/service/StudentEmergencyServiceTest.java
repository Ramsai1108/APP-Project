package com.campus.emergency.service;

import com.campus.emergency.dao.IEmergencyReportDao;
import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.service.impl.EmergencyManagerImpl;
import com.campus.emergency.service.impl.NotificationServiceImpl;
import com.campus.emergency.service.impl.PriorityCalculationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StudentEmergencyServiceTest {

    private EmergencyManager emergencyManager;
    private MockEmergencyReportDao mockReportDao;
    private MockIncidentDao mockIncidentDao;
    private final Long studentId = 6L;
    private final Long otherStudentId = 99L;

    @BeforeEach
    public void setUp() {
        mockReportDao = new MockEmergencyReportDao();
        mockIncidentDao = new MockIncidentDao();
        emergencyManager = new EmergencyManagerImpl(
                mockReportDao,
                mockIncidentDao,
                new PriorityCalculationServiceImpl(),
                new NotificationServiceImpl()
        );
    }

    @Test
    public void testValidEmergencyReportSubmissionCreatesReportAndIncident() {
        EmergencyReport report = createSampleReport(studentId);

        EmergencyManager.EmergencySubmissionResult result = emergencyManager.submitReport(report);

        assertNotNull(result.reportId(), "Generated report ID should not be null");
        assertNotNull(result.incidentId(), "Generated incident ID should not be null");
        assertTrue(result.calculatedPriority() > 0.0, "Calculated priority should be greater than 0");
        assertEquals("SUBMITTED", result.status());

        assertEquals(1, mockReportDao.getReports().size());
        assertEquals(1, mockIncidentDao.getIncidents().size());
    }

    @Test
    public void testEmptyTitleValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setTitle("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("title"));
    }

    @Test
    public void testEmptyDescriptionValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setDescription(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("description"));
    }

    @Test
    public void testMissingCategoryValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setCategory("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("category"));
    }

    @Test
    public void testInvalidSeverityValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setSeverity(6); // Out of bounds > 5

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("Severity must be between 1 and 5"));
    }

    @Test
    public void testMissingLocationValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setLocation("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("location"));
    }

    @Test
    public void testNegativeAffectedPeopleValidationFailure() {
        EmergencyReport report = createSampleReport(studentId);
        report.setAffectedPeople(-3);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.submitReport(report);
        });
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    public void testDatabaseInsertionFailure() {
        mockReportDao.setSimulateDbError(true);
        EmergencyReport report = createSampleReport(studentId);

        assertThrows(DatabaseAccessException.class, () -> {
            emergencyManager.submitReport(report);
        });
    }

    @Test
    public void testStudentRetrievesOwnReports() {
        emergencyManager.submitReport(createSampleReport(studentId));
        emergencyManager.submitReport(createSampleReport(studentId));

        List<EmergencyReport> studentReports = emergencyManager.getReportsForStudent(studentId);
        assertEquals(2, studentReports.size());
        assertTrue(studentReports.stream().allMatch(r -> r.getReporterId().equals(studentId)));
    }

    @Test
    public void testStudentCannotRetrieveAnotherStudentsReportDetails() {
        EmergencyManager.EmergencySubmissionResult otherResult = emergencyManager.submitReport(createSampleReport(otherStudentId));

        CampusEmergencyException ex = assertThrows(CampusEmergencyException.class, () -> {
            emergencyManager.getReportDetails(otherResult.reportId(), studentId);
        });
        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    public void testValidImageAttachment() {
        assertDoesNotThrow(() -> emergencyManager.validateFileAttachment("C:/uploads/photo.png", false));
        assertDoesNotThrow(() -> emergencyManager.validateFileAttachment("C:/uploads/photo.jpg", false));
    }

    @Test
    public void testInvalidImageAttachment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.validateFileAttachment("C:/uploads/script.exe", false);
        });
        assertTrue(ex.getMessage().contains("Invalid image format"));
    }

    @Test
    public void testValidVideoAttachment() {
        assertDoesNotThrow(() -> emergencyManager.validateFileAttachment("C:/uploads/clip.mp4", true));
    }

    @Test
    public void testInvalidVideoAttachment() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            emergencyManager.validateFileAttachment("C:/uploads/music.mp3", true);
        });
        assertTrue(ex.getMessage().contains("Invalid video format"));
    }

    private EmergencyReport createSampleReport(Long reporterId) {
        return new EmergencyReport(
                reporterId,
                "Chemical Spill in Science Lab",
                "Chemical spill in Science Building Room 305.",
                "FIRE",
                4,
                "Science Building Room 305",
                2,
                null
        );
    }

    // =========================================================================
    // Mock DAOs
    // =========================================================================
    private static class MockEmergencyReportDao implements IEmergencyReportDao {
        private final List<EmergencyReport> reports = new ArrayList<>();
        private long idCounter = 1L;
        private boolean simulateDbError = false;

        public void setSimulateDbError(boolean simulateDbError) {
            this.simulateDbError = simulateDbError;
        }

        public List<EmergencyReport> getReports() {
            return reports;
        }

        @Override
        public EmergencyReport create(EmergencyReport entity) {
            if (simulateDbError) {
                throw new DatabaseAccessException("Simulated database failure", new java.sql.SQLException());
            }
            entity.setId(idCounter++);
            reports.add(entity);
            return entity;
        }

        @Override
        public Optional<EmergencyReport> findById(Long id) {
            return reports.stream().filter(r -> r.getId().equals(id)).findFirst();
        }

        @Override
        public List<EmergencyReport> findByReporterId(Long reporterId) {
            return reports.stream().filter(r -> r.getReporterId().equals(reporterId)).toList();
        }

        @Override
        public List<EmergencyReport> findAll() {
            return new ArrayList<>(reports);
        }

        @Override
        public int countTotalByReporterId(Long reporterId) {
            return (int) reports.stream().filter(r -> r.getReporterId().equals(reporterId)).count();
        }

        @Override
        public int countOpenByReporterId(Long reporterId) {
            return (int) reports.stream().filter(r -> r.getReporterId().equals(reporterId) && "SUBMITTED".equals(r.getStatus())).count();
        }

        @Override
        public int countResolvedByReporterId(Long reporterId) {
            return (int) reports.stream().filter(r -> r.getReporterId().equals(reporterId) && "RESOLVED".equals(r.getStatus())).count();
        }

        @Override
        public boolean update(EmergencyReport entity) {
            return true;
        }

        @Override
        public boolean deleteById(Long id) {
            return true;
        }
    }

    private static class MockIncidentDao implements IIncidentDao {
        private final List<EmergencyIncident> incidents = new ArrayList<>();
        private long idCounter = 100L;

        public List<EmergencyIncident> getIncidents() {
            return incidents;
        }

        @Override
        public EmergencyIncident create(EmergencyIncident entity) {
            entity.setId(idCounter++);
            incidents.add(entity);
            return entity;
        }

        @Override
        public Optional<EmergencyIncident> findById(Long id) {
            return incidents.stream().filter(i -> i.getId().equals(id)).findFirst();
        }

        @Override
        public List<EmergencyIncident> findByStatus(String status) {
            return incidents.stream().filter(i -> i.getStatus().equals(status)).toList();
        }

        @Override
        public List<EmergencyIncident> findActiveIncidentsSortedByPriority() {
            return new ArrayList<>(incidents);
        }

        @Override
        public List<EmergencyIncident> findAll() {
            return new ArrayList<>(incidents);
        }

        @Override
        public boolean update(EmergencyIncident entity) {
            return true;
        }

        @Override
        public boolean deleteById(Long id) {
            return true;
        }
    }
}
