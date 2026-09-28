package com.campus.emergency.service;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase4EmergencyManagementTest {

    private static ConnectionPoolManager poolManager;
    private static DatabaseManager dbManager;
    private static boolean isDbAvailable = false;

    private static UserDAO userDAO;
    private static LocationDAO locationDAO;
    private static EmergencyReportDAO reportDAO;
    private static EmergencyIncidentDAO incidentDAO;
    private static SystemLogDAO logDAO;

    private static PriorityEngine priorityEngine;
    private static NotificationBroker notificationBroker;
    private static DataAggregator dataAggregator;
    private static EmergencyManager emergencyManager;

    private static User testUser;
    private static Location testLocation;

    @BeforeAll
    public static void setUpClass() {
        priorityEngine = new PriorityEngine();
        notificationBroker = new NotificationBroker();

        DatabaseConfig config = new DatabaseConfig();
        try {
            poolManager = ConnectionPoolManager.getInstance(config);
            try (Connection conn = poolManager.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    isDbAvailable = true;
                }
            }
        } catch (Exception e) {
            isDbAvailable = false;
        }

        if (isDbAvailable) {
            dbManager = new DatabaseManager(poolManager);
            userDAO = new JdbcUserDAO(poolManager);
            locationDAO = new JdbcLocationDAO(poolManager);
            reportDAO = new JdbcEmergencyReportDAO(poolManager);
            incidentDAO = new JdbcEmergencyIncidentDAO(poolManager);
            logDAO = new JdbcSystemLogDAO(poolManager);

            dataAggregator = new DataAggregator(reportDAO, incidentDAO);
            emergencyManager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine, dbManager, notificationBroker, logDAO);

            // Fetch test user and location from seed data
            testUser = userDAO.findById(1).orElseGet(() ->
                    userDAO.create(new User(0, "p4user_" + System.currentTimeMillis(), "hash", "Phase4 User", "p4@campus.edu", "555-004", Role.STUDENT))
            );
            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "Phase4 Science Hall", "1", "101", 37.77, -122.41, "Lab"))
            );
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 4 integration tests");
    }

    @Test
    @Order(1)
    @DisplayName("1. Valid Emergency Report Creation")
    public void test1_validReportCreation() throws EmergencyProcessingException {
        notificationBroker.clearNotifications();

        EmergencyReport report = emergencyManager.createReport(
                testUser, "FIRE", "Fire alarm triggered in Chemistry lab", testLocation.getId(), 1, null, null
        );

        assertNotNull(report);
        assertTrue(report.getId() > 0);
        assertEquals("FIRE", report.getCategory());
        assertEquals(testUser.getId(), report.getReporterId());
        assertEquals("SUBMITTED", report.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("2. Missing Required Data Is Rejected")
    public void test2_missingRequiredDataRejected() {
        // Missing category
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(testUser, "", "Detailed text description", testLocation.getId(), 1, null, null)
        );

        // Missing description
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(testUser, "FIRE", "   ", testLocation.getId(), 1, null, null)
        );

        // Description too short
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(testUser, "FIRE", "fire", testLocation.getId(), 1, null, null)
        );

        // Invalid location ID
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(testUser, "FIRE", "Detailed description text", -1, 1, null, null)
        );
    }

    @Test
    @Order(3)
    @DisplayName("3. Unauthenticated Reporting Is Rejected")
    public void test3_unauthenticatedReportingRejected() {
        // Null user
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(null, "FIRE", "Detailed description text", testLocation.getId(), 1, null, null)
        );

        // User with invalid ID
        User invalidUser = new User(0, "anon", "hash", "Anon", "anon@campus.edu", "", Role.STUDENT);
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.createReport(invalidUser, "FIRE", "Detailed description text", testLocation.getId(), 1, null, null)
        );
    }

    @Test
    @Order(4)
    @DisplayName("4. Incident Created From Valid Report")
    public void test4_incidentCreatedFromReport() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(
                testUser, "MEDICAL", "Student fainted during lecture", testLocation.getId(), 1, null, null
        );

        Optional<EmergencyIncident> incidentOpt = incidentDAO.findByReportId(report.getId());
        assertTrue(incidentOpt.isPresent());

        EmergencyIncident incident = incidentOpt.get();
        assertEquals(report.getId(), incident.getReportId());
        assertEquals("HIGH", incident.getPriority());
        assertEquals("OPEN", incident.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("5. Priority Calculation Is Deterministic")
    public void test5_priorityCalculationIsDeterministic() {
        String p1 = priorityEngine.calculatePriority("FIRE", "Building on fire", 1);
        String p2 = priorityEngine.calculatePriority("FIRE", "Building on fire", 1);
        assertEquals("CRITICAL", p1);
        assertEquals(p1, p2, "Deterministic inputs must yield identical priority");
    }

    @Test
    @Order(6)
    @DisplayName("6. Severity/Category Rule Priorities")
    public void test6_categorySeverityPriorities() {
        assertEquals("CRITICAL", priorityEngine.calculatePriority("ACTIVE_SHOOTER", "Armed individual", 1));
        assertEquals("HIGH", priorityEngine.calculatePriority("MEDICAL", "Single injury", 1));
        assertEquals("CRITICAL", priorityEngine.calculatePriority("MEDICAL", "Mass casualty incident", 5));
        assertEquals("LOW", priorityEngine.calculatePriority("THEFT", "Bicycle stolen", 1));
        assertEquals("MEDIUM", priorityEngine.calculatePriority("OTHER", "Elevator stuck", 1));
    }

    @Test
    @Order(7)
    @DisplayName("7. Initial Incident Status Is OPEN")
    public void test7_initialIncidentStatus() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(
                testUser, "HAZARD", "Chemical spill in hallway", testLocation.getId(), 1, null, null
        );

        Optional<EmergencyIncident> incidentOpt = incidentDAO.findByReportId(report.getId());
        assertTrue(incidentOpt.isPresent());
        assertEquals("OPEN", incidentOpt.get().getStatus());
    }

    @Test
    @Order(8)
    @DisplayName("8. Valid Status Transition Succeeds")
    public void test8_validStatusTransitionSucceeds() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(
                testUser, "FACILITY", "Broken pipe flooding lobby", testLocation.getId(), 1, null, null
        );

        Optional<EmergencyIncident> incidentOpt = incidentDAO.findByReportId(report.getId());
        assertTrue(incidentOpt.isPresent());
        int incidentId = incidentOpt.get().getId();

        // Valid transitions: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
        assertTrue(emergencyManager.updateIncidentStatus(incidentId, "IN_PROGRESS"));
        assertEquals("IN_PROGRESS", incidentDAO.findById(incidentId).get().getStatus());

        assertTrue(emergencyManager.updateIncidentStatus(incidentId, "RESOLVED"));
        assertEquals("RESOLVED", incidentDAO.findById(incidentId).get().getStatus());

        assertTrue(emergencyManager.updateIncidentStatus(incidentId, "CLOSED"));
        assertEquals("CLOSED", incidentDAO.findById(incidentId).get().getStatus());
    }

    @Test
    @Order(9)
    @DisplayName("9. Invalid Status Transition Is Rejected")
    public void test9_invalidStatusTransitionRejected() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(
                testUser, "NOISE", "Loud party in dormitory", testLocation.getId(), 1, null, null
        );

        Optional<EmergencyIncident> incidentOpt = incidentDAO.findByReportId(report.getId());
        assertTrue(incidentOpt.isPresent());
        int incidentId = incidentOpt.get().getId();

        // Close incident
        emergencyManager.updateIncidentStatus(incidentId, "CLOSED");

        // Attempt invalid transition from terminal state CLOSED -> IN_PROGRESS
        assertThrows(EmergencyProcessingException.class, () ->
                emergencyManager.updateIncidentStatus(incidentId, "IN_PROGRESS")
        );
    }

    @Test
    @Order(10)
    @DisplayName("10. Notification Is Generated for Events")
    public void test10_notificationGeneratedForEvents() throws EmergencyProcessingException {
        notificationBroker.clearNotifications();

        EmergencyReport report = emergencyManager.createReport(
                testUser, "FIRE", "Smoke in elevator shaft", testLocation.getId(), 1, null, null
        );

        List<NotificationBroker.NotificationMessage> notifications = notificationBroker.getSentNotifications();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());

        boolean reportEventFound = notifications.stream()
                .anyMatch(n -> n.getEventType() == NotificationBroker.EventType.EMERGENCY_REPORTED);
        assertTrue(reportEventFound, "Notification for EMERGENCY_REPORTED should be captured");
    }

    @Test
    @Order(11)
    @DisplayName("11. Business Event Is Logged")
    public void test11_businessEventIsLogged() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(
                testUser, "MEDICAL", "Heat exhaustion in courtyard", testLocation.getId(), 1, null, null
        );

        List<SystemLog> logs = logDAO.findByUserId(testUser.getId());
        assertNotNull(logs);
        assertFalse(logs.isEmpty(), "SystemLogDAO should contain log entry for user");

        boolean logFound = logs.stream().anyMatch(l -> "EMERGENCY_REPORT_CREATED".equals(l.getEventType()));
        assertTrue(logFound, "System log should record EMERGENCY_REPORT_CREATED event");
    }

    @Test
    @Order(12)
    @DisplayName("12. DataAggregator Returns Correct Incident Counts")
    public void test12_dataAggregatorReturnsIncidentCounts() {
        int total = dataAggregator.getTotalIncidentsCount();
        int active = dataAggregator.getActiveIncidentsCount();
        int resolved = dataAggregator.getResolvedIncidentsCount();

        assertTrue(total >= 0);
        assertTrue(active >= 0);
        assertTrue(resolved >= 0);
        assertTrue(total >= active);
    }

    @Test
    @Order(13)
    @DisplayName("13. Transaction Rollback Occurs When Incident Creation Fails")
    public void test13_transactionRollbackOnFailure() {
        // Create an EmergencyManager with a failing IncidentDAO mock-like object
        EmergencyIncidentDAO failingIncidentDAO = new JdbcEmergencyIncidentDAO(poolManager) {
            @Override
            public EmergencyIncident create(EmergencyIncident incident) {
                throw new RuntimeException("Simulated Incident DAO Database Exception");
            }

            @Override
            public EmergencyIncident create(EmergencyIncident incident, Connection conn) {
                throw new RuntimeException("Simulated Incident DAO Database Exception");
            }
        };

        EmergencyManager transactionalManager = new EmergencyManager(
                reportDAO, failingIncidentDAO, priorityEngine, dbManager, notificationBroker, logDAO
        );

        int countBefore = reportDAO.findAll().size();

        assertThrows(Exception.class, () ->
                transactionalManager.createReport(testUser, "FIRE", "Test transactional rollback fire", testLocation.getId(), 1, null, null)
        );

        int countAfter = reportDAO.findAll().size();
        assertEquals(countBefore, countAfter, "EmergencyReport should be rolled back if incident creation fails");
    }
}
