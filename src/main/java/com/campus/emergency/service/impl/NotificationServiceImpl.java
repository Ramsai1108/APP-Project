package com.campus.emergency.service.impl;

import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.service.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Multi-threaded notification dispatcher implementation.
 */
public class NotificationServiceImpl implements INotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Override
    public void broadcastCampusAlert(String title, String message, String alertLevel) {
        TaskExecutorManager.submitAsyncTask(() -> {
            logger.info("BROADCAST ALERT [{}]: {} - {}", alertLevel, title, message);
        });
    }

    @Override
    public void sendTargetedAlert(Long userId, String title, String message) {
        TaskExecutorManager.submitAsyncTask(() -> {
            logger.info("TARGETED ALERT [User {}]: {} - {}", userId, title, message);
        });
    }
}
