package com.campus.emergency.service;

import com.campus.emergency.dao.UserDAO;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.security.PasswordHasher;
import com.campus.emergency.util.ValidationUtils;

import java.util.Optional;
import java.util.logging.Logger;

public class IdentityService {
    private static final Logger LOGGER = Logger.getLogger(IdentityService.class.getName());
    private final UserDAO userDAO;

    public IdentityService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User register(String username, String password, String fullName, String email, String phone, Role role) throws AuthenticationException {
        if (!ValidationUtils.isNotNullOrEmpty(username)) {
            throw new AuthenticationException("Username cannot be empty");
        }
        if (password == null || password.length() < 6) {
            throw new AuthenticationException("Password must be at least 6 characters long");
        }
        if (!ValidationUtils.isNotNullOrEmpty(fullName)) {
            throw new AuthenticationException("Full name cannot be empty");
        }
        if (!ValidationUtils.isValidEmail(email)) {
            throw new AuthenticationException("Invalid email address format");
        }

        // Reject duplicate username
        Optional<User> existingUsername = userDAO.findByUsername(username.trim());
        if (existingUsername.isPresent()) {
            throw new AuthenticationException("Username already exists: " + username);
        }

        // Reject duplicate email
        Optional<User> existingEmail = userDAO.findByEmail(email.trim());
        if (existingEmail.isPresent()) {
            throw new AuthenticationException("Email address is already registered: " + email);
        }

        Role assignedRole = (role != null) ? role : Role.STUDENT;
        String securePasswordHash = PasswordHasher.hashPassword(password);

        User newUser = new User(0, username.trim(), securePasswordHash, fullName.trim(), email.trim(), phone != null ? phone.trim() : "", assignedRole);
        User createdUser = userDAO.create(newUser);

        LOGGER.info(String.format("User registration successful for username: %s with role: %s", createdUser.getUsername(), createdUser.getRole()));
        return createdUser;
    }

    public User login(String username, String password) throws AuthenticationException {
        if (!ValidationUtils.isNotNullOrEmpty(username) || password == null) {
            throw new AuthenticationException("Invalid username or password");
        }

        User user = userDAO.findByUsername(username.trim())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!PasswordHasher.verifyPassword(password, user.getPasswordHash())) {
            LOGGER.warning("Authentication failure for username: " + username);
            throw new AuthenticationException("Invalid credentials");
        }

        AuthenticationContext.setCurrentUser(user);
        LOGGER.info(String.format("User login successful: %s [%s]", user.getUsername(), user.getRole()));
        return user;
    }

    public void logout() {
        User current = AuthenticationContext.getCurrentUser();
        if (current != null) {
            LOGGER.info("User logged out: " + current.getUsername());
        }
        AuthenticationContext.clear();
    }
}
