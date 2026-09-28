package com.campus.emergency.service;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.security.AuthorizationService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase8EndToEndTest {

    private static ConnectionPoolManager poolManager;
    private static DatabaseManager dbManager;
    private static boolean isDbAvailable = false;

    private static UserDAO userDAO;
    private static LocationDAO locationDAO;
    private static EmergencyReportDAO reportDAO;
    private static EmergencyIncidentDAO incidentDAO;
    private static ResponderDAO responderDAO;
    private static ResourceDAO resourceDAO;
    private static SystemLogDAO logDAO;

    private static NotificationBroker notificationBroker;
    private static AuthorizationService authService;
    private static PriorityEngine priorityEngine;
    private static IdentityService identityService;
    private static EmergencyManager emergencyManager;
    private static ResourceDispatcher resourceDispatcher;
    private static TrackingService trackingService;
    private static DataAggregator dataAggregator;

    private static User registeredStudent;
    private static User registeredDispatcher;
    private static User registeredAdmin;
    private static Location testLocation;
    private static EmergencyReport testReport;
    private static EmergencyIncident testIncident;
    private static Responder testResponder;
    private static Resource testResource;

    private static final long TS = System.currentTimeMillis();

    @BeforeAll
    public static void setUpClass() {
        notificationBroker = new NotificationBroker();
        authService = new AuthorizationService();
        priorityEngine = new PriorityEngine();

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
            responderDAO = new JdbcResponderDAO(poolManager);
            resourceDAO = new JdbcResourceDAO(poolManager);
            logDAO = new JdbcSystemLogDAO(poolManager);

            identityService = new IdentityService(userDAO);
            emergencyManager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine, dbManager, notificationBroker, logDAO);
            resourceDispatcher = new ResourceDispatcher(responderDAO, resourceDAO, incidentDAO, dbManager, notificationBroker, logDAO, authService);
            trackingService = new TrackingService(locationDAO, responderDAO, resourceDAO, incidentDAO, reportDAO, dbManager, notificationBroker, logDAO, authService);
            dataAggregator = new DataAggregator(reportDAO, incidentDAO, responderDAO, resourceDAO);

            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "P8 Main Quad", "1", "101", 37.7749, -122.4194, "North Wing"))
            );

            User respUser = userDAO.create(new User(0, "p8resp_" + TS, "$pbkdf2-sha256$10000$hash", "P8 Responder", "p8resp_" + TS + "@campus.edu", "555-081", Role.STAFF));
            testResponder = responderDAO.create(new Responder(0, respUser.getId(), "BADGE_P8_" + TS, "MEDICAL", "AVAILABLE", testLocation.getId()));
            testResource = resourceDAO.create(new Resource(0, "P8 Ambulance " + TS, "AMBULANCE", "AVAILABLE", testLocation.getId(), null));
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 8 integration tests");
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: User Registration via IdentityService")
    public void test01_UserRegistration() throws AuthenticationException {
        String studentUser = "p8stud_" + TS;
        String studentEmail = "p8stud_" + TS + "@campus.edu";
        registeredStudent = identityService.register(studentUser, "Pass123!", "P8 Student User", studentEmail, "555-082", Role.STUDENT);

        assertNotNull(registeredStudent, "Registered student should not be null");
        assertTrue(registeredStudent.getId() > 0, "Student user ID should be auto-incremented");
        assertEquals(Role.STUDENT, registeredStudent.getRole());

        String dispUser = "p8disp_" + TS;
        String dispEmail = "p8disp_" + TS + "@campus.edu";
        registeredDispatcher = identityService.register(dispUser, "DispPass123!", "P8 Dispatcher User", dispEmail, "555-083", Role.DISPATCHER);
        assertEquals(Role.DISPATCHER, registeredDispatcher.getRole());

        String adminUserStr = "p8admin_" + TS;
        String adminEmail = "p8admin_" + TS + "@campus.edu";
        registeredAdmin = identityService.register(adminUserStr, "AdminPass123!", "P8 Admin User", adminEmail, "555-084", Role.ADMIN);
        assertEquals(Role.ADMIN, registeredAdmin.getRole());
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: User Login & Session Context Setup")
    public void test02_UserLogin() throws AuthenticationException {
        User loggedInUser = identityService.login(registeredStudent.getUsername(), "Pass123!");
        assertNotNull(loggedInUser);
        assertEquals(registeredStudent.getId(), loggedInUser.getId());
        assertEquals(registeredStudent.getId(), AuthenticationContext.getCurrentUser().getId());
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: Role-Based Authorization Audit")
    public void test03_RoleAuthentication() {
        assertFalse(authService.isDispatcher(registeredStudent), "Student should not have DISPATCHER role");
        assertFalse(authService.isAdmin(registeredStudent), "Student should not have ADMIN role");
        assertFalse(authService.canDispatchResources(registeredStudent), "Student cannot dispatch resources");

        assertTrue(authService.isDispatcher(registeredDispatcher), "Dispatcher should have DISPATCHER role");
        assertTrue(authService.canDispatchResources(registeredDispatcher), "Dispatcher can dispatch resources");

        assertTrue(authService.isAdmin(registeredAdmin), "Admin should have ADMIN role");
        assertTrue(authService.canDispatchResources(registeredAdmin), "Admin can dispatch resources");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Emergency Report Submission & Priority Calculation")
    public void test04_EmergencyReportAndPriorityCalculation() throws EmergencyProcessingException {
        testReport = emergencyManager.createReport(
                registeredStudent,
                "MEDICAL",
                "Student collapsed in P8 Main Quad",
                testLocation.getId(),
                3,
                "photo.jpg",
                "video.mp4"
        );

        assertNotNull(testReport, "Report should be created successfully");
        assertTrue(testReport.getId() > 0);
        assertEquals("MEDICAL", testReport.getCategory());

        Optional<EmergencyIncident> incidentOpt = incidentDAO.findByReportId(testReport.getId());
        assertTrue(incidentOpt.isPresent(), "EmergencyIncident should be auto-created for the report");
        testIncident = incidentOpt.get();

        assertNotNull(testIncident.getPriority(), "Priority should be calculated");
        assertTrue("CRITICAL".equalsIgnoreCase(testIncident.getPriority()) || "HIGH".equalsIgnoreCase(testIncident.getPriority()),
                "High severity medical emergency should be assigned CRITICAL or HIGH priority");
        assertEquals("OPEN", testIncident.getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: Dispatcher Notification Delivery")
    public void test05_DispatcherNotification() {
        List<NotificationBroker.NotificationMessage> notifications = notificationBroker.getSentNotifications();
        assertFalse(notifications.isEmpty(), "NotificationBroker should contain sent notifications");

        boolean foundEmergencyEvent = notifications.stream()
                .anyMatch(n -> n.getEventType() == NotificationBroker.EventType.EMERGENCY_REPORTED);
        assertTrue(foundEmergencyEvent, "Notification for EMERGENCY_REPORTED should be published");
    }

    @Test
    @Order(6)
    @DisplayName("Step 6: Responder & Resource Assignment by Dispatcher")
    public void test06_ResponderAndResourceAssignment() throws Exception {
        identityService.login(registeredDispatcher.getUsername(), "DispPass123!");

        boolean respAssigned = resourceDispatcher.assignResponderToIncident(registeredDispatcher, testIncident.getId(), testResponder.getId());
        assertTrue(respAssigned, "Responder assignment should succeed");

        Responder updatedResp = responderDAO.findById(testResponder.getId()).orElseThrow();
        assertEquals("DISPATCHED", updatedResp.getStatus());

        boolean resAssigned = resourceDispatcher.assignResourceToIncident(registeredDispatcher, testResource.getId(), testIncident.getId());
        assertTrue(resAssigned, "Resource assignment should succeed");

        Resource updatedRes = resourceDAO.findById(testResource.getId()).orElseThrow();
        assertEquals("IN_USE", updatedRes.getStatus());
        assertEquals(testIncident.getId(), updatedRes.getAssignedIncidentId().intValue());
    }

    @Test
    @Order(7)
    @DisplayName("Step 7: Incident Status Update to IN_PROGRESS")
    public void test07_IncidentStatusUpdate() throws EmergencyProcessingException {
        boolean updated = emergencyManager.updateIncidentStatus(testIncident.getId(), "IN_PROGRESS");
        assertTrue(updated, "Status update should succeed");

        EmergencyIncident updatedIncident = incidentDAO.findById(testIncident.getId()).orElseThrow();
        assertEquals("IN_PROGRESS", updatedIncident.getStatus());
    }

    @Test
    @Order(8)
    @DisplayName("Step 8: Responder Location Update & Tracking")
    public void test08_ResponderLocationUpdate() throws EmergencyProcessingException {
        boolean locUpdated = trackingService.updateResponderLocation(registeredDispatcher, testResponder.getId(), 37.7755, -122.4201, "Main Quad", "1", "101");
        assertTrue(locUpdated, "Responder location update should succeed");
    }

    @Test
    @Order(9)
    @DisplayName("Step 9: Operational Tracking & Aggregation Verification")
    public void test09_OperationalTracking() throws EmergencyProcessingException {
        List<EmergencyIncident> activeIncidents = trackingService.getActiveIncidents(registeredDispatcher);
        assertNotNull(activeIncidents);
        assertTrue(activeIncidents.stream().anyMatch(i -> i.getId() == testIncident.getId()), "Active incident list should contain test incident");

        int activeCount = dataAggregator.getActiveIncidentsCount();
        assertTrue(activeCount > 0, "Active incidents count should be greater than zero");
    }

    @Test
    @Order(10)
    @DisplayName("Step 10: Incident Resolution & Resource Release")
    public void test10_IncidentResolutionAndResourceRelease() throws EmergencyProcessingException {
        boolean resolved = emergencyManager.updateIncidentStatus(testIncident.getId(), "RESOLVED");
        assertTrue(resolved, "Incident resolution should succeed");

        EmergencyIncident resolvedIncident = incidentDAO.findById(testIncident.getId()).orElseThrow();
        assertEquals("RESOLVED", resolvedIncident.getStatus());

        boolean respReleased = resourceDispatcher.releaseResponder(registeredDispatcher, testResponder.getId());
        assertTrue(respReleased, "Responder release should succeed");
        assertEquals("AVAILABLE", responderDAO.findById(testResponder.getId()).orElseThrow().getStatus());

        boolean resReleased = resourceDispatcher.releaseResource(registeredDispatcher, testResource.getId());
        assertTrue(resReleased, "Resource release should succeed");
        assertEquals("AVAILABLE", resourceDAO.findById(testResource.getId()).orElseThrow().getStatus());
        assertNull(resourceDAO.findById(testResource.getId()).orElseThrow().getAssignedIncidentId());
    }

    @Test
    @Order(11)
    @DisplayName("Step 11: System Log Audit & Historical Data Retrieval")
    public void test11_AuditLoggingAndHistoricalData() throws EmergencyProcessingException {
        List<SystemLog> logs = logDAO.findAll();
        assertNotNull(logs);
        assertFalse(logs.isEmpty(), "System logs should record operations");

        List<EmergencyIncident> historicalIncidents = trackingService.getHistoricalIncidents(registeredDispatcher, null, null, null);
        assertNotNull(historicalIncidents, "Historical incident list should return data");
        assertFalse(historicalIncidents.isEmpty());
    }

    @Test
    @Order(12)
    @DisplayName("Step 12: Identity Logout & Context Clean")
    public void test12_Logout() {
        identityService.logout();
        assertNull(AuthenticationContext.getCurrentUser(), "AuthenticationContext should be null after logout");
    }
}
