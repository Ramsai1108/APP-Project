package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.LocationDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.Location;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcLocationDAO implements LocationDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcLocationDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    public Location create(Location location, Connection conn) throws SQLException {
        String sql = "INSERT INTO locations (building_name, floor, room_number, latitude, longitude, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, location.getBuildingName());
            stmt.setString(2, location.getFloor());
            stmt.setString(3, location.getRoomNumber());
            stmt.setDouble(4, location.getLatitude());
            stmt.setDouble(5, location.getLongitude());
            stmt.setString(6, location.getDescription());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating location failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    location.setId(generatedKeys.getInt(1));
                }
            }
            return location;
        }
    }

    @Override
    public Location create(Location location) {
        try (Connection conn = getConnection()) {
            return create(location, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating location: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Location> findById(int id) {
        String sql = "SELECT id, building_name, floor, room_number, latitude, longitude, description, created_at " +
                "FROM locations WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLocation(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding location by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Location> findAll() {
        String sql = "SELECT id, building_name, floor, room_number, latitude, longitude, description, created_at " +
                "FROM locations ORDER BY id ASC";

        List<Location> locations = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                locations.add(mapResultSetToLocation(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing locations: " + e.getMessage(), e);
        }
        return locations;
    }

    private Location mapResultSetToLocation(ResultSet rs) throws SQLException {
        Location loc = new Location();
        loc.setId(rs.getInt("id"));
        loc.setBuildingName(rs.getString("building_name"));
        loc.setFloor(rs.getString("floor"));
        loc.setRoomNumber(rs.getString("room_number"));
        loc.setLatitude(rs.getDouble("latitude"));
        loc.setLongitude(rs.getDouble("longitude"));
        loc.setDescription(rs.getString("description"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            loc.setCreatedAt(createdAt.toLocalDateTime());
        }
        return loc;
    }
}
