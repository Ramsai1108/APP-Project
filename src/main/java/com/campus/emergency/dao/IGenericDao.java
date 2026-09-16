package com.campus.emergency.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface exposing CRUD contracts.
 *
 * @param <T>  Entity type
 * @param <ID> Primary key type
 */
public interface IGenericDao<T, ID> {
    T create(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    boolean update(T entity);
    boolean deleteById(ID id);
}
