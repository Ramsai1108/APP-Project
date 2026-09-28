package com.campus.emergency.dao;

import com.campus.emergency.model.Resource;
import java.util.List;
import java.util.Optional;

public interface ResourceDAO {
    Resource create(Resource resource);
    Optional<Resource> findById(int id);
    List<Resource> findAvailable();
    List<Resource> findAll();
    boolean updateStatus(int id, String status);
    boolean assignToIncident(int resourceId, int incidentId);
    boolean releaseResource(int resourceId);
}
