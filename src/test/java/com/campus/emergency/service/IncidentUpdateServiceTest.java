package com.campus.emergency.service;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentUpdateServiceTest {

    private InMemoryIncidentDAO mockIncidentDAO;
    private IncidentUpdateService updateService;

    @BeforeEach
    public void setUp() {
        mockIncidentDAO = new InMemoryIncidentDAO();
        updateService = new IncidentUpdateService(mockIncidentDAO);
    }

    @Test
    public void testDetectNewlyInsertedIncident() {
        EmergencyIncident inc1 = new EmergencyIncident(1, 101, "HIGH", "OPEN", null);
        mockIncidentDAO.addIncident(inc1);

        // Initial poll to set initial state
        updateService.pollForUpdates();
        assertEquals(1, updateService.getKnownIncidentIds().size());

        AtomicReference<List<EmergencyIncident>> receivedNew = new AtomicReference<>();
        updateService.addListener(new IncidentUpdateService.IncidentUpdateListener() {
            @Override
            public void onNewIncidents(List<EmergencyIncident> newIncidents) {
                receivedNew.set(newIncidents);
            }

            @Override
            public void onIncidentsChanged(List<EmergencyIncident> currentIncidents) {
            }
        });

        // Add second incident
        EmergencyIncident inc2 = new EmergencyIncident(2, 102, "CRITICAL", "OPEN", null);
        mockIncidentDAO.addIncident(inc2);

        updateService.pollForUpdates();

        assertNotNull(receivedNew.get());
        assertEquals(1, receivedNew.get().size());
        assertEquals(2, receivedNew.get().get(0).getId());
        assertTrue(updateService.getKnownIncidentIds().contains(2));
    }

    @Test
    public void testIgnoreAlreadySeenIncidents() {
        EmergencyIncident inc1 = new EmergencyIncident(1, 101, "MEDIUM", "OPEN", null);
        mockIncidentDAO.addIncident(inc1);

        updateService.pollForUpdates();

        AtomicInteger newIncidentCallCount = new AtomicInteger(0);
        updateService.addListener(new IncidentUpdateService.IncidentUpdateListener() {
            @Override
            public void onNewIncidents(List<EmergencyIncident> newIncidents) {
                newIncidentCallCount.incrementAndGet();
            }

            @Override
            public void onIncidentsChanged(List<EmergencyIncident> currentIncidents) {
            }
        });

        // Subsequent poll with no new incidents
        updateService.pollForUpdates();
        updateService.pollForUpdates();

        assertEquals(0, newIncidentCallCount.get(), "No new incident callbacks should fire for already-seen incidents.");
    }

    @Test
    public void testDetectMultipleNewIncidents() {
        EmergencyIncident inc1 = new EmergencyIncident(1, 101, "LOW", "OPEN", null);
        mockIncidentDAO.addIncident(inc1);

        updateService.pollForUpdates();

        AtomicReference<List<EmergencyIncident>> batchReceived = new AtomicReference<>();
        updateService.addListener(new IncidentUpdateService.IncidentUpdateListener() {
            @Override
            public void onNewIncidents(List<EmergencyIncident> newIncidents) {
                batchReceived.set(newIncidents);
            }

            @Override
            public void onIncidentsChanged(List<EmergencyIncident> currentIncidents) {
            }
        });

        EmergencyIncident inc2 = new EmergencyIncident(2, 102, "HIGH", "OPEN", null);
        EmergencyIncident inc3 = new EmergencyIncident(3, 103, "CRITICAL", "OPEN", null);
        mockIncidentDAO.addIncident(inc2);
        mockIncidentDAO.addIncident(inc3);

        updateService.pollForUpdates();

        assertNotNull(batchReceived.get());
        assertEquals(2, batchReceived.get().size());
        assertEquals(2, batchReceived.get().get(0).getId());
        assertEquals(3, batchReceived.get().get(1).getId());
    }

    @Test
    public void testRefreshingKpiValuesOnDataChange() {
        EmergencyIncident inc1 = new EmergencyIncident(1, 101, "CRITICAL", "OPEN", null);
        mockIncidentDAO.addIncident(inc1);

        updateService.pollForUpdates();

        AtomicBoolean changedFired = new AtomicBoolean(false);
        updateService.addListener(new IncidentUpdateService.IncidentUpdateListener() {
            @Override
            public void onNewIncidents(List<EmergencyIncident> newIncidents) {
            }

            @Override
            public void onIncidentsChanged(List<EmergencyIncident> currentIncidents) {
                changedFired.set(true);
            }
        });

        // Update status from OPEN to RESOLVED
        inc1.setStatus("RESOLVED");
        updateService.pollForUpdates();

        assertTrue(changedFired.get(), "onIncidentsChanged should trigger when status changes to allow KPI refresh.");
    }

    @Test
    public void testDatabaseOutageRetryBehavior() {
        mockIncidentDAO.setShouldThrowException(true);

        assertDoesNotThrow(() -> updateService.pollForUpdates(),
                "Database exception should be caught gracefully without crashing polling loop.");

        mockIncidentDAO.setShouldThrowException(false);
        EmergencyIncident inc1 = new EmergencyIncident(1, 101, "HIGH", "OPEN", null);
        mockIncidentDAO.addIncident(inc1);

        assertDoesNotThrow(() -> updateService.pollForUpdates(),
                "Polling should recover and function after transient database failure.");
        assertTrue(updateService.getKnownIncidentIds().contains(1));
    }

    @Test
    public void testCorrectShutdownOfPollingService() {
        updateService.start(1);
        assertTrue(updateService.isRunning());

        updateService.stop();
        assertFalse(updateService.isRunning());
    }

    @Test
    public void testStudentSubmissionPersistence() throws Exception {
        InMemoryReportDAO reportDAO = new InMemoryReportDAO();
        InMemoryIncidentDAO incidentDAO = new InMemoryIncidentDAO();
        PriorityEngine priorityEngine = new PriorityEngine();

        EmergencyManager manager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine);

        User student = new User(10, "student1", "password", "Student One", "student@campus.edu", "555-0100", Role.STUDENT);

        EmergencyReport report = manager.createReport(student, "FIRE", "Smoke in Science Lab room 302", 1, 3, null, null);

        assertNotNull(report);
        assertTrue(report.getId() > 0);
        assertEquals("FIRE", report.getCategory());
        assertEquals("SUBMITTED", report.getStatus());

        List<EmergencyIncident> incidents = incidentDAO.findAll();
        assertFalse(incidents.isEmpty());
        assertEquals(report.getId(), incidents.get(0).getReportId());
    }

    @Test
    public void testExistingAuthenticationAndAuthorization() {
        AuthorizationService authService = new AuthorizationService();

        User user = new User(1, "user1", "pass", "User One", "user@campus.edu", "555-0101", Role.STUDENT);
        User admin = new User(2, "admin1", "pass", "Admin One", "admin@campus.edu", "555-0102", Role.ADMIN);

        assertTrue(authService.canReportEmergency(user));
        assertFalse(authService.canDispatchResources(user));

        assertTrue(authService.canDispatchResources(admin));
        assertTrue(authService.canAccessAdminConsole(admin));
    }

    // Helper Stubs for Testing
    private static class InMemoryIncidentDAO implements EmergencyIncidentDAO {
        private final List<EmergencyIncident> incidents = new ArrayList<>();
        private boolean shouldThrowException = false;
        private int idCounter = 1;

        public void setShouldThrowException(boolean throwEx) {
            this.shouldThrowException = throwEx;
        }

        public synchronized void addIncident(EmergencyIncident inc) {
            if (inc.getId() <= 0) {
                inc.setId(idCounter++);
            }
            incidents.add(inc);
        }

        @Override
        public synchronized EmergencyIncident create(EmergencyIncident incident) {
            addIncident(incident);
            return incident;
        }

        @Override
        public synchronized Optional<EmergencyIncident> findById(int id) {
            return incidents.stream().filter(i -> i.getId() == id).findFirst();
        }

        @Override
        public synchronized Optional<EmergencyIncident> findByReportId(int reportId) {
            return incidents.stream().filter(i -> i.getReportId() == reportId).findFirst();
        }

        @Override
        public synchronized List<EmergencyIncident> findAllActive() {
            if (shouldThrowException) throw new DatabaseException("Simulated DB Connection Failure");
            List<EmergencyIncident> active = new ArrayList<>();
            for (EmergencyIncident i : incidents) {
                if (!"RESOLVED".equalsIgnoreCase(i.getStatus()) && !"CLOSED".equalsIgnoreCase(i.getStatus())) {
                    active.add(i);
                }
            }
            return active;
        }

        @Override
        public synchronized List<EmergencyIncident> findAll() {
            if (shouldThrowException) throw new DatabaseException("Simulated DB Connection Failure");
            return new ArrayList<>(incidents);
        }

        @Override
        public synchronized boolean updatePriority(int id, String priority) {
            Optional<EmergencyIncident> inc = findById(id);
            if (inc.isPresent()) {
                inc.get().setPriority(priority);
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean updateStatus(int id, String status) {
            Optional<EmergencyIncident> inc = findById(id);
            if (inc.isPresent()) {
                inc.get().setStatus(status);
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean assignDispatcher(int incidentId, int dispatcherId) {
            Optional<EmergencyIncident> inc = findById(incidentId);
            if (inc.isPresent()) {
                inc.get().setAssignedDispatcherId(dispatcherId);
                return true;
            }
            return false;
        }
    }

    private static class InMemoryReportDAO implements EmergencyReportDAO {
        private final List<EmergencyReport> reports = new ArrayList<>();
        private int idCounter = 1;

        @Override
        public synchronized EmergencyReport create(EmergencyReport report) {
            if (report.getId() <= 0) {
                report.setId(idCounter++);
            }
            reports.add(report);
            return report;
        }

        @Override
        public synchronized Optional<EmergencyReport> findById(int id) {
            return reports.stream().filter(r -> r.getId() == id).findFirst();
        }

        @Override
        public synchronized List<EmergencyReport> findByReporterId(int reporterId) {
            List<EmergencyReport> list = new ArrayList<>();
            for (EmergencyReport r : reports) {
                if (r.getReporterId() == reporterId) list.add(r);
            }
            return list;
        }

        @Override
        public synchronized List<EmergencyReport> findAll() {
            return new ArrayList<>(reports);
        }

        @Override
        public synchronized boolean updateStatus(int id, String status) {
            Optional<EmergencyReport> rep = findById(id);
            if (rep.isPresent()) {
                rep.get().setStatus(status);
                return true;
            }
            return false;
        }
    }
}
