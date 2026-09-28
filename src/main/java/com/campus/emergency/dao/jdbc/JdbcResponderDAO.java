package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.ResponderDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.Responder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcResponderDAO implements ResponderDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcResponderDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    public Responder create(Responder responder, Connection conn) throws SQLException {
        String sql = "INSERT INTO responders (user_id, badge_number, specialization, status, current_location_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, responder.getUserId());
            stmt.setString(2, responder.getBadgeNumber());
            stmt.setString(3, responder.getSpecialization());
            stmt.setString(4, responder.getStatus() != null ? responder.getStatus() : "AVAILABLE");
            if (responder.getCurrentLocationId() != null) {
                stmt.setInt(5, responder.getCurrentLocationId());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating responder failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    responder.setId(generatedKeys.getInt(1));
                }
            }
            return responder;
        }
    }

    @Override
    public Responder create(Responder responder) {
        try (Connection conn = getConnection()) {
            return create(responder, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating responder: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Responder> findById(int id) {
        String sql = "SELECT id, user_id, badge_number, specialization, status, current_location_id FROM responders WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResponder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding responder by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Responder> findByUserId(int userId) {
        String sql = "SELECT id, user_id, badge_number, specialization, status, current_location_id FROM responders WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToResponder(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding responder by user ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Responder> findAvailable() {
        String sql = "SELECT id, user_id, badge_number, specialization, status, current_location_id FROM responders WHERE status = 'AVAILABLE' ORDER BY id ASC";

        List<Responder> responders = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                responders.add(mapResultSetToResponder(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing available responders: " + e.getMessage(), e);
        }
        return responders;
    }

    @Override
    public List<Responder> findAll() {
        String sql = "SELECT id, user_id, badge_number, specialization, status, current_location_id FROM responders ORDER BY id ASC";

        List<Responder> responders = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                responders.add(mapResultSetToResponder(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing all responders: " + e.getMessage(), e);
        }
        return responders;
    }

    public boolean updateStatus(int id, String status, Connection conn) throws SQLException {
        String sql = "UPDATE responders SET status = ? WHERE id = ?";

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
            throw new DatabaseException("Error updating responder status: " + e.getMessage(), e);
        }
    }

    public boolean updateLocation(int id, int locationId, Connection conn) throws SQLException {
        String sql = "UPDATE responders SET current_location_id = ? WHERE id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, locationId);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateLocation(int id, int locationId) {
        try (Connection conn = getConnection()) {
            return updateLocation(id, locationId, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error updating responder location: " + e.getMessage(), e);
        }
    }

    private Responder mapResultSetToResponder(ResultSet rs) throws SQLException {
        Responder responder = new Responder();
        responder.setId(rs.getInt("id"));
        responder.setUserId(rs.getInt("user_id"));
        responder.setBadgeNumber(rs.getString("badge_number"));
        responder.setSpecialization(rs.getString("specialization"));
        responder.setStatus(rs.getString("status"));

        int locId = rs.getInt("current_location_id");
        if (!rs.wasNull()) {
            responder.setCurrentLocationId(locId);
        } else {
            responder.setCurrentLocationId(null);
        }
        return responder;
    }
}
