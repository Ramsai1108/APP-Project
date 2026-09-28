package com.campus.emergency.service;

import com.campus.emergency.dao.EmergencyIncidentDAO;
import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.dao.ResourceDAO;
import com.campus.emergency.dao.ResponderDAO;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class DataAggregator {
    private final EmergencyReportDAO reportDAO;
    private final EmergencyIncidentDAO incidentDAO;
    private final ResponderDAO responderDAO;
    private final ResourceDAO resourceDAO;

    public DataAggregator(EmergencyReportDAO reportDAO, EmergencyIncidentDAO incidentDAO) {
        this(reportDAO, incidentDAO, null, null);
    }

    public DataAggregator(EmergencyReportDAO reportDAO, EmergencyIncidentDAO incidentDAO,
                          ResponderDAO responderDAO, ResourceDAO resourceDAO) {
        this.reportDAO = reportDAO;
        this.incidentDAO = incidentDAO;
        this.responderDAO = responderDAO;
        this.resourceDAO = resourceDAO;
    }

    public List<EmergencyIncident> getActiveIncidents() {
        return incidentDAO.findAllActive();
    }

    public List<EmergencyIncident> getAllIncidents() {
        return incidentDAO.findAll();
    }

    public List<EmergencyReport> getAllReports() {
        return reportDAO.findAll();
    }

    public int getTotalIncidentsCount() {
        return incidentDAO.findAll().size();
    }

    public int getActiveIncidentsCount() {
        return incidentDAO.findAllActive().size();
    }

    public int getResolvedIncidentsCount() {
        return (int) incidentDAO.findAll().stream()
                .filter(inc -> "RESOLVED".equalsIgnoreCase(inc.getStatus()) || "CLOSED".equalsIgnoreCase(inc.getStatus()))
                .count();
    }

    public int getIncidentsCountByPriority(String priority) {
        if (priority == null) return 0;
        return (int) incidentDAO.findAll().stream()
                .filter(inc -> priority.equalsIgnoreCase(inc.getPriority()))
                .count();
    }

    public int getIncidentsCountByStatus(String status) {
        if (status == null) return 0;
        return (int) incidentDAO.findAll().stream()
                .filter(inc -> status.equalsIgnoreCase(inc.getStatus()))
                .count();
    }

    public int getAvailableRespondersCount() {
        if (responderDAO == null) return 0;
        return responderDAO.findAvailable().size();
    }

    public int getBusyRespondersCount() {
        if (responderDAO == null) return 0;
        return (int) responderDAO.findAll().stream()
                .filter(r -> "DISPATCHED".equalsIgnoreCase(r.getStatus()) || "ON_BREAK".equalsIgnoreCase(r.getStatus()))
                .count();
    }

    public int getAvailableResourcesCount() {
        if (resourceDAO == null) return 0;
        return resourceDAO.findAvailable().size();
    }

    public int getDispatchedResourcesCount() {
        if (resourceDAO == null) return 0;
        return (int) resourceDAO.findAll().stream()
                .filter(res -> "IN_USE".equalsIgnoreCase(res.getStatus()))
                .count();
    }

    public List<EmergencyIncident> getRecentIncidents(int limit) {
        return incidentDAO.findAll().stream()
                .sorted(Comparator.comparing(EmergencyIncident::getId).reversed())
                .limit(limit > 0 ? limit : 10)
                .collect(Collectors.toList());
    }
}
