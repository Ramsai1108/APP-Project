package com.campus.emergency.service;

import com.campus.emergency.dao.IUserDao;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.security.PasswordUtil;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.impl.IdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceTest {

    private IdentityService identityService;
    private MockUserDao mockUserDao;

    @BeforeEach
    public void setUp() {
        SessionManager.destroySession();
        mockUserDao = new MockUserDao();
        identityService = new IdentityServiceImpl(mockUserDao);
    }

    @Test
    public void testSuccessfulLogin() {
        // Arrange
        String rawPassword = "Password@123";
        String hashedPassword = PasswordUtil.hashPassword(rawPassword);

        Role studentRole = new Role(1L, "STUDENT", "Student Role");
        User testUser = new User(10L, "student_alex", "alex.student@smartcampus.edu", hashedPassword, "Alex Johnson", "+1-555-0201", 1L, true);
        testUser.setRole(studentRole);

        mockUserDao.addUser(testUser);

        // Act
        User authenticatedUser = identityService.authenticate("alex.student@smartcampus.edu", rawPassword);

        // Assert
        assertNotNull(authenticatedUser, "Authenticated user should not be null");
        assertEquals("alex.student@smartcampus.edu", authenticatedUser.getEmail());
        assertEquals("STUDENT", authenticatedUser.getRoleName());
        assertTrue(SessionManager.isAuthenticated(), "SessionManager should report authenticated");
        assertEquals(authenticatedUser.getId(), SessionManager.getCurrentUser().getId());
    }

    @Test
    public void testIncorrectPassword() {
        // Arrange
        String hashedPassword = PasswordUtil.hashPassword("CorrectPassword123");
        Role staffRole = new Role(2L, "STAFF", "Staff Role");
        User testUser = new User(11L, "prof_david", "david.prof@smartcampus.edu", hashedPassword, "Dr. David", "+1-555-0301", 2L, true);
        testUser.setRole(staffRole);

        mockUserDao.addUser(testUser);

        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            identityService.authenticate("david.prof@smartcampus.edu", "WrongPassword456");
        });

        assertEquals("Invalid email or password.", exception.getMessage());
        assertFalse(SessionManager.isAuthenticated(), "SessionManager should remain unauthenticated on failure");
    }

    @Test
    public void testNonexistentUser() {
        // Act & Assert
        AuthenticationException exception = assertThrows(AuthenticationException.class, () -> {
            identityService.authenticate("unknown.user@smartcampus.edu", "AnyPassword123");
        });

        assertEquals("Invalid email or password.", exception.getMessage());
        assertFalse(SessionManager.isAuthenticated());
    }

    @Test
    public void testDatabaseFailure() {
        // Arrange
        mockUserDao.setSimulateDatabaseFailure(true);

        // Act & Assert
        DatabaseAccessException exception = assertThrows(DatabaseAccessException.class, () -> {
            identityService.authenticate("any.email@smartcampus.edu", "AnyPassword");
        });

        assertTrue(exception.getMessage().contains("Database Connection Failure"), "Exception message should indicate DB failure");
        assertFalse(SessionManager.isAuthenticated());
    }

    @Test
    public void testLogout() {
        // Arrange
        String hashedPassword = PasswordUtil.hashPassword("Pass123");
        User user = new User(1L, "admin", "admin@smartcampus.edu", hashedPassword, "Admin User", "123", 4L, true);
        user.setRole(new Role(4L, "ADMINISTRATOR", "Admin"));
        mockUserDao.addUser(user);

        identityService.authenticate("admin@smartcampus.edu", "Pass123");
        assertTrue(SessionManager.isAuthenticated());

        // Act
        identityService.logout();

        // Assert
        assertFalse(SessionManager.isAuthenticated());
        assertNull(SessionManager.getCurrentUser());
    }

    // =========================================================================
    // Mock IUserDao Inner Class
    // =========================================================================
    private static class MockUserDao implements IUserDao {
        private final List<User> users = new ArrayList<>();
        private boolean simulateDatabaseFailure = false;

        public void addUser(User user) {
            users.add(user);
        }

        public void setSimulateDatabaseFailure(boolean simulateDatabaseFailure) {
            this.simulateDatabaseFailure = simulateDatabaseFailure;
        }

        @Override
        public Optional<User> findByEmailWithRole(String email) {
            if (simulateDatabaseFailure) {
                throw new DatabaseAccessException("Database Connection Failure: Communications link failure", new java.sql.SQLException());
            }
            return users.stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return findByEmailWithRole(email);
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public User create(User entity) {
            users.add(entity);
            return entity;
        }

        @Override
        public Optional<User> findById(Long id) {
            return users.stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users);
        }

        @Override
        public boolean update(User entity) {
            return true;
        }

        @Override
        public boolean deleteById(Long id) {
            return true;
        }
    }
}
