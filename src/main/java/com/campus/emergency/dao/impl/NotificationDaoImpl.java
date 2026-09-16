package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.INotificationDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationChannelType;
import com.campus.emergency.model.NotificationStatus;
import com.campus.emergency.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of INotificationDao using HikariCP and PreparedStatements.
 */
public class NotificationDaoImpl extends AbstractJDBCDao<Notification, Long> implements INotificationDao {

    private static final Logger logger = LoggerFactory.getLogger(NotificationDaoImpl.class);

    private static final String INSERT_NOTIFICATION =
            "INSERT INTO notifications (recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT id, recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at " +
            "FROM notifications WHERE id = ?";

    private static final String SELECT_BY_RECIPIENT =
            "SELECT id, recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at " +
            "FROM notifications WHERE recipient_user_id = ? ORDER BY created_at DESC";

    private static final String SELECT_UNREAD_BY_RECIPIENT =
            "SELECT id, recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at " +
            "FROM notifications WHERE recipient_user_id = ? AND status != 'READ' ORDER BY created_at DESC";

    private static final String SELECT_BY_INCIDENT =
            "SELECT id, recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at " +
            "FROM notifications WHERE incident_id = ? ORDER BY created_at DESC";

    private static final String COUNT_UNREAD_BY_RECIPIENT =
            "SELECT COUNT(*) FROM notifications WHERE recipient_user_id = ? AND status != 'READ'";

    private static final String MARK_AS_READ =
            "UPDATE notifications SET status = 'READ', read_at = ? WHERE id = ?";

    private static final String MARK_ALL_AS_READ =
            "UPDATE notifications SET status = 'READ', read_at = ? WHERE recipient_user_id = ? AND status != 'READ'";

    private static final String UPDATE_STATUS =
            "UPDATE notifications SET status = ?, sent_at = ? WHERE id = ?";

    @Override
    public Notification create(Notification notification) {
        try (Connection conn = getConnection()) {
            return createWithConnection(notification, conn);
        } catch (SQLException e) {
            logger.error("Database error while creating notification", e);
            throw new DatabaseAccessException("Failed to insert notification into database", e);
        }
    }

    public Notification createWithConnection(Notification notification, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_NOTIFICATION, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, notification.getRecipientUserId());

            if (notification.getIncidentId() != null) {
                stmt.setLong(2, notification.getIncidentId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            stmt.setString(3, notification.getNotificationType() != null ? notification.getNotificationType().name() : NotificationType.SYSTEM_ALERT.name());
            stmt.setString(4, notification.getChannel() != null ? notification.getChannel().name() : NotificationChannelType.IN_APP.name());
            stmt.setString(5, notification.getTitle());
            stmt.setString(6, notification.getMessage());
            stmt.setString(7, notification.getPriority() != null ? notification.getPriority() : "MEDIUM");
            stmt.setString(8, notification.getStatus() != null ? notification.getStatus().name() : NotificationStatus.PENDING.name());

            stmt.setTimestamp(9, notification.getCreatedAt() != null ? Timestamp.valueOf(notification.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(10, notification.getSentAt() != null ? Timestamp.valueOf(notification.getSentAt()) : null);
            stmt.setTimestamp(11, notification.getReadAt() != null ? Timestamp.valueOf(notification.getReadAt()) : null);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating notification failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    notification.setId(generatedKeys.getLong(1));
                }
            }

            logger.info("Notification created successfully with ID #{}", notification.getId());
            return notification;
        }
    }

    @Override
    public Optional<Notification> findById(Long id) {
        if (id == null) return Optional.empty();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error finding notification by ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query notification by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Notification> findByRecipientUserId(Long recipientUserId) {
        List<Notification> notifications = new ArrayList<>();
        if (recipientUserId == null) return notifications;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_RECIPIENT)) {

            stmt.setLong(1, recipientUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying notifications for user ID: {}", recipientUserId, e);
            throw new DatabaseAccessException("Failed to query notifications by recipient user ID", e);
        }
        return notifications;
    }

    @Override
    public List<Notification> findUnreadByRecipientUserId(Long recipientUserId) {
        List<Notification> notifications = new ArrayList<>();
        if (recipientUserId == null) return notifications;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_UNREAD_BY_RECIPIENT)) {

            stmt.setLong(1, recipientUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying unread notifications for user ID: {}", recipientUserId, e);
            throw new DatabaseAccessException("Failed to query unread notifications", e);
        }
        return notifications;
    }

    @Override
    public boolean markAsRead(Long notificationId) {
        if (notificationId == null) return false;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(MARK_AS_READ)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, notificationId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error marking notification ID #{} as read", notificationId, e);
            throw new DatabaseAccessException("Failed to mark notification as read", e);
        }
    }

    @Override
    public boolean markAllAsRead(Long recipientUserId) {
        if (recipientUserId == null) return false;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(MARK_ALL_AS_READ)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(2, recipientUserId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error marking all notifications as read for user ID: {}", recipientUserId, e);
            throw new DatabaseAccessException("Failed to mark all notifications as read", e);
        }
    }

    @Override
    public boolean updateStatus(Long notificationId, NotificationStatus status) {
        if (notificationId == null || status == null) return false;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_STATUS)) {

            stmt.setString(1, status.name());
            if (status == NotificationStatus.SENT) {
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            } else {
                stmt.setNull(2, Types.TIMESTAMP);
            }
            stmt.setLong(3, notificationId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating status for notification ID #{}", notificationId, e);
            throw new DatabaseAccessException("Failed to update notification status", e);
        }
    }

    @Override
    public List<Notification> findByIncidentId(Long incidentId) {
        List<Notification> notifications = new ArrayList<>();
        if (incidentId == null) return notifications;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_INCIDENT)) {

            stmt.setLong(1, incidentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notifications.add(mapResultSetToNotification(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying notifications for incident ID: {}", incidentId, e);
            throw new DatabaseAccessException("Failed to query notifications by incident ID", e);
        }
        return notifications;
    }

    @Override
    public int countUnreadByRecipientUserId(Long recipientUserId) {
        if (recipientUserId == null) return 0;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_UNREAD_BY_RECIPIENT)) {

            stmt.setLong(1, recipientUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error counting unread notifications for user ID: {}", recipientUserId, e);
            throw new DatabaseAccessException("Failed to count unread notifications", e);
        }
        return 0;
    }

    @Override
    public List<Notification> findAll() {
        String sql = "SELECT id, recipient_user_id, incident_id, notification_type, channel, title, message, priority, status, created_at, sent_at, read_at FROM notifications ORDER BY created_at DESC";
        List<Notification> notifications = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error querying all notifications", e);
            throw new DatabaseAccessException("Failed to query all notifications", e);
        }
        return notifications;
    }

    @Override
    public boolean update(Notification notification) {
        if (notification == null || notification.getId() == null) return false;
        String sql = "UPDATE notifications SET recipient_user_id = ?, incident_id = ?, notification_type = ?, channel = ?, title = ?, message = ?, priority = ?, status = ?, sent_at = ?, read_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, notification.getRecipientUserId());
            if (notification.getIncidentId() != null) {
                stmt.setLong(2, notification.getIncidentId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, notification.getNotificationType() != null ? notification.getNotificationType().name() : "SYSTEM_ALERT");
            stmt.setString(4, notification.getChannel() != null ? notification.getChannel().name() : "IN_APP");
            stmt.setString(5, notification.getTitle());
            stmt.setString(6, notification.getMessage());
            stmt.setString(7, notification.getPriority() != null ? notification.getPriority() : "MEDIUM");
            stmt.setString(8, notification.getStatus() != null ? notification.getStatus().name() : "PENDING");
            stmt.setTimestamp(9, notification.getSentAt() != null ? Timestamp.valueOf(notification.getSentAt()) : null);
            stmt.setTimestamp(10, notification.getReadAt() != null ? Timestamp.valueOf(notification.getReadAt()) : null);
            stmt.setLong(11, notification.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating notification ID: {}", notification.getId(), e);
            throw new DatabaseAccessException("Failed to update notification", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting notification ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete notification", e);
        }
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setRecipientUserId(rs.getLong("recipient_user_id"));

        long incidentId = rs.getLong("incident_id");
        if (!rs.wasNull()) {
            notification.setIncidentId(incidentId);
        }

        String typeStr = rs.getString("notification_type");
        if (typeStr != null) {
            try {
                notification.setNotificationType(NotificationType.valueOf(typeStr));
            } catch (Exception e) {
                notification.setNotificationType(NotificationType.SYSTEM_ALERT);
            }
        }

        String channelStr = rs.getString("channel");
        if (channelStr != null) {
            try {
                notification.setChannel(NotificationChannelType.valueOf(channelStr));
            } catch (Exception e) {
                notification.setChannel(NotificationChannelType.IN_APP);
            }
        }

        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setPriority(rs.getString("priority"));

        String statusStr = rs.getString("status");
        if (statusStr != null) {
            try {
                notification.setStatus(NotificationStatus.valueOf(statusStr));
            } catch (Exception e) {
                notification.setStatus(NotificationStatus.PENDING);
            }
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) notification.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp sentAt = rs.getTimestamp("sent_at");
        if (sentAt != null) notification.setSentAt(sentAt.toLocalDateTime());

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) notification.setReadAt(readAt.toLocalDateTime());

        return notification;
    }
}
