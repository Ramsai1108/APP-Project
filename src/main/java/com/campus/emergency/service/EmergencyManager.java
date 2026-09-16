package com.campus.emergency.service;

import com.campus.emergency.model.EmergencyReport;
import java.util.List;
import java.util.Optional;

/**
 * Service Manager interface for Emergency Reporting workflows, priority calculation, and dispatcher notifications.
 */
public interface EmergencyManager {

    EmergencySubmissionResult submitReport(EmergencyReport report);

    List<EmergencyReport> getReportsForStudent(Long studentUserId);

    Optional<EmergencyReport> getReportDetails(Long reportId, Long requestingUserId);

    StudentDashboardMetrics getDashboardMetrics(Long studentUserId);

    void validateFileAttachment(String filePath, boolean isVideo);

    /**
     * DTO containing result of transacted emergency submission (Report ID, Incident ID, Priority).
     */
    record EmergencySubmissionResult(Long reportId, Long incidentId, double calculatedPriority, String status) {}

    /**
     * DTO containing aggregated dashboard counters.
     */
    record StudentDashboardMetrics(int totalReports, int openReports, int resolvedReports) {}
}
