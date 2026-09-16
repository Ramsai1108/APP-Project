package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IEmergencyReportDao;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.EmergencyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of IEmergencyReportDao.
 */
public class EmergencyReportDaoImpl extends AbstractJDBCDao<EmergencyReport, Long> implements IEmergencyReportDao {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyReportDaoImpl.class);

    private static final String INSERT_REPORT = 
        "INSERT INTO emergency_reports (reporter_id, title, description, category, severity, location, location_id, affected_people, media_path, status, reported_at) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_REPORT_BY_ID = 
        "SELECT id, reporter_id, title, description, category, severity, location, location_id, affected_people, media_path, status, reported_at " +
        "FROM emergency_reports WHERE id = ?";

    private static final String SELECT_REPORTS_BY_REPORTER = 
        "SELECT id, reporter_id, title, description, category, severity, location, location_id, affected_people, media_path, status, reported_at " +
        "FROM emergency_reports WHERE reporter_id = ? ORDER BY reported_at DESC";

    private static final String COUNT_TOTAL_BY_REPORTER = 
        "SELECT COUNT(*) FROM emergency_reports WHERE reporter_id = ?";

    private static final String COUNT_OPEN_BY_REPORTER = 
        "SELECT COUNT(*) FROM emergency_reports WHERE reporter_id = ? AND status IN ('SUBMITTED', 'OPEN', 'TRIAGED', 'IN_PROGRESS')";

    private static final String COUNT_RESOLVED_BY_REPORTER = 
        "SELECT COUNT(*) FROM emergency_reports WHERE reporter_id = ? AND status IN ('RESOLVED', 'CLOSED')";

    @Override
    public EmergencyReport create(EmergencyReport report) {
        try (Connection conn = getConnection()) {
            return createWithConnection(report, conn);
        } catch (SQLException e) {
            logger.error("Database error while creating emergency report", e);
            throw new DatabaseAccessException("Failed to save emergency report to database", e);
        }
    }

    public EmergencyReport createWithConnection(EmergencyReport report, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_REPORT, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, report.getReporterId());
            stmt.setString(2, report.getTitle());
            stmt.setString(3, report.getDescription());
            stmt.setString(4, report.getCategory());
            stmt.setInt(5, report.getSeverity());
            stmt.setString(6, report.getLocation());

            if (report.getLocationId() != null) {
                stmt.setLong(7, report.getLocationId());
            } else {
                stmt.setNull(7, Types.BIGINT);
            }

            stmt.setInt(8, report.getAffectedPeople());
            stmt.setString(9, report.getMediaPath());
            stmt.setString(10, report.getStatus() != null ? report.getStatus() : "SUBMITTED");

            Timestamp reportedAt = report.getReportedAt() != null ? 
                Timestamp.valueOf(report.getReportedAt()) : new Timestamp(System.currentTimeMillis());
            stmt.setTimestamp(11, reportedAt);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating emergency report failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    report.setId(generatedKeys.getLong(1));
                }
            }

            logger.info("Emergency report created successfully with ID {}", report.getId());
            return report;
        }
    }

    @Override
    public Optional<EmergencyReport> findById(Long id) {
        if (id == null) return Optional.empty();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_REPORT_BY_ID)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching emergency report by ID: {}", id, e);
            throw new DatabaseAccessException("Failed to query emergency report by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public List<EmergencyReport> findByReporterId(Long reporterId) {
        List<EmergencyReport> reports = new ArrayList<>();
        if (reporterId == null) return reports;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_REPORTS_BY_REPORTER)) {

            stmt.setLong(1, reporterId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(mapResultSetToReport(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Database error while fetching reports for reporter ID: {}", reporterId, e);
            throw new DatabaseAccessException("Failed to query user emergency reports", e);
        }

        return reports;
    }

    @Override
    public List<EmergencyReport> findAll() {
        List<EmergencyReport> reports = new ArrayList<>();
        String sql = "SELECT id, reporter_id, title, description, category, severity, location, location_id, affected_people, media_path, status, reported_at FROM emergency_reports ORDER BY reported_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reports.add(mapResultSetToReport(rs));
            }
        } catch (SQLException e) {
            logger.error("Database error while retrieving all emergency reports", e);
            throw new DatabaseAccessException("Failed to query all emergency reports", e);
        }
        return reports;
    }

    @Override
    public int countTotalByReporterId(Long reporterId) {
        return executeCountQuery(COUNT_TOTAL_BY_REPORTER, reporterId);
    }

    @Override
    public int countOpenByReporterId(Long reporterId) {
        return executeCountQuery(COUNT_OPEN_BY_REPORTER, reporterId);
    }

    @Override
    public int countResolvedByReporterId(Long reporterId) {
        return executeCountQuery(COUNT_RESOLVED_BY_REPORTER, reporterId);
    }

    @Override
    public boolean update(EmergencyReport report) {
        String sql = "UPDATE emergency_reports SET title = ?, description = ?, category = ?, severity = ?, location = ?, affected_people = ?, media_path = ?, status = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, report.getTitle());
            stmt.setString(2, report.getDescription());
            stmt.setString(3, report.getCategory());
            stmt.setInt(4, report.getSeverity());
            stmt.setString(5, report.getLocation());
            stmt.setInt(6, report.getAffectedPeople());
            stmt.setString(7, report.getMediaPath());
            stmt.setString(8, report.getStatus());
            stmt.setLong(9, report.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error while updating emergency report ID: {}", report.getId(), e);
            throw new DatabaseAccessException("Failed to update emergency report", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM emergency_reports WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Database error while deleting report ID: {}", id, e);
            throw new DatabaseAccessException("Failed to delete emergency report", e);
        }
    }

    private int executeCountQuery(String sql, Long reporterId) {
        if (reporterId == null) return 0;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, reporterId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error executing report count query for reporter ID {}", reporterId, e);
            throw new DatabaseAccessException("Failed to compute report metrics", e);
        }
        return 0;
    }

    private EmergencyReport mapResultSetToReport(ResultSet rs) throws SQLException {
        EmergencyReport report = new EmergencyReport();
        report.setId(rs.getLong("id"));
        report.setReporterId(rs.getLong("reporter_id"));
        report.setTitle(rs.getString("title"));
        report.setDescription(rs.getString("description"));
        report.setCategory(rs.getString("category"));
        report.setSeverity(rs.getInt("severity"));
        report.setLocation(rs.getString("location"));

        long locId = rs.getLong("location_id");
        if (!rs.wasNull()) {
            report.setLocationId(locId);
        }

        report.setAffectedPeople(rs.getInt("affected_people"));
        report.setMediaPath(rs.getString("media_path"));
        report.setStatus(rs.getString("status"));

        Timestamp reportedAt = rs.getTimestamp("reported_at");
        if (reportedAt != null) {
            report.setReportedAt(reportedAt.toLocalDateTime());
        }

        return report;
    }
}
