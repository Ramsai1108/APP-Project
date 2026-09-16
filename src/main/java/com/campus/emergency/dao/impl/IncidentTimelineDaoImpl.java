package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IIncidentTimelineDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.IncidentTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IIncidentTimelineDao using HikariCP and PreparedStatements.
 */
public class IncidentTimelineDaoImpl extends AbstractJDBCDao<IncidentTimeline, Long> implements IIncidentTimelineDao {

    private static final Logger logger = LoggerFactory.getLogger(IncidentTimelineDaoImpl.class);

    private static final String INSERT_TIMELINE =
            "INSERT INTO incident_timeline (incident_id, action_by_user_id, action_type, description, created_at) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_INCIDENT =
            "SELECT id, incident_id, action_by_user_id, action_type, description, created_at " +
            "FROM incident_timeline WHERE incident_id = ? ORDER BY created_at ASC";

    private static final String SELECT_BY_ID =
            "SELECT id, incident_id, action_by_user_id, action_type, description, created_at " +
            "FROM incident_timeline WHERE id = ?";

    @Override
    public IncidentTimeline create(IncidentTimeline timeline) {
        try (Connection conn = getConnection()) {
            return createWithConnection(timeline, conn);
        } catch (SQLException e) {
            logger.error("Database error creating incident timeline entry", e);
            throw new DatabaseAccessException("Failed to insert incident timeline event into database", e);
        }
    }

    public IncidentTimeline createWithConnection(IncidentTimeline timeline, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_TIMELINE, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, timeline.getIncidentId());

            if (timeline.getActionByUserId() != null) {
                stmt.setLong(2, timeline.getActionByUserId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            stmt.setString(3, timeline.getActionType() != null ? timeline.getActionType() : "STATUS_CHANGED");
            stmt.setString(4, timeline.getDescription() != null ? timeline.getDescription() : "");
            stmt.setTimestamp(5, timeline.getCreatedAt() != null ? Timestamp.valueOf(timeline.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating incident timeline failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    timeline.setId(generatedKeys.getLong(1));
                }
            }

            logger.info("Incident timeline event logged successfully with ID #{}", timeline.getId());
            return timeline;
        }
    }

    @Override
    public List<IncidentTimeline> findByIncidentId(Long incidentId) {
        List<IncidentTimeline> timelineList = new ArrayList<>();
        if (incidentId == null) return timelineList;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_INCIDENT)) {

            stmt.setLong(1, incidentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    timelineList.add(mapResultSetToTimeline(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying incident timeline for incident ID: {}", incidentId, e);
            throw new DatabaseAccessException("Failed to query incident timeline", e);
        }

        return timelineList;
    }

    @Override
    public Optional<IncidentTimeline> findById(Long id) {
        if (id == null) return Optional.empty();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTimeline(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error finding timeline entry by ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query timeline entry by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public List<IncidentTimeline> findAll() {
        String sql = "SELECT id, incident_id, action_by_user_id, action_type, description, created_at FROM incident_timeline ORDER BY created_at DESC";
        List<IncidentTimeline> timelineList = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                timelineList.add(mapResultSetToTimeline(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error querying all timeline entries", e);
            throw new DatabaseAccessException("Failed to query all timeline entries", e);
        }
        return timelineList;
    }

    @Override
    public boolean update(IncidentTimeline entity) {
        return true;
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM incident_timeline WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting timeline entry ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete timeline entry", e);
        }
    }

    private IncidentTimeline mapResultSetToTimeline(ResultSet rs) throws SQLException {
        IncidentTimeline timeline = new IncidentTimeline();
        timeline.setId(rs.getLong("id"));
        timeline.setIncidentId(rs.getLong("incident_id"));

        long userId = rs.getLong("action_by_user_id");
        if (!rs.wasNull()) {
            timeline.setActionByUserId(userId);
        }

        timeline.setActionType(rs.getString("action_type"));
        timeline.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            timeline.setCreatedAt(createdAt.toLocalDateTime());
        }

        return timeline;
    }
}
