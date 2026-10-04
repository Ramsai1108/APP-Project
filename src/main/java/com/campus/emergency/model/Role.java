package com.campus.emergency.model;

public enum Role {
    USER,
    ADMIN,
    STUDENT,
    STAFF,
    DISPATCHER;

    public static Role fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) return USER;
        try {
            return Role.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public String getDisplayRole() {
        return (this == ADMIN) ? "ADMIN" : "USER";
    }
}
