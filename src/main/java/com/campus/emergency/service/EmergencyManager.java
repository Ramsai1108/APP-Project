package com.campus.emergency.service;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.dao.SystemLogDAO;
import com.campus.emergency.dao.jdbc.JdbcEmergencyIncidentDAO;
import com.campus.emergency.dao.jdbc.JdbcEmergencyReportDAO;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.User;
import com.campus.emergency.util.ValidationUtils;

import java.util.Arrays;
import java.util.List;

public class EmergencyManager {
    private final EmergencyReportDAO reportDAO;
    private final EmergencyIncidentDAO incidentDAO;
    private final PriorityEngine priorityEngine;
    private final DatabaseManager databaseManager;
    private final NotificationBroker notificationBroker;
    private final SystemLogDAO logDAO;

    public EmergencyManager(EmergencyReportDAO reportDAO, EmergencyIncidentDAO incidentDAO, PriorityEngine priorityEngine) {
        this(reportDAO, incidentDAO, priorityEngine, null, new NotificationBroker(), null);
    }

    public EmergencyManager(EmergencyReportDAO reportDAO, EmergencyIncidentDAO incidentDAO,
                            PriorityEngine priorityEngine, DatabaseManager databaseManager,
                            NotificationBroker notificationBroker, SystemLogDAO logDAO) {
        this.reportDAO = reportDAO;
        this.incidentDAO = incidentDAO;
        this.priorityEngine = priorityEngine;
        this.databaseManager = databaseManager;
        this.notificationBroker = (notificationBroker != null) ? notificationBroker : new NotificationBroker();
        this.logDAO = logDAO;
    }

    public EmergencyReport createReport(User reporter, String category, String description, int locationId, int peopleAffected, String photoPath, String videoPath) throws EmergencyProcessingException {
        if (reporter == null || reporter.getId() <= 0) {
            throw new EmergencyProcessingException("Unauthenticated report submission rejected");
        }
        if (!ValidationUtils.isNotNullOrEmpty(category)) {
            throw new EmergencyProcessingException("Emergency category is required");
        }
        if (!ValidationUtils.isNotNullOrEmpty(description) || description.trim().length() < 5) {
            throw new EmergencyProcessingException("Detailed description is required");
        }
        if (locationId <= 0) {
            throw new EmergencyProcessingException("Valid emergency location is required");
        }

        String priority = priorityEngine.calculatePriority(category, description, peopleAffected);

        EmergencyReport report = new EmergencyReport(0, reporter.getId(), category.trim(), description.trim(), locationId, "SUBMITTED");
        report.setPhotoPath(photoPath);
        report.setVideoPath(videoPath);

        EmergencyReport createdReport;

        if (databaseManager != null && reportDAO instanceof JdbcEmergencyReportDAO && incidentDAO instanceof JdbcEmergencyIncidentDAO) {
            JdbcEmergencyReportDAO jdbcReportDAO = (JdbcEmergencyReportDAO) reportDAO;
            JdbcEmergencyIncidentDAO jdbcIncidentDAO = (JdbcEmergencyIncidentDAO) incidentDAO;

            try {
                createdReport = databaseManager.executeTransaction(conn -> {
                    EmergencyReport rep = jdbcReportDAO.create(report, conn);
                    EmergencyIncident incident = new EmergencyIncident(0, rep.getId(), priority, "OPEN", null);
                    jdbcIncidentDAO.create(incident, conn);
                    return rep;
                });
            } catch (Exception e) {
                throw new EmergencyProcessingException("Failed to process emergency report transaction: " + e.getMessage(), e);
            }
        } else {
            createdReport = reportDAO.create(report);
            try {
                EmergencyIncident incident = new EmergencyIncident(0, createdReport.getId(), priority, "OPEN", null);
                incidentDAO.create(incident);
            } catch (Exception e) {
                reportDAO.updateStatus(createdReport.getId(), "CANCELLED");
                throw new EmergencyProcessingException("Failed to create emergency incident from report: " + e.getMessage(), e);
            }
        }

        LoggingService.info(String.format("Emergency report #%d created with priority: %s for location #%d", createdReport.getId(), priority, locationId));
        if (logDAO != null) {
            logDAO.log("EMERGENCY_REPORT_CREATED", reporter.getId(), String.format("Report #%d, Category: %s, Priority: %s", createdReport.getId(), category, priority));
        }

        notificationBroker.notifyEvent(
                NotificationBroker.EventType.EMERGENCY_REPORTED,
                "DISPATCH_CENTER",
                String.format("Emergency Report #%d Created [%s] - Priority: %s", createdReport.getId(), category, priority),
                NotificationBroker.NotificationType.IN_APP
        );

        return createdReport;
    }

    public EmergencyReport createReport(int reporterId, String category, String description, int locationId) throws EmergencyProcessingException {
        User reporter = new User();
        reporter.setId(reporterId);
        return createReport(reporter, category, description, locationId, 1, null, null);
    }

    public boolean updateIncidentStatus(int incidentId, String newStatus) throws EmergencyProcessingException {
        if (!ValidationUtils.isNotNullOrEmpty(newStatus)) {
            throw new EmergencyProcessingException("New incident status cannot be null or empty");
        }

        EmergencyIncident incident = incidentDAO.findById(incidentId)
                .orElseThrow(() -> new EmergencyProcessingException("Incident not found: " + incidentId));

        String currentStatus = incident.getStatus() != null ? incident.getStatus().toUpperCase() : "OPEN";
        String targetStatus = newStatus.trim().toUpperCase();

        if (currentStatus.equals(targetStatus)) {
            return true;
        }

        if (!isValidStatusTransition(currentStatus, targetStatus)) {
            throw new EmergencyProcessingException(String.format("Invalid status transition from %s to %s", currentStatus, targetStatus));
        }

        boolean updated = incidentDAO.updateStatus(incidentId, targetStatus);
        if (updated) {
            LoggingService.info(String.format("Incident #%d status updated from %s to %s", incidentId, currentStatus, targetStatus));
            if (logDAO != null) {
                logDAO.log("STATUS_CHANGED", null, String.format("Incident #%d status changed: %s -> %s", incidentId, currentStatus, targetStatus));
            }

            NotificationBroker.EventType eventType = targetStatus.equals("RESOLVED") ?
                    NotificationBroker.EventType.INCIDENT_RESOLVED : NotificationBroker.EventType.STATUS_CHANGED;

            notificationBroker.notifyEvent(
                    eventType,
                    "ALL_STAKEHOLDERS",
                    String.format("Incident #%d Status Updated: %s", incidentId, targetStatus),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return updated;
    }

    public boolean isValidStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        String cur = currentStatus.trim().toUpperCase();
        String nxt = newStatus.trim().toUpperCase();

        if (cur.equals(nxt)) return true;

        switch (cur) {
            case "OPEN":
                return Arrays.asList("ACKNOWLEDGED", "IN_PROGRESS", "CLOSED", "RESOLVED").contains(nxt);
            case "ACKNOWLEDGED":
                return Arrays.asList("IN_PROGRESS", "CLOSED", "RESOLVED").contains(nxt);
            case "IN_PROGRESS":
                return Arrays.asList("RESOLVED", "CLOSED").contains(nxt);
            case "RESOLVED":
                return Arrays.asList("CLOSED").contains(nxt);
            case "CLOSED":
                return false; // Terminal state
            default:
                return false;
        }
    }
}
