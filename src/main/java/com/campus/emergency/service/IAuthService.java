package com.campus.emergency.service;

import com.campus.emergency.model.User;
import java.util.Optional;

/**
 * Authentication service interface.
 */
public interface IAuthService {
    Optional<User> login(String usernameOrEmail, String password);
    boolean logout(Long userId);
    boolean validatePermission(Long userId, String permission);
}
