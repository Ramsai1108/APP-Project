package com.campus.emergency.service.impl;

import com.campus.emergency.dao.IUserDao;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.logging.AuditLogger;
import com.campus.emergency.model.User;
import com.campus.emergency.security.PasswordUtil;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Implementation of IdentityService performing user authentication and session management.
 */
public class IdentityServiceImpl implements IdentityService {

    private static final Logger logger = LoggerFactory.getLogger(IdentityServiceImpl.class);

    private final IUserDao userDao;

    public IdentityServiceImpl(IUserDao userDao) {
        this.userDao = userDao;
    }

    public IdentityServiceImpl() {
        this(new com.campus.emergency.dao.impl.UserDaoImpl());
    }

    @Override
    public User authenticate(String email, String plainPassword) {
        logger.info("Attempting authentication for email: {}", email);

        if (email == null || email.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            logger.warn("Authentication failed: Empty email or password submitted.");
            AuditLogger.log(null, "LOGIN_FAILED", "USER", "Empty credentials submitted for email: " + email);
            throw new AuthenticationException("Email and password must not be empty.");
        }

        Optional<User> userOpt;
        try {
            userOpt = userDao.findByEmailWithRole(email);
        } catch (DatabaseAccessException e) {
            logger.error("Authentication halted due to database connection error", e);
            throw e;
        }

        if (userOpt.isEmpty()) {
            logger.warn("Authentication failed: Nonexistent user account for email: {}", email);
            AuditLogger.log(null, "LOGIN_FAILED", "USER", "User not found for email: " + email);
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            logger.warn("Authentication failed: User account ID {} is deactivated.", user.getId());
            AuditLogger.log(user.getId(), "LOGIN_FAILED", "USER", "Account disabled for user: " + user.getUsername());
            throw new AuthenticationException("User account has been deactivated. Contact System Administrator.");
        }

        boolean passwordMatch = PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash());
        if (!passwordMatch) {
            logger.warn("Authentication failed: Password mismatch for user ID {}", user.getId());
            AuditLogger.log(user.getId(), "LOGIN_FAILED", "USER", "Incorrect password for user: " + user.getUsername());
            throw new AuthenticationException("Invalid email or password.");
        }

        // Authentication Successful -> Initialize Session
        SessionManager.createSession(user);
        logger.info("Authentication SUCCESSFUL for user ID: {} [Role: {}]", user.getId(), user.getRoleName());
        AuditLogger.log(user.getId(), "LOGIN_SUCCESS", "USER", "User logged in successfully with role: " + user.getRoleName());

        return user;
    }

    @Override
    public void logout() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            logger.info("User ID {} logged out.", currentUser.getId());
            AuditLogger.log(currentUser.getId(), "LOGOUT", "USER", "User session destroyed");
        }
        SessionManager.destroySession();
    }

    @Override
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(SessionManager.getCurrentUser());
    }
}
