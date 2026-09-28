-- Campus Emergency Response System Initial Seed Data
USE campus_emergency_db;

-- Insert Roles
INSERT INTO roles (id, name, description) VALUES
(1, 'STUDENT', 'Campus Student User'),
(2, 'STAFF', 'Campus Staff Member'),
(3, 'ADMIN', 'System Administrator'),
(4, 'DISPATCHER', 'Emergency Dispatch Officer')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Sample Users (Passwords hashed using SHA-256 for demo; initial raw password is 'password123')
-- SHA256 of 'password123' = ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f
INSERT INTO users (id, username, password_hash, full_name, email, phone, role_id) VALUES
(1, 'admin', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'System Admin', 'admin@campus.edu', '555-0100', 3),
(2, 'staff_john', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'John Doe (Staff)', 'john.staff@campus.edu', '555-0101', 2),
(3, 'student_jane', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Jane Smith (Student)', 'jane.student@campus.edu', '555-0102', 1),
(4, 'dispatcher_mike', 'ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f', 'Mike Officer (Dispatcher)', 'mike.dispatcher@campus.edu', '555-0103', 4)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Insert Locations
INSERT INTO locations (id, building_name, floor, room_number, latitude, longitude, description) VALUES
(1, 'Science Hall', '2', '204', 37.77490000, -122.41940000, 'Chemistry Lab Area'),
(2, 'Student Union', '1', 'Lobby', 37.77510000, -122.41900000, 'Main Entrance Desk'),
(3, 'North Residence Hall', '3', '312', 37.77600000, -122.41850000, 'Dormitory Wing B')
ON DUPLICATE KEY UPDATE building_name=VALUES(building_name);

-- Insert Responder
INSERT INTO responders (id, user_id, badge_number, specialization, status, current_location_id) VALUES
(1, 4, 'BADGE-101', 'First Aid & Security', 'AVAILABLE', 2)
ON DUPLICATE KEY UPDATE badge_number=VALUES(badge_number);

-- Insert Resource
INSERT INTO resources (id, name, type, status, location_id) VALUES
(1, 'Campus Ambulance 1', 'AMBULANCE', 'AVAILABLE', 2),
(2, 'Science Hall Defibrillator', 'FIRST_AID_KIT', 'AVAILABLE', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert System Log
INSERT INTO system_logs (event_type, user_id, details) VALUES
('SYSTEM_INIT', 1, 'Initial seed data populated successfully.');
