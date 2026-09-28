package com.campus.emergency.dao;

import com.campus.emergency.model.SystemLog;
import java.util.List;

public interface SystemLogDAO {
    void log(String eventType, Integer userId, String details);
    List<SystemLog> findAll();
    List<SystemLog> findByUserId(int userId);
}
