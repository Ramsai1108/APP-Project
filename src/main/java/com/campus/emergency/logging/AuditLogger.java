package com.campus.emergency.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Audit Logging Service wrapper.
 */
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT_LOGGER");

    private AuditLogger() {}

    public static void log(Long userId, String action, String targetEntity, String details) {
        auditLog.info("USER_ID={} | ACTION={} | TARGET={} | DETAILS={}", userId, action, targetEntity, details);
    }
}
