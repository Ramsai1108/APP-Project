package com.campus.emergency.dao;

import com.campus.emergency.model.Responder;
import java.util.List;
import java.util.Optional;

public interface ResponderDAO {
    Responder create(Responder responder);
    Optional<Responder> findById(int id);
    Optional<Responder> findByUserId(int userId);
    List<Responder> findAvailable();
    List<Responder> findAll();
    boolean updateStatus(int id, String status);
    boolean updateLocation(int id, int locationId);
}
