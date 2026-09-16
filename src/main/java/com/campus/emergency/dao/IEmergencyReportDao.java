package com.campus.emergency.dao;

import com.campus.emergency.model.EmergencyReport;
import java.util.List;

/**
 * EmergencyReport DAO interface.
 */
public interface IEmergencyReportDao extends IGenericDao<EmergencyReport, Long> {
    List<EmergencyReport> findByReporterId(Long reporterId);
    int countTotalByReporterId(Long reporterId);
    int countOpenByReporterId(Long reporterId);
    int countResolvedByReporterId(Long reporterId);
}
