package com.campus.emergency.service.impl;

import com.campus.emergency.dao.IUserDao;
import com.campus.emergency.model.User;
import com.campus.emergency.service.IAuthService;
import java.util.Optional;

/**
 * Authentication service implementation stub.
 */
public class AuthServiceImpl implements IAuthService {

    private final IUserDao userDao;

    public AuthServiceImpl(IUserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public Optional<User> login(String usernameOrEmail, String password) {
        return Optional.empty();
    }

    @Override
    public boolean logout(Long userId) {
        return true;
    }

    @Override
    public boolean validatePermission(Long userId, String permission) {
        return true;
    }
}
