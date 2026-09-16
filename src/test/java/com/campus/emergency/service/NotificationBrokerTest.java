package com.campus.emergency.service;

import com.campus.emergency.dao.INotificationDao;
import com.campus.emergency.model.*;
import com.campus.emergency.service.channel.*;
import com.campus.emergency.service.impl.NotificationBrokerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationBrokerTest {

    private NotificationBroker notificationBroker;
    private INotificationDao mockNotificationDao;

    @BeforeEach
    public void setUp() {
        mockNotificationDao = new INotificationDao() {
            private final List<Notification> memoryStorage = new ArrayList<>();
            private final AtomicLong idGen = new AtomicLong(1);

            @Override
            public synchronized Notification create(Notification n) {
                if (n.getId() == null) n.setId(idGen.getAndIncrement());
                memoryStorage.add(n);
                return n;
            }

            @Override
            public synchronized Optional<Notification> findById(Long id) {
                return memoryStorage.stream().filter(n -> id.equals(n.getId())).findFirst();
            }

            @Override
            public synchronized List<Notification> findByRecipientUserId(Long recipientUserId) {
                return memoryStorage.stream().filter(n -> recipientUserId.equals(n.getRecipientUserId())).collect(Collectors.toList());
            }

            @Override
            public synchronized List<Notification> findUnreadByRecipientUserId(Long recipientUserId) {
                return memoryStorage.stream()
                        .filter(n -> recipientUserId.equals(n.getRecipientUserId()) && n.getStatus() != NotificationStatus.READ)
                        .collect(Collectors.toList());
            }

            @Override
            public synchronized boolean markAsRead(Long notificationId) {
                Optional<Notification> opt = findById(notificationId);
                if (opt.isPresent()) {
                    opt.get().setStatus(NotificationStatus.READ);
                    return true;
                }
                return false;
            }

            @Override
            public synchronized boolean markAllAsRead(Long recipientUserId) {
                List<Notification> list = findUnreadByRecipientUserId(recipientUserId);
                list.forEach(n -> n.setStatus(NotificationStatus.READ));
                return true;
            }

            @Override
            public synchronized boolean updateStatus(Long notificationId, NotificationStatus status) {
                Optional<Notification> opt = findById(notificationId);
                if (opt.isPresent()) {
                    opt.get().setStatus(status);
                    return true;
                }
                return false;
            }

            @Override
            public synchronized List<Notification> findByIncidentId(Long incidentId) {
                return memoryStorage.stream().filter(n -> incidentId.equals(n.getIncidentId())).collect(Collectors.toList());
            }

            @Override
            public synchronized int countUnreadByRecipientUserId(Long recipientUserId) {
                return findUnreadByRecipientUserId(recipientUserId).size();
            }

            @Override
            public synchronized List<Notification> findAll() {
                return new ArrayList<>(memoryStorage);
            }

            @Override public boolean update(Notification entity) { return true; }
            @Override public boolean deleteById(Long id) { return true; }
        };

        notificationBroker = new NotificationBrokerImpl(mockNotificationDao);
    }

    @Test
    public void test1_LowPriorityChannelSelection() {
        Set<NotificationChannelType> channels = notificationBroker.determineChannelsForPriority("LOW");
        assertEquals(1, channels.size());
        assertTrue(channels.contains(NotificationChannelType.IN_APP));
    }

    @Test
    public void test2_MediumPriorityChannelSelection() {
        Set<NotificationChannelType> channels = notificationBroker.determineChannelsForPriority("MEDIUM");
        assertEquals(1, channels.size());
        assertTrue(channels.contains(NotificationChannelType.IN_APP));
    }

    @Test
    public void test3_HighPriorityChannelSelection() {
        Set<NotificationChannelType> channels = notificationBroker.determineChannelsForPriority("HIGH");
        assertEquals(3, channels.size());
        assertTrue(channels.contains(NotificationChannelType.IN_APP));
        assertTrue(channels.contains(NotificationChannelType.EMAIL));
        assertTrue(channels.contains(NotificationChannelType.SMS));
    }

    @Test
    public void test4_CriticalPriorityChannelSelection() {
        Set<NotificationChannelType> channels = notificationBroker.determineChannelsForPriority("CRITICAL");
        assertEquals(4, channels.size());
        assertTrue(channels.contains(NotificationChannelType.IN_APP));
        assertTrue(channels.contains(NotificationChannelType.EMAIL));
        assertTrue(channels.contains(NotificationChannelType.SMS));
        assertTrue(channels.contains(NotificationChannelType.PUSH));
    }

    @Test
    public void test5_InAppNotificationChannelSend() {
        InAppNotificationChannel inApp = new InAppNotificationChannel(mockNotificationDao);
        Notification n = new Notification(5L, 200L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "InApp Test", "Msg", "LOW");
        inApp.send(n);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
    }

    @Test
    public void test6_EmailNotificationChannelSend() {
        EmailNotificationChannel email = new EmailNotificationChannel();
        Notification n = new Notification(5L, 200L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.EMAIL, "Email Test", "Msg", "HIGH");
        email.send(n);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
    }

    @Test
    public void test7_SmsNotificationChannelSend() {
        SmsNotificationChannel sms = new SmsNotificationChannel();
        Notification n = new Notification(5L, 200L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.SMS, "SMS Test", "Msg", "HIGH");
        sms.send(n);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
    }

    @Test
    public void test8_PushNotificationChannelSend() {
        PushNotificationChannel push = new PushNotificationChannel();
        Notification n = new Notification(5L, 200L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.PUSH, "Push Test", "Msg", "CRITICAL");
        push.send(n);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertNotNull(n.getSentAt());
    }

    @Test
    public void test9_NotifyEmergencyCreated_Recipients() throws Exception {
        notificationBroker.notifyEmergencyCreated(501L, 10L, "FIRE", "Library 2nd Floor", 85.0);
        Thread.sleep(300); // Allow async task completion

        List<Notification> reporterNotifs = notificationBroker.getNotificationsForUser(10L);
        assertFalse(reporterNotifs.isEmpty(), "Reporter should receive confirmation notification");

        List<Notification> dispatcherNotifs = notificationBroker.getNotificationsForUser(2L);
        assertFalse(dispatcherNotifs.isEmpty(), "Control Room Dispatcher should receive emergency alert");
    }

    @Test
    public void test10_NotifyResourceDispatched_Recipients() throws Exception {
        notificationBroker.notifyResourceDispatched(502L, 10L, 12L, 2L, "Ambulance #1 Dispatched");
        Thread.sleep(300);

        List<Notification> responderNotifs = notificationBroker.getNotificationsForUser(12L);
        assertFalse(responderNotifs.isEmpty(), "Assigned responder should receive dispatch notification");

        List<Notification> reporterNotifs = notificationBroker.getNotificationsForUser(10L);
        assertFalse(reporterNotifs.isEmpty(), "Reporter should receive dispatch update notification");
    }

    @Test
    public void test11_NotifyIncidentStatusChanged_Resolution() throws Exception {
        notificationBroker.notifyIncidentStatusChanged(503L, 10L, 12L, "IN_PROGRESS", "RESOLVED");
        Thread.sleep(300);

        List<Notification> reporterNotifs = notificationBroker.getNotificationsForUser(10L);
        assertFalse(reporterNotifs.isEmpty(), "Reporter should receive status resolution notification");
    }

    @Test
    public void test12_UnauthorizedNotificationAccessPrevention() {
        Notification n = mockNotificationDao.create(new Notification(10L, 504L, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, "Alert", "Msg", "LOW"));
        
        // User 99 attempting to mark User 10's notification as read
        boolean result = notificationBroker.markAsRead(n.getId(), 99L);
        assertFalse(result, "Unauthorized user should not be allowed to mark another user's notification as read");
    }

    @Test
    public void test13_FailedNotificationChannelResilience() {
        NotificationBroker brokerWithFailingDao = new NotificationBrokerImpl(new INotificationDao() {
            @Override public Notification create(Notification n) { throw new RuntimeException("Simulated DB connection failure"); }
            @Override public Optional<Notification> findById(Long id) { return Optional.empty(); }
            @Override public List<Notification> findByRecipientUserId(Long recipientUserId) { return List.of(); }
            @Override public List<Notification> findUnreadByRecipientUserId(Long recipientUserId) { return List.of(); }
            @Override public boolean markAsRead(Long notificationId) { return false; }
            @Override public boolean markAllAsRead(Long recipientUserId) { return false; }
            @Override public boolean updateStatus(Long notificationId, NotificationStatus status) { return false; }
            @Override public List<Notification> findByIncidentId(Long incidentId) { return List.of(); }
            @Override public int countUnreadByRecipientUserId(Long recipientUserId) { return 0; }
            @Override public List<Notification> findAll() { return List.of(); }
            @Override public boolean update(Notification entity) { return false; }
            @Override public boolean deleteById(Long id) { return false; }
        });

        // Dispatching notification should not throw an unhandled exception despite channel DB failure
        assertDoesNotThrow(() -> {
            Notification n = new Notification(5L, 600L, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, "Resilience Test", "Msg", "CRITICAL");
            brokerWithFailingDao.dispatchNotification(n, "CRITICAL");
        });
    }

    @Test
    public void test14_MultipleChannelsDispatching() throws Exception {
        Notification n = new Notification(8L, 700L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Multi Channel", "Msg", "CRITICAL");
        notificationBroker.dispatchNotification(n, "CRITICAL");
        Thread.sleep(300);

        List<Notification> notifs = mockNotificationDao.findByRecipientUserId(8L);
        assertTrue(notifs.size() >= 3, "CRITICAL priority should send across IN_APP, EMAIL, SMS, PUSH channels");
    }
}
