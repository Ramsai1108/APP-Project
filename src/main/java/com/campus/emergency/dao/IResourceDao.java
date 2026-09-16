package com.campus.emergency.dao;

import com.campus.emergency.model.Resource;
import java.util.List;

/**
 * Resource DAO interface.
 */
public interface IResourceDao extends IGenericDao<Resource, Long> {
    List<Resource> findByStatus(String status);
    List<Resource> findByTypeAndStatus(String type, String status);
}
