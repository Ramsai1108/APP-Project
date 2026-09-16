package com.campus.emergency.service;

/**
 * Multi-threaded notification delivery service interface.
 */
public interface INotificationService {
    void broadcastCampusAlert(String title, String message, String alertLevel);
    void sendTargetedAlert(Long userId, String title, String message);
}
