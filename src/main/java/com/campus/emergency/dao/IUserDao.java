package com.campus.emergency.dao;

import com.campus.emergency.model.User;
import java.util.Optional;

/**
 * User Data Access Object Interface.
 */
public interface IUserDao extends IGenericDao<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailWithRole(String email);
}
