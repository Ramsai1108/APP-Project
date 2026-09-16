package com.campus.emergency.concurrency;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled Executor Service Manager for periodic auto-escalations and background monitoring.
 */
public class ScheduledTaskManager {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private ScheduledTaskManager() {}

    public static void schedulePeriodicTask(Runnable task, long initialDelaySeconds, long periodSeconds) {
        scheduler.scheduleAtFixedRate(task, initialDelaySeconds, periodSeconds, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
