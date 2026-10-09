package com.campus.emergency.service;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.model.EmergencyIncident;

import javax.swing.SwingUtilities;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IncidentUpdateService {

    public interface IncidentUpdateListener {
        void onNewIncidents(List<EmergencyIncident> newIncidents);
        void onIncidentsChanged(List<EmergencyIncident> currentIncidents);
    }

    private final EmergencyIncidentDAO incidentDAO;
    private final List<IncidentUpdateListener> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService scheduler;
    private final Set<Integer> knownIncidentIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, String> knownStatuses = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> knownDispatchers = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public IncidentUpdateService(EmergencyIncidentDAO incidentDAO) {
        this.incidentDAO = incidentDAO;
    }

    public synchronized void start(int intervalSeconds) {
        if (isRunning.get()) {
            return;
        }
        isRunning.set(true);
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "IncidentUpdateService-Poller");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleWithFixedDelay(this::pollForUpdates, 0, intervalSeconds, TimeUnit.SECONDS);
        LoggingService.info("IncidentUpdateService started with poll interval of " + intervalSeconds + " seconds.");
    }

    public synchronized void stop() {
        if (!isRunning.get()) {
            return;
        }
        isRunning.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        LoggingService.info("IncidentUpdateService stopped.");
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public void addListener(IncidentUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(IncidentUpdateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void pollForUpdates() {
        try {
            List<EmergencyIncident> currentIncidents = incidentDAO.findAll();
            if (currentIncidents == null) return;

            List<EmergencyIncident> newIncidents = new ArrayList<>();
            boolean anyChangeDetected = false;

            if (!initialized.get()) {
                for (EmergencyIncident inc : currentIncidents) {
                    knownIncidentIds.add(inc.getId());
                    if (inc.getStatus() != null) knownStatuses.put(inc.getId(), inc.getStatus());
                    if (inc.getAssignedDispatcherId() != null) knownDispatchers.put(inc.getId(), inc.getAssignedDispatcherId());
                }
                initialized.set(true);
            } else {
                for (EmergencyIncident inc : currentIncidents) {
                    int id = inc.getId();
                    if (!knownIncidentIds.contains(id)) {
                        knownIncidentIds.add(id);
                        if (inc.getStatus() != null) knownStatuses.put(id, inc.getStatus());
                        if (inc.getAssignedDispatcherId() != null) knownDispatchers.put(id, inc.getAssignedDispatcherId());
                        newIncidents.add(inc);
                        anyChangeDetected = true;
                    } else {
                        String prevStatus = knownStatuses.get(id);
                        Integer prevDispatcher = knownDispatchers.get(id);

                        String currentStatus = inc.getStatus();
                        Integer currentDispatcher = inc.getAssignedDispatcherId();

                        if (!Objects.equals(prevStatus, currentStatus) || !Objects.equals(prevDispatcher, currentDispatcher)) {
                            if (currentStatus != null) knownStatuses.put(id, currentStatus);
                            if (currentDispatcher != null) knownDispatchers.put(id, currentDispatcher); else knownDispatchers.remove(id);
                            anyChangeDetected = true;
                        }
                    }
                }
            }

            if (!newIncidents.isEmpty()) {
                List<EmergencyIncident> copyNew = Collections.unmodifiableList(newIncidents);
                List<EmergencyIncident> copyCurrent = Collections.unmodifiableList(currentIncidents);
                for (IncidentUpdateListener listener : listeners) {
                    try {
                        listener.onNewIncidents(copyNew);
                        listener.onIncidentsChanged(copyCurrent);
                    } catch (Exception ex) {
                        LoggingService.error("Error in IncidentUpdateListener onNewIncidents: " + ex.getMessage(), ex);
                    }
                }
            } else if (anyChangeDetected) {
                List<EmergencyIncident> copyCurrent = Collections.unmodifiableList(currentIncidents);
                for (IncidentUpdateListener listener : listeners) {
                    try {
                        listener.onIncidentsChanged(copyCurrent);
                    } catch (Exception ex) {
                        LoggingService.error("Error in IncidentUpdateListener onIncidentsChanged: " + ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception e) {
            LoggingService.warn("IncidentUpdateService poll warning (database retry active): " + e.getMessage());
        }
    }

    public Set<Integer> getKnownIncidentIds() {
        return Collections.unmodifiableSet(knownIncidentIds);
    }

    public void markIncidentAsSeen(int incidentId) {
        knownIncidentIds.add(incidentId);
    }

    public void reset() {
        knownIncidentIds.clear();
        knownStatuses.clear();
        knownDispatchers.clear();
        initialized.set(false);
    }
}
