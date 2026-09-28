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
public class Phase6TrackingTest {

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
    private static TrackingService trackingService;
    private static DataAggregator dataAggregator;

    private static User dispatcherUser;
    private static User adminUser;
    private static User studentUser;
    private static Location testLocation;
    private static EmergencyIncident testIncident;
    private static Responder testResponder;
    private static Resource testResource;

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
            trackingService = new TrackingService(locationDAO, responderDAO, resourceDAO, incidentDAO, reportDAO, dbManager, notificationBroker, logDAO, authService);
            dataAggregator = new DataAggregator(reportDAO, incidentDAO, responderDAO, resourceDAO);

            long ts = System.currentTimeMillis();
            dispatcherUser = userDAO.create(new User(0, "p6disp_" + ts, "hash", "P6 Dispatcher", "p6disp_" + ts + "@campus.edu", "555-061", Role.DISPATCHER));
            adminUser = userDAO.create(new User(0, "p6admin_" + ts, "hash", "P6 Admin", "p6admin_" + ts + "@campus.edu", "555-062", Role.ADMIN));
            studentUser = userDAO.create(new User(0, "p6stud_" + ts, "hash", "P6 Student", "p6stud_" + ts + "@campus.edu", "555-063", Role.STUDENT));

            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "P6 Science Center", "2", "201", 37.77, -122.41, "Lab A"))
            );

            User respUser = userDAO.create(new User(0, "p6resp_" + ts, "hash", "P6 Staff Responder", "p6resp_" + ts + "@campus.edu", "555-064", Role.STAFF));
            testResponder = responderDAO.create(new Responder(0, respUser.getId(), "B6_" + ts, "HAZMAT", "AVAILABLE", testLocation.getId()));

            testResource = resourceDAO.create(new Resource(0, "P6 Command Vehicle " + ts, "VEHICLE", "AVAILABLE", testLocation.getId(), null));

            try {
                EmergencyReport report = emergencyManager.createReport(studentUser, "HAZARD", "Chemical leak in Phase 6 tracking lab", testLocation.getId(), 2, null, null);
                testIncident = incidentDAO.findByReportId(report.getId()).orElseThrow();
            } catch (Exception e) {
                fail("Failed to setup test report/incident: " + e.getMessage());
            }
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 6 integration tests");
    }

    private String uid() {
        return System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }

    @Test
    @Order(1)
    @DisplayName("1. Valid Location Creation")
    public void test1_validLocationCreation() throws EmergencyProcessingException {
        Location loc = trackingService.createLocation(dispatcherUser, "Engineering Center", "4", "405", 37.7749, -122.4194, "Robotics Lab");
        assertNotNull(loc);
        assertTrue(loc.getId() > 0);
        assertEquals("Engineering Center", loc.getBuildingName());
        assertEquals(37.7749, loc.getLatitude(), 0.0001);
        assertEquals(-122.4194, loc.getLongitude(), 0.0001);
    }

    @Test
    @Order(2)
    @DisplayName("2. Invalid Latitude Rejected")
    public void test2_invalidLatitudeRejected() {
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.createLocation(dispatcherUser, "Tower", "1", "101", 95.0, -122.4194, "Invalid Lat")
        );
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.createLocation(dispatcherUser, "Tower", "1", "101", -91.5, -122.4194, "Invalid Lat")
        );
    }

    @Test
    @Order(3)
    @DisplayName("3. Invalid Longitude Rejected")
    public void test3_invalidLongitudeRejected() {
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.createLocation(dispatcherUser, "Tower", "1", "101", 37.7749, 185.0, "Invalid Lon")
        );
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.createLocation(dispatcherUser, "Tower", "1", "101", 37.7749, -190.0, "Invalid Lon")
        );
    }

    @Test
    @Order(4)
    @DisplayName("4. Responder Location Update Succeeds")
    public void test4_responderLocationUpdateSucceeds() throws EmergencyProcessingException {
        boolean updated = trackingService.updateResponderLocation(dispatcherUser, testResponder.getId(), 37.7800, -122.4200, "North Quad", "1", "Field");
        assertTrue(updated);

        Optional<Location> latestLoc = trackingService.getLatestResponderLocation(testResponder.getId());
        assertTrue(latestLoc.isPresent());
        assertEquals(37.7800, latestLoc.get().getLatitude(), 0.0001);
        assertEquals(-122.4200, latestLoc.get().getLongitude(), 0.0001);
    }

    @Test
    @Order(5)
    @DisplayName("5. Unauthorized Location Update Rejected")
    public void test5_unauthorizedLocationUpdateRejected() {
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.updateResponderLocation(studentUser, testResponder.getId(), 37.7800, -122.4200, "Unauthorized", "1", "100")
        );
    }

    @Test
    @Order(6)
    @DisplayName("6. Latest Responder Location Can Be Retrieved")
    public void test6_latestResponderLocationRetrieved() throws EmergencyProcessingException {
        trackingService.updateResponderLocation(dispatcherUser, testResponder.getId(), 37.7850, -122.4250, "South Hall", "2", "210");
        Optional<Location> locOpt = trackingService.getLatestResponderLocation(testResponder.getId());
        assertTrue(locOpt.isPresent());
        assertEquals("South Hall", locOpt.get().getBuildingName());
    }

    @Test
    @Order(7)
    @DisplayName("7. Active Incidents Can Be Retrieved")
    public void test7_activeIncidentsCanBeRetrieved() throws EmergencyProcessingException {
        List<EmergencyIncident> active = trackingService.getActiveIncidents(dispatcherUser);
        assertNotNull(active);
        assertFalse(active.isEmpty());
        assertTrue(active.stream().anyMatch(inc -> inc.getId() == testIncident.getId()));
    }

    @Test
    @Order(8)
    @DisplayName("8. Active Incident Contains Correct Priority/Status")
    public void test8_activeIncidentContainsCorrectPriorityStatus() throws EmergencyProcessingException {
        Optional<EmergencyIncident> details = trackingService.getIncidentDetails(dispatcherUser, testIncident.getId());
        assertTrue(details.isPresent());
        assertEquals("OPEN", details.get().getStatus());
        assertNotNull(details.get().getPriority());
    }

    @Test
    @Order(9)
    @DisplayName("9. Assigned Responder Information Is Available")
    public void test9_assignedResponderInformationAvailable() throws EmergencyProcessingException {
        String u = uid();
        User respUser = userDAO.create(new User(0, "p6r9_" + u, "hash", "Responder 9", "r9_" + u + "@campus.edu", "555-069", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B69_" + u, "FIRE", "AVAILABLE", testLocation.getId()));

        resourceDispatcher.assignResponderToIncident(dispatcherUser, testIncident.getId(), responder.getId());

        Optional<Responder> check = responderDAO.findById(responder.getId());
        assertTrue(check.isPresent());
        assertEquals("DISPATCHED", check.get().getStatus());
    }

    @Test
    @Order(10)
    @DisplayName("10. Assigned Resource Information Is Available")
    public void test10_assignedResourceInformationAvailable() throws EmergencyProcessingException {
        Resource res = resourceDAO.create(new Resource(0, "P6 Engine 10 " + uid(), "FIRE_TRUCK", "AVAILABLE", testLocation.getId(), null));
        resourceDispatcher.assignResourceToIncident(dispatcherUser, res.getId(), testIncident.getId());

        Optional<Resource> check = resourceDAO.findById(res.getId());
        assertTrue(check.isPresent());
        assertEquals("IN_USE", check.get().getStatus());
        assertEquals(testIncident.getId(), check.get().getAssignedIncidentId());
    }

    @Test
    @Order(11)
    @DisplayName("11. Available Responder Count Is Correct")
    public void test11_availableResponderCountCorrect() {
        int availCount = dataAggregator.getAvailableRespondersCount();
        assertTrue(availCount >= 0);

        int actual = (int) responderDAO.findAvailable().size();
        assertEquals(actual, availCount);
    }

    @Test
    @Order(12)
    @DisplayName("12. Available Resource Count Is Correct")
    public void test12_availableResourceCountCorrect() {
        int availCount = dataAggregator.getAvailableResourcesCount();
        assertTrue(availCount >= 0);

        int actual = (int) resourceDAO.findAvailable().size();
        assertEquals(actual, availCount);
    }

    @Test
    @Order(13)
    @DisplayName("13. Notification Event Is Generated")
    public void test13_notificationEventGenerated() throws EmergencyProcessingException {
        notificationBroker.clearNotifications();
        trackingService.updateResponderLocation(dispatcherUser, testResponder.getId(), 37.7900, -122.4300, "West Gate", "1", "Gate");

        List<NotificationBroker.NotificationMessage> list = notificationBroker.getSentNotifications();
        assertFalse(list.isEmpty());
        assertTrue(list.stream().anyMatch(n -> n.getEventType() == NotificationBroker.EventType.LOCATION_UPDATED));
    }

    @Test
    @Order(14)
    @DisplayName("14. Authorized User Can Retrieve Appropriate Notifications")
    public void test14_authorizedUserRetrievesNotifications() throws EmergencyProcessingException {
        notificationBroker.clearNotifications();
        notificationBroker.notifyEvent(NotificationBroker.EventType.EMERGENCY_REPORTED, "DISPATCH_CENTER", "Alert for dispatchers", NotificationBroker.NotificationType.IN_APP);

        List<NotificationBroker.NotificationMessage> dispNotifs = trackingService.getNotifications(dispatcherUser);
        assertFalse(dispNotifs.isEmpty());
        assertTrue(dispNotifs.stream().anyMatch(n -> "Alert for dispatchers".equals(n.getContent())));
    }

    @Test
    @Order(15)
    @DisplayName("15. Unauthorized User Cannot Retrieve Restricted Operational Data")
    public void test15_unauthorizedUserCannotRetrieveOperationalData() {
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.getResponderTrackingData(studentUser)
        );
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.getResourceTrackingData(studentUser)
        );
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.getDashboardData(studentUser)
        );
    }

    @Test
    @Order(16)
    @DisplayName("16. Historical Incidents Can Be Retrieved")
    public void test16_historicalIncidentsCanBeRetrieved() throws EmergencyProcessingException {
        List<EmergencyIncident> historical = trackingService.getHistoricalIncidents(dispatcherUser, null, null, null);
        assertNotNull(historical);
        assertFalse(historical.isEmpty());
    }

    @Test
    @Order(17)
    @DisplayName("17. Status Filtering Works")
    public void test17_statusFilteringWorks() throws EmergencyProcessingException {
        List<EmergencyIncident> openIncidents = trackingService.getHistoricalIncidents(dispatcherUser, "OPEN", null, null);
        assertNotNull(openIncidents);
        assertTrue(openIncidents.stream().allMatch(inc -> "OPEN".equalsIgnoreCase(inc.getStatus())));
    }

    @Test
    @Order(18)
    @DisplayName("18. Priority Filtering Works")
    public void test18_priorityFilteringWorks() throws EmergencyProcessingException {
        String testPriority = testIncident.getPriority();
        List<EmergencyIncident> filtered = trackingService.getHistoricalIncidents(dispatcherUser, null, testPriority, null);
        assertNotNull(filtered);
        assertTrue(filtered.stream().allMatch(inc -> testPriority.equalsIgnoreCase(inc.getPriority())));
    }

    @Test
    @Order(19)
    @DisplayName("19. DataAggregator Returns Correct Operational Statistics")
    public void test19_dataAggregatorReturnsOperationalStatistics() throws EmergencyProcessingException {
        DataAggregator agg = trackingService.getDashboardData(dispatcherUser);
        assertNotNull(agg);

        assertTrue(agg.getTotalIncidentsCount() >= 0);
        assertTrue(agg.getActiveIncidentsCount() >= 0);
        assertTrue(agg.getResolvedIncidentsCount() >= 0);
        assertTrue(agg.getAvailableRespondersCount() >= 0);
        assertTrue(agg.getBusyRespondersCount() >= 0);
        assertTrue(agg.getAvailableResourcesCount() >= 0);
        assertTrue(agg.getDispatchedResourcesCount() >= 0);
        assertNotNull(agg.getRecentIncidents(5));
    }

    @Test
    @Order(20)
    @DisplayName("20. Transaction Rollback Works for Multi-Step Updates")
    public void test20_transactionRollbackWorksForMultiStepUpdates() {
        ResponderDAO failingResponderDAO = new JdbcResponderDAO(poolManager) {
            @Override
            public boolean updateLocation(int id, int locationId, Connection conn) throws SQLException {
                throw new SQLException("Simulated database failure during responder location update");
            }
        };

        TrackingService failingService = new TrackingService(
                locationDAO, failingResponderDAO, resourceDAO, incidentDAO, reportDAO, dbManager, notificationBroker, logDAO, authService
        );

        Optional<Location> beforeLoc = trackingService.getLatestResponderLocation(testResponder.getId());
        Integer beforeLocId = beforeLoc.map(Location::getId).orElse(null);

        assertThrows(EmergencyProcessingException.class, () ->
                failingService.updateResponderLocation(dispatcherUser, testResponder.getId(), 37.9999, -122.9999, "Failed Tower", "99", "999")
        );

        Optional<Location> afterLoc = trackingService.getLatestResponderLocation(testResponder.getId());
        Integer afterLocId = afterLoc.map(Location::getId).orElse(null);

        assertEquals(beforeLocId, afterLocId, "Responder location update must be rolled back if DAO fails");
    }
}
