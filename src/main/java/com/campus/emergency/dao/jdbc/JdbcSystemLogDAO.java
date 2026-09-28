package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.SystemLogDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.SystemLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcSystemLogDAO implements SystemLogDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcSystemLogDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    @Override
    public void log(String eventType, Integer userId, String details) {
        String sql = "INSERT INTO system_logs (event_type, user_id, details) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, eventType);
            if (userId != null) {
                stmt.setInt(2, userId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, details);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error creating system log: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SystemLog> findAll() {
        String sql = "SELECT id, event_type, user_id, details, created_at FROM system_logs ORDER BY id DESC";

        List<SystemLog> logs = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                logs.add(mapResultSetToSystemLog(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing system logs: " + e.getMessage(), e);
        }
        return logs;
    }

    @Override
    public List<SystemLog> findByUserId(int userId) {
        String sql = "SELECT id, event_type, user_id, details, created_at FROM system_logs WHERE user_id = ? ORDER BY id DESC";

        List<SystemLog> logs = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToSystemLog(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding system logs by user ID: " + e.getMessage(), e);
        }
        return logs;
    }

    private SystemLog mapResultSetToSystemLog(ResultSet rs) throws SQLException {
        SystemLog log = new SystemLog();
        log.setId(rs.getInt("id"));
        log.setEventType(rs.getString("event_type"));

        int uId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            log.setUserId(uId);
        } else {
            log.setUserId(null);
        }
        log.setDetails(rs.getString("details"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            log.setCreatedAt(createdAt.toLocalDateTime());
        }
        return log;
    }
}
