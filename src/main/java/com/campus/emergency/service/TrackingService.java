package com.campus.emergency.service;

import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthorizationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TrackingService {
    private final LocationDAO locationDAO;
    private final ResponderDAO responderDAO;
    private final ResourceDAO resourceDAO;
    private final EmergencyIncidentDAO incidentDAO;
    private final EmergencyReportDAO reportDAO;
    private final DatabaseManager databaseManager;
    private final NotificationBroker notificationBroker;
    private final SystemLogDAO logDAO;
    private final AuthorizationService authService;
    private final DataAggregator dataAggregator;

    public TrackingService(LocationDAO locationDAO, ResponderDAO responderDAO, ResourceDAO resourceDAO,
                           EmergencyIncidentDAO incidentDAO, EmergencyReportDAO reportDAO,
                           DatabaseManager databaseManager, NotificationBroker notificationBroker,
                           SystemLogDAO logDAO) {
        this(locationDAO, responderDAO, resourceDAO, incidentDAO, reportDAO, databaseManager,
                notificationBroker, logDAO, new AuthorizationService());
    }

    public TrackingService(LocationDAO locationDAO, ResponderDAO responderDAO, ResourceDAO resourceDAO,
                           EmergencyIncidentDAO incidentDAO, EmergencyReportDAO reportDAO,
                           DatabaseManager databaseManager, NotificationBroker notificationBroker,
                           SystemLogDAO logDAO, AuthorizationService authService) {
        this.locationDAO = locationDAO;
        this.responderDAO = responderDAO;
        this.resourceDAO = resourceDAO;
        this.incidentDAO = incidentDAO;
        this.reportDAO = reportDAO;
        this.databaseManager = databaseManager;
        this.notificationBroker = (notificationBroker != null) ? notificationBroker : new NotificationBroker();
        this.logDAO = logDAO;
        this.authService = (authService != null) ? authService : new AuthorizationService();
        this.dataAggregator = new DataAggregator(reportDAO, incidentDAO, responderDAO, resourceDAO);
    }

    // ==================================================
    // 1. LOCATION MANAGEMENT
    // ==================================================

    public Location createLocation(User operator, String buildingName, String floor, String roomNumber,
                                   double latitude, double longitude, String description) throws EmergencyProcessingException {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new EmergencyProcessingException("Invalid geographic coordinates: Latitude must be between -90.0 and 90.0");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new EmergencyProcessingException("Invalid geographic coordinates: Longitude must be between -180.0 and 180.0");
        }

        Location location = new Location(0, buildingName, floor, roomNumber, latitude, longitude, description);
        Location created = locationDAO.create(location);
        LoggingService.info(String.format("Location #%d created [%s] at coordinates (%f, %f)", created.getId(), buildingName, latitude, longitude));
        return created;
    }

    public Optional<Location> getLocation(int locationId) {
        return locationDAO.findById(locationId);
    }

    public boolean updateResponderLocation(User operator, int responderId, double latitude, double longitude,
                                           String buildingName, String floor, String roomNumber) throws EmergencyProcessingException {
        if (operator == null) {
            throw new EmergencyProcessingException("Unauthenticated user cannot update responder location");
        }
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new EmergencyProcessingException("Invalid geographic coordinates for responder location update");
        }

        Responder responder = responderDAO.findById(responderId)
                .orElseThrow(() -> new EmergencyProcessingException("Responder not found with ID: " + responderId));

        boolean isOwnProfile = (responder.getUserId() == operator.getId());
        boolean isOperationalManager = authService.isDispatcher(operator) || authService.isAdmin(operator);

        if (!isOwnProfile && !isOperationalManager) {
            throw new EmergencyProcessingException(String.format("Access denied: User '%s' is not authorized to update location for Responder #%d", operator.getUsername(), responderId));
        }

        Location newLocation = new Location(0, buildingName != null ? buildingName : "Updated GPS", floor, roomNumber, latitude, longitude, "Responder GPS Update");
        boolean success;

        if (databaseManager != null && locationDAO instanceof JdbcLocationDAO && responderDAO instanceof JdbcResponderDAO) {
            JdbcLocationDAO jdbcLocDAO = (JdbcLocationDAO) locationDAO;
            JdbcResponderDAO jdbcRespDAO = (JdbcResponderDAO) responderDAO;

            try {
                success = databaseManager.executeTransaction(conn -> {
                    Location loc = jdbcLocDAO.create(newLocation, conn);
                    return jdbcRespDAO.updateLocation(responderId, loc.getId(), conn);
                });
            } catch (Exception e) {
                throw new EmergencyProcessingException("Failed transaction for responder location update: " + e.getMessage(), e);
            }
        } else {
            Location loc = locationDAO.create(newLocation);
            success = responderDAO.updateLocation(responderId, loc.getId());
        }

        if (success) {
            LoggingService.info(String.format("Responder #%d location updated to (%f, %f) by User #%d", responderId, latitude, longitude, operator.getId()));
            if (logDAO != null) {
                logDAO.log("LOCATION_UPDATED", operator.getId(), String.format("Responder #%d location updated to (%f, %f)", responderId, latitude, longitude));
            }
            notificationBroker.notifyEvent(
                    NotificationBroker.EventType.LOCATION_UPDATED,
                    "DISPATCH_CENTER",
                    String.format("Responder #%d updated location to (%f, %f)", responderId, latitude, longitude),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return success;
    }

    public Optional<Location> getLatestResponderLocation(int responderId) {
        return responderDAO.findById(responderId)
                .filter(r -> r.getCurrentLocationId() != null)
                .flatMap(r -> locationDAO.findById(r.getCurrentLocationId()));
    }

    // ==================================================
    // 2. ACTIVE INCIDENT & TRACKING
    // ==================================================

    public List<EmergencyIncident> getActiveIncidents(User user) throws EmergencyProcessingException {
        if (user == null) {
            throw new EmergencyProcessingException("Authentication required to view active incidents");
        }

        List<EmergencyIncident> active = incidentDAO.findAllActive();

        if (user.getRole() == Role.STUDENT) {
            // Filter to student's own reports only
            List<EmergencyReport> myReports = reportDAO.findAll().stream()
                    .filter(r -> r.getReporterId() == user.getId())
                    .collect(Collectors.toList());
            List<Integer> myReportIds = myReports.stream().map(EmergencyReport::getId).collect(Collectors.toList());

            return active.stream()
                    .filter(inc -> myReportIds.contains(inc.getReportId()))
                    .collect(Collectors.toList());
        }

        return active;
    }

    public Optional<EmergencyIncident> getIncidentDetails(User user, int incidentId) throws EmergencyProcessingException {
        if (user == null) {
            throw new EmergencyProcessingException("Authentication required to view incident details");
        }

        EmergencyIncident incident = incidentDAO.findById(incidentId)
                .orElseThrow(() -> new EmergencyProcessingException("Incident not found with ID: " + incidentId));

        if (user.getRole() == Role.STUDENT) {
            Optional<EmergencyReport> reportOpt = reportDAO.findById(incident.getReportId());
            if (reportOpt.isEmpty() || reportOpt.get().getReporterId() != user.getId()) {
                throw new EmergencyProcessingException("Access denied: You are not authorized to view this incident.");
            }
        }

        if (logDAO != null) {
            logDAO.log("INCIDENT_VIEWED", user.getId(), "Viewed details for Incident #" + incidentId);
        }

        return Optional.of(incident);
    }

    public List<Responder> getResponderTrackingData(User operator) throws EmergencyProcessingException {
        validateOperationalAccess(operator, "view responder tracking data");
        return responderDAO.findAll();
    }

    public List<Resource> getResourceTrackingData(User operator) throws EmergencyProcessingException {
        validateOperationalAccess(operator, "view resource tracking data");
        return resourceDAO.findAll();
    }

    // ==================================================
    // 3. HISTORICAL DATA & FILTERING
    // ==================================================

    public List<EmergencyIncident> getHistoricalIncidents(User operator, String statusFilter, String priorityFilter, String categoryFilter) throws EmergencyProcessingException {
        if (operator == null) {
            throw new EmergencyProcessingException("Authentication required to view historical incidents");
        }

        List<EmergencyIncident> incidents = incidentDAO.findAll();

        if (operator.getRole() == Role.STUDENT) {
            List<Integer> myReportIds = reportDAO.findAll().stream()
                    .filter(r -> r.getReporterId() == operator.getId())
                    .map(EmergencyReport::getId)
                    .collect(Collectors.toList());
            incidents = incidents.stream()
                    .filter(inc -> myReportIds.contains(inc.getReportId()))
                    .collect(Collectors.toList());
        }

        if (statusFilter != null && !statusFilter.trim().isEmpty()) {
            incidents = incidents.stream()
                    .filter(inc -> statusFilter.equalsIgnoreCase(inc.getStatus()))
                    .collect(Collectors.toList());
        }

        if (priorityFilter != null && !priorityFilter.trim().isEmpty()) {
            incidents = incidents.stream()
                    .filter(inc -> priorityFilter.equalsIgnoreCase(inc.getPriority()))
                    .collect(Collectors.toList());
        }

        if (categoryFilter != null && !categoryFilter.trim().isEmpty()) {
            incidents = incidents.stream()
                    .filter(inc -> {
                        Optional<EmergencyReport> rep = reportDAO.findById(inc.getReportId());
                        return rep.isPresent() && categoryFilter.equalsIgnoreCase(rep.get().getCategory());
                    })
                    .collect(Collectors.toList());
        }

        return incidents;
    }

    public List<EmergencyIncident> getHistoricalIncidentsByDateRange(User operator, LocalDateTime start, LocalDateTime end) throws EmergencyProcessingException {
        if (operator == null) {
            throw new EmergencyProcessingException("Authentication required");
        }

        List<EmergencyIncident> incidents = getHistoricalIncidents(operator, null, null, null);

        return incidents.stream()
                .filter(inc -> {
                    LocalDateTime created = inc.getCreatedAt();
                    if (created == null) return false;
                    boolean afterStart = (start == null || !created.isBefore(start));
                    boolean beforeEnd = (end == null || !created.isAfter(end));
                    return afterStart && beforeEnd;
                })
                .collect(Collectors.toList());
    }

    // ==================================================
    // 4. OPERATIONAL DASHBOARD DATA & NOTIFICATIONS
    // ==================================================

    public DataAggregator getDashboardData(User operator) throws EmergencyProcessingException {
        validateOperationalAccess(operator, "access operational dashboard data");
        return dataAggregator;
    }

    public List<NotificationBroker.NotificationMessage> getNotifications(User operator) throws EmergencyProcessingException {
        return notificationBroker.getNotificationsForUser(operator);
    }

    private void validateOperationalAccess(User operator, String action) throws EmergencyProcessingException {
        if (operator == null) {
            throw new EmergencyProcessingException("Authentication required to " + action);
        }
        if (operator.getRole() == Role.STUDENT) {
            throw new EmergencyProcessingException(String.format("Access denied: Role '%s' cannot %s", operator.getRole(), action));
        }
    }
}
