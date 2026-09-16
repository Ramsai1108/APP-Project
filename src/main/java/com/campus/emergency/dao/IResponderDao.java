package com.campus.emergency.dao;

import com.campus.emergency.model.Responder;
import java.util.List;

/**
 * Responder DAO interface.
 */
public interface IResponderDao extends IGenericDao<Responder, Long> {
    List<Responder> findByDutyStatus(String dutyStatus);
    List<Responder> findAvailableBySpecialization(String specialization);
}
