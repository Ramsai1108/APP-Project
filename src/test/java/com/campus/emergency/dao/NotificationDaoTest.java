package com.campus.emergency.dao;

import com.campus.emergency.dao.impl.NotificationDaoImpl;
import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import com.campus.emergency.model.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationDaoTest {

    private INotificationDao notificationDao;

    @BeforeEach
    public void setUp() {
        // In-memory test implementation of INotificationDao
        notificationDao = new INotificationDao() {
            private final List<Notification> storage = new ArrayList<>();
            private final AtomicLong idGenerator = new AtomicLong(1);

            @Override
            public Notification create(Notification n) {
                if (n.getId() == null) {
                    n.setId(idGenerator.getAndIncrement());
                }
                storage.add(n);
                return n;
            }

            @Override
            public Optional<Notification> findById(Long id) {
                return storage.stream().filter(n -> n.getId().equals(id)).findFirst();
            }

            @Override
            public List<Notification> findByRecipientUserId(Long recipientUserId) {
                return storage.stream().filter(n -> n.getRecipientUserId().equals(recipientUserId)).collect(Collectors.toList());
            }

            @Override
            public List<Notification> findUnreadByRecipientUserId(Long recipientUserId) {
                return storage.stream()
                        .filter(n -> n.getRecipientUserId().equals(recipientUserId) && n.getStatus() != NotificationStatus.READ)
                        .collect(Collectors.toList());
            }

            @Override
            public boolean markAsRead(Long notificationId) {
                Optional<Notification> opt = findById(notificationId);
                if (opt.isPresent()) {
                    opt.get().setStatus(NotificationStatus.READ);
                    return true;
                }
                return false;
            }

            @Override
            public boolean markAllAsRead(Long recipientUserId) {
                List<Notification> unread = findUnreadByRecipientUserId(recipientUserId);
                unread.forEach(n -> n.setStatus(NotificationStatus.READ));
                return !unread.isEmpty();
            }

            @Override
            public boolean updateStatus(Long notificationId, NotificationStatus status) {
                Optional<Notification> opt = findById(notificationId);
                if (opt.isPresent()) {
                    opt.get().setStatus(status);
                    return true;
                }
                return false;
            }

            @Override
            public List<Notification> findByIncidentId(Long incidentId) {
                return storage.stream().filter(n -> incidentId.equals(n.getIncidentId())).collect(Collectors.toList());
            }

            @Override
            public int countUnreadByRecipientUserId(Long recipientUserId) {
                return findUnreadByRecipientUserId(recipientUserId).size();
            }

            @Override
            public List<Notification> findAll() {
                return new ArrayList<>(storage);
            }

            @Override
            public boolean update(Notification entity) {
                return true;
            }

            @Override
            public boolean deleteById(Long id) {
                return storage.removeIf(n -> n.getId().equals(id));
            }
        };
    }

    @Test
    public void test1_CreateNotification() {
        Notification n = new Notification(10L, 100L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Fire Alert", "Fire reported", "HIGH");
        Notification saved = notificationDao.create(n);
        assertNotNull(saved.getId());
        assertEquals("Fire Alert", saved.getTitle());
    }

    @Test
    public void test2_RetrieveNotificationById() {
        Notification n = new Notification(10L, 100L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Test Title", "Test Message", "MEDIUM");
        Notification saved = notificationDao.create(n);

        Optional<Notification> retrieved = notificationDao.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Test Title", retrieved.get().getTitle());
    }

    @Test
    public void test3_RetrieveUserNotifications() {
        notificationDao.create(new Notification(15L, 101L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Title 1", "Msg 1", "LOW"));
        notificationDao.create(new Notification(15L, 102L, NotificationType.RESOURCE_DISPATCHED, NotificationChannelType.EMAIL, "Title 2", "Msg 2", "HIGH"));

        List<Notification> userNotifs = notificationDao.findByRecipientUserId(15L);
        assertEquals(2, userNotifs.size());
    }

    @Test
    public void test4_RetrieveUnreadNotifications() {
        Notification n1 = notificationDao.create(new Notification(20L, 103L, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, "Unread Alert", "Msg", "CRITICAL"));
        Notification n2 = notificationDao.create(new Notification(20L, 104L, NotificationType.SYSTEM_ALERT, NotificationChannelType.IN_APP, "Read Alert", "Msg", "LOW"));
        n2.setStatus(NotificationStatus.READ);

        List<Notification> unread = notificationDao.findUnreadByRecipientUserId(20L);
        assertEquals(1, unread.size());
        assertEquals("Unread Alert", unread.get(0).getTitle());
    }

    @Test
    public void test5_MarkNotificationAsRead() {
        Notification n = notificationDao.create(new Notification(25L, 105L, NotificationType.INCIDENT_STATUS_CHANGED, NotificationChannelType.IN_APP, "Status Update", "Msg", "MEDIUM"));
        assertTrue(notificationDao.markAsRead(n.getId()));
        assertEquals(NotificationStatus.READ, notificationDao.findById(n.getId()).get().getStatus());
    }

    @Test
    public void test6_MarkAllNotificationsAsRead() {
        notificationDao.create(new Notification(30L, 106L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Alert 1", "Msg", "HIGH"));
        notificationDao.create(new Notification(30L, 107L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Alert 2", "Msg", "HIGH"));

        assertTrue(notificationDao.markAllAsRead(30L));
        assertEquals(0, notificationDao.countUnreadByRecipientUserId(30L));
    }

    @Test
    public void test7_FindNotificationsByIncidentId() {
        notificationDao.create(new Notification(1L, 999L, NotificationType.EMERGENCY_CREATED, NotificationChannelType.IN_APP, "Incident Alert", "Msg", "HIGH"));
        notificationDao.create(new Notification(2L, 999L, NotificationType.RESOURCE_DISPATCHED, NotificationChannelType.EMAIL, "Dispatch Alert", "Msg", "HIGH"));

        List<Notification> incidentNotifs = notificationDao.findByIncidentId(999L);
        assertEquals(2, incidentNotifs.size());
    }
}
