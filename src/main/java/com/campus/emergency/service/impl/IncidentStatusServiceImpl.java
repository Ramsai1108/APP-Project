package com.campus.emergency.service.impl;

import com.campus.emergency.dao.IEmergencyReportDao;
import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.dao.IIncidentTimelineDao;
import com.campus.emergency.dao.impl.EmergencyReportDaoImpl;
import com.campus.emergency.dao.impl.IncidentDaoImpl;
import com.campus.emergency.dao.impl.IncidentTimelineDaoImpl;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.logging.AuditLogger;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.IncidentTimeline;
import com.campus.emergency.service.IncidentStatusService;
import com.campus.emergency.service.NotificationBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Implementation of IncidentStatusService enforcing incident lifecycle state machine,
 * event timeline logging, audit trails, and multi-channel notifications.
 */
public class IncidentStatusServiceImpl implements IncidentStatusService {

    private static final Logger logger = LoggerFactory.getLogger(IncidentStatusServiceImpl.class);

    private final IIncidentDao incidentDao;
    private final IEmergencyReportDao reportDao;
    private final IIncidentTimelineDao timelineDao;
    private final NotificationBroker notificationBroker;

    public IncidentStatusServiceImpl(IIncidentDao incidentDao,
                                     IEmergencyReportDao reportDao,
                                     IIncidentTimelineDao timelineDao,
                                     NotificationBroker notificationBroker) {
        this.incidentDao = incidentDao;
        this.reportDao = reportDao;
        this.timelineDao = timelineDao;
        this.notificationBroker = notificationBroker;
    }

    public IncidentStatusServiceImpl() {
        this(new IncidentDaoImpl(), new EmergencyReportDaoImpl(), new IncidentTimelineDaoImpl(), new NotificationBrokerImpl());
    }

    @Override
    public boolean isValidTransition(String currentStatus, String targetStatus) {
        if (currentStatus == null || targetStatus == null) return false;
        String cur = currentStatus.trim().toUpperCase();
        String target = targetStatus.trim().toUpperCase();

        if (cur.equals(target)) return true;

        return switch (cur) {
            case "SUBMITTED", "REPORTED" -> target.equals("OPEN") || target.equals("TRIAGED") || target.equals("CANCELLED") || target.equals("DISPATCHED");
            case "OPEN", "TRIAGED" -> target.equals("PRIORITIZED") || target.equals("DISPATCHED") || target.equals("CANCELLED");
            case "PRIORITIZED" -> target.equals("DISPATCHED") || target.equals("CANCELLED");
            case "DISPATCHED" -> target.equals("IN_PROGRESS") || target.equals("RESOLVED") || target.equals("CANCELLED");
            case "IN_PROGRESS" -> target.equals("RESOLVED") || target.equals("ESCALATED") || target.equals("CANCELLED");
            case "RESOLVED" -> target.equals("CLOSED");
            case "CLOSED", "CANCELLED" -> false; // Closed and cancelled incidents cannot be transitioned without admin override
            default -> false;
        };
    }

    @Override
    public EmergencyIncident updateIncidentStatus(Long incidentId, String targetStatus, Long actingUserId, String reason) {
        if (incidentId == null || targetStatus == null || actingUserId == null) {
            throw new IllegalArgumentException("Incident ID, target status, and acting user ID are required.");
        }

        EmergencyIncident incident = incidentDao.findById(incidentId)
                .orElseThrow(() -> new CampusEmergencyException("Emergency incident not found with ID #" + incidentId));

        String currentStatus = incident.getStatus() != null ? incident.getStatus() : "REPORTED";
        String cleanTargetStatus = targetStatus.trim().toUpperCase();

        if (!isValidTransition(currentStatus, cleanTargetStatus)) {
            logger.warn("Invalid incident status transition attempted for incident #{}: {} -> {}", incidentId, currentStatus, cleanTargetStatus);
            throw new CampusEmergencyException(String.format("Invalid status transition from %s to %s for incident #%d.",
                    currentStatus, cleanTargetStatus, incidentId));
        }

        // Update timestamps according to status lifecycle
        incident.setStatus(cleanTargetStatus);
        LocalDateTime now = LocalDateTime.now();

        if ("DISPATCHED".equals(cleanTargetStatus) || "TRIAGED".equals(cleanTargetStatus)) {
            if (incident.getTriagedAt() == null) incident.setTriagedAt(now);
            if (incident.getTriagedByUserId() == null) incident.setTriagedByUserId(actingUserId);
        } else if ("RESOLVED".equals(cleanTargetStatus)) {
            incident.setResolvedAt(now);
        } else if ("CLOSED".equals(cleanTargetStatus)) {
            incident.setClosedAt(now);
        }

        incident.setUpdatedAt(now);
        boolean updated = incidentDao.update(incident);
        if (!updated) {
            throw new CampusEmergencyException("Failed to update incident status in database.");
        }

        // Update corresponding EmergencyReport status if linked
        Long reporterId = null;
        if (incident.getReportId() != null) {
            try {
                reportDao.findById(incident.getReportId()).ifPresent(report -> {
                    report.setStatus(cleanTargetStatus);
                    reportDao.update(report);
                });
            } catch (Exception ex) {
                logger.warn("Non-fatal: could not synchronize emergency report status: {}", ex.getMessage());
            }
            reporterId = reportDao.findById(incident.getReportId()).map(EmergencyReport::getReporterId).orElse(null);
        }

        // Log Event in Incident Timeline
        String actionType = "STATUS_CHANGED_" + cleanTargetStatus;
        String desc = (reason != null && !reason.trim().isEmpty())
                ? "Status updated from " + currentStatus + " to " + cleanTargetStatus + ". Reason: " + reason
                : "Status updated from " + currentStatus + " to " + cleanTargetStatus;

        try {
            timelineDao.create(new IncidentTimeline(incidentId, actingUserId, actionType, desc));
        } catch (Exception ex) {
            logger.warn("Non-fatal: failed to create timeline entry for incident #{}: {}", incidentId, ex.getMessage());
        }

        // Audit Logger
        AuditLogger.log(actingUserId, "UPDATE_INCIDENT_STATUS", "EMERGENCY_INCIDENT",
                "Incident ID: #" + incidentId + " | Transition: " + currentStatus + " -> " + cleanTargetStatus);

        // Notify Broker
        try {
            notificationBroker.notifyIncidentStatusChanged(incidentId, reporterId, null, currentStatus, cleanTargetStatus);
        } catch (Exception ex) {
            logger.warn("Non-fatal: notification dispatch failed during status transition: {}", ex.getMessage());
        }

        logger.info("Incident #{} status updated successfully to {}", incidentId, cleanTargetStatus);
        return incident;
    }
}
