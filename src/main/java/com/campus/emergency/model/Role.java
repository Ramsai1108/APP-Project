package com.campus.emergency.model;

public enum Role {
    STUDENT,
    STAFF,
    ADMIN,
    DISPATCHER;

    public static Role fromString(String roleStr) {
        if (roleStr == null) return STUDENT;
        try {
            return Role.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return STUDENT;
        }
    }
}
