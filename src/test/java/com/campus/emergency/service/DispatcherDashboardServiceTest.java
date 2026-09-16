package com.campus.emergency.service;

import com.campus.emergency.dao.*;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.model.*;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.impl.DispatcherDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DispatcherDashboardServiceTest {

    private DispatcherDashboardService dashboardService;
    private IIncidentDao mockIncidentDao;
    private IEmergencyReportDao mockReportDao;
    private IResponderDao mockResponderDao;
    private IResourceDao mockResourceDao;
    private IUserDao mockUserDao;

    private final List<EmergencyIncident> incidentList = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        SessionManager.destroySession();

        // Create Session for Dispatcher
        User dispatcher = new User(2L, "dispatcher1", "dispatcher@campus.local", "hash", "Control Room Dispatcher", "555-0199", 4L, true);
        dispatcher.setRole(new Role(4L, "DISPATCHER", "Dispatcher Role"));
        SessionManager.createSession(dispatcher);

        incidentList.clear();

        EmergencyIncident i1 = new EmergencyIncident();
        i1.setId(101L); i1.setReportId(10L); i1.setCalculatedPriority(90.0); i1.setStatus("OPEN"); i1.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        EmergencyIncident i2 = new EmergencyIncident();
        i2.setId(102L); i2.setReportId(11L); i2.setCalculatedPriority(65.0); i2.setStatus("DISPATCHED"); i2.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        EmergencyIncident i3 = new EmergencyIncident();
        i3.setId(103L); i3.setReportId(12L); i3.setCalculatedPriority(30.0); i3.setStatus("OPEN"); i3.setCreatedAt(LocalDateTime.now());

        incidentList.add(i1);
        incidentList.add(i2);
        incidentList.add(i3);

        mockIncidentDao = new IIncidentDao() {
            @Override public EmergencyIncident create(EmergencyIncident entity) { return entity; }
            @Override public Optional<EmergencyIncident> findById(Long id) { return incidentList.stream().filter(i -> id.equals(i.getId())).findFirst(); }
            @Override public List<EmergencyIncident> findByStatus(String status) { return List.of(); }
            @Override public List<EmergencyIncident> findActiveIncidentsSortedByPriority() { return new ArrayList<>(incidentList); }
            @Override public List<EmergencyIncident> findAll() { return new ArrayList<>(incidentList); }
            @Override public boolean update(EmergencyIncident entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockReportDao = new IEmergencyReportDao() {
            @Override public EmergencyReport create(EmergencyReport entity) { return entity; }
            @Override public Optional<EmergencyReport> findById(Long id) { return Optional.of(new EmergencyReport(5L, "Building Fire", "Fire in lab", "FIRE", 5, "Science Block", 3, null)); }
            @Override public List<EmergencyReport> findByReporterId(Long reporterId) { return List.of(); }
            @Override public int countTotalByReporterId(Long reporterId) { return 0; }
            @Override public int countOpenByReporterId(Long reporterId) { return 0; }
            @Override public int countResolvedByReporterId(Long reporterId) { return 0; }
            @Override public List<EmergencyReport> findAll() { return List.of(); }
            @Override public boolean update(EmergencyReport entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockResponderDao = new IResponderDao() {
            @Override public Responder create(Responder entity) { return entity; }
            @Override public Optional<Responder> findById(Long id) { return Optional.empty(); }
            @Override public List<Responder> findByDutyStatus(String dutyStatus) { return "ON_DUTY".equals(dutyStatus) ? List.of(new Responder()) : List.of(); }
            @Override public List<Responder> findAvailableBySpecialization(String specialization) { return List.of(); }
            @Override public List<Responder> findAll() { return List.of(new Responder()); }
            @Override public boolean update(Responder entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockResourceDao = new IResourceDao() {
            @Override public Resource create(Resource entity) { return entity; }
            @Override public Optional<Resource> findById(Long id) { return Optional.empty(); }
            @Override public List<Resource> findByStatus(String status) { return "AVAILABLE".equals(status) ? List.of(new Resource()) : List.of(); }
            @Override public List<Resource> findByTypeAndStatus(String type, String status) { return List.of(); }
            @Override public List<Resource> findAll() { return List.of(new Resource()); }
            @Override public boolean update(Resource entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockUserDao = new IUserDao() {
            @Override public User create(User entity) { return entity; }
            @Override public Optional<User> findById(Long id) { return Optional.of(dispatcher); }
            @Override public Optional<User> findByUsername(String username) { return Optional.empty(); }
            @Override public Optional<User> findByEmail(String email) { return Optional.empty(); }
            @Override public Optional<User> findByEmailWithRole(String email) { return Optional.of(dispatcher); }
            @Override public List<User> findAll() { return List.of(); }
            @Override public boolean update(User entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        dashboardService = new DispatcherDashboardServiceImpl(mockIncidentDao, mockReportDao, mockResponderDao, mockResourceDao, mockUserDao);
    }

    @Test
    public void test1_DispatcherAuthorization_Allowed() {
        assertDoesNotThrow(() -> dashboardService.validateDispatcherAuthorization(2L));
    }

    @Test
    public void test2_StudentAuthorization_Denied() {
        SessionManager.destroySession();
        User student = new User(10L, "student1", "student@campus.local", "hash", "Student User", "555-0100", 1L, true);
        student.setRole(new Role(1L, "STUDENT", "Student Role"));
        SessionManager.createSession(student);

        assertThrows(AuthenticationException.class, () -> dashboardService.validateDispatcherAuthorization(10L));
    }

    @Test
    public void test3_GetDashboardMetrics() {
        DispatcherDashboardMetrics metrics = dashboardService.getDashboardMetrics(2L);
        assertEquals(3, metrics.activeIncidentCount());
        assertEquals(1, metrics.criticalIncidentCount()); // priority 90.0
        assertEquals(1, metrics.highPriorityIncidentCount()); // priority 65.0
        assertEquals(1, metrics.availableResponderCount());
        assertEquals(1, metrics.availableResourceCount());
    }

    @Test
    public void test4_GetCriticalIncidents() {
        List<EmergencyIncident> criticals = dashboardService.getCriticalIncidents(2L);
        assertEquals(2, criticals.size(), "Should return CRITICAL (90.0) and HIGH (65.0) incidents");
        assertEquals(101L, criticals.get(0).getId(), "Highest priority should be first");
    }

    @Test
    public void test5_SearchAndFilterIncidents() {
        List<EmergencyIncident> filtered = dashboardService.searchAndFilterIncidents(2L, null, null, "CRITICAL", "ALL");
        assertEquals(1, filtered.size());
        assertEquals(101L, filtered.get(0).getId());
    }

    @Test
    public void test6_SortIncidents() {
        List<EmergencyIncident> sortedByTime = dashboardService.sortIncidents(incidentList, "CREATED_TIME");
        assertEquals(103L, sortedByTime.get(0).getId(), "Newest incident should be first when sorted by CREATED_TIME");
    }
}
