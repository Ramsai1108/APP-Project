package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.ResourceDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.Resource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcResourceDAO implements ResourceDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcResourceDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    public Resource create(Resource resource, Connection conn) throws SQLException {
        String sql = "INSERT INTO resources (name, type, status, location_id, assigned_incident_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, resource.getName());
            stmt.setString(2, resource.getType());
            stmt.setString(3, resource.getStatus() != null ? resource.getStatus() : "AVAILABLE");
            if (resource.getLocationId() != null) {
                stmt.setInt(4, resource.getLocationId());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            if (resource.getAssignedIncidentId() != null) {
                stmt.setInt(5, resource.getAssignedIncidentId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating resource failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    resource.setId(generatedKeys.getInt(1));
                }
            }
            return resource;
        }
    }

    @Override
    public Resource create(Resource resource) {
        try (Connection conn = getConnection()) {
            return create(resource, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating resource: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Resource> findById(int id) {
        String sql = "SELECT id, name, type, status, location_id, assigned_incident_id FROM resources WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding resource by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Resource> findAvailable() {
        String sql = "SELECT id, name, type, status, location_id, assigned_incident_id FROM resources WHERE status = 'AVAILABLE' ORDER BY id ASC";

        List<Resource> resources = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing available resources: " + e.getMessage(), e);
        }
        return resources;
    }

    @Override
    public List<Resource> findAll() {
        String sql = "SELECT id, name, type, status, location_id, assigned_incident_id FROM resources ORDER BY id ASC";

        List<Resource> resources = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                resources.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing all resources: " + e.getMessage(), e);
        }
        return resources;
    }

    public boolean updateStatus(int id, String status, Connection conn) throws SQLException {
        String sql = "UPDATE resources SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateStatus(int id, String status) {
        try (Connection conn = getConnection()) {
            return updateStatus(id, status, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating resource status: " + e.getMessage(), e);
        }
    }

    public boolean assignToIncident(int resourceId, int incidentId, Connection conn) throws SQLException {
        String sql = "UPDATE resources SET assigned_incident_id = ?, status = 'IN_USE' WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, incidentId);
            stmt.setInt(2, resourceId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean assignToIncident(int resourceId, int incidentId) {
        try (Connection conn = getConnection()) {
            return assignToIncident(resourceId, incidentId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error assigning resource to incident: " + e.getMessage(), e);
        }
    }

    public boolean releaseResource(int resourceId, Connection conn) throws SQLException {
        String sql = "UPDATE resources SET assigned_incident_id = NULL, status = 'AVAILABLE' WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, resourceId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean releaseResource(int resourceId) {
        try (Connection conn = getConnection()) {
            return releaseResource(resourceId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error releasing resource: " + e.getMessage(), e);
        }
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id"));
        resource.setName(rs.getString("name"));
        resource.setType(rs.getString("type"));
        resource.setStatus(rs.getString("status"));

        int locId = rs.getInt("location_id");
        if (!rs.wasNull()) {
            resource.setLocationId(locId);
        } else {
            resource.setLocationId(null);
        }

        int incId = rs.getInt("assigned_incident_id");
        if (!rs.wasNull()) {
            resource.setAssignedIncidentId(incId);
        } else {
            resource.setAssignedIncidentId(null);
        }
        return resource;
    }
}
