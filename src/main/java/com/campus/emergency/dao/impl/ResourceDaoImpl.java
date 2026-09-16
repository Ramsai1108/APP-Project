package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IResourceDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IResourceDao for MySQL.
 */
public class ResourceDaoImpl extends AbstractJDBCDao<Resource, Long> implements IResourceDao {

    private static final Logger logger = LoggerFactory.getLogger(ResourceDaoImpl.class);

    private static final String INSERT_RESOURCE = 
        "INSERT INTO resources (resource_name, resource_type, current_status, location_id) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_ID = 
        "SELECT id, resource_name, resource_type, current_status, location_id, created_at, updated_at FROM resources WHERE id = ?";

    private static final String SELECT_ALL = 
        "SELECT id, resource_name, resource_type, current_status, location_id, created_at, updated_at FROM resources";

    private static final String SELECT_BY_STATUS = 
        "SELECT id, resource_name, resource_type, current_status, location_id, created_at, updated_at FROM resources WHERE current_status = ?";

    private static final String SELECT_BY_TYPE_AND_STATUS = 
        "SELECT id, resource_name, resource_type, current_status, location_id, created_at, updated_at FROM resources WHERE resource_type = ? AND current_status = ?";

    @Override
    public Resource create(Resource entity) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_RESOURCE, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, entity.getResourceName());
            stmt.setString(2, entity.getResourceType());
            stmt.setString(3, entity.getCurrentStatus() != null ? entity.getCurrentStatus() : "AVAILABLE");
            stmt.setLong(4, entity.getLocationId());

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            logger.error("Database error creating resource", e);
            throw new DatabaseAccessException("Failed to create resource", e);
        }
    }

    @Override
    public Optional<Resource> findById(Long id) {
        if (id == null) return Optional.empty();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying resource ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query resource by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Resource> findByStatus(String status) {
        List<Resource> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_STATUS)) {

            stmt.setString(1, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying resources by status: {}", status, e);
            throw new DatabaseAccessException("Failed to query resources by status", e);
        }
        return list;
    }

    @Override
    public List<Resource> findByTypeAndStatus(String type, String status) {
        List<Resource> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_TYPE_AND_STATUS)) {

            stmt.setString(1, type);
            stmt.setString(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResource(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying resources by type {} and status {}", type, status, e);
            throw new DatabaseAccessException("Failed to query resources by type and status", e);
        }
        return list;
    }

    @Override
    public List<Resource> findAll() {
        List<Resource> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToResource(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error retrieving all resources", e);
            throw new DatabaseAccessException("Failed to query all resources", e);
        }
        return list;
    }

    @Override
    public boolean update(Resource entity) {
        String sql = "UPDATE resources SET resource_name = ?, resource_type = ?, current_status = ?, location_id = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entity.getResourceName());
            stmt.setString(2, entity.getResourceType());
            stmt.setString(3, entity.getCurrentStatus());
            stmt.setLong(4, entity.getLocationId());
            stmt.setLong(5, entity.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating resource ID: {}", entity.getId(), e);
            throw new DatabaseAccessException("Failed to update resource", e);
        }
    }

    public boolean updateStatusWithConnection(Long resourceId, String newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE resources SET current_status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, resourceId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting resource ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete resource", e);
        }
    }

    private Resource mapResultSetToResource(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getLong("id"));
        r.setResourceName(rs.getString("resource_name"));
        r.setResourceType(rs.getString("resource_type"));
        r.setCurrentStatus(rs.getString("current_status"));
        r.setLocationId(rs.getLong("location_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

        return r;
    }
}
