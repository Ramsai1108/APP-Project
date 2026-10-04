package com.campus.emergency.dao.jdbc;

import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.exception.DatabaseException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.EmergencyReport;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcEmergencyReportDAO implements EmergencyReportDAO {
    private final ConnectionPoolManager connectionPoolManager;

    public JdbcEmergencyReportDAO(ConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
        ensureSchemaUpToDate();
    }

    private void ensureSchemaUpToDate() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN latitude DECIMAL(10, 8) NULL"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN longitude DECIMAL(11, 8) NULL"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN location_accuracy DOUBLE NULL"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN location_timestamp TIMESTAMP NULL"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN location_source VARCHAR(30) DEFAULT 'CAMPUS_MANUAL'"); } catch (SQLException ignored) {}
            try { stmt.executeUpdate("ALTER TABLE emergency_reports ADD COLUMN campus_location_id INT NULL"); } catch (SQLException ignored) {}
        } catch (Exception ignored) {}
    }

    private Connection getConnection() throws SQLException {
        return connectionPoolManager.getConnection();
    }

    public EmergencyReport create(EmergencyReport report, Connection conn) throws SQLException {
        String sql = "INSERT INTO emergency_reports (reporter_id, category, description, location_id, status, photo_path, video_path, " +
                "latitude, longitude, location_accuracy, location_timestamp, location_source, campus_location_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, report.getReporterId());
            stmt.setString(2, report.getCategory());
            stmt.setString(3, report.getDescription());
            stmt.setInt(4, report.getLocationId());
            stmt.setString(5, report.getStatus() != null ? report.getStatus() : "SUBMITTED");
            stmt.setString(6, report.getPhotoPath());
            stmt.setString(7, report.getVideoPath());

            if (report.getLatitude() != null) stmt.setDouble(8, report.getLatitude()); else stmt.setNull(8, Types.DECIMAL);
            if (report.getLongitude() != null) stmt.setDouble(9, report.getLongitude()); else stmt.setNull(9, Types.DECIMAL);
            if (report.getLocationAccuracy() != null) stmt.setDouble(10, report.getLocationAccuracy()); else stmt.setNull(10, Types.DOUBLE);
            if (report.getLocationTimestamp() != null) stmt.setTimestamp(11, Timestamp.valueOf(report.getLocationTimestamp())); else stmt.setNull(11, Types.TIMESTAMP);
            stmt.setString(12, report.getLocationSource() != null ? report.getLocationSource() : "CAMPUS_MANUAL");
            if (report.getCampusLocationId() != null) stmt.setInt(13, report.getCampusLocationId()); else stmt.setNull(13, Types.INTEGER);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating emergency report failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    report.setId(generatedKeys.getInt(1));
                }
            }
            return report;
        }
    }

    @Override
    public EmergencyReport create(EmergencyReport report) {
        try (Connection conn = getConnection()) {
            return create(report, conn);
        } catch (SQLException e) {
            throw new DatabaseException("Error creating emergency report: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EmergencyReport> findById(int id) {
        String sql = "SELECT id, reporter_id, category, description, location_id, status, photo_path, video_path, " +
                "latitude, longitude, location_accuracy, location_timestamp, location_source, campus_location_id, created_at, updated_at " +
                "FROM emergency_reports WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding emergency report by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<EmergencyReport> findByReporterId(int reporterId) {
        String sql = "SELECT id, reporter_id, category, description, location_id, status, photo_path, video_path, " +
                "latitude, longitude, location_accuracy, location_timestamp, location_source, campus_location_id, created_at, updated_at " +
                "FROM emergency_reports WHERE reporter_id = ? ORDER BY id DESC";

        List<EmergencyReport> reports = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reporterId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding reports by reporter ID: " + e.getMessage(), e);
        }
        return reports;
    }

    @Override
    public List<EmergencyReport> findAll() {
        String sql = "SELECT id, reporter_id, category, description, location_id, status, photo_path, video_path, " +
                "latitude, longitude, location_accuracy, location_timestamp, location_source, campus_location_id, created_at, updated_at " +
                "FROM emergency_reports ORDER BY id DESC";

        List<EmergencyReport> reports = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error listing emergency reports: " + e.getMessage(), e);
        }
        return reports;
    }

    @Override
    public boolean updateStatus(int id, String status) {
        String sql = "UPDATE emergency_reports SET status = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating report status: " + e.getMessage(), e);
        }
    }

    private EmergencyReport mapResultSetToReport(ResultSet rs) throws SQLException {
        EmergencyReport report = new EmergencyReport();
        report.setId(rs.getInt("id"));
        report.setReporterId(rs.getInt("reporter_id"));
        report.setCategory(rs.getString("category"));
        report.setDescription(rs.getString("description"));
        report.setLocationId(rs.getInt("location_id"));
        report.setStatus(rs.getString("status"));
        report.setPhotoPath(rs.getString("photo_path"));
        report.setVideoPath(rs.getString("video_path"));

        try {
            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) report.setLatitude(lat);
            double lon = rs.getDouble("longitude");
            if (!rs.wasNull()) report.setLongitude(lon);
            double acc = rs.getDouble("location_accuracy");
            if (!rs.wasNull()) report.setLocationAccuracy(acc);
            Timestamp ts = rs.getTimestamp("location_timestamp");
            if (ts != null) report.setLocationTimestamp(ts.toLocalDateTime());
            report.setLocationSource(rs.getString("location_source"));
            int campId = rs.getInt("campus_location_id");
            if (!rs.wasNull()) report.setCampusLocationId(campId);
        } catch (SQLException ignored) {}

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            report.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            report.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return report;
    }
}
