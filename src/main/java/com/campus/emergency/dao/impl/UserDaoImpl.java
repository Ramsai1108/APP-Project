package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IUserDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IUserDao for MySQL database operations.
 */
public class UserDaoImpl extends AbstractJDBCDao<User, Long> implements IUserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    private static final String SELECT_USER_WITH_ROLE_BY_EMAIL = 
        "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.role_id, u.is_active, " +
        "u.created_at, u.updated_at, r.role_name, r.description AS role_desc " +
        "FROM users u " +
        "JOIN roles r ON u.role_id = r.id " +
        "WHERE u.email = ?";

    private static final String SELECT_USER_WITH_ROLE_BY_ID = 
        "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.role_id, u.is_active, " +
        "u.created_at, u.updated_at, r.role_name, r.description AS role_desc " +
        "FROM users u " +
        "JOIN roles r ON u.role_id = r.id " +
        "WHERE u.id = ?";

    private static final String SELECT_ALL_USERS_WITH_ROLES = 
        "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.role_id, u.is_active, " +
        "u.created_at, u.updated_at, r.role_name, r.description AS role_desc " +
        "FROM users u " +
        "JOIN roles r ON u.role_id = r.id";

    private static final String INSERT_USER = 
        "INSERT INTO users (username, email, password_hash, full_name, phone, role_id, is_active) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    @Override
    public Optional<User> findByEmailWithRole(String email) {
        if (email == null || email.trim().isEmpty()) {
            return Optional.empty();
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USER_WITH_ROLE_BY_EMAIL)) {

            stmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by email: {}", email, e);
            throw new DatabaseAccessException("Failed to query user by email from database", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findByEmailWithRole(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }

        String sql = "SELECT u.id, u.username, u.email, u.password_hash, u.full_name, u.phone, u.role_id, u.is_active, " +
                     "u.created_at, u.updated_at, r.role_name, r.description AS role_desc " +
                     "FROM users u JOIN roles r ON u.role_id = r.id WHERE u.username = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by username: {}", username, e);
            throw new DatabaseAccessException("Failed to query user by username from database", e);
        }

        return Optional.empty();
    }

    @Override
    public User create(User user) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getPhone());
            stmt.setLong(6, user.getRoleId());
            stmt.setBoolean(7, user.isActive());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseAccessException("Creating user failed, no rows affected.", null);
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                }
            }

            return user;
        } catch (SQLException e) {
            logger.error("Database error while creating user: {}", user.getUsername(), e);
            throw new DatabaseAccessException("Failed to insert user into database", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USER_WITH_ROLE_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while finding user by ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query user by ID from database", e);
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_USERS_WITH_ROLES);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while retrieving all users", e);
            throw new DatabaseAccessException("Failed to query users from database", e);
        }
        return users;
    }

    @Override
    public boolean update(User entity) {
        String sql = "UPDATE users SET username = ?, email = ?, full_name = ?, phone = ?, role_id = ?, is_active = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, entity.getUsername());
            stmt.setString(2, entity.getEmail());
            stmt.setString(3, entity.getFullName());
            stmt.setString(4, entity.getPhone());
            stmt.setLong(5, entity.getRoleId());
            stmt.setBoolean(6, entity.isActive());
            stmt.setLong(7, entity.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error while updating user ID: {}", entity.getId(), e);
            throw new DatabaseAccessException("Failed to update user in database", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error while deleting user ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete user from database", e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setRoleId(rs.getLong("role_id"));
        user.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        Role role = new Role();
        role.setId(rs.getLong("role_id"));
        role.setRoleName(rs.getString("role_name"));
        role.setDescription(rs.getString("role_desc"));
        user.setRole(role);

        return user;
    }
}
