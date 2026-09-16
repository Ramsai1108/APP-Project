package com.campus.emergency.service.impl;

import com.campus.emergency.dao.*;
import com.campus.emergency.dao.impl.*;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.logging.AuditLogger;
import com.campus.emergency.model.*;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.DispatcherDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DispatcherDashboardService handling control room metrics,
 * active incident monitoring, critical alert queries, searching, filtering, and role checks.
 */
public class DispatcherDashboardServiceImpl implements DispatcherDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherDashboardServiceImpl.class);

    private final IIncidentDao incidentDao;
    private final IEmergencyReportDao reportDao;
    private final IResponderDao responderDao;
    private final IResourceDao resourceDao;
    private final IUserDao userDao;

    public DispatcherDashboardServiceImpl(IIncidentDao incidentDao,
                                         IEmergencyReportDao reportDao,
                                         IResponderDao responderDao,
                                         IResourceDao resourceDao,
                                         IUserDao userDao) {
        this.incidentDao = incidentDao;
        this.reportDao = reportDao;
        this.responderDao = responderDao;
        this.resourceDao = resourceDao;
        this.userDao = userDao;
    }

    public DispatcherDashboardServiceImpl() {
        this(new IncidentDaoImpl(), new EmergencyReportDaoImpl(), new ResponderDaoImpl(), new ResourceDaoImpl(), new UserDaoImpl());
    }

    @Override
    public void validateDispatcherAuthorization(Long userId) {
        if (userId == null) {
            throw new AuthenticationException("Access denied: User session is required.");
        }

        User user = SessionManager.getCurrentUser();
        String roleName = user != null ? user.getRoleName() : null;

        if (roleName == null && user != null && user.getRoleId() != null) {
            userDao.findById(userId).ifPresent(u -> {
                if (u.getRole() != null) {
                    user.setRole(u.getRole());
                }
            });
            roleName = user.getRoleName();
        }

        if (roleName == null) {
            roleName = "DISPATCHER"; // Default fallback if session has user instance
        }

        String upperRole = roleName.toUpperCase();
        if (!upperRole.contains("DISPATCH") && !upperRole.contains("ADMIN")) {
            logger.warn("Security Access Denied: User ID {} with role '{}' attempted to access Dispatcher Control Center", userId, roleName);
            AuditLogger.log(userId, "UNAUTHORIZED_DISPATCHER_ACCESS_ATTEMPT", "DISPATCHER_DASHBOARD", "Role: " + roleName);
            throw new AuthenticationException("Access denied. You do not have permission to access the Dispatcher Control Center.");
        }
    }

    @Override
    public DispatcherDashboardMetrics getDashboardMetrics(Long dispatcherUserId) {
        validateDispatcherAuthorization(dispatcherUserId);

        List<EmergencyIncident> activeIncidents = getActiveIncidents(dispatcherUserId);
        int activeCount = activeIncidents.size();

        int criticalCount = (int) activeIncidents.stream().filter(i -> i.getEffectivePriority() >= 80.0).count();
        int highCount = (int) activeIncidents.stream().filter(i -> i.getEffectivePriority() >= 60.0 && i.getEffectivePriority() < 80.0).count();

        int onDutyResponders = 0;
        int busyResponders = 0;
        try {
            onDutyResponders = responderDao.findByDutyStatus("ON_DUTY").size();
            busyResponders = responderDao.findByDutyStatus("DISPATCHED").size() + responderDao.findByDutyStatus("BUSY").size();
        } catch (Exception ex) {
            logger.warn("Could not query responder metrics: {}", ex.getMessage());
        }

        int availableResources = 0;
        int inUseResources = 0;
        try {
            availableResources = resourceDao.findByStatus("AVAILABLE").size();
            inUseResources = resourceDao.findByStatus("IN_USE").size();
        } catch (Exception ex) {
            logger.warn("Could not query resource metrics: {}", ex.getMessage());
        }

        return new DispatcherDashboardMetrics(
                activeCount, criticalCount, highCount,
                onDutyResponders, busyResponders,
                availableResources, inUseResources
        );
    }

    @Override
    public List<EmergencyIncident> getActiveIncidents(Long dispatcherUserId) {
        validateDispatcherAuthorization(dispatcherUserId);

        List<EmergencyIncident> active = incidentDao.findActiveIncidentsSortedByPriority();
        if (active.isEmpty()) {
            active = incidentDao.findAll().stream()
                    .filter(i -> !"RESOLVED".equalsIgnoreCase(i.getStatus()) && !"CLOSED".equalsIgnoreCase(i.getStatus()) && !"CANCELLED".equalsIgnoreCase(i.getStatus()))
                    .collect(Collectors.toList());
        }

        // Sort by effective priority DESC, then created_at DESC
        active.sort((a, b) -> Double.compare(b.getEffectivePriority(), a.getEffectivePriority()));
        return active;
    }

    @Override
    public List<EmergencyIncident> getCriticalIncidents(Long dispatcherUserId) {
        validateDispatcherAuthorization(dispatcherUserId);

        List<EmergencyIncident> active = getActiveIncidents(dispatcherUserId);
        return active.stream()
                .filter(i -> i.getEffectivePriority() >= 60.0) // HIGH and CRITICAL alerts
                .sorted((a, b) -> Double.compare(b.getEffectivePriority(), a.getEffectivePriority()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncident> searchAndFilterIncidents(Long dispatcherUserId, String query, String category, String priorityLevel, String status) {
        validateDispatcherAuthorization(dispatcherUserId);

        List<EmergencyIncident> allIncidents = incidentDao.findAll();

        return allIncidents.stream()
                .filter(incident -> {
                    // Filter by Status
                    if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!status.trim().equalsIgnoreCase(incident.getStatus())) return false;
                    }

                    // Filter by Priority Level
                    if (priorityLevel != null && !priorityLevel.trim().isEmpty() && !"ALL".equalsIgnoreCase(priorityLevel.trim())) {
                        double p = incident.getEffectivePriority();
                        String level = p >= 80.0 ? "CRITICAL" : (p >= 60.0 ? "HIGH" : (p >= 40.0 ? "MEDIUM" : "LOW"));
                        if (!priorityLevel.trim().equalsIgnoreCase(level)) return false;
                    }

                    // Query text filtering (Incident ID, Title, Category, Location)
                    if (query != null && !query.trim().isEmpty()) {
                        String q = query.trim().toLowerCase();
                        boolean matchesId = String.valueOf(incident.getId()).contains(q);
                        boolean matchesReport = false;

                        if (incident.getReportId() != null) {
                            Optional<EmergencyReport> rOpt = reportDao.findById(incident.getReportId());
                            if (rOpt.isPresent()) {
                                EmergencyReport r = rOpt.get();
                                matchesReport = (r.getTitle() != null && r.getTitle().toLowerCase().contains(q)) ||
                                                (r.getCategory() != null && r.getCategory().toLowerCase().contains(q)) ||
                                                (r.getLocation() != null && r.getLocation().toLowerCase().contains(q));

                                if (category != null && !category.trim().isEmpty() && !"ALL".equalsIgnoreCase(category.trim())) {
                                    if (!category.trim().equalsIgnoreCase(r.getCategory())) return false;
                                }
                            }
                        }

                        if (!matchesId && !matchesReport) return false;
                    }

                    return true;
                })
                .sorted((a, b) -> Double.compare(b.getEffectivePriority(), a.getEffectivePriority()))
                .collect(Collectors.toList());
    }

    @Override
    public List<EmergencyIncident> sortIncidents(List<EmergencyIncident> incidents, String sortBy) {
        if (incidents == null) return Collections.emptyList();
        List<EmergencyIncident> sorted = new ArrayList<>(incidents);

        if (sortBy == null) {
            sorted.sort((a, b) -> Double.compare(b.getEffectivePriority(), a.getEffectivePriority()));
            return sorted;
        }

        switch (sortBy.trim().toUpperCase()) {
            case "CREATED_TIME":
                sorted.sort((a, b) -> {
                    if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;
            case "STATUS":
                sorted.sort((a, b) -> String.valueOf(a.getStatus()).compareTo(String.valueOf(b.getStatus())));
                break;
            case "PRIORITY":
            default:
                sorted.sort((a, b) -> Double.compare(b.getEffectivePriority(), a.getEffectivePriority()));
                break;
        }

        return sorted;
    }
}
