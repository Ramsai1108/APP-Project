package com.campus.emergency.service;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthorizationService;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase8ConcurrencyTest {

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

    private static User disp1;
    private static User disp2;
    private static User studentUser;
    private static Location testLocation;
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

            emergencyManager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine, dbManager, notificationBroker, logDAO);
            resourceDispatcher = new ResourceDispatcher(responderDAO, resourceDAO, incidentDAO, dbManager, notificationBroker, logDAO, authService);
            trackingService = new TrackingService(locationDAO, responderDAO, resourceDAO, incidentDAO, reportDAO, dbManager, notificationBroker, logDAO, authService);
            dataAggregator = new DataAggregator(reportDAO, incidentDAO, responderDAO, resourceDAO);

            disp1 = userDAO.create(new User(0, "c_disp1_" + TS, "$hash", "Conc Dispatcher 1", "c_disp1_" + TS + "@campus.edu", "555-801", Role.DISPATCHER));
            disp2 = userDAO.create(new User(0, "c_disp2_" + TS, "$hash", "Conc Dispatcher 2", "c_disp2_" + TS + "@campus.edu", "555-802", Role.DISPATCHER));
            studentUser = userDAO.create(new User(0, "c_stud_" + TS, "$hash", "Conc Student", "c_stud_" + TS + "@campus.edu", "555-803", Role.STUDENT));

            testLocation = locationDAO.findById(1).orElseGet(() ->
                    locationDAO.create(new Location(0, "Concurrency Hall", "3", "303", 37.77, -122.41, "East Wing"))
            );
        }
    }

    @BeforeEach
    public void checkDb() {
        assumeTrue(isDbAvailable, "Database server is required for Phase 8 concurrency tests");
    }

    @Test
    @Order(1)
    @DisplayName("Concurrency: Simultaneous Responder Assignment Prevention")
    public void test01_ConcurrentResponderAssignment() throws Exception {
        User respUser = userDAO.create(new User(0, "c_resp1_" + TS, "$hash", "Conc Responder 1", "c_resp1_" + TS + "@campus.edu", "555-804", Role.STAFF));
        Responder responder = responderDAO.create(new Responder(0, respUser.getId(), "C_BADGE1_" + TS, "SECURITY", "AVAILABLE", testLocation.getId()));

        EmergencyReport report = emergencyManager.createReport(studentUser, "SECURITY", "Concurrent responder test report", testLocation.getId(), 2, null, null);
        EmergencyIncident incident = incidentDAO.findByReportId(report.getId()).orElseThrow();

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable assignTask1 = () -> {
            try {
                latch.await();
                boolean res = resourceDispatcher.assignResponderToIncident(disp1, incident.getId(), responder.getId());
                if (res) successCount.incrementAndGet(); else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        Runnable assignTask2 = () -> {
            try {
                latch.await();
                boolean res = resourceDispatcher.assignResponderToIncident(disp2, incident.getId(), responder.getId());
                if (res) successCount.incrementAndGet(); else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(assignTask1);
        executor.submit(assignTask2);

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, successCount.get(), "Exactly one dispatcher should successfully assign the responder");
        assertEquals(1, failureCount.get(), "The second dispatcher should fail to assign an already dispatched responder");

        Responder finalResp = responderDAO.findById(responder.getId()).orElseThrow();
        assertEquals("DISPATCHED", finalResp.getStatus());
    }

    @Test
    @Order(2)
    @DisplayName("Concurrency: Simultaneous Resource Assignment Prevention")
    public void test02_ConcurrentResourceAssignment() throws Exception {
        Resource resource = resourceDAO.create(new Resource(0, "Conc Drone " + TS, "DRONE", "AVAILABLE", testLocation.getId(), null));

        EmergencyReport report = emergencyManager.createReport(studentUser, "HAZARD", "Concurrent resource test report", testLocation.getId(), 2, null, null);
        EmergencyIncident incident = incidentDAO.findByReportId(report.getId()).orElseThrow();

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable assignTask1 = () -> {
            try {
                latch.await();
                boolean res = resourceDispatcher.assignResourceToIncident(disp1, resource.getId(), incident.getId());
                if (res) successCount.incrementAndGet(); else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        Runnable assignTask2 = () -> {
            try {
                latch.await();
                boolean res = resourceDispatcher.assignResourceToIncident(disp2, resource.getId(), incident.getId());
                if (res) successCount.incrementAndGet(); else failureCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(assignTask1);
        executor.submit(assignTask2);

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, successCount.get(), "Exactly one dispatcher should successfully assign the resource");
        assertEquals(1, failureCount.get(), "The second dispatcher should fail to assign an already assigned resource");

        Resource finalRes = resourceDAO.findById(resource.getId()).orElseThrow();
        assertEquals("IN_USE", finalRes.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Concurrency: Simultaneous Incident Status Updates")
    public void test03_ConcurrentStatusUpdates() throws Exception {
        EmergencyReport report = emergencyManager.createReport(studentUser, "FIRE", "Concurrent status test report", testLocation.getId(), 3, null, null);
        EmergencyIncident incident = incidentDAO.findByReportId(report.getId()).orElseThrow();

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        String[] statuses = {"IN_PROGRESS", "IN_PROGRESS", "RESOLVED", "CLOSED"};

        for (int i = 0; i < threadCount; i++) {
            final String st = statuses[i];
            executor.submit(() -> {
                try {
                    latch.await();
                    boolean res = emergencyManager.updateIncidentStatus(incident.getId(), st);
                    if (res) successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertTrue(successCount.get() > 0, "At least one status update should complete successfully");
        EmergencyIncident finalIncident = incidentDAO.findById(incident.getId()).orElseThrow();
        assertNotNull(finalIncident.getStatus(), "Incident status should remain valid and non-null");
    }

    @Test
    @Order(4)
    @DisplayName("Concurrency: Simultaneous Dashboard Aggregation Queries")
    public void test04_ConcurrentDashboardRefreshes() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    int total = dataAggregator.getTotalIncidentsCount();
                    int active = dataAggregator.getActiveIncidentsCount();
                    if (total >= 0 && active >= 0) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threadCount, successCount.get(), "All concurrent dashboard aggregation queries should succeed");
    }

    @Test
    @Order(5)
    @DisplayName("Concurrency: Concurrent Notification Processing")
    public void test05_ConcurrentNotificationEvents() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    notificationBroker.notifyEvent(
                            NotificationBroker.EventType.EMERGENCY_REPORTED,
                            "DISPATCH_CENTER",
                            "Concurrent Notification #" + id,
                            NotificationBroker.NotificationType.IN_APP
                    );
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threadCount, successCount.get(), "All concurrent notification events should publish safely");
    }

    @Test
    @Order(6)
    @DisplayName("Concurrency: HikariCP Connection Pool Multi-Threaded Load")
    public void test06_ConcurrentHikariCPAccess() throws Exception {
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    try (Connection conn = poolManager.getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT 1")) {
                        stmt.executeQuery();
                        successCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(threadCount, successCount.get(), "All HikariCP pool queries should acquire and release connections safely");
    }

    @Test
    @Order(7)
    @DisplayName("Transactions: Commit, Rollback and Connection Recovery")
    public void test07_TransactionSuccessAndRollback() {
        String testUserSuccess = "txn_succ_" + TS;
        dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, full_name, email, phone, role_id) VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = 'STUDENT'))")) {
                stmt.setString(1, testUserSuccess);
                stmt.setString(2, "$hash");
                stmt.setString(3, "Txn Success User");
                stmt.setString(4, testUserSuccess + "@campus.edu");
                stmt.setString(5, "555-901");
                stmt.executeUpdate();
            }
            return null;
        });

        assertTrue(userDAO.findByUsername(testUserSuccess).isPresent(), "Transaction commit should persist user");

        String testUserRollback = "txn_roll_" + TS;
        assertThrows(DatabaseException.class, () -> {
            dbManager.executeTransaction(conn -> {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, full_name, email, phone, role_id) VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = 'STUDENT'))")) {
                    stmt.setString(1, testUserRollback);
                    stmt.setString(2, "$hash");
                    stmt.setString(3, "Txn Rollback User");
                    stmt.setString(4, testUserRollback + "@campus.edu");
                    stmt.setString(5, "555-902");
                    stmt.executeUpdate();
                }
                throw new DatabaseException("Simulated transaction failure for rollback verification", new Exception("rollback test"));
            });
        }, "Exception inside transaction callback must trigger rollback and throw DatabaseException");

        assertFalse(userDAO.findByUsername(testUserRollback).isPresent(), "Rolled back transaction must not persist user");

        String testUserRecovery = "txn_rec_" + TS;
        dbManager.executeTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, full_name, email, phone, role_id) VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = 'STUDENT'))")) {
                stmt.setString(1, testUserRecovery);
                stmt.setString(2, "$hash");
                stmt.setString(3, "Txn Recovered User");
                stmt.setString(4, testUserRecovery + "@campus.edu");
                stmt.setString(5, "555-903");
                stmt.executeUpdate();
            }
            return null;
        });

        assertTrue(userDAO.findByUsername(testUserRecovery).isPresent(), "Post-rollback transaction should succeed cleanly");
    }
}
