package com.campus.emergency.service;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.dao.ResourceDAO;
import com.campus.emergency.dao.ResponderDAO;
import com.campus.emergency.dao.SystemLogDAO;
import com.campus.emergency.dao.jdbc.JdbcEmergencyIncidentDAO;
import com.campus.emergency.dao.jdbc.JdbcResourceDAO;
import com.campus.emergency.dao.jdbc.JdbcResponderDAO;
import com.campus.emergency.exception.EmergencyProcessingException;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.Resource;
import com.campus.emergency.model.Responder;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthorizationService;

import java.util.List;
import java.util.Optional;

public class ResourceDispatcher {
    private final ResponderDAO responderDAO;
    private final ResourceDAO resourceDAO;
    private final EmergencyIncidentDAO incidentDAO;
    private final DatabaseManager databaseManager;
    private final NotificationBroker notificationBroker;
    private final SystemLogDAO logDAO;
    private final AuthorizationService authService;

    public ResourceDispatcher(ResponderDAO responderDAO, ResourceDAO resourceDAO) {
        this(responderDAO, resourceDAO, null, null, new NotificationBroker(), null, new AuthorizationService());
    }

    public ResourceDispatcher(ResponderDAO responderDAO, ResourceDAO resourceDAO,
                              EmergencyIncidentDAO incidentDAO, DatabaseManager databaseManager,
                              NotificationBroker notificationBroker, SystemLogDAO logDAO) {
        this(responderDAO, resourceDAO, incidentDAO, databaseManager, notificationBroker, logDAO, new AuthorizationService());
    }

    public ResourceDispatcher(ResponderDAO responderDAO, ResourceDAO resourceDAO,
                              EmergencyIncidentDAO incidentDAO, DatabaseManager databaseManager,
                              NotificationBroker notificationBroker, SystemLogDAO logDAO,
                              AuthorizationService authService) {
        this.responderDAO = responderDAO;
        this.resourceDAO = resourceDAO;
        this.incidentDAO = incidentDAO;
        this.databaseManager = databaseManager;
        this.notificationBroker = (notificationBroker != null) ? notificationBroker : new NotificationBroker();
        this.logDAO = logDAO;
        this.authService = (authService != null) ? authService : new AuthorizationService();
    }

    public List<Responder> getAvailableResponders() {
        return responderDAO.findAvailable();
    }

    public List<Resource> getAvailableResources() {
        return resourceDAO.findAvailable();
    }

    public Responder autoSelectResponder(EmergencyIncident incident) {
        List<Responder> available = responderDAO.findAvailable();
        if (available == null || available.isEmpty()) {
            return null;
        }
        return available.get(0);
    }

    public Resource autoSelectResource(EmergencyIncident incident) {
        List<Resource> available = resourceDAO.findAvailable();
        if (available == null || available.isEmpty()) {
            return null;
        }
        return available.get(0);
    }

    public synchronized boolean assignResponderToIncident(User operator, int incidentId, int responderId) throws EmergencyProcessingException {
        validateOperator(operator);

        Responder responder = responderDAO.findById(responderId)
                .orElseThrow(() -> new EmergencyProcessingException("Responder not found with ID: " + responderId));

        if (!"AVAILABLE".equalsIgnoreCase(responder.getStatus())) {
            throw new EmergencyProcessingException(String.format("Responder #%d is not available (Status: %s)", responderId, responder.getStatus()));
        }

        boolean success = responderDAO.updateStatus(responderId, "DISPATCHED");
        if (success) {
            if (incidentDAO != null && operator != null) {
                incidentDAO.assignDispatcher(incidentId, operator.getId());
            }
            LoggingService.info(String.format("Responder #%d dispatched to Incident #%d by Operator #%d", responderId, incidentId, operator != null ? operator.getId() : 0));
            if (logDAO != null && operator != null) {
                logDAO.log("RESPONDER_DISPATCHED", operator.getId(), String.format("Assigned Responder #%d to Incident #%d", responderId, incidentId));
            }
            notificationBroker.notifyEvent(
                    NotificationBroker.EventType.RESPONDER_ASSIGNED,
                    "DISPATCH_CENTER",
                    String.format("Responder #%d assigned to Incident #%d", responderId, incidentId),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return success;
    }

    public boolean updateResponderStatus(int responderId, String status) {
        return responderDAO.updateStatus(responderId, status);
    }

    public boolean assignResourceToIncident(int resourceId, int incidentId) {
        try {
            return assignResourceToIncident(null, resourceId, incidentId);
        } catch (EmergencyProcessingException e) {
            return false;
        }
    }

    public synchronized boolean assignResourceToIncident(User operator, int resourceId, int incidentId) throws EmergencyProcessingException {
        if (operator != null) {
            validateOperator(operator);
        }

        Resource resource = resourceDAO.findById(resourceId)
                .orElseThrow(() -> new EmergencyProcessingException("Resource not found with ID: " + resourceId));

        if (!"AVAILABLE".equalsIgnoreCase(resource.getStatus()) || resource.getAssignedIncidentId() != null) {
            throw new EmergencyProcessingException(String.format("Resource #%d is already actively assigned or unavailable (Status: %s, AssignedIncident: %s)",
                    resourceId, resource.getStatus(), resource.getAssignedIncidentId()));
        }

        boolean success = resourceDAO.assignToIncident(resourceId, incidentId);
        if (success) {
            LoggingService.info(String.format("Resource #%d assigned to Incident #%d by Operator #%d", resourceId, incidentId, operator != null ? operator.getId() : 0));
            if (logDAO != null && operator != null) {
                logDAO.log("RESOURCE_DISPATCHED", operator.getId(), String.format("Assigned Resource #%d to Incident #%d", resourceId, incidentId));
            }
            notificationBroker.notifyEvent(
                    NotificationBroker.EventType.RESOURCE_ASSIGNED,
                    "DISPATCH_CENTER",
                    String.format("Resource #%d assigned to Incident #%d", resourceId, incidentId),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return success;
    }

    public boolean releaseResource(int resourceId) {
        try {
            return releaseResource(null, resourceId);
        } catch (EmergencyProcessingException e) {
            return false;
        }
    }

    public boolean releaseResource(User operator, int resourceId) throws EmergencyProcessingException {
        if (operator != null) {
            validateOperator(operator);
        }

        Resource resource = resourceDAO.findById(resourceId)
                .orElseThrow(() -> new EmergencyProcessingException("Resource not found with ID: " + resourceId));

        boolean success = resourceDAO.releaseResource(resourceId);
        if (success) {
            LoggingService.info(String.format("Resource #%d released by Operator #%d", resourceId, operator != null ? operator.getId() : 0));
            if (logDAO != null && operator != null) {
                logDAO.log("RESOURCE_RELEASED", operator.getId(), String.format("Released Resource #%d", resourceId));
            }
            notificationBroker.notifyEvent(
                    NotificationBroker.EventType.RESOURCE_RELEASED,
                    "DISPATCH_CENTER",
                    String.format("Resource #%d has been released and is now available", resourceId),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return success;
    }

    public boolean releaseResponder(User operator, int responderId) throws EmergencyProcessingException {
        if (operator != null) {
            validateOperator(operator);
        }

        Responder responder = responderDAO.findById(responderId)
                .orElseThrow(() -> new EmergencyProcessingException("Responder not found with ID: " + responderId));

        boolean success = responderDAO.updateStatus(responderId, "AVAILABLE");
        if (success) {
            LoggingService.info(String.format("Responder #%d released to AVAILABLE by Operator #%d", responderId, operator != null ? operator.getId() : 0));
            if (logDAO != null && operator != null) {
                logDAO.log("RESPONDER_RELEASED", operator.getId(), String.format("Released Responder #%d to AVAILABLE", responderId));
            }
            notificationBroker.notifyEvent(
                    NotificationBroker.EventType.DISPATCH_UPDATED,
                    "DISPATCH_CENTER",
                    String.format("Responder #%d is now available", responderId),
                    NotificationBroker.NotificationType.IN_APP
            );
        }
        return success;
    }

    public boolean dispatch(User operator, int incidentId, Integer responderId, Integer resourceId) throws EmergencyProcessingException {
        validateOperator(operator);

        if (responderId == null && resourceId == null) {
            throw new EmergencyProcessingException("Dispatch operation requires at least one responder or resource.");
        }

        if (responderId != null) {
            Responder responder = responderDAO.findById(responderId)
                    .orElseThrow(() -> new EmergencyProcessingException("Responder not found with ID: " + responderId));
            if (!"AVAILABLE".equalsIgnoreCase(responder.getStatus())) {
                throw new EmergencyProcessingException(String.format("Responder #%d is not available (Status: %s)", responderId, responder.getStatus()));
            }
        }

        if (resourceId != null) {
            Resource resource = resourceDAO.findById(resourceId)
                    .orElseThrow(() -> new EmergencyProcessingException("Resource not found with ID: " + resourceId));
            if (!"AVAILABLE".equalsIgnoreCase(resource.getStatus()) || resource.getAssignedIncidentId() != null) {
                throw new EmergencyProcessingException(String.format("Resource #%d is unavailable or already assigned to Incident #%s", resourceId, resource.getAssignedIncidentId()));
            }
        }

        if (databaseManager != null && responderDAO instanceof JdbcResponderDAO && resourceDAO instanceof JdbcResourceDAO) {
            JdbcResponderDAO jdbcResponderDAO = (JdbcResponderDAO) responderDAO;
            JdbcResourceDAO jdbcResourceDAO = (JdbcResourceDAO) resourceDAO;
            JdbcEmergencyIncidentDAO jdbcIncidentDAO = (incidentDAO instanceof JdbcEmergencyIncidentDAO) ? (JdbcEmergencyIncidentDAO) incidentDAO : null;

            try {
                return databaseManager.executeTransaction(conn -> {
                    if (responderId != null) {
                        jdbcResponderDAO.updateStatus(responderId, "DISPATCHED", conn);
                    }
                    if (resourceId != null) {
                        jdbcResourceDAO.assignToIncident(resourceId, incidentId, conn);
                    }
                    if (jdbcIncidentDAO != null && operator != null) {
                        jdbcIncidentDAO.assignDispatcher(incidentId, operator.getId(), conn);
                        jdbcIncidentDAO.updateStatus(incidentId, "IN_PROGRESS", conn);
                    }
                    return true;
                });
            } catch (Exception e) {
                throw new EmergencyProcessingException("Dispatch transaction failed and was rolled back: " + e.getMessage(), e);
            }
        } else {
            // Non-transactional fallback for unit mock testing
            if (responderId != null) {
                responderDAO.updateStatus(responderId, "DISPATCHED");
            }
            if (resourceId != null) {
                resourceDAO.assignToIncident(resourceId, incidentId);
            }
            if (incidentDAO != null && operator != null) {
                incidentDAO.assignDispatcher(incidentId, operator.getId());
                incidentDAO.updateStatus(incidentId, "IN_PROGRESS");
            }
        }

        if (logDAO != null && operator != null) {
            logDAO.log("DISPATCH_EXECUTE", operator.getId(), String.format("Dispatched to Incident #%d: Responder=%s, Resource=%s", incidentId, responderId, resourceId));
        }

        notificationBroker.notifyEvent(
                NotificationBroker.EventType.RESOURCE_DISPATCHED,
                "DISPATCH_CENTER",
                String.format("Dispatch executed for Incident #%d (Responder: %s, Resource: %s)", incidentId, responderId, resourceId),
                NotificationBroker.NotificationType.IN_APP
        );

        return true;
    }

    public boolean dispatch(int incidentId, Integer responderId, Integer resourceId) throws EmergencyProcessingException {
        return dispatch(null, incidentId, responderId, resourceId);
    }

    private void validateOperator(User operator) throws EmergencyProcessingException {
        if (operator == null) {
            throw new EmergencyProcessingException("Unauthenticated operator. Dispatch operation rejected.");
        }
        if (!authService.canDispatchResources(operator)) {
            throw new EmergencyProcessingException(String.format("Access denied: User '%s' with role '%s' cannot dispatch resources.", operator.getUsername(), operator.getRole()));
        }
    }
}
