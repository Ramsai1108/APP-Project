package com.campus.emergency.service;

import com.campus.emergency.dao.IEmergencyReportDao;
import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.dao.IIncidentTimelineDao;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.IncidentTimeline;
import com.campus.emergency.model.Notification;
import com.campus.emergency.service.impl.IncidentStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

public class IncidentStatusServiceTest {

    private IncidentStatusService statusService;
    private IIncidentDao mockIncidentDao;
    private IEmergencyReportDao mockReportDao;
    private IIncidentTimelineDao mockTimelineDao;
    private NotificationBroker mockNotificationBroker;

    private final Map<Long, EmergencyIncident> incidentStore = new HashMap<>();

    @BeforeEach
    public void setUp() {
        incidentStore.clear();

        EmergencyIncident inc1 = new EmergencyIncident();
        inc1.setId(100L);
        inc1.setReportId(10L);
        inc1.setCalculatedPriority(75.0);
        inc1.setStatus("OPEN");
        incidentStore.put(100L, inc1);

        mockIncidentDao = new IIncidentDao() {
            @Override public EmergencyIncident create(EmergencyIncident entity) { return entity; }
            @Override public Optional<EmergencyIncident> findById(Long id) { return Optional.ofNullable(incidentStore.get(id)); }
            @Override public List<EmergencyIncident> findByStatus(String status) { return List.of(); }
            @Override public List<EmergencyIncident> findActiveIncidentsSortedByPriority() { return List.of(); }
            @Override public List<EmergencyIncident> findAll() { return new ArrayList<>(incidentStore.values()); }
            @Override public boolean update(EmergencyIncident entity) { incidentStore.put(entity.getId(), entity); return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockReportDao = new IEmergencyReportDao() {
            @Override public EmergencyReport create(EmergencyReport entity) { return entity; }
            @Override public Optional<EmergencyReport> findById(Long id) { return Optional.of(new EmergencyReport(5L, "Test", "Desc", "FIRE", 4, "Library", 2, null)); }
            @Override public List<EmergencyReport> findByReporterId(Long reporterId) { return List.of(); }
            @Override public int countTotalByReporterId(Long reporterId) { return 0; }
            @Override public int countOpenByReporterId(Long reporterId) { return 0; }
            @Override public int countResolvedByReporterId(Long reporterId) { return 0; }
            @Override public List<EmergencyReport> findAll() { return List.of(); }
            @Override public boolean update(EmergencyReport entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockTimelineDao = new IIncidentTimelineDao() {
            private final List<IncidentTimeline> timelines = new ArrayList<>();
            @Override public IncidentTimeline create(IncidentTimeline entity) { timelines.add(entity); return entity; }
            @Override public Optional<IncidentTimeline> findById(Long id) { return Optional.empty(); }
            @Override public List<IncidentTimeline> findByIncidentId(Long incidentId) { return timelines; }
            @Override public List<IncidentTimeline> findAll() { return timelines; }
            @Override public boolean update(IncidentTimeline entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        mockNotificationBroker = new NotificationBroker() {
            @Override public void dispatchNotification(Notification notification, String priorityLevel) {}
            @Override public void notifyEmergencyCreated(Long incidentId, Long reporterId, String category, String location, double priorityScore) {}
            @Override public void notifyResourceDispatched(Long incidentId, Long reporterId, Long responderUserId, Long dispatcherUserId, String details) {}
            @Override public void notifyIncidentStatusChanged(Long incidentId, Long reporterId, Long responderUserId, String oldStatus, String newStatus) {}
            @Override public Set<com.campus.emergency.model.NotificationChannelType> determineChannelsForPriority(String priorityLevel) { return Set.of(); }
            @Override public List<Notification> getNotificationsForUser(Long userId) { return List.of(); }
            @Override public List<Notification> getUnreadNotificationsForUser(Long userId) { return List.of(); }
            @Override public boolean markAsRead(Long notificationId, Long requestingUserId) { return true; }
            @Override public boolean markAllAsRead(Long userId) { return true; }
            @Override public int countUnreadForUser(Long userId) { return 0; }
        };

        statusService = new IncidentStatusServiceImpl(mockIncidentDao, mockReportDao, mockTimelineDao, mockNotificationBroker);
    }

    @Test
    public void testValidStatusTransitions() {
        assertTrue(statusService.isValidTransition("OPEN", "DISPATCHED"));
        assertTrue(statusService.isValidTransition("DISPATCHED", "IN_PROGRESS"));
        assertTrue(statusService.isValidTransition("IN_PROGRESS", "RESOLVED"));
        assertTrue(statusService.isValidTransition("RESOLVED", "CLOSED"));
        assertTrue(statusService.isValidTransition("OPEN", "CANCELLED"));
    }

    @Test
    public void testInvalidStatusTransitions() {
        assertFalse(statusService.isValidTransition("CLOSED", "OPEN"));
        assertFalse(statusService.isValidTransition("RESOLVED", "DISPATCHED"));
        assertFalse(statusService.isValidTransition("CANCELLED", "IN_PROGRESS"));
    }

    @Test
    public void testUpdateIncidentStatus_Success() {
        EmergencyIncident updated = statusService.updateIncidentStatus(100L, "DISPATCHED", 2L, "Dispatched to responder");
        assertEquals("DISPATCHED", updated.getStatus());
        assertNotNull(updated.getTriagedAt());
        assertEquals(2L, updated.getTriagedByUserId());
    }

    @Test
    public void testUpdateIncidentStatus_InvalidTransitionException() {
        EmergencyIncident inc = incidentStore.get(100L);
        inc.setStatus("CLOSED");

        assertThrows(CampusEmergencyException.class, () -> {
            statusService.updateIncidentStatus(100L, "OPEN", 2L, "Invalid re-open attempt");
        });
    }

    @Test
    public void testUpdateIncidentStatus_NotFoundException() {
        assertThrows(CampusEmergencyException.class, () -> {
            statusService.updateIncidentStatus(9999L, "DISPATCHED", 2L, "Non-existent incident");
        });
    }
}
