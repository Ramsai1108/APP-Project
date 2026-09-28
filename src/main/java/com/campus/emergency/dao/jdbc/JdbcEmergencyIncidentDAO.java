package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.EmergencyIncident;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEmergencyIncidentDAO implements EmergencyIncidentDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcEmergencyIncidentDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    public EmergencyIncident create(EmergencyIncident incident, Connection conn) throws SQLException {
        String sql = "INSERT INTO emergency_incidents (report_id, priority, status, assigned_dispatcher_id) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, incident.getReportId());
            stmt.setString(2, incident.getPriority() != null ? incident.getPriority() : "MEDIUM");
            stmt.setString(3, incident.getStatus() != null ? incident.getStatus() : "OPEN");
            if (incident.getAssignedDispatcherId() != null) {
                stmt.setInt(4, incident.getAssignedDispatcherId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating emergency incident failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    incident.setId(generatedKeys.getInt(1));
                }
            }
            return incident;
        }
    }

    @Override
    public EmergencyIncident create(EmergencyIncident incident) {
        try (Connection conn = getConnection()) {
            return create(incident, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating emergency incident: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EmergencyIncident> findById(int id) {
        String sql = "SELECT id, report_id, priority, status, assigned_dispatcher_id, created_at, resolved_at " +
                "FROM emergency_incidents WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToIncident(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding emergency incident by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<EmergencyIncident> findByReportId(int reportId) {
        String sql = "SELECT id, report_id, priority, status, assigned_dispatcher_id, created_at, resolved_at " +
                "FROM emergency_incidents WHERE report_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reportId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToIncident(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding incident by report ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<EmergencyIncident> findAllActive() {
        String sql = "SELECT id, report_id, priority, status, assigned_dispatcher_id, created_at, resolved_at " +
                "FROM emergency_incidents WHERE status IN ('OPEN', 'IN_PROGRESS') ORDER BY priority DESC, id DESC";

        List<EmergencyIncident> incidents = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                incidents.add(mapResultSetToIncident(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing active incidents: " + e.getMessage(), e);
        }
        return incidents;
    }

    @Override
    public List<EmergencyIncident> findAll() {
        String sql = "SELECT id, report_id, priority, status, assigned_dispatcher_id, created_at, resolved_at " +
                "FROM emergency_incidents ORDER BY id DESC";

        List<EmergencyIncident> incidents = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                incidents.add(mapResultSetToIncident(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing all incidents: " + e.getMessage(), e);
        }
        return incidents;
    }

    @Override
    public boolean updatePriority(int id, String priority) {
        String sql = "UPDATE emergency_incidents SET priority = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, priority);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating incident priority: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int id, String status, Connection conn) throws SQLException {
        String sql = "UPDATE emergency_incidents SET status = ?, " +
                "resolved_at = CASE WHEN ? IN ('RESOLVED', 'CLOSED') THEN CURRENT_TIMESTAMP ELSE resolved_at END " +
                "WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, status);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateStatus(int id, String status) {
        try (Connection conn = getConnection()) {
            return updateStatus(id, status, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating incident status: " + e.getMessage(), e);
        }
    }

    public boolean assignDispatcher(int incidentId, int dispatcherId, Connection conn) throws SQLException {
        String sql = "UPDATE emergency_incidents SET assigned_dispatcher_id = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, dispatcherId);
            stmt.setInt(2, incidentId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean assignDispatcher(int incidentId, int dispatcherId) {
        try (Connection conn = getConnection()) {
            return assignDispatcher(incidentId, dispatcherId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error assigning dispatcher to incident: " + e.getMessage(), e);
        }
    }

    private EmergencyIncident mapResultSetToIncident(ResultSet rs) throws SQLException {
        EmergencyIncident incident = new EmergencyIncident();
        incident.setId(rs.getInt("id"));
        incident.setReportId(rs.getInt("report_id"));
        incident.setPriority(rs.getString("priority"));
        incident.setStatus(rs.getString("status"));

        int dispatcherId = rs.getInt("assigned_dispatcher_id");
        if (!rs.wasNull()) {
            incident.setAssignedDispatcherId(dispatcherId);
        } else {
            incident.setAssignedDispatcherId(null);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            incident.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp resolvedAt = rs.getTimestamp("resolved_at");
        if (resolvedAt != null) {
            incident.setResolvedAt(resolvedAt.toLocalDateTime());
        }
        return incident;
    }
}
