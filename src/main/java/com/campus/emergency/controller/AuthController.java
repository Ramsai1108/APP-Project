package com.campus.emergency.controller;

import com.campus.emergency.model.User;
import com.campus.emergency.service.IAuthService;
import java.util.Optional;

/**
 * Controller mediating authentication UI interactions and service calls.
 */
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    public Optional<User> handleLogin(String usernameOrEmail, String password) {
        return authService.login(usernameOrEmail, password);
    }

    public boolean handleLogout(Long userId) {
        return authService.logout(userId);
    }
}
