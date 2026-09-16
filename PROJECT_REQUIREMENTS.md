# Project Requirements Specification
## SMART CAMPUS EMERGENCY & RESOURCE CO-ORDINATION SYSTEM

### 1. Overview
The Smart Campus Emergency & Resource Co-ordination System is a desktop management application designed to handle real-time emergency reporting, dispatcher triage, responder allocation, resource management, notifications, analytics, and system audit logging across campus environments.

### 2. Supported User Roles
1. **Student Users**: Report emergencies, upload evidence attachments, track emergency status, update personal safety profiles.
2. **Staff Users**: Report emergencies/hazards, view zone alerts, request facility emergency resources.
3. **Administrators**: User account lifecycle management, system settings, resource master catalog, audit log review.
4. **Emergency Dispatchers**: Emergency triage, priority score overrides, resource & responder dispatching, broadcast notifications.
5. **External Emergency Agencies**: Read/update access for escalated off-campus emergency coordination (Police, Fire, Ambulance).

### 3. Main System Modules (16 Modules)
1. **Authentication**: Username/email login, BCrypt password hashing, session management, RBAC control.
2. **User Registration**: Registration for students/staff, identity validation, emergency contact collection.
3. **User Profile**: Profile maintenance, medical alerts, blood group, dorm/office location details.
4. **Emergency Reporting**: SOS emergency submission, emergency types (Fire, Medical, Active Threat, Hazard, Security), location details.
5. **Photo/Video Attachment**: Attachment upload (PNG, JPG, MP4), size/type validation, SHA-256 hash integrity check.
6. **Emergency Priority Calculation**: Priority calculation algorithm combining incident severity, casualty count, hazard level, and location vulnerability.
7. **Incident Management**: Incident state lifecycle (`REPORTED`, `TRIAGED`, `DISPATCHED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `ESCALATED`).
8. **Resource Management**: Master inventory tracking for campus resources (Ambulances, First Aid Kits, AEDs, Fire Extinguishers, Patrol Cars).
9. **Responder Management**: Responder roster management, specialization, duty status (`ON_DUTY`, `DISPATCHED`, `BUSY`, `OFF_DUTY`).
10. **Resource Allocation**: Automatic recommendation and atomic locking of resources/responders to active incidents.
11. **Emergency Tracking**: Real-time incident status tracking, timeline events, responder ETA tracking.
12. **Notifications**: Multi-threaded async alerts (`ExecutorService`), campus wide bulletins, responder notifications.
13. **Admin Dashboard**: System metrics overview, user management, system health monitoring, master controls.
14. **Analytics**: Statistical reports (response time averages, incident frequency heatmaps, resource usage, export CSV/PDF).
15. **Audit Logging**: Immutable logging of all user activities, security events, triage actions, and resource allocations.
16. **System Error Logging**: Centralized error logging using SLF4J/Logback and DB log storage for system exceptions.

### 4. Technology Stack & Layer Architecture
- **Language**: Java (OOP, Concurrent API, Generics)
- **Presentation**: Java Swing (FlatLaf / Modern Custom Look & Feel)
- **Data Access**: JDBC + HikariCP Connection Pooling
- **Database**: MySQL 8.0+
- **Concurrency**: `ExecutorService`, `ScheduledExecutorService`
- **Build System**: Apache Maven
- **Logging**: SLF4J / Logback

#### 5-Layer Architectural Pattern
- **Layer 1**: External Actors & Clients
- **Layer 2**: Presentation Layer - Swing GUI
- **Layer 3**: Business Logic & Application Services
- **Layer 4**: Infrastructure & Shared Services
- **Layer 5**: Data Access & Persistence
