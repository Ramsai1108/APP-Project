package com.campus.emergency.dao.impl;

import com.campus.emergency.dao.IAuditLogDao;
import com.campus.emergency.model.AuditLog;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation stub for IAuditLogDao.
 */
public class AuditLogDaoImpl extends AbstractJDBCDao<AuditLog, Long> implements IAuditLogDao {

    @Override
    public AuditLog logAction(AuditLog auditLog) {
        return auditLog;
    }

    @Override
    public List<AuditLog> findRecentLogs(int limit) {
        return new ArrayList<>();
    }

    @Override
    public List<AuditLog> findByUserId(Long userId) {
        return new ArrayList<>();
    }

    @Override
    public AuditLog create(AuditLog entity) {
        return logAction(entity);
    }

    @Override
    public java.util.Optional<AuditLog> findById(Long id) {
        return java.util.Optional.empty();
    }

    @Override
    public List<AuditLog> findAll() {
        return findRecentLogs(100);
    }

    @Override
    public boolean update(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be updated.");
    }

    @Override
    public boolean deleteById(Long id) {
        throw new UnsupportedOperationException("Audit logs are immutable and cannot be deleted.");
    }
}
