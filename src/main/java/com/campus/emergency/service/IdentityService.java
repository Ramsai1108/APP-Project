package com.campus.emergency.service;

import com.campus.emergency.model.User;
import java.util.Optional;

/**
 * Service interface handling identity verification, authentication, and session control.
 */
public interface IdentityService {
    User authenticate(String email, String plainPassword);
    void logout();
    Optional<User> getCurrentUser();
}
