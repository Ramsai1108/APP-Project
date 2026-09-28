package com.campus.emergency.service;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthorizationService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase5DispatchTest {

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
    private static EmergencyManager emergencyManager;
    private static ResourceDispatcher resourceDispatcher;

    private static User dispatcherUser;
    private static User adminUser;
    private static User studentUser;
    private static Location testLocation;
    private static EmergencyIncident testIncident;

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

            emergencyManager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine, dbManager, notificationBroker, logDAO);
            resourceDispatcher = new ResourceDispatcher(responderDAO, resourceDAO, incidentDAO, dbManager, notificationBroker, logDAO, authService);

            long ts = System.currentTimeMillis();
            dispatcherUser = userDAO.create(new User(0, "p5disp_" + ts, "hash", "P5 Dispatcher", "p5disp_" + ts + "@campus.edu", "555-051", Role.DISPATCHER));
            adminUser = userDAO.create(new User(0, "p5admin_" + ts, "hash", "P5 Admin", "p5admin_" + ts + "@campus.edu", "555-052", Role.ADMIN));
            studentUser = userDAO.create(new User(0, "p5stud_" + ts, "hash", "P5 Student", "p5stud_" + ts + "@campus.edu", "555-053", Role.STUDENT));

            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "P5 Dispatch Hall", "5", "501", 37.78, -122.42, "Dispatch Desk"))
            );

            try {
                EmergencyReport report = emergencyManager.createReport(studentUser, "FIRE", "Fire alert for Phase 5 dispatch test", testLocation.getId(), 1, null, null);
                testIncident = incidentDAO.findByReportId(report.getId()).orElseThrow();
            } catch (Exception e) {
                fail("Failed to setup test report/incident: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 5 integration tests");
    }

    private String uid() {
        return System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    @Test
    @Order(1)
    @DisplayName("1. Available Responder Can Be Assigned")
    public void test1_availableResponderCanBeAssigned() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r1_" + u, "hash", "Responder 1", "r1_" + u + "@campus.edu", "555-101", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B1_" + u, "FIRE_SAFETY", "AVAILABLE", testLocation.getId()));

        boolean success = resourceDispatcher.assignResponderToIncident(dispatcherUser, testIncident.getId(), responder.getId());
        assertTrue(success, "Assigning an AVAILABLE responder should succeed");

        Optional<Responder> updated = responderDAO.findById(responder.getId());
        assertTrue(updated.isPresent());
        assertEquals("DISPATCHED", updated.get().getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("2. Unavailable Responder Cannot Be Assigned")
    public void test2_unavailableResponderCannotBeAssigned() {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r2_" + u, "hash", "Responder 2", "r2_" + u + "@campus.edu", "555-102", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B2_" + u, "MEDICAL", "OFFLINE", testLocation.getId()));

        assertThrows(EmergencyProcessingException.class, () ->
                resourceDispatcher.assignResponderToIncident(dispatcherUser, testIncident.getId(), responder.getId())
        );
    }

    @Test
    @Order(3)
    @DisplayName("3. Available Resource Can Be Assigned")
    public void test3_availableResourceCanBeAssigned() throws EmergencyProcessingException {
        Resource resource = resourceDAO.create(new Resource(0, "Ambulance Unit " + uid(), "AMBULANCE", "AVAILABLE", testLocation.getId(), null));

        boolean success = resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId());
        assertTrue(success, "Assigning an AVAILABLE resource should succeed");

        Optional<Resource> updated = resourceDAO.findById(resource.getId());
        assertTrue(updated.isPresent());
        assertEquals("IN_USE", updated.get().getStatus());
        assertEquals(testIncident.getId(), updated.get().getAssignedIncidentId());
    }

    @Test
    @Order(4)
    @DisplayName("4. Unavailable Resource Cannot Be Assigned")
    public void test4_unavailableResourceCannotBeAssigned() {
        Resource resource = resourceDAO.create(new Resource(0, "Fire Truck " + uid(), "FIRE_TRUCK", "MAINTENANCE", testLocation.getId(), null));

        assertThrows(EmergencyProcessingException.class, () ->
                resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId())
        );
    }

    @Test
    @Order(5)
    @DisplayName("5. Duplicate Active Assignment Rejected")
    public void test5_duplicateActiveAssignmentRejected() throws EmergencyProcessingException {
        Resource resource = resourceDAO.create(new Resource(0, "Security Patrol " + uid(), "SECURITY_VEHICLE", "AVAILABLE", testLocation.getId(), null));
        resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId());

        assertThrows(EmergencyProcessingException.class, () ->
                resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId())
        );
    }

    @Test
    @Order(6)
    @DisplayName("6. Dispatcher Can Dispatch")
    public void test6_dispatcherCanDispatch() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r6_" + u, "hash", "Responder 6", "r6_" + u + "@campus.edu", "555-106", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B6_" + u, "GENERAL", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "First Aid Kit " + u, "FIRST_AID_KIT", "AVAILABLE", testLocation.getId(), null));

        boolean result = resourceDispatcher.dispatch(dispatcherUser, testIncident.getId(), responder.getId(), resource.getId());
        assertTrue(result, "DISPATCHER role must be allowed to dispatch");
    }

    @Test
    @Order(7)
    @DisplayName("7. Admin Can Dispatch")
    public void test7_adminCanDispatch() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r7_" + u, "hash", "Responder 7", "r7_" + u + "@campus.edu", "555-107", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B7_" + u, "GENERAL", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "AED " + u, "MEDICAL", "AVAILABLE", testLocation.getId(), null));

        boolean result = resourceDispatcher.dispatch(adminUser, testIncident.getId(), responder.getId(), resource.getId());
        assertTrue(result, "ADMIN role must be allowed to dispatch");
    }

    @Test
    @Order(8)
    @DisplayName("8. Student Cannot Dispatch")
    public void test8_studentCannotDispatch() {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r8_" + u, "hash", "Responder 8", "r8_" + u + "@campus.edu", "555-108", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B8_" + u, "GENERAL", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "Radio " + u, "COMMUNICATION", "AVAILABLE", testLocation.getId(), null));

        assertThrows(EmergencyProcessingException.class, () ->
                resourceDispatcher.dispatch(studentUser, testIncident.getId(), responder.getId(), resource.getId())
        );
    }

    @Test
    @Order(9)
    @DisplayName("9. Assignment Updates Availability")
    public void test9_assignmentUpdatesAvailability() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r9_" + u, "hash", "Responder 9", "r9_" + u + "@campus.edu", "555-109", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B9_" + u, "HAZMAT", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "Hazmat Suit " + u, "EQUIPMENT", "AVAILABLE", testLocation.getId(), null));

        assertEquals("AVAILABLE", responderDAO.findById(responder.getId()).get().getStatus());
        assertEquals("AVAILABLE", resourceDAO.findById(resource.getId()).get().getStatus());

        resourceDispatcher.dispatch(dispatcherUser, testIncident.getId(), responder.getId(), resource.getId());

        assertEquals("DISPATCHED", responderDAO.findById(responder.getId()).get().getStatus());
        assertEquals("IN_USE", resourceDAO.findById(resource.getId()).get().getStatus());
    }

    @Test
    @Order(10)
    @DisplayName("10. Release Restores Availability")
    public void test10_releaseRestoresAvailability() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r10_" + u, "hash", "Responder 10", "r10_" + u + "@campus.edu", "555-110", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B10_" + u, "RESCUE", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "Rescue Boat " + u, "BOAT", "AVAILABLE", testLocation.getId(), null));

        resourceDispatcher.dispatch(dispatcherUser, testIncident.getId(), responder.getId(), resource.getId());

        boolean resReleased = resourceDispatcher.releaseResource(dispatcherUser, resource.getId());
        assertTrue(resReleased);
        Resource freedRes = resourceDAO.findById(resource.getId()).get();
        assertEquals("AVAILABLE", freedRes.getStatus());
        assertNull(freedRes.getAssignedIncidentId());

        boolean respReleased = resourceDispatcher.releaseResponder(dispatcherUser, responder.getId());
        assertTrue(respReleased);
        Responder freedResp = responderDAO.findById(responder.getId()).get();
        assertEquals("AVAILABLE", freedResp.getStatus());
    }

    @Test
    @Order(11)
    @DisplayName("11. No-Resource Condition Handled Correctly")
    public void test11_noResourceConditionHandledCorrectly() {
        assertThrows(EmergencyProcessingException.class, () ->
                resourceDispatcher.dispatch(dispatcherUser, testIncident.getId(), null, null)
        );

        Responder autoResp = resourceDispatcher.autoSelectResponder(testIncident);
        assertTrue(autoResp == null || autoResp instanceof Responder);
    }

    @Test
    @Order(12)
    @DisplayName("12. Transaction Rollback Works")
    public void test12_transactionRollbackWorks() {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p5r12_" + u, "hash", "Responder 12", "r12_" + u + "@campus.edu", "555-112", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B12_" + u, "EOD", "AVAILABLE", testLocation.getId()));

        ResourceDAO failingResourceDAO = new JdbcResourceDAO(poolManager) {
            @Override
            public boolean assignToIncident(int resourceId, int incidentId, Connection conn) throws SQLException {
                throw new SQLException("Simulated database failure during resource assignment");
            }
        };

        Resource resource = resourceDAO.create(new Resource(0, "Drone " + u, "AERIAL", "AVAILABLE", testLocation.getId(), null));

        ResourceDispatcher failingDispatcher = new ResourceDispatcher(
                responderDAO, failingResourceDAO, incidentDAO, dbManager, notificationBroker, logDAO, authService
        );

        assertThrows(EmergencyProcessingException.class, () ->
                failingDispatcher.dispatch(dispatcherUser, testIncident.getId(), responder.getId(), resource.getId())
        );

        Responder checkResp = responderDAO.findById(responder.getId()).get();
        assertEquals("AVAILABLE", checkResp.getStatus(), "Responder status update must be rolled back if resource assignment fails");
    }

    @Test
    @Order(13)
    @DisplayName("13. Notification Generated for Dispatch Events")
    public void test13_notificationGenerated() throws EmergencyProcessingException {
        notificationBroker.clearNotifications();

        Resource resource = resourceDAO.create(new Resource(0, "Megaphone " + uid(), "AUDIO", "AVAILABLE", testLocation.getId(), null));
        resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId());

        List<NotificationBroker.NotificationMessage> notifications = notificationBroker.getSentNotifications();
        assertNotNull(notifications);
        assertFalse(notifications.isEmpty());
        boolean found = notifications.stream().anyMatch(n -> n.getEventType() == NotificationBroker.EventType.RESOURCE_ASSIGNED);
        assertTrue(found, "Notification for RESOURCE_ASSIGNED should be sent");
    }

    @Test
    @Order(14)
    @DisplayName("14. Dispatch Event Logged")
    public void test14_dispatchEventLogged() throws EmergencyProcessingException {
        Resource resource = resourceDAO.create(new Resource(0, "Generator " + uid(), "POWER", "AVAILABLE", testLocation.getId(), null));
        resourceDispatcher.assignResourceToIncident(dispatcherUser, resource.getId(), testIncident.getId());

        List<SystemLog> logs = logDAO.findByUserId(dispatcherUser.getId());
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        boolean found = logs.stream().anyMatch(l -> "RESOURCE_DISPATCHED".equals(l.getEventType()));
        assertTrue(found, "SystemLogDAO should record RESOURCE_DISPATCHED event");
    }
}
