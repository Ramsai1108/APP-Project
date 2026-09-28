package com.campus.emergency.security;

import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;

public class AuthorizationService {

    public boolean hasRole(User user, Role requiredRole) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        return user.getRole() == requiredRole;
    }

    public boolean isAdmin(User user) {
        return hasRole(user, Role.ADMIN);
    }

    public boolean isDispatcher(User user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole() == Role.DISPATCHER || user.getRole() == Role.ADMIN;
    }

    public boolean isStaff(User user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole() == Role.STAFF || user.getRole() == Role.DISPATCHER || user.getRole() == Role.ADMIN;
    }

    public boolean isStudent(User user) {
        return user != null && user.getRole() != null;
    }

    public boolean canAccessAdminConsole(User user) {
        return isAdmin(user);
    }

    public boolean canDispatchResources(User user) {
        return isDispatcher(user);
    }

    public boolean canReportEmergency(User user) {
        return user != null && user.getRole() != null;
    }

    public void requireAuthenticated(User user) throws AuthenticationException {
        if (user == null) {
            throw new AuthenticationException("Authentication required. Unauthenticated access rejected.");
        }
    }

    public void requireRole(User user, Role requiredRole) throws AuthenticationException {
        requireAuthenticated(user);
        if (!hasRole(user, requiredRole)) {
            throw new AuthenticationException(String.format("Access denied. Role %s is required.", requiredRole));
        }
    }

    public void requireAdmin(User user) throws AuthenticationException {
        requireAuthenticated(user);
        if (!isAdmin(user)) {
            throw new AuthenticationException("Access denied. Admin authorization required.");
        }
    }

    public void requireDispatcher(User user) throws AuthenticationException {
        requireAuthenticated(user);
        if (!isDispatcher(user)) {
            throw new AuthenticationException("Access denied. Dispatcher authorization required.");
        }
    }
}
