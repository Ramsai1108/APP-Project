package com.campus.emergency.security;

import com.campus.emergency.model.User;

/**
 * Thread-safe active user session context manager.
 */
public class UserSessionContext {

    private static User currentUser;

    private UserSessionContext() {}

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized void clearSession() {
        currentUser = null;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }
}
