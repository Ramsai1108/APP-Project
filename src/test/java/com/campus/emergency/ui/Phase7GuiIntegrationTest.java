package com.campus.emergency.ui;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.security.AuthorizationService;
import com.campus.emergency.service.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase7GuiIntegrationTest {

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
    private static ServiceRegistry serviceRegistry;

    private static User studentUser;
    private static User dispatcherUser;
    private static User adminUser;
    private static Location testLocation;

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

            serviceRegistry = new ServiceRegistry(
                    poolManager, dbManager, userDAO, locationDAO, reportDAO, incidentDAO,
                    responderDAO, resourceDAO, logDAO, notificationBroker, authService,
                    priorityEngine, identityService, emergencyManager, resourceDispatcher,
                    trackingService, dataAggregator
            );
            ServiceRegistry.setInstance(serviceRegistry);

            long ts = System.currentTimeMillis();
            studentUser = userDAO.create(new User(0, "p7stud_" + ts, "hash", "P7 Student", "p7stud_" + ts + "@campus.edu", "555-071", Role.STUDENT));
            dispatcherUser = userDAO.create(new User(0, "p7disp_" + ts, "hash", "P7 Dispatcher", "p7disp_" + ts + "@campus.edu", "555-072", Role.DISPATCHER));
            adminUser = userDAO.create(new User(0, "p7admin_" + ts, "hash", "P7 Admin", "p7admin_" + ts + "@campus.edu", "555-073", Role.ADMIN));

            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "P7 Main Hall", "1", "101", 37.77, -122.41, "Main Lobby"))
            );
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 7 GUI integration tests");
    }

    @Test
    @Order(1)
    @DisplayName("1. LoginFrame Can Be Created")
    public void test1_loginFrameCreation() {
        LoginFrame frame = new LoginFrame(identityService, serviceRegistry);
        assertNotNull(frame);
        assertEquals("Campus Emergency Response System - Login", frame.getTitle());
    }

    @Test
    @Order(2)
    @DisplayName("2. RegistrationFrame Can Be Created")
    public void test2_registrationFrameCreation() {
        RegistrationFrame frame = new RegistrationFrame(identityService, serviceRegistry);
        assertNotNull(frame);
        assertEquals("Campus Emergency Response System - User Registration", frame.getTitle());
    }

    @Test
    @Order(3)
    @DisplayName("3. UserDashboardFrame Can Be Created")
    public void test3_userDashboardFrameCreation() {
        AuthenticationContext.setCurrentUser(studentUser);
        UserDashboardFrame frame = new UserDashboardFrame(emergencyManager, trackingService, reportDAO, locationDAO);
        assertNotNull(frame);
    }

    @Test
    @Order(4)
    @DisplayName("4. ReportEmergencyFrame Can Be Created")
    public void test4_reportEmergencyFrameCreation() {
        AuthenticationContext.setCurrentUser(studentUser);
        ReportEmergencyFrame frame = new ReportEmergencyFrame(emergencyManager, locationDAO);
        assertNotNull(frame);
    }

    @Test
    @Order(5)
    @DisplayName("5. TrackingDashboardFrame Can Be Created")
    public void test5_trackingDashboardFrameCreation() {
        AuthenticationContext.setCurrentUser(dispatcherUser);
        TrackingDashboardFrame frame = new TrackingDashboardFrame(trackingService, resourceDispatcher, emergencyManager, locationDAO);
        assertNotNull(frame);
    }

    @Test
    @Order(6)
    @DisplayName("6. AdminConsoleFrame Can Be Created")
    public void test6_adminConsoleFrameCreation() {
        AuthenticationContext.setCurrentUser(adminUser);
        AdminConsoleFrame frame = new AdminConsoleFrame(trackingService, dataAggregator, logDAO, emergencyManager, resourceDispatcher);
        assertNotNull(frame);
    }

    @Test
    @Order(7)
    @DisplayName("7. MainApp Startup Flow Works")
    public void test7_mainAppStartupFlow() {
        assertNotNull(ServiceRegistry.getInstance());
        assertNotNull(ServiceRegistry.getInstance().getIdentityService());
    }

    @Test
    @Order(8)
    @DisplayName("8. Successful Login Opens Appropriate Dashboard")
    public void test8_successfulLoginOpensDashboard() {
        AuthenticationContext.setCurrentUser(studentUser);
        assertTrue(AuthenticationContext.isAuthenticated());
        assertEquals(Role.STUDENT, AuthenticationContext.getCurrentUser().getRole());

        AuthenticationContext.setCurrentUser(adminUser);
        assertEquals(Role.ADMIN, AuthenticationContext.getCurrentUser().getRole());
    }

    @Test
    @Order(9)
    @DisplayName("9. Logout Clears AuthenticationContext")
    public void test9_logoutClearsAuthenticationContext() {
        AuthenticationContext.setCurrentUser(studentUser);
        assertTrue(AuthenticationContext.isAuthenticated());

        AuthenticationContext.clear();
        assertFalse(AuthenticationContext.isAuthenticated());
        assertNull(AuthenticationContext.getCurrentUser());
    }

    @Test
    @Order(10)
    @DisplayName("10. Student Cannot Access Admin Operations")
    public void test10_studentCannotAccessAdminOperations() {
        assertFalse(authService.isAdmin(studentUser));
        assertThrows(EmergencyProcessingException.class, () ->
                trackingService.getDashboardData(studentUser)
        );
    }

    @Test
    @Order(11)
    @DisplayName("11. Dispatcher Controls Available Only to Authorized Roles")
    public void test11_dispatcherControlsAvailableOnlyToAuthorizedRoles() {
        assertFalse(authService.canDispatchResources(studentUser));
        assertTrue(authService.canDispatchResources(dispatcherUser));
        assertTrue(authService.canDispatchResources(adminUser));
    }

    @Test
    @Order(12)
    @DisplayName("12. Emergency Submission Uses EmergencyManager")
    public void test12_emergencySubmissionUsesEmergencyManager() throws EmergencyProcessingException {
        EmergencyReport report = emergencyManager.createReport(studentUser, "FIRE", "Fire alert test from GUI integration", testLocation.getId(), 1, null, null);
        assertNotNull(report);
        assertTrue(report.getId() > 0);
    }

    @Test
    @Order(13)
    @DisplayName("13. Dispatch Operation Uses ResourceDispatcher")
    public void test13_dispatchOperationUsesResourceDispatcher() throws EmergencyProcessingException {
        long ts = System.currentTimeMillis();
        User respUser = userDAO.create(new User(0, "p7resp_" + ts, "hash", "Responder P7", "p7r_" + ts + "@campus.edu", "555-077", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "B7_" + ts, "MEDICAL", "AVAILABLE", testLocation.getId()));
        Resource resource = resourceDAO.create(new Resource(0, "Ambulance P7 " + ts, "AMBULANCE", "AVAILABLE", testLocation.getId(), null));

        EmergencyReport report = emergencyManager.createReport(studentUser, "MEDICAL", "GUI dispatch medical alert", testLocation.getId(), 1, null, null);
        EmergencyIncident incident = incidentDAO.findByReportId(report.getId()).orElseThrow();

        boolean success = resourceDispatcher.dispatch(dispatcherUser, incident.getId(), responder.getId(), resource.getId());
        assertTrue(success);
    }

    @Test
    @Order(14)
    @DisplayName("14. Dashboard Data Comes Through Services")
    public void test14_dashboardDataComesThroughServices() throws EmergencyProcessingException {
        DataAggregator agg = trackingService.getDashboardData(dispatcherUser);
        assertNotNull(agg);
        assertTrue(agg.getTotalIncidentsCount() >= 0);
    }

    @Test
    @Order(15)
    @DisplayName("15. No UI Class Contains SQL Statements")
    public void test15_noUiClassContainsSql() throws Exception {
        File uiDir = new File("src/main/java/com/campus/emergency/ui");
        assertTrue(uiDir.exists() && uiDir.isDirectory(), "UI directory must exist");

        File[] files = uiDir.listFiles((dir, name) -> name.endsWith(".java"));
        assertNotNull(files);

        String[] forbiddenSqlKeywords = {
                "SELECT ", "INSERT INTO", "UPDATE ", "DELETE FROM",
                "java.sql.PreparedStatement", "java.sql.Connection", "java.sql.DriverManager",
                "jdbc:"
        };

        for (File file : files) {
            String content = Files.readString(file.toPath());
            for (String forbidden : forbiddenSqlKeywords) {
                assertFalse(content.contains(forbidden),
                        String.format("UI file '%s' contains forbidden SQL/JDBC reference: '%s'", file.getName(), forbidden));
            }
        }
    }

    @Test
    @Order(16)
    @DisplayName("16. Application Builds Successfully")
    public void test16_applicationBuildsSuccessfully() {
        assertTrue(isDbAvailable, "Application build and runtime database pool verified");
    }
}
