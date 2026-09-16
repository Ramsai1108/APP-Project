package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IResponderDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.Responder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IResponderDao for MySQL.
 */
public class ResponderDaoImpl extends AbstractJDBCDao<Responder, Long> implements IResponderDao {

    private static final Logger logger = LoggerFactory.getLogger(ResponderDaoImpl.class);

    private static final String INSERT_RESPONDER = 
        "INSERT INTO responders (user_id, full_name, specialization, phone, duty_status, assigned_location_id) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID = 
        "SELECT id, user_id, full_name, specialization, phone, duty_status, assigned_location_id, created_at, updated_at FROM responders WHERE id = ?";

    private static final String SELECT_ALL = 
        "SELECT id, user_id, full_name, specialization, phone, duty_status, assigned_location_id, created_at, updated_at FROM responders";

    private static final String SELECT_BY_DUTY_STATUS = 
        "SELECT id, user_id, full_name, specialization, phone, duty_status, assigned_location_id, created_at, updated_at FROM responders WHERE duty_status = ?";

    private static final String SELECT_AVAILABLE_BY_SPECIALIZATION = 
        "SELECT id, user_id, full_name, specialization, phone, duty_status, assigned_location_id, created_at, updated_at FROM responders WHERE specialization = ? AND duty_status = 'ON_DUTY'";

    @Override
    public Responder create(Responder entity) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_RESPONDER, Statement.RETURN_GENERATED_KEYS)) {

            if (entity.getUserId() != null) {
                stmt.setLong(1, entity.getUserId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, entity.getFullName());
            stmt.setString(3, entity.getSpecialization());
            stmt.setString(4, entity.getPhone());
            stmt.setString(5, entity.getDutyStatus() != null ? entity.getDutyStatus() : "OFF_DUTY");

            if (entity.getAssignedLocationId() != null) {
                stmt.setLong(6, entity.getAssignedLocationId());
            } else {
                stmt.setNull(6, Types.BIGINT);
            }

            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            logger.error("Database error creating responder", e);
            throw new DatabaseAccessException("Failed to create responder", e);
        }
    }

    @Override
    public Optional<Responder> findById(Long id) {
        if (id == null) return Optional.empty();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResponder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying responder ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query responder by ID", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Responder> findByDutyStatus(String dutyStatus) {
        List<Responder> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_DUTY_STATUS)) {

            stmt.setString(1, dutyStatus);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResponder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying responders by duty status: {}", dutyStatus, e);
            throw new DatabaseAccessException("Failed to query responders by duty status", e);
        }
        return list;
    }

    @Override
    public List<Responder> findAvailableBySpecialization(String specialization) {
        List<Responder> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_AVAILABLE_BY_SPECIALIZATION)) {

            stmt.setString(1, specialization);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToResponder(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error querying available responders by specialization: {}", specialization, e);
            throw new DatabaseAccessException("Failed to query available responders by specialization", e);
        }
        return list;
    }

    @Override
    public List<Responder> findAll() {
        List<Responder> list = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapResultSetToResponder(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error retrieving all responders", e);
            throw new DatabaseAccessException("Failed to query all responders", e);
        }
        return list;
    }

    @Override
    public boolean update(Responder entity) {
        String sql = "UPDATE responders SET full_name = ?, specialization = ?, phone = ?, duty_status = ?, assigned_location_id = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entity.getFullName());
            stmt.setString(2, entity.getSpecialization());
            stmt.setString(3, entity.getPhone());
            stmt.setString(4, entity.getDutyStatus());
            if (entity.getAssignedLocationId() != null) {
                stmt.setLong(5, entity.getAssignedLocationId());
            } else {
                stmt.setNull(5, Types.BIGINT);
            }
            stmt.setLong(6, entity.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error updating responder ID: {}", entity.getId(), e);
            throw new DatabaseAccessException("Failed to update responder", e);
        }
    }

    public boolean updateDutyStatusWithConnection(Long responderId, String newStatus, Connection conn) throws SQLException {
        String sql = "UPDATE responders SET duty_status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, responderId);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM responders WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error deleting responder ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete responder", e);
        }
    }

    private Responder mapResultSetToResponder(ResultSet rs) throws SQLException {
        Responder r = new Responder();
        r.setId(rs.getLong("id"));
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) r.setUserId(uid);

        r.setFullName(rs.getString("full_name"));
        r.setSpecialization(rs.getString("specialization"));
        r.setPhone(rs.getString("phone"));
        r.setDutyStatus(rs.getString("duty_status"));

        long locId = rs.getLong("assigned_location_id");
        if (!rs.wasNull()) r.setAssignedLocationId(locId);

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) r.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) r.setUpdatedAt(updatedAt.toLocalDateTime());

        return r;
    }
}
