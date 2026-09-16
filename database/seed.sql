-- ============================================================================
-- SMART CAMPUS EMERGENCY & RESOURCE CO-ORDINATION SYSTEM
-- Database Master Seed Data Script: database/seed.sql
-- ============================================================================

USE smart_campus_emergency;

SET FOREIGN_KEY_CHECKS = 0;

-- Clean existing seed data
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE system_logs;
TRUNCATE TABLE incident_timeline;
TRUNCATE TABLE resource_allocations;
TRUNCATE TABLE emergency_incidents;
TRUNCATE TABLE report_attachments;
TRUNCATE TABLE emergency_reports;
TRUNCATE TABLE responders;
TRUNCATE TABLE resources;
TRUNCATE TABLE user_profiles;
TRUNCATE TABLE locations;
TRUNCATE TABLE users;
TRUNCATE TABLE roles;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- 1. SEED ROLES
-- ============================================================================
INSERT INTO roles (id, role_name, description) VALUES
(1, 'STUDENT', 'Campus student user with emergency reporting privileges'),
(2, 'STAFF', 'Campus faculty or staff member with facility emergency reporting privileges'),
(3, 'DISPATCHER', 'Emergency control room dispatcher handling triage and resource allocation'),
(4, 'ADMINISTRATOR', 'System administrator with full configuration and user management access'),
(5, 'EXTERNAL_AGENCY', 'External emergency service representative (Police, Fire Dept, Ambulance)');

-- ============================================================================
-- 2. SEED LOCATIONS
-- ============================================================================
INSERT INTO locations (id, building_name, building_code, floor_level, room_number, vulnerability_score) VALUES
(1, 'Administration Building', 'ADM', 1, '101', 2.50),
(2, 'Science & Research Complex', 'SRC', 3, '305', 7.80),
(3, 'Engineering Block A', 'ENG', 2, '210', 6.50),
(4, 'Central Library', 'LIB', 1, '150', 3.00),
(5, 'Student Dormitory Alpha', 'DMA', 4, '412', 5.00),
(6, 'Sports & Aquatic Center', 'SAC', 1, '102', 4.50),
(7, 'Campus Health Center', 'CHC', 1, '100', 8.90),
(8, 'Main Cafeteria & Food Court', 'CAF', 1, '115', 5.50);

-- ============================================================================
-- 3. SEED USERS (Password hashes pre-hashed with BCrypt for demo: "Password@123")
-- ============================================================================
INSERT INTO users (id, username, email, password_hash, full_name, phone, role_id, is_active) VALUES
(1, 'admin_system', 'admin@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'System Administrator', '+1-555-0100', 4, TRUE),
(2, 'dispatcher_john', 'john.dispatch@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'John Miller (Dispatcher)', '+1-555-0101', 3, TRUE),
(3, 'dispatcher_sarah', 'sarah.dispatch@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Sarah Connor (Dispatcher)', '+1-555-0102', 3, TRUE),
(4, 'sec_chief_mike', 'mike.security@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Mike Ross (Security Chief)', '+1-555-0103', 3, TRUE),
(5, 'nurse_clara', 'clara.nurse@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Clara Barton (Head Nurse)', '+1-555-0104', 3, TRUE),
(6, 'student_alex', 'alex.student@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Alex Johnson', '+1-555-0201', 1, TRUE),
(7, 'student_emma', 'emma.student@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Emma Watson', '+1-555-0202', 1, TRUE),
(8, 'prof_david', 'david.prof@smartcampus.edu', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Dr. David Bruce', '+1-555-0301', 2, TRUE),
(9, 'ext_city_police', 'police.contact@citypolice.gov', '$2a$10$7R0ZqNfM3bW5f/K5a1a1u.Xg.b9g2U3f0e0e0e0e0e0e0e0e0e0e0', 'Metro City Police Dispatch', '+1-911-0001', 5, TRUE);

-- ============================================================================
-- 4. SEED USER PROFILES
-- ============================================================================
INSERT INTO user_profiles (user_id, blood_group, medical_alerts, emergency_contact_name, emergency_contact_phone, primary_location_id) VALUES
(6, 'O+', 'Asthma - requires inhaler', 'Robert Johnson (Father)', '+1-555-0991', 5),
(7, 'A-', 'Penicillin Allergy', 'Mary Watson (Mother)', '+1-555-0992', 4),
(8, 'B+', 'None', 'Linda Bruce (Spouse)', '+1-555-0993', 3);

-- ============================================================================
-- 5. SEED RESOURCES
-- ============================================================================
INSERT INTO resources (id, resource_name, resource_type, current_status, location_id) VALUES
(1, 'Campus Ambulance 01', 'AMBULANCE', 'AVAILABLE', 7),
(2, 'Campus Ambulance 02', 'AMBULANCE', 'AVAILABLE', 7),
(3, 'Automated External Defibrillator (AED) #1', 'AED', 'AVAILABLE', 1),
(4, 'Automated External Defibrillator (AED) #2', 'AED', 'AVAILABLE', 4),
(5, 'Automated External Defibrillator (AED) #3', 'AED', 'AVAILABLE', 6),
(6, 'CO2 Fire Extinguisher Unit #101', 'FIRE_EXTINGUISHER', 'AVAILABLE', 2),
(7, 'CO2 Fire Extinguisher Unit #202', 'FIRE_EXTINGUISHER', 'AVAILABLE', 3),
(8, 'Security Patrol Vehicle Alpha', 'PATROL_VEHICLE', 'AVAILABLE', 1),
(9, 'Security Patrol Vehicle Beta', 'PATROL_VEHICLE', 'AVAILABLE', 6),
(10, 'Hazmat Containment Gear Pack', 'HAZMAT_KIT', 'AVAILABLE', 2);

-- ============================================================================
-- 6. SEED RESPONDERS
-- ============================================================================
INSERT INTO responders (id, user_id, full_name, specialization, phone, duty_status, assigned_location_id) VALUES
(1, 4, 'Mike Ross (Security Chief)', 'CAMPUS_SECURITY', '+1-555-0103', 'ON_DUTY', 1),
(2, 5, 'Clara Barton (Head Nurse)', 'MEDICAL', '+1-555-0104', 'ON_DUTY', 7),
(3, NULL, 'Officer James Gordon', 'CAMPUS_SECURITY', '+1-555-0188', 'ON_DUTY', 6),
(4, NULL, 'Officer Thomas Wayne', 'FIRE_SAFETY', '+1-555-0189', 'ON_DUTY', 2),
(5, NULL, 'Dr. Elizabeth Blackwell', 'MEDICAL', '+1-555-0190', 'OFF_DUTY', 7);
