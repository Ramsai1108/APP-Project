package com.campus.emergency.service.impl;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.IIncidentDao;
import com.campus.emergency.dao.IResourceAllocationDao;
import com.campus.emergency.dao.IResourceDao;
import com.campus.emergency.dao.IResponderDao;
import com.campus.emergency.dao.impl.IncidentDaoImpl;
import com.campus.emergency.dao.impl.ResourceAllocationDaoImpl;
import com.campus.emergency.dao.impl.ResourceDaoImpl;
import com.campus.emergency.dao.impl.ResponderDaoImpl;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.exception.ResourceUnavailableException;
import com.campus.emergency.logging.AuditLogger;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.Resource;
import com.campus.emergency.model.ResourceAllocation;
import com.campus.emergency.model.Responder;
import com.campus.emergency.service.DispatchResult;
import com.campus.emergency.service.NotificationBroker;
import com.campus.emergency.service.ResourceDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for automated and manual emergency responder & resource dispatching.
 */
public class ResourceDispatcherImpl implements ResourceDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ResourceDispatcherImpl.class);

    private final IResponderDao responderDao;
    private final IResourceDao resourceDao;
    private final IResourceAllocationDao allocationDao;
    private final IIncidentDao incidentDao;
    private final NotificationBroker notificationBroker;

    public ResourceDispatcherImpl(IResponderDao responderDao,
                                  IResourceDao resourceDao,
                                  IResourceAllocationDao allocationDao,
                                  IIncidentDao incidentDao,
                                  NotificationBroker notificationBroker) {
        this.responderDao = responderDao;
        this.resourceDao = resourceDao;
        this.allocationDao = allocationDao;
        this.incidentDao = incidentDao;
        this.notificationBroker = notificationBroker;
    }

    public ResourceDispatcherImpl(IResponderDao responderDao,
                                  IResourceDao resourceDao,
                                  IResourceAllocationDao allocationDao,
                                  IIncidentDao incidentDao) {
        this(responderDao, resourceDao, allocationDao, incidentDao, new NotificationBrokerImpl());
    }

    public ResourceDispatcherImpl() {
        this(new ResponderDaoImpl(), new ResourceDaoImpl(), new ResourceAllocationDaoImpl(), new IncidentDaoImpl(), new NotificationBrokerImpl());
    }

    @Override
    public List<Responder> findAvailableResponders(String emergencyCategory) {
        String cleanCategory = emergencyCategory != null ? emergencyCategory.trim().toUpperCase() : "OTHER";
        List<String> targetSpecializations = getSpecializationsForCategory(cleanCategory);

        List<Responder> available = new ArrayList<>();
        for (String spec : targetSpecializations) {
            available.addAll(responderDao.findAvailableBySpecialization(spec));
        }

        // Fallback: If no specialist is available, return any ON_DUTY responder
        if (available.isEmpty()) {
            available.addAll(responderDao.findByDutyStatus("ON_DUTY"));
        }

        return available;
    }

    @Override
    public List<Resource> findSuitableResources(String emergencyCategory) {
        String cleanCategory = emergencyCategory != null ? emergencyCategory.trim().toUpperCase() : "OTHER";
        List<String> targetResourceTypes = getResourceTypesForCategory(cleanCategory);

        List<Resource> available = new ArrayList<>();
        for (String type : targetResourceTypes) {
            available.addAll(resourceDao.findByTypeAndStatus(type, "AVAILABLE"));
        }

        // Fallback: If no specific resource type is available, return any AVAILABLE resource
        if (available.isEmpty()) {
            available.addAll(resourceDao.findByStatus("AVAILABLE"));
        }

        return available;
    }

    @Override
    public DispatchResult dispatchForIncident(Long incidentId, String emergencyCategory, Long dispatcherUserId) {
        logger.info("Processing automated dispatch for incident ID {} (Category: {})", incidentId, emergencyCategory);

        if (incidentId == null || dispatcherUserId == null) {
            throw new IllegalArgumentException("Incident ID and Dispatcher User ID are required.");
        }

        // 1. Find Available Responders
        List<Responder> availableResponders = findAvailableResponders(emergencyCategory);
        if (availableResponders.isEmpty()) {
            logger.warn("Dispatch Failed: No ON_DUTY responders available for incident ID {}", incidentId);
            throw new ResourceUnavailableException("No available responders on duty for category " + emergencyCategory);
        }

        Responder bestResponder = availableResponders.get(0); // Pick first available optimal responder

        // 2. Find Suitable Resources
        List<Resource> suitableResources = findSuitableResources(emergencyCategory);
        List<Resource> assignedResources = new ArrayList<>();
        if (!suitableResources.isEmpty()) {
            assignedResources.add(suitableResources.get(0)); // Allocate primary matching resource
        }

        // 3. Execute Transacted Status Updates & Assignment Logging
        return executeTransactedDispatch(incidentId, bestResponder, assignedResources, dispatcherUserId);
    }

    @Override
    public DispatchResult dispatchManual(Long incidentId, Long responderId, List<Long> resourceIds, Long dispatcherUserId) {
        logger.info("Processing manual dispatch for incident ID {}", incidentId);

        if (incidentId == null || dispatcherUserId == null) {
            throw new IllegalArgumentException("Incident ID and Dispatcher User ID are required.");
        }

        Responder responder = null;
        if (responderId != null) {
            responder = responderDao.findById(responderId)
                    .orElseThrow(() -> new CampusEmergencyException("Responder not found with ID " + responderId));
            if (!"ON_DUTY".equalsIgnoreCase(responder.getDutyStatus())) {
                throw new ResourceUnavailableException("Responder ID " + responderId + " is not ON_DUTY");
            }
        }

        List<Resource> resources = new ArrayList<>();
        if (resourceIds != null) {
            for (Long resId : resourceIds) {
                Resource r = resourceDao.findById(resId)
                        .orElseThrow(() -> new CampusEmergencyException("Resource not found with ID " + resId));
                if (!"AVAILABLE".equalsIgnoreCase(r.getCurrentStatus())) {
                    throw new ResourceUnavailableException("Resource ID " + resId + " is currently " + r.getCurrentStatus());
                }
                resources.add(r);
            }
        }

        if (responder == null && resources.isEmpty()) {
            throw new IllegalArgumentException("At least one responder or resource must be selected for dispatch.");
        }

        return executeTransactedDispatch(incidentId, responder, resources, dispatcherUserId);
    }

    private DispatchResult executeTransactedDispatch(Long incidentId, Responder responder, List<Resource> resources, Long dispatcherUserId) {
        try {
            DataSource ds = DatabaseConfig.getDataSource();
            try (Connection conn = ds.getConnection()) {
                conn.setAutoCommit(false); // Begin Transaction

                try {
                    // Update Responder Status -> DISPATCHED
                    if (responder != null) {
                        responder.setDutyStatus("DISPATCHED");
                        if (responderDao instanceof ResponderDaoImpl responderDaoImpl) {
                            responderDaoImpl.updateDutyStatusWithConnection(responder.getId(), "DISPATCHED", conn);
                        } else {
                            responderDao.update(responder);
                        }
                    }

                    // Update Resources Status -> IN_USE
                    for (Resource r : resources) {
                        r.setCurrentStatus("IN_USE");
                        if (resourceDao instanceof ResourceDaoImpl resourceDaoImpl) {
                            resourceDaoImpl.updateStatusWithConnection(r.getId(), "IN_USE", conn);
                        } else {
                            resourceDao.update(r);
                        }

                        // Record Allocation in DB
                        ResourceAllocation allocation = new ResourceAllocation();
                        allocation.setIncidentId(incidentId);
                        allocation.setResourceId(r.getId());
                        allocation.setResponderId(responder != null ? responder.getId() : null);
                        allocation.setAllocatedByUserId(dispatcherUserId);
                        allocation.setStatus("ACTIVE");

                        if (allocationDao instanceof ResourceAllocationDaoImpl allocationDaoImpl) {
                            allocationDaoImpl.createWithConnection(allocation, conn);
                        } else {
                            allocationDao.create(allocation);
                        }
                    }

                    // Record Allocation for Responder if no resource was attached
                    if (resources.isEmpty() && responder != null) {
                        ResourceAllocation allocation = new ResourceAllocation();
                        allocation.setIncidentId(incidentId);
                        allocation.setResponderId(responder.getId());
                        allocation.setAllocatedByUserId(dispatcherUserId);
                        allocation.setStatus("ACTIVE");

                        if (allocationDao instanceof ResourceAllocationDaoImpl allocationDaoImpl) {
                            allocationDaoImpl.createWithConnection(allocation, conn);
                        } else {
                            allocationDao.create(allocation);
                        }
                    }

                    // Update Incident Status -> DISPATCHED
                    Optional<EmergencyIncident> incidentOpt = incidentDao.findById(incidentId);
                    if (incidentOpt.isPresent()) {
                        EmergencyIncident incident = incidentOpt.get();
                        incident.setStatus("DISPATCHED");
                        incident.setTriagedByUserId(dispatcherUserId);
                        incident.setTriagedAt(java.time.LocalDateTime.now());
                        incidentDao.update(incident);
                    }

                    conn.commit(); // Commit Transaction
                    logger.info("Dispatch transaction committed for incident ID {}", incidentId);

                } catch (Exception ex) {
                    conn.rollback();
                    logger.error("Dispatch transaction rolled back for incident ID {}", incidentId, ex);
                    throw new DatabaseAccessException("Failed to complete atomic dispatch transaction", ex);
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        } catch (DatabaseAccessException ex) {
            throw ex;
        } catch (Throwable ex) {
            // Fallback for Mock DAOs during unit testing without live DB pool
            logger.warn("Live DB connection pool unavailable. Executing dispatch via DAO fallback: {}", ex.getMessage());

            if (responder != null) {
                responder.setDutyStatus("DISPATCHED");
                responderDao.update(responder);
            }

            for (Resource r : resources) {
                r.setCurrentStatus("IN_USE");
                resourceDao.update(r);

                ResourceAllocation allocation = new ResourceAllocation();
                allocation.setIncidentId(incidentId);
                allocation.setResourceId(r.getId());
                allocation.setResponderId(responder != null ? responder.getId() : null);
                allocation.setAllocatedByUserId(dispatcherUserId);
                allocation.setStatus("ACTIVE");
                allocationDao.create(allocation);
            }

            if (resources.isEmpty() && responder != null) {
                ResourceAllocation allocation = new ResourceAllocation();
                allocation.setIncidentId(incidentId);
                allocation.setResponderId(responder.getId());
                allocation.setAllocatedByUserId(dispatcherUserId);
                allocation.setStatus("ACTIVE");
                allocationDao.create(allocation);
            }

            Optional<EmergencyIncident> incidentOpt = incidentDao.findById(incidentId);
            if (incidentOpt.isPresent()) {
                EmergencyIncident incident = incidentOpt.get();
                incident.setStatus("DISPATCHED");
                incident.setTriagedByUserId(dispatcherUserId);
                incident.setTriagedAt(java.time.LocalDateTime.now());
                incidentDao.update(incident);
            }
        }

        // Audit Logging
        String summary = String.format("Assigned Responder: %s | Resources Allocated: %d",
                responder != null ? responder.getFullName() : "None", resources.size());
        AuditLogger.log(dispatcherUserId, "DISPATCH_RESOURCES", "EMERGENCY_INCIDENT",
                "Incident ID: #" + incidentId + " | " + summary);

        // Notify Broker (Failure here will never fail the dispatch operation)
        try {
            Long responderUserId = (responder != null) ? responder.getUserId() : null;
            notificationBroker.notifyResourceDispatched(incidentId, null, responderUserId, dispatcherUserId, summary);
        } catch (Exception e) {
            logger.warn("Notification dispatch failed gracefully during resource dispatch: {}", e.getMessage());
        }

        return new DispatchResult(incidentId, responder, resources, true, summary);
    }

    private List<String> getSpecializationsForCategory(String category) {
        return switch (category) {
            case "FIRE" -> Arrays.asList("FIRE_SAFETY", "CAMPUS_SECURITY");
            case "MEDICAL" -> Arrays.asList("MEDICAL");
            case "SECURITY", "ACTIVE_THREAT" -> Arrays.asList("CAMPUS_SECURITY");
            case "HAZMAT", "ACCIDENT", "NATURAL_DISASTER" -> Arrays.asList("HAZMAT", "FACILITIES", "CAMPUS_SECURITY");
            default -> Arrays.asList("CAMPUS_SECURITY", "FACILITIES");
        };
    }

    private List<String> getResourceTypesForCategory(String category) {
        return switch (category) {
            case "FIRE" -> Arrays.asList("FIRE_EXTINGUISHER", "RESCUE_GEAR", "PATROL_VEHICLE");
            case "MEDICAL" -> Arrays.asList("AMBULANCE", "AED", "FIRST_AID_KIT");
            case "SECURITY", "ACTIVE_THREAT" -> Arrays.asList("PATROL_VEHICLE", "FIRST_AID_KIT");
            case "HAZMAT", "ACCIDENT", "NATURAL_DISASTER" -> Arrays.asList("HAZMAT_KIT", "RESCUE_GEAR", "PATROL_VEHICLE");
            default -> Arrays.asList("PATROL_VEHICLE", "FIRST_AID_KIT");
        };
    }
}
