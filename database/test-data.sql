-- ============================================================================
-- SMART CAMPUS EMERGENCY & RESOURCE CO-ORDINATION SYSTEM
-- Database Test Data Script: database/test-data.sql
-- ============================================================================

USE smart_campus_emergency;

-- ============================================================================
-- 1. TEST EMERGENCY REPORTS
-- ============================================================================
INSERT INTO emergency_reports (id, reporter_id, title, description, category, severity, location, location_id, affected_people, media_path, status, reported_at) VALUES
(1, 6, 'Chemical Lab Explosion', 'Chemical lab explosion and dense smoke outbreak in Science Complex Room 305.', 'FIRE', 4, 'Science Complex Room 305', 2, 2, '/storage/attachments/2026/09/lab_fire_smoke.jpg', 'SUBMITTED', NOW() - INTERVAL 25 MINUTE),
(2, 7, 'Unconscious Student in Library', 'Student collapsed in Central Library Room 150, unresponsive with weak pulse.', 'MEDICAL', 2, 'Central Library Room 150', 4, 1, NULL, 'SUBMITTED', NOW() - INTERVAL 15 MINUTE),
(3, 8, 'Armed Trespasser Spotted', 'Suspicious trespasser with potential weapon spotted near Engineering Block A Room 210.', 'SECURITY', 5, 'Engineering Block A Room 210', 3, 0, '/storage/attachments/2026/09/suspicious_activity.mp4', 'SUBMITTED', NOW() - INTERVAL 5 MINUTE);

-- ============================================================================
-- 2. TEST REPORT ATTACHMENTS
-- ============================================================================
INSERT INTO report_attachments (id, report_id, file_name, file_path, mime_type, file_size_bytes, sha256_hash, uploaded_at) VALUES
(1, 1, 'lab_fire_smoke.jpg', '/storage/attachments/2026/09/lab_fire_smoke.jpg', 'image/jpeg', 2450800, 'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0', NOW() - INTERVAL 24 MINUTE),
(2, 3, 'suspicious_activity.mp4', '/storage/attachments/2026/09/suspicious_activity.mp4', 'video/mp4', 18450900, 'f6e5d4c3b2a109876543210fedcba9876543210fedcba9876543210fedcba987', NOW() - INTERVAL 4 MINUTE);

-- ============================================================================
-- 3. TEST EMERGENCY INCIDENTS
-- ============================================================================
INSERT INTO emergency_incidents (id, report_id, calculated_priority, override_priority, status, triaged_by_user_id, triaged_at, created_at) VALUES
(1, 1, 88.50, NULL, 'IN_PROGRESS', 2, NOW() - INTERVAL 20 MINUTE, NOW() - INTERVAL 25 MINUTE),
(2, 2, 62.00, 75.00, 'DISPATCHED', 3, NOW() - INTERVAL 12 MINUTE, NOW() - INTERVAL 15 MINUTE),
(3, 3, 96.00, NULL, 'ESCALATED', 2, NOW() - INTERVAL 3 MINUTE, NOW() - INTERVAL 5 MINUTE);

-- ============================================================================
-- 4. UPDATE RESOURCE & RESPONDER STATUSES TO REFLECT ACTIVE TEST INCIDENTS
-- ============================================================================
UPDATE resources SET current_status = 'IN_USE' WHERE id IN (1, 6, 8);
UPDATE responders SET duty_status = 'DISPATCHED' WHERE id IN (1, 2, 4);

-- ============================================================================
-- 5. TEST RESOURCE ALLOCATIONS
-- ============================================================================
INSERT INTO resource_allocations (id, incident_id, resource_id, responder_id, allocated_by_user_id, allocated_at, status) VALUES
(1, 1, 6, 4, 2, NOW() - INTERVAL 18 MINUTE, 'ACTIVE'),
(2, 1, 8, 1, 2, NOW() - INTERVAL 18 MINUTE, 'ACTIVE'),
(3, 2, 1, 2, 3, NOW() - INTERVAL 10 MINUTE, 'ACTIVE');

-- ============================================================================
-- 6. TEST INCIDENT TIMELINE
-- ============================================================================
INSERT INTO incident_timeline (id, incident_id, action_by_user_id, action_type, description, created_at) VALUES
(1, 1, 6, 'REPORT_SUBMITTED', 'Emergency SOS reported by Alex Johnson.', NOW() - INTERVAL 25 MINUTE),
(2, 1, 2, 'TRIAGED', 'Dispatcher John Miller triaged incident. Initial priority score calculated at 88.50.', NOW() - INTERVAL 20 MINUTE),
(3, 1, 2, 'RESOURCED_DISPATCHED', 'Dispatched Fire Officer Thomas Wayne and Security Patrol Vehicle Alpha.', NOW() - INTERVAL 18 MINUTE);

-- ============================================================================
-- 7. TEST SYSTEM LOGS
-- ============================================================================
INSERT INTO system_logs (id, log_level, component_name, log_message, exception_trace, user_id, ip_address, created_at) VALUES
(1, 'INFO', 'AuthServiceImpl', 'User admin_system logged in successfully.', NULL, 1, '127.0.0.1', NOW() - INTERVAL 60 MINUTE),
(2, 'INFO', 'IncidentServiceImpl', 'New emergency report created with ID 1.', NULL, 6, '192.168.1.45', NOW() - INTERVAL 25 MINUTE);

-- ============================================================================
-- 8. TEST AUDIT LOGS
-- ============================================================================
INSERT INTO audit_logs (id, user_id, action, target_entity, entity_id, details, ip_address, created_at) VALUES
(1, 1, 'LOGIN_SUCCESS', 'USER', 1, 'Admin logged into system console', '127.0.0.1', NOW() - INTERVAL 60 MINUTE),
(2, 6, 'EMERGENCY_REPORT_CREATE', 'EMERGENCY_REPORT', 1, 'Reported FIRE emergency at Science Complex Room 305', '192.168.1.45', NOW() - INTERVAL 25 MINUTE);
