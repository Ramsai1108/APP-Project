package com.campus.emergency.security;

import com.campus.emergency.model.User;
import com.campus.emergency.model.Role;
import java.time.LocalDateTime;

/**
 * Global Session Manager tracking active authenticated user session.
 */
public class SessionManager {

    private static User currentUser;
    private static LocalDateTime loginTime;

    private SessionManager() {}

    public static synchronized void createSession(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Cannot create session for null user");
        }
        currentUser = user;
        loginTime = LocalDateTime.now();
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized LocalDateTime getLoginTime() {
        return loginTime;
    }

    public static synchronized Role getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public static synchronized String getCurrentRoleName() {
        return currentUser != null ? currentUser.getRoleName() : null;
    }

    public static synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public static synchronized void destroySession() {
        currentUser = null;
        loginTime = null;
    }
}
