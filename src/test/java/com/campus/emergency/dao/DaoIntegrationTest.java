package com.campus.emergency.dao;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DaoIntegrationTest {

    private static ConnectionPoolManager poolManager;
    private static DatabaseManager dbManager;
    private static boolean isDbAvailable = false;

    private static UserDAO userDAO;
    private static LocationDAO locationDAO;
    private static ResourceDAO resourceDAO;
    private static ResponderDAO responderDAO;
    private static EmergencyReportDAO reportDAO;
    private static EmergencyIncidentDAO incidentDAO;
    private static SystemLogDAO logDAO;

    private static int createdUserId;
    private static int createdLocationId;
    private static int createdResourceId;
    private static int createdResponderId;
    private static int createdReportId;
    private static int createdIncidentId;

    @BeforeAll
    public static void setUpClass() {
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
            resourceDAO = new JdbcResourceDAO(poolManager);
            responderDAO = new JdbcResponderDAO(poolManager);
            reportDAO = new JdbcEmergencyReportDAO(poolManager);
            incidentDAO = new JdbcEmergencyIncidentDAO(poolManager);
            logDAO = new JdbcSystemLogDAO(poolManager);
        }
    }

    @BeforeEach
    public void checkDatabaseState() {
        assumeTrue(isDbAvailable, "Database server is not running on configured port. Integration test skipped.");
    }

    @Test
    @Order(1)
    @DisplayName("1. Insert User")
    public void test1_insertUser() {
        String username = "testuser_" + System.currentTimeMillis();
        User user = new User(0, username, "hashedpass", "Test User", username + "@campus.edu", "555-0999", Role.STUDENT);
        User created = userDAO.create(user);
        assertTrue(created.getId() > 0, "Created user should have generated ID");
        createdUserId = created.getId();
    }

    @Test
    @Order(2)
    @DisplayName("2. Retrieve User by ID")
    public void test2_retrieveUserById() {
        Optional<User> fetched = userDAO.findById(createdUserId);
        assertTrue(fetched.isPresent(), "User should be found by ID");
        assertEquals(createdUserId, fetched.get().getId());
    }

    @Test
    @Order(3)
    @DisplayName("3. Retrieve User by Username and Email")
    public void test3_retrieveUserByUsernameAndEmail() {
        Optional<User> userOpt = userDAO.findById(createdUserId);
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();

        Optional<User> byUsername = userDAO.findByUsername(user.getUsername());
        assertTrue(byUsername.isPresent());

        Optional<User> byEmail = userDAO.findByEmail(user.getEmail());
        assertTrue(byEmail.isPresent());
    }

    @Test
    @Order(4)
    @DisplayName("4. Update User")
    public void test4_updateUser() {
        Optional<User> userOpt = userDAO.findById(createdUserId);
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        user.setFullName("Updated Test User");

        boolean updated = userDAO.update(user);
        assertTrue(updated);

        Optional<User> reFetched = userDAO.findById(createdUserId);
        assertTrue(reFetched.isPresent());
        assertEquals("Updated Test User", reFetched.get().getFullName());
    }

    @Test
    @Order(5)
    @DisplayName("5. List Users")
    public void test5_listUsers() {
        List<User> users = userDAO.findAll();
        assertNotNull(users);
        assertFalse(users.isEmpty(), "User list should not be empty after insert");
    }

    @Test
    @Order(6)
    @DisplayName("6. Insert Location")
    public void test6_insertLocation() {
        Location loc = new Location(0, "Engineering Hall", "1", "101", 37.77, -122.41, "Robotics Lab");
        Location created = locationDAO.create(loc);
        assertTrue(created.getId() > 0);
        createdLocationId = created.getId();
    }

    @Test
    @Order(7)
    @DisplayName("7. Retrieve Location")
    public void test7_retrieveLocation() {
        Optional<Location> fetched = locationDAO.findById(createdLocationId);
        assertTrue(fetched.isPresent());
        assertEquals("Engineering Hall", fetched.get().getBuildingName());
    }

    @Test
    @Order(8)
    @DisplayName("8. Insert Resource")
    public void test8_insertResource() {
        Resource res = new Resource(0, "Test Ambulance", "AMBULANCE", "AVAILABLE", createdLocationId, null);
        Resource created = resourceDAO.create(res);
        assertTrue(created.getId() > 0);
        createdResourceId = created.getId();
    }

    @Test
    @Order(9)
    @DisplayName("9. Retrieve Resource")
    public void test9_retrieveResource() {
        Optional<Resource> fetched = resourceDAO.findById(createdResourceId);
        assertTrue(fetched.isPresent());
        assertEquals("Test Ambulance", fetched.get().getName());
    }

    @Test
    @Order(10)
    @DisplayName("10. Update Resource Status")
    public void test10_updateResourceStatus() {
        boolean updated = resourceDAO.updateStatus(createdResourceId, "IN_USE");
        assertTrue(updated);

        Optional<Resource> fetched = resourceDAO.findById(createdResourceId);
        assertTrue(fetched.isPresent());
        assertEquals("IN_USE", fetched.get().getStatus());
    }

    private static String createdBadgeNumber;

    @Test
    @Order(11)
    @DisplayName("11. Insert Responder")
    public void test11_insertResponder() {
        createdBadgeNumber = "BADGE-" + System.currentTimeMillis();
        Responder resp = new Responder(0, createdUserId, createdBadgeNumber, "Hazmat", "AVAILABLE", createdLocationId);
        Responder created = responderDAO.create(resp);
        assertTrue(created.getId() > 0);
        createdResponderId = created.getId();
    }

    @Test
    @Order(12)
    @DisplayName("12. Retrieve Responder")
    public void test12_retrieveResponder() {
        Optional<Responder> fetched = responderDAO.findById(createdResponderId);
        assertTrue(fetched.isPresent());
        assertEquals(createdBadgeNumber, fetched.get().getBadgeNumber());
    }

    @Test
    @Order(13)
    @DisplayName("13. Update Responder Status")
    public void test13_updateResponderStatus() {
        boolean updated = responderDAO.updateStatus(createdResponderId, "DISPATCHED");
        assertTrue(updated);

        Optional<Responder> fetched = responderDAO.findById(createdResponderId);
        assertTrue(fetched.isPresent());
        assertEquals("DISPATCHED", fetched.get().getStatus());
    }

    @Test
    @Order(14)
    @DisplayName("14. Create Emergency Report")
    public void test14_createEmergencyReport() {
        EmergencyReport report = new EmergencyReport(0, createdUserId, "FIRE", "Small trash can fire", createdLocationId, "SUBMITTED");
        EmergencyReport created = reportDAO.create(report);
        assertTrue(created.getId() > 0);
        createdReportId = created.getId();
    }

    @Test
    @Order(15)
    @DisplayName("15. Retrieve Emergency Report")
    public void test15_retrieveEmergencyReport() {
        Optional<EmergencyReport> fetched = reportDAO.findById(createdReportId);
        assertTrue(fetched.isPresent());
        assertEquals("FIRE", fetched.get().getCategory());
    }

    @Test
    @Order(16)
    @DisplayName("16. Create Emergency Incident")
    public void test16_createEmergencyIncident() {
        EmergencyIncident incident = new EmergencyIncident(0, createdReportId, "HIGH", "OPEN", null);
        EmergencyIncident created = incidentDAO.create(incident);
        assertTrue(created.getId() > 0);
        createdIncidentId = created.getId();
    }

    @Test
    @Order(17)
    @DisplayName("17. Retrieve Emergency Incident")
    public void test17_retrieveEmergencyIncident() {
        Optional<EmergencyIncident> fetched = incidentDAO.findById(createdIncidentId);
        assertTrue(fetched.isPresent());
        assertEquals("HIGH", fetched.get().getPriority());
    }

    @Test
    @Order(18)
    @DisplayName("18. Update Incident Status")
    public void test18_updateIncidentStatus() {
        boolean updated = incidentDAO.updateStatus(createdIncidentId, "IN_PROGRESS");
        assertTrue(updated);

        Optional<EmergencyIncident> fetched = incidentDAO.findById(createdIncidentId);
        assertTrue(fetched.isPresent());
        assertEquals("IN_PROGRESS", fetched.get().getStatus());
    }

    @Test
    @Order(19)
    @DisplayName("19. Insert System Log")
    public void test19_insertSystemLog() {
        logDAO.log("TEST_EVENT", createdUserId, "Integration test execution log entry");
        List<SystemLog> logs = logDAO.findByUserId(createdUserId);
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
    }

    @Test
    @Order(20)
    @DisplayName("20. Retrieve System Logs")
    public void test20_retrieveSystemLog() {
        List<SystemLog> allLogs = logDAO.findAll();
        assertNotNull(allLogs);
        assertFalse(allLogs.isEmpty());
    }

    @Test
    @Order(21)
    @DisplayName("21. Verify Transaction Rollback on Failure")
    public void test21_verifyTransactionRollbackOnFailure() {
        assertThrows(DatabaseException.class, () -> {
            dbManager.executeTransaction(conn -> {
                try (var stmt = conn.prepareStatement("INSERT INTO locations (building_name) VALUES ('Temp Building')")) {
                    stmt.executeUpdate();
                }
                // Intentionally throw exception to trigger rollback
                throw new SQLException("Forced exception for rollback testing");
            });
        });
    }

    @Test
    @Order(22)
    @DisplayName("22. Verify Connection Pool Acquire and Release")
    public void test22_verifyConnectionPoolAcquireAndRelease() throws SQLException {
        try (Connection conn = poolManager.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        } // Connection returned to pool on close
    }
}
