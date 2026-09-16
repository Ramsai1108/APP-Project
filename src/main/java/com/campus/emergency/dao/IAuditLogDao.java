package com.campus.emergency.dao;

import com.campus.emergency.model.AuditLog;
import java.util.List;

/**
 * Immutable AuditLog DAO interface.
 */
public interface IAuditLogDao {
    AuditLog logAction(AuditLog auditLog);
    List<AuditLog> findRecentLogs(int limit);
    List<AuditLog> findByUserId(Long userId);
}
