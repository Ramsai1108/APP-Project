package com.campus.emergency.security;

import com.campus.emergency.config.DatabaseConfig;
import com.campus.emergency.dao.UserDAO;
import com.campus.emergency.dao.jdbc.JdbcUserDAO;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.infrastructure.ConnectionPoolManager;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.service.IdentityService;
import org.junit.jupiter.api.*;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Phase3AuthenticationTest {

    private static ConnectionPoolManager poolManager;
    private static UserDAO userDAO;
    private static IdentityService identityService;
    private static AuthorizationService authorizationService;
    private static boolean isDbAvailable = false;

    @BeforeAll
    public static void setUp() {
        authorizationService = new AuthorizationService();

        DatabaseConfig config = new DatabaseConfig();
        try {
            poolManager = ConnectionPoolManager.getInstance(config);
            try (Connection conn = poolManager.getConnection()) {
                if (conn != null && !conn.isClosed()) {
                    isDbAvailable = true;
                }
            }
        } catch (Exception e) {
            isDbAvailable = false;
        }

        if (isDbAvailable) {
            userDAO = new JdbcUserDAO(poolManager);
            identityService = new IdentityService(userDAO);
        }
    }

    @AfterEach
    public void tearDownSession() {
        AuthenticationContext.clear();
    }

    // --- Unit Tests for Password Hashing & Security (Runs Always) ---

    @Test
    @Order(1)
    @DisplayName("1. Password Hashing Succeeds")
    public void test1_passwordHashingSucceeds() {
        String hash = PasswordHasher.hashPassword("mySecretPassword123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("PBKDF2:"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Same Password Verifies Successfully")
    public void test2_samePasswordVerifiesSuccessfully() {
        String password = "mySecretPassword123";
        String hash = PasswordHasher.hashPassword(password);
        assertTrue(PasswordHasher.verifyPassword(password, hash));
    }

    @Test
    @Order(3)
    @DisplayName("3. Incorrect Password Fails Verification")
    public void test3_incorrectPasswordFails() {
        String hash = PasswordHasher.hashPassword("mySecretPassword123");
        assertFalse(PasswordHasher.verifyPassword("wrongPassword456", hash));
    }

    @Test
    @Order(4)
    @DisplayName("4. Different Passwords Do Not Verify Crosswise")
    public void test4_differentPasswordsDoNotVerify() {
        String hash1 = PasswordHasher.hashPassword("PasswordAlpha123");
        String hash2 = PasswordHasher.hashPassword("PasswordBeta456");
        assertFalse(PasswordHasher.verifyPassword("PasswordAlpha123", hash2));
        assertFalse(PasswordHasher.verifyPassword("PasswordBeta456", hash1));
    }

    @Test
    @Order(5)
    @DisplayName("5. Each Password Receives a Unique Salt")
    public void test5_uniqueSaltPerHash() {
        String password = "samePassword123";
        String hash1 = PasswordHasher.hashPassword(password);
        String hash2 = PasswordHasher.hashPassword(password);

        assertNotEquals(hash1, hash2, "Hashing identical passwords should produce distinct salted hashes");
        assertTrue(PasswordHasher.verifyPassword(password, hash1));
        assertTrue(PasswordHasher.verifyPassword(password, hash2));
    }

    @Test
    @Order(6)
    @DisplayName("6. Plaintext Password Is Never Stored or Exposed")
    public void test6_plaintextPasswordNeverStored() {
        String rawPassword = "rawPassword999";
        String hash = PasswordHasher.hashPassword(rawPassword);

        assertFalse(hash.contains(rawPassword), "Hash format must not contain raw plaintext password");
        User user = new User(10, "testuser", hash, "Test User", "test@campus.edu", "555-0000", Role.STUDENT);
        assertFalse(user.toString().contains(rawPassword), "User.toString() must never contain raw password");
        assertFalse(user.toString().contains(hash), "User.toString() must not include password hash");
    }

    // --- Authorization Unit Tests (Runs Always) ---

    @Test
    @Order(12)
    @DisplayName("12. Student Authorization Rules")
    public void test12_studentAuthorizationRules() {
        User student = new User(1, "student_user", "hash", "Student User", "student@campus.edu", "555-111", Role.STUDENT);

        assertTrue(authorizationService.isStudent(student));
        assertTrue(authorizationService.canReportEmergency(student));
        assertFalse(authorizationService.isStaff(student));
        assertFalse(authorizationService.isDispatcher(student));
        assertFalse(authorizationService.isAdmin(student));
        assertFalse(authorizationService.canAccessAdminConsole(student));
        assertFalse(authorizationService.canDispatchResources(student));
    }

    @Test
    @Order(13)
    @DisplayName("13. Staff Authorization Rules")
    public void test13_staffAuthorizationRules() {
        User staff = new User(2, "staff_user", "hash", "Staff User", "staff@campus.edu", "555-222", Role.STAFF);

        assertTrue(authorizationService.isStaff(staff));
        assertTrue(authorizationService.canReportEmergency(staff));
        assertFalse(authorizationService.isDispatcher(staff));
        assertFalse(authorizationService.isAdmin(staff));
        assertFalse(authorizationService.canAccessAdminConsole(staff));
    }

    @Test
    @Order(14)
    @DisplayName("14. Dispatcher Authorization Rules")
    public void test14_dispatcherAuthorizationRules() {
        User dispatcher = new User(3, "dispatcher_user", "hash", "Dispatcher User", "dispatcher@campus.edu", "555-333", Role.DISPATCHER);

        assertTrue(authorizationService.isDispatcher(dispatcher));
        assertTrue(authorizationService.canDispatchResources(dispatcher));
        assertTrue(authorizationService.isStaff(dispatcher));
        assertTrue(authorizationService.canReportEmergency(dispatcher));
        assertFalse(authorizationService.isAdmin(dispatcher));
        assertFalse(authorizationService.canAccessAdminConsole(dispatcher));
    }

    @Test
    @Order(15)
    @DisplayName("15. Admin Authorization Rules")
    public void test15_adminAuthorizationRules() {
        User admin = new User(4, "admin_user", "hash", "Admin User", "admin@campus.edu", "555-444", Role.ADMIN);

        assertTrue(authorizationService.isAdmin(admin));
        assertTrue(authorizationService.canAccessAdminConsole(admin));
        assertTrue(authorizationService.isDispatcher(admin));
        assertTrue(authorizationService.canDispatchResources(admin));
        assertTrue(authorizationService.isStaff(admin));
        assertTrue(authorizationService.canReportEmergency(admin));
    }

    @Test
    @Order(16)
    @DisplayName("16. Unauthenticated Access Is Rejected")
    public void test16_unauthenticatedAccessRejected() {
        assertThrows(AuthenticationException.class, () -> authorizationService.requireAuthenticated(null));
        assertThrows(AuthenticationException.class, () -> authorizationService.requireAdmin(null));
        assertThrows(AuthenticationException.class, () -> authorizationService.requireDispatcher(null));
        assertThrows(AuthenticationException.class, () -> authorizationService.requireRole(null, Role.STUDENT));

        User student = new User(1, "student_user", "hash", "Student User", "student@campus.edu", "555-111", Role.STUDENT);
        assertThrows(AuthenticationException.class, () -> authorizationService.requireAdmin(student));
        assertThrows(AuthenticationException.class, () -> authorizationService.requireDispatcher(student));
    }

    // --- Integration Tests against MySQL Database ---

    @Test
    @Order(7)
    @DisplayName("7. Registration Succeeds with Valid Data")
    public void test7_registrationSucceeds() throws AuthenticationException {
        assumeTrue(isDbAvailable, "Database required for registration integration test");

        String username = "reg_user_" + System.currentTimeMillis();
        String email = username + "@campus.edu";
        User registered = identityService.register(username, "securePass123", "Registered User", email, "555-777", Role.STUDENT);

        assertNotNull(registered);
        assertTrue(registered.getId() > 0);
        assertEquals(username, registered.getUsername());
        assertEquals(Role.STUDENT, registered.getRole());
        assertTrue(PasswordHasher.verifyPassword("securePass123", registered.getPasswordHash()));
    }

    @Test
    @Order(8)
    @DisplayName("8. Duplicate Registration Is Rejected")
    public void test8_duplicateRegistrationRejected() throws AuthenticationException {
        assumeTrue(isDbAvailable, "Database required for duplicate registration test");

        String username = "dup_user_" + System.currentTimeMillis();
        String email = username + "@campus.edu";
        identityService.register(username, "securePass123", "Dup User", email, "555-888", Role.STUDENT);

        // Attempt duplicate username
        assertThrows(AuthenticationException.class, () -> {
            identityService.register(username, "differentPass123", "Other Name", "unique_" + email, "555-889", Role.STUDENT);
        });

        // Attempt duplicate email
        assertThrows(AuthenticationException.class, () -> {
            identityService.register("unique_" + username, "differentPass123", "Other Name", email, "555-889", Role.STUDENT);
        });
    }

    @Test
    @Order(9)
    @DisplayName("9. Valid Login Succeeds and Sets Context")
    public void test9_validLoginSucceeds() throws AuthenticationException {
        assumeTrue(isDbAvailable, "Database required for login integration test");

        String username = "login_user_" + System.currentTimeMillis();
        String email = username + "@campus.edu";
        String password = "validLoginPass123";
        identityService.register(username, password, "Login User", email, "555-999", Role.STAFF);

        User loggedIn = identityService.login(username, password);
        assertNotNull(loggedIn);
        assertEquals(username, loggedIn.getUsername());

        assertTrue(AuthenticationContext.isAuthenticated());
        assertEquals(username, AuthenticationContext.getCurrentUser().getUsername());
        assertEquals(Role.STAFF, AuthenticationContext.getCurrentUser().getRole());
    }

    @Test
    @Order(10)
    @DisplayName("10. Invalid Password Is Rejected")
    public void test10_invalidPasswordRejected() throws AuthenticationException {
        assumeTrue(isDbAvailable, "Database required for invalid password test");

        String username = "invpass_user_" + System.currentTimeMillis();
        String email = username + "@campus.edu";
        identityService.register(username, "correctPass123", "Inv Pass User", email, "555-101", Role.STUDENT);

        assertThrows(AuthenticationException.class, () -> {
            identityService.login(username, "wrongPass456");
        });
        assertFalse(AuthenticationContext.isAuthenticated());
    }

    @Test
    @Order(11)
    @DisplayName("11. Unknown User Is Rejected")
    public void test11_unknownUserRejected() {
        assumeTrue(isDbAvailable, "Database required for unknown user test");

        assertThrows(AuthenticationException.class, () -> {
            identityService.login("non_existent_user_99999", "somePassword123");
        });
        assertFalse(AuthenticationContext.isAuthenticated());
    }

    @Test
    @Order(17)
    @DisplayName("17. Admin Registration and Login Succeeds")
    public void test17_adminRegistrationAndLoginSucceeds() throws AuthenticationException {
        assumeTrue(isDbAvailable, "Database required for admin registration test");

        String username = "admin_reg_" + System.currentTimeMillis();
        String email = username + "@campus.edu";
        String password = "adminPass123";

        User adminUser = identityService.register(username, password, "Admin Reg User", email, "555-4321", Role.ADMIN);
        assertNotNull(adminUser);
        assertEquals(Role.ADMIN, adminUser.getRole());

        User loggedIn = identityService.login(username, password);
        assertNotNull(loggedIn);
        assertEquals(Role.ADMIN, loggedIn.getRole());
        assertTrue(authorizationService.canAccessAdminConsole(loggedIn));
    }

    @Test
    @Order(18)
    @DisplayName("18. Invalid Role Rejection")
    public void test18_invalidRoleRejection() {
        if (!isDbAvailable) return;

        assertThrows(AuthenticationException.class, () -> {
            identityService.register("badrole_user", "password123", "Bad Role", "badrole@campus.edu", "555-000", "SUPER_ADMIN");
        });

        assertThrows(AuthenticationException.class, () -> {
            identityService.register("nullrole_user", "password123", "Null Role", "nullrole@campus.edu", "555-000", (Role) null);
        });
    }
}
