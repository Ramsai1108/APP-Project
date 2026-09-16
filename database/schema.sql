-- ============================================================================
-- SMART CAMPUS EMERGENCY & RESOURCE CO-ORDINATION SYSTEM
-- Database Schema Script: database/schema.sql
-- Database Engine: MySQL 8.0+
-- ============================================================================

CREATE DATABASE IF NOT EXISTS smart_campus_emergency
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_campus_emergency;

-- Disable Foreign Key checks during table setup
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS anonymous_sos;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS incident_timeline;
DROP TABLE IF EXISTS resource_allocations;
DROP TABLE IF EXISTS emergency_incidents;
DROP TABLE IF EXISTS report_attachments;
DROP TABLE IF EXISTS emergency_reports;
DROP TABLE IF EXISTS responders;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS locations;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. ROLES TABLE
-- ============================================================================
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_name UNIQUE (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_users_active ON users(is_active);

-- ============================================================================
-- 3. LOCATIONS TABLE
-- ============================================================================
CREATE TABLE locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_name VARCHAR(100) NOT NULL,
    building_code VARCHAR(20) NOT NULL,
    floor_level INT NOT NULL DEFAULT 1,
    room_number VARCHAR(20) NOT NULL,
    vulnerability_score DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_locations_building_room UNIQUE (building_code, floor_level, room_number),
    CONSTRAINT chk_vulnerability_score CHECK (vulnerability_score >= 1.00 AND vulnerability_score <= 10.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_locations_building ON locations(building_code);

-- ============================================================================
-- 4. USER PROFILES TABLE
-- ============================================================================
CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY,
    blood_group VARCHAR(5) NULL,
    medical_alerts TEXT NULL,
    emergency_contact_name VARCHAR(100) NOT NULL,
    emergency_contact_phone VARCHAR(20) NOT NULL,
    primary_location_id BIGINT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_profiles_location FOREIGN KEY (primary_location_id) REFERENCES locations(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. RESOURCES TABLE
-- ============================================================================
CREATE TABLE resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_name VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    current_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    location_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_resources_location FOREIGN KEY (location_id) REFERENCES locations(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_resource_type CHECK (resource_type IN ('AMBULANCE', 'AED', 'FIRE_EXTINGUISHER', 'FIRST_AID_KIT', 'PATROL_VEHICLE', 'HAZMAT_KIT', 'RESCUE_GEAR')),
    CONSTRAINT chk_resource_status CHECK (current_status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'DEPLETED', 'DECOMMISSIONED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_resources_status ON resources(current_status);
CREATE INDEX idx_resources_type ON resources(resource_type);
CREATE INDEX idx_resources_location ON resources(location_id);

-- ============================================================================
-- 6. RESPONDERS TABLE
-- ============================================================================
CREATE TABLE responders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    full_name VARCHAR(100) NOT NULL,
    specialization VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    duty_status VARCHAR(30) NOT NULL DEFAULT 'OFF_DUTY',
    assigned_location_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_responders_user UNIQUE (user_id),
    CONSTRAINT fk_responders_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_responders_location FOREIGN KEY (assigned_location_id) REFERENCES locations(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_responder_specialization CHECK (specialization IN ('MEDICAL', 'FIRE_SAFETY', 'CAMPUS_SECURITY', 'HAZMAT', 'FACILITIES', 'DISPATCH')),
    CONSTRAINT chk_responder_status CHECK (duty_status IN ('ON_DUTY', 'DISPATCHED', 'BUSY', 'OFF_DUTY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_responders_status ON responders(duty_status);
CREATE INDEX idx_responders_specialization ON responders(specialization);

-- ============================================================================
-- 7. EMERGENCY REPORTS TABLE (MODIFIED FOR MODULE 2)
-- ============================================================================
CREATE TABLE emergency_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    severity INT NOT NULL DEFAULT 1,
    location VARCHAR(255) NOT NULL,
    location_id BIGINT NULL,
    affected_people INT NOT NULL DEFAULT 0,
    media_path VARCHAR(500) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    reported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reports_location FOREIGN KEY (location_id) REFERENCES locations(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_severity CHECK (severity BETWEEN 1 AND 5),
    CONSTRAINT chk_affected_people CHECK (affected_people >= 0),
    CONSTRAINT chk_report_status CHECK (status IN ('SUBMITTED', 'OPEN', 'TRIAGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_reports_reporter ON emergency_reports(reporter_id);
CREATE INDEX idx_reports_location ON emergency_reports(location_id);
CREATE INDEX idx_reports_category ON emergency_reports(category);
CREATE INDEX idx_reports_status ON emergency_reports(status);
CREATE INDEX idx_reports_timestamp ON emergency_reports(reported_at);

-- ============================================================================
-- 8. REPORT ATTACHMENTS TABLE
-- ============================================================================
CREATE TABLE report_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sha256_hash VARCHAR(64) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_report FOREIGN KEY (report_id) REFERENCES emergency_reports(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_attachments_report ON report_attachments(report_id);

-- ============================================================================
-- 9. EMERGENCY INCIDENTS TABLE
-- ============================================================================
CREATE TABLE emergency_incidents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    calculated_priority DECIMAL(5,2) NOT NULL,
    override_priority DECIMAL(5,2) NULL,
    effective_priority DECIMAL(5,2) GENERATED ALWAYS AS (COALESCE(override_priority, calculated_priority)) STORED,
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    triaged_by_user_id BIGINT NULL,
    triaged_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    closed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_incidents_report UNIQUE (report_id),
    CONSTRAINT fk_incidents_report FOREIGN KEY (report_id) REFERENCES emergency_reports(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_incidents_triaged_by FOREIGN KEY (triaged_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_calculated_priority CHECK (calculated_priority >= 0.00 AND calculated_priority <= 100.00),
    CONSTRAINT chk_override_priority CHECK (override_priority IS NULL OR (override_priority >= 0.00 AND override_priority <= 100.00)),
    CONSTRAINT chk_incident_status CHECK (status IN ('REPORTED', 'TRIAGED', 'DISPATCHED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_incidents_status ON emergency_incidents(status);
CREATE INDEX idx_incidents_priority ON emergency_incidents(effective_priority);

-- ============================================================================
-- 10. RESOURCE ALLOCATIONS TABLE
-- ============================================================================
CREATE TABLE resource_allocations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    resource_id BIGINT NULL,
    responder_id BIGINT NULL,
    allocated_by_user_id BIGINT NOT NULL,
    allocated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_allocations_incident FOREIGN KEY (incident_id) REFERENCES emergency_incidents(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_allocations_resource FOREIGN KEY (resource_id) REFERENCES resources(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_allocations_responder FOREIGN KEY (responder_id) REFERENCES responders(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_allocations_user FOREIGN KEY (allocated_by_user_id) REFERENCES users(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_allocation_entity CHECK (resource_id IS NOT NULL OR responder_id IS NOT NULL),
    CONSTRAINT chk_allocation_status CHECK (status IN ('ACTIVE', 'RELEASED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_allocations_incident ON resource_allocations(incident_id);
CREATE INDEX idx_allocations_resource ON resource_allocations(resource_id);
CREATE INDEX idx_allocations_responder ON resource_allocations(responder_id);

-- ============================================================================
-- 11. INCIDENT TIMELINE TABLE
-- ============================================================================
CREATE TABLE incident_timeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT NOT NULL,
    action_by_user_id BIGINT NULL,
    action_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_timeline_incident FOREIGN KEY (incident_id) REFERENCES emergency_incidents(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_timeline_user FOREIGN KEY (action_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_timeline_incident ON incident_timeline(incident_id);
CREATE INDEX idx_timeline_created ON incident_timeline(created_at);

-- ============================================================================
-- 12. NOTIFICATIONS TABLE
-- ============================================================================
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    incident_id BIGINT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_user_id) REFERENCES users(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_notifications_incident FOREIGN KEY (incident_id) REFERENCES emergency_incidents(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_notification_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ')),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_incident ON notifications(incident_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created ON notifications(created_at);

-- ============================================================================
-- 13. ANONYMOUS SOS TABLE
-- ============================================================================
CREATE TABLE anonymous_sos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sos_reference VARCHAR(50) NOT NULL,
    incident_id BIGINT NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'ANONYMOUS_SOS',
    emergency_category VARCHAR(50) NOT NULL DEFAULT 'EMERGENCY_SOS',
    latitude DECIMAL(10,8) NULL,
    longitude DECIMAL(11,8) NULL,
    location_status VARCHAR(20) NOT NULL DEFAULT 'UNAVAILABLE',
    location_description VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_anonymous_sos_ref UNIQUE (sos_reference),
    CONSTRAINT fk_anonymous_sos_incident FOREIGN KEY (incident_id) REFERENCES emergency_incidents(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_sos_location_status CHECK (location_status IN ('AVAILABLE', 'UNAVAILABLE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_anonymous_sos_ref ON anonymous_sos(sos_reference);
CREATE INDEX idx_anonymous_sos_incident ON anonymous_sos(incident_id);
CREATE INDEX idx_anonymous_sos_created ON anonymous_sos(created_at);

-- ============================================================================
-- 12. SYSTEM LOGS TABLE
-- ============================================================================
CREATE TABLE system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_level VARCHAR(10) NOT NULL,
    component_name VARCHAR(100) NOT NULL,
    log_message TEXT NOT NULL,
    exception_trace LONGTEXT NULL,
    user_id BIGINT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_syslogs_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_log_level CHECK (log_level IN ('INFO', 'WARN', 'ERROR', 'FATAL', 'DEBUG'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_syslogs_level ON system_logs(log_level);
CREATE INDEX idx_syslogs_component ON system_logs(component_name);
CREATE INDEX idx_syslogs_created ON system_logs(created_at);

-- ============================================================================
-- 13. AUDIT LOGS TABLE
-- ============================================================================
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    target_entity VARCHAR(50) NOT NULL,
    entity_id BIGINT NULL,
    details TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
