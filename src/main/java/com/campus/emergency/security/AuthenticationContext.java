package com.campus.emergency.security;

import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;

public class AuthenticationContext {
    private static User currentUser;

    public static synchronized void setCurrentUser(User user) {
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isAuthenticated() {
        return currentUser != null;
    }

    public static synchronized boolean hasRole(Role requiredRole) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        return currentUser.getRole() == requiredRole;
    }

    public static synchronized void clear() {
        currentUser = null;
    }
}
