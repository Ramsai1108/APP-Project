package com.campus.emergency.dao;

import com.campus.emergency.model.EmergencyReport;
import java.util.List;
import java.util.Optional;

public interface EmergencyReportDAO {
    EmergencyReport create(EmergencyReport report);
    Optional<EmergencyReport> findById(int id);
    List<EmergencyReport> findByReporterId(int reporterId);
    List<EmergencyReport> findAll();
    boolean updateStatus(int id, String status);
}
