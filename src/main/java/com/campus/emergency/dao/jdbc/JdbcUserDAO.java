package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.UserDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserDAO implements UserDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcUserDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone, role_id) " +
                "VALUES (?, ?, ?, ?, ?, (SELECT id FROM roles WHERE name = ?))";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getRole() != null ? user.getRole().name() : Role.STUDENT.name());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new DatabaseException("Error creating user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.phone, " +
                "u.created_at, u.updated_at, r.name AS role_name " +
                "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.phone, " +
                "u.created_at, u.updated_at, r.name AS role_name " +
                "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by username: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.phone, " +
                "u.created_at, u.updated_at, r.name AS role_name " +
                "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.email = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding user by email: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, u.phone, " +
                "u.created_at, u.updated_at, r.name AS role_name " +
                "FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.id ASC";

        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing users: " + e.getMessage(), e);
        }
        return users;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE users SET username = ?, password_hash = ?, full_name = ?, email = ?, phone = ?, " +
                "role_id = (SELECT id FROM roles WHERE name = ?) WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPasswordHash());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getPhone());
            stmt.setString(6, user.getRole() != null ? user.getRole().name() : Role.STUDENT.name());
            stmt.setInt(7, user.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting user: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));

        String roleStr = rs.getString("role_name");
        user.setRole(Role.fromString(roleStr));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return user;
    }
}
