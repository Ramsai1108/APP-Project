package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IResourceAllocationDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.ResourceAllocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IResourceAllocationDao for MySQL.
 */
public class ResourceAllocationDaoImpl extends AbstractJDBCDao<ResourceAllocation, Long> implements IResourceAllocationDao {

    private static final Logger logger = LoggerFactory.getLogger(ResourceAllocationDaoImpl.class);

    private static final String INSERT_ALLOCATION = 
        "INSERT INTO resource_allocations (incident_id, resource_id, responder_id, allocated_by_user_id, status) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_INCIDENT = 
        "SELECT id, incident_id, resource_id, responder_id, allocated_by_user_id, allocated_at, released_at, status FROM resource_allocations WHERE incident_id = ?";

    private static final String SELECT_ACTIVE = 
        "SELECT id, incident_id, resource_id, responder_id, allocated_by_user_id, allocated_at, released_at, status FROM resource_allocations WHERE status = 'ACTIVE'";

    @Override
    public ResourceAllocation create(ResourceAllocation entity) {
        try (Connection conn = getConnection()) {
            return createWithConnection(entity, conn);
        } catch (SQLException e) {
            logger.error("Database error creating resource allocation", e);
            throw new DatabaseAccessException("Failed to record resource allocation", e);
        }
    }

    public ResourceAllocation createWithConnection(ResourceAllocation entity, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_ALLOCATION, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, entity.getIncidentId());

            if (entity.getResourceId() != null) {
                stmt.setLong(2, entity.getResourceId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }

            if (entity.getResponderId() != null) {
                stmt.setLong(3, entity.getResponderId());
            } else {
                stmt.setNull(3, Types.BIGINT);
            }

            stmt.setLong(4, entity.getAllocatedByUserId());
            stmt.setString(5, entity.getStatus() != null ? entity.getStatus() : "ACTIVE");

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }

            logger.info("Resource allocation recorded with ID {}", entity.getId());
            return entity;
        }
    }

    @Override
    public Optional<ResourceAllocation> findById(Long id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, incident_id, resource_id, responder_id, allocated_by_user_id, allocated_at, released_at, status FROM resource_allocations WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAllocation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying allocation ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query allocation by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<ResourceAllocation> findByIncidentId(Long incidentId) {
        List<ResourceAllocation> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_INCIDENT)) {

            stmt.setLong(1, incidentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToAllocation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying allocations for incident ID: {}", incidentId, e);
            throw new DatabaseAccessException("Failed to query allocations for incident", e);
        }
        return list;
    }

    @Override
    public List<ResourceAllocation> findActiveAllocations() {
        List<ResourceAllocation> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ACTIVE);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToAllocation(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error querying active allocations", e);
            throw new DatabaseAccessException("Failed to query active allocations", e);
        }
        return list;
    }

    @Override
    public List<ResourceAllocation> findAll() {
        return findActiveAllocations();
    }

    @Override
    public boolean update(ResourceAllocation entity) {
        String sql = "UPDATE resource_allocations SET released_at = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, entity.getReleasedAt() != null ? Timestamp.valueOf(entity.getReleasedAt()) : null);
            stmt.setString(2, entity.getStatus());
            stmt.setLong(3, entity.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating allocation ID: {}", entity.getId(), e);
            throw new DatabaseAccessException("Failed to update allocation", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM resource_allocations WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting allocation ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete allocation", e);
        }
    }

    private ResourceAllocation mapResultSetToAllocation(ResultSet rs) throws SQLException {
        ResourceAllocation ra = new ResourceAllocation();
        ra.setId(rs.getLong("id"));
        ra.setIncidentId(rs.getLong("incident_id"));

        long resId = rs.getLong("resource_id");
        if (!rs.wasNull()) ra.setResourceId(resId);

        long respId = rs.getLong("responder_id");
        if (!rs.wasNull()) ra.setResponderId(respId);

        ra.setAllocatedByUserId(rs.getLong("allocated_by_user_id"));

        Timestamp allocatedAt = rs.getTimestamp("allocated_at");
        if (allocatedAt != null) ra.setAllocatedAt(allocatedAt.toLocalDateTime());

        Timestamp releasedAt = rs.getTimestamp("released_at");
        if (releasedAt != null) ra.setReleasedAt(releasedAt.toLocalDateTime());

        ra.setStatus(rs.getString("status"));
        return ra;
    }
}
