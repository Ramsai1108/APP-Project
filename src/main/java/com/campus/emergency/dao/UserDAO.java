package com.campus.emergency.dao;

import com.campus.emergency.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDAO {
    User create(User user);
    Optional<User> findById(int id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    boolean update(User user);
    boolean delete(int id);
}
