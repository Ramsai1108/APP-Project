package com.campus.emergency.service;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.*;
import com.campus.emergency.dao.jdbc.*;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.infrastructure.DatabaseManager;
import com.campus.emergency.security.AuthorizationService;

public class ServiceRegistry {
    private static ServiceRegistry instance;

    private final ConnectionPoolManager poolManager;
    private final DatabaseManager databaseManager;

    private final UserDAO userDAO;
    private final LocationDAO locationDAO;
    private final EmergencyReportDAO reportDAO;
    private final EmergencyIncidentDAO incidentDAO;
    private final ResponderDAO responderDAO;
    private final ResourceDAO resourceDAO;
    private final SystemLogDAO logDAO;

    private final NotificationBroker notificationBroker;
    private final AuthorizationService authService;
    private final PriorityEngine priorityEngine;
    private final IdentityService identityService;
    private final EmergencyManager emergencyManager;
    private final ResourceDispatcher resourceDispatcher;
    private final TrackingService trackingService;
    private final DataAggregator dataAggregator;
    private final LocationService locationService;
    private final IncidentUpdateService incidentUpdateService;

    public ServiceRegistry(DatabaseConfig config) {
        this.poolManager = ConnectionPoolManager.getInstance(config);
        this.databaseManager = new DatabaseManager(poolManager);

        this.userDAO = new JdbcUserDAO(poolManager);
        this.locationDAO = new JdbcLocationDAO(poolManager);
        this.reportDAO = new JdbcEmergencyReportDAO(poolManager);
        this.incidentDAO = new JdbcEmergencyIncidentDAO(poolManager);
        this.responderDAO = new JdbcResponderDAO(poolManager);
        this.resourceDAO = new JdbcResourceDAO(poolManager);
        this.logDAO = new JdbcSystemLogDAO(poolManager);

        this.notificationBroker = new NotificationBroker();
        this.authService = new AuthorizationService();
        this.priorityEngine = new PriorityEngine();

        this.identityService = new IdentityService(userDAO);
        this.emergencyManager = new EmergencyManager(reportDAO, incidentDAO, priorityEngine, databaseManager, notificationBroker, logDAO);
        this.resourceDispatcher = new ResourceDispatcher(responderDAO, resourceDAO, incidentDAO, databaseManager, notificationBroker, logDAO, authService);
        this.trackingService = new TrackingService(locationDAO, responderDAO, resourceDAO, incidentDAO, reportDAO, databaseManager, notificationBroker, logDAO, authService);
        this.dataAggregator = new DataAggregator(reportDAO, incidentDAO, responderDAO, resourceDAO);
        this.locationService = new LocationService();
        this.incidentUpdateService = new IncidentUpdateService(incidentDAO);
    }

    public ServiceRegistry(ConnectionPoolManager poolManager, DatabaseManager databaseManager,
                           UserDAO userDAO, LocationDAO locationDAO, EmergencyReportDAO reportDAO,
                           EmergencyIncidentDAO incidentDAO, ResponderDAO responderDAO,
                           ResourceDAO resourceDAO, SystemLogDAO logDAO,
                           NotificationBroker notificationBroker, AuthorizationService authService,
                           PriorityEngine priorityEngine, IdentityService identityService,
                           EmergencyManager emergencyManager, ResourceDispatcher resourceDispatcher,
                           TrackingService trackingService, DataAggregator dataAggregator) {
        this(poolManager, databaseManager, userDAO, locationDAO, reportDAO, incidentDAO, responderDAO,
                resourceDAO, logDAO, notificationBroker, authService, priorityEngine, identityService,
                emergencyManager, resourceDispatcher, trackingService, dataAggregator, new LocationService());
    }

    public ServiceRegistry(ConnectionPoolManager poolManager, DatabaseManager databaseManager,
                           UserDAO userDAO, LocationDAO locationDAO, EmergencyReportDAO reportDAO,
                           EmergencyIncidentDAO incidentDAO, ResponderDAO responderDAO,
                           ResourceDAO resourceDAO, SystemLogDAO logDAO,
                           NotificationBroker notificationBroker, AuthorizationService authService,
                           PriorityEngine priorityEngine, IdentityService identityService,
                           EmergencyManager emergencyManager, ResourceDispatcher resourceDispatcher,
                           TrackingService trackingService, DataAggregator dataAggregator,
                           LocationService locationService) {
        this.poolManager = poolManager;
        this.databaseManager = databaseManager;
        this.userDAO = userDAO;
        this.locationDAO = locationDAO;
        this.reportDAO = reportDAO;
        this.incidentDAO = incidentDAO;
        this.responderDAO = responderDAO;
        this.resourceDAO = resourceDAO;
        this.logDAO = logDAO;
        this.notificationBroker = notificationBroker;
        this.authService = authService;
        this.priorityEngine = priorityEngine;
        this.identityService = identityService;
        this.emergencyManager = emergencyManager;
        this.resourceDispatcher = resourceDispatcher;
        this.trackingService = trackingService;
        this.dataAggregator = dataAggregator;
        this.locationService = (locationService != null) ? locationService : new LocationService();
        this.incidentUpdateService = new IncidentUpdateService(this.incidentDAO);
    }

    public static synchronized ServiceRegistry getInstance() {
        if (instance == null) {
            instance = new ServiceRegistry(new DatabaseConfig());
        }
        return instance;
    }

    public static synchronized void setInstance(ServiceRegistry customInstance) {
        instance = customInstance;
    }

    public ConnectionPoolManager getPoolManager() { return poolManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public UserDAO getUserDAO() { return userDAO; }
    public LocationDAO getLocationDAO() { return locationDAO; }
    public EmergencyReportDAO getReportDAO() { return reportDAO; }
    public EmergencyIncidentDAO getIncidentDAO() { return incidentDAO; }
    public ResponderDAO getResponderDAO() { return responderDAO; }
    public ResourceDAO getResourceDAO() { return resourceDAO; }
    public SystemLogDAO getLogDAO() { return logDAO; }
    public NotificationBroker getNotificationBroker() { return notificationBroker; }
    public AuthorizationService getAuthService() { return authService; }
    public PriorityEngine getPriorityEngine() { return priorityEngine; }
    public IdentityService getIdentityService() { return identityService; }
    public EmergencyManager getEmergencyManager() { return emergencyManager; }
    public ResourceDispatcher getResourceDispatcher() { return resourceDispatcher; }
    public TrackingService getTrackingService() { return trackingService; }
    public DataAggregator getDataAggregator() { return dataAggregator; }
    public LocationService getLocationService() { return locationService; }
    public IncidentUpdateService getIncidentUpdateService() { return incidentUpdateService; }
}
