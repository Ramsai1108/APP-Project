package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.EmergencyIncident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IIncidentDao for MySQL.
 */
public class IncidentDaoImpl extends AbstractJDBCDao<EmergencyIncident, Long> implements IIncidentDao {

    private static final Logger logger = LoggerFactory.getLogger(IncidentDaoImpl.class);

    private static final String INSERT_INCIDENT = 
        "INSERT INTO emergency_incidents (report_id, calculated_priority, override_priority, status, triaged_by_user_id, triaged_at) " +
        "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_INCIDENT_BY_ID = 
        "SELECT id, report_id, calculated_priority, override_priority, effective_priority, status, triaged_by_user_id, triaged_at, resolved_at, closed_at, created_at, updated_at " +
        "FROM emergency_incidents WHERE id = ?";

    private static final String SELECT_BY_STATUS = 
        "SELECT id, report_id, calculated_priority, override_priority, effective_priority, status, triaged_by_user_id, triaged_at, resolved_at, closed_at, created_at, updated_at " +
        "FROM emergency_incidents WHERE status = ? ORDER BY effective_priority DESC, created_at ASC";

    private static final String SELECT_ACTIVE_SORTED = 
        "SELECT id, report_id, calculated_priority, override_priority, effective_priority, status, triaged_by_user_id, triaged_at, resolved_at, closed_at, created_at, updated_at " +
        "FROM emergency_incidents WHERE status NOT IN ('RESOLVED', 'CLOSED') ORDER BY effective_priority DESC, created_at ASC";

    @Override
    public EmergencyIncident create(EmergencyIncident incident) {
        try (Connection conn = getConnection()) {
            return createWithConnection(incident, conn);
        } catch (SQLException e) {
            logger.error("Database error while creating emergency incident", e);
            throw new DatabaseAccessException("Failed to insert emergency incident into database", e);
        }
    }

    public EmergencyIncident createWithConnection(EmergencyIncident incident, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_INCIDENT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, incident.getReportId());
            stmt.setDouble(2, incident.getCalculatedPriority());

            if (incident.getOverridePriority() != null) {
                stmt.setDouble(3, incident.getOverridePriority());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }

            stmt.setString(4, incident.getStatus() != null ? incident.getStatus() : "REPORTED");

            if (incident.getTriagedByUserId() != null) {
                stmt.setLong(5, incident.getTriagedByUserId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }

            if (incident.getTriagedAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(incident.getTriagedAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating incident failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    incident.setId(generatedKeys.getLong(1));
                }
            }

            logger.info("Emergency incident created successfully with ID {}", incident.getId());
            return incident;
        }
    }

    @Override
    public Optional<EmergencyIncident> findById(Long id) {
        if (id == null) return Optional.empty();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_INCIDENT_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToIncident(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error finding incident by ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query incident by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public List<EmergencyIncident> findByStatus(String status) {
        List<EmergencyIncident> incidents = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_STATUS)) {

            stmt.setString(1, status);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    incidents.add(mapResultSetToIncident(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying incidents by status: {}", status, e);
            throw new DatabaseAccessException("Failed to query incidents by status", e);
        }
        return incidents;
    }

    @Override
    public List<EmergencyIncident> findActiveIncidentsSortedByPriority() {
        List<EmergencyIncident> incidents = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACTIVE_SORTED);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                incidents.add(mapResultSetToIncident(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error querying active incidents sorted by priority", e);
            throw new DatabaseAccessException("Failed to query active incidents", e);
        }
        return incidents;
    }

    @Override
    public List<EmergencyIncident> findAll() {
        return findActiveIncidentsSortedByPriority();
    }

    @Override
    public boolean update(EmergencyIncident incident) {
        String sql = "UPDATE emergency_incidents SET calculated_priority = ?, override_priority = ?, status = ?, triaged_by_user_id = ?, triaged_at = ?, resolved_at = ?, closed_at = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, incident.getCalculatedPriority());

            if (incident.getOverridePriority() != null) {
                stmt.setDouble(2, incident.getOverridePriority());
            } else {
                stmt.setNull(2, Types.DECIMAL);
            }

            stmt.setString(3, incident.getStatus());

            if (incident.getTriagedByUserId() != null) {
                stmt.setLong(4, incident.getTriagedByUserId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }

            stmt.setTimestamp(5, incident.getTriagedAt() != null ? Timestamp.valueOf(incident.getTriagedAt()) : null);
            stmt.setTimestamp(6, incident.getResolvedAt() != null ? Timestamp.valueOf(incident.getResolvedAt()) : null);
            stmt.setTimestamp(7, incident.getClosedAt() != null ? Timestamp.valueOf(incident.getClosedAt()) : null);
            stmt.setLong(8, incident.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating incident ID: {}", incident.getId(), e);
            throw new DatabaseAccessException("Failed to update emergency incident", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM emergency_incidents WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting incident ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete emergency incident", e);
        }
    }

    private EmergencyIncident mapResultSetToIncident(ResultSet rs) throws SQLException {
        EmergencyIncident incident = new EmergencyIncident();
        incident.setId(rs.getLong("id"));
        incident.setReportId(rs.getLong("report_id"));
        incident.setCalculatedPriority(rs.getDouble("calculated_priority"));

        double override = rs.getDouble("override_priority");
        if (!rs.wasNull()) {
            incident.setOverridePriority(override);
        }

        incident.setEffectivePriority(rs.getDouble("effective_priority"));
        incident.setStatus(rs.getString("status"));

        long triagedBy = rs.getLong("triaged_by_user_id");
        if (!rs.wasNull()) {
            incident.setTriagedByUserId(triagedBy);
        }

        Timestamp triagedAt = rs.getTimestamp("triaged_at");
        if (triagedAt != null) incident.setTriagedAt(triagedAt.toLocalDateTime());

        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) incident.setResolvedAt(resolvedAt.toLocalDateTime());

        Timestamp closedAt = rs.getTimestamp("closed_at");
        if (closedAt != null) incident.setClosedAt(closedAt.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) incident.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) incident.setUpdatedAt(updatedAt.toLocalDateTime());

        return incident;
    }
}
