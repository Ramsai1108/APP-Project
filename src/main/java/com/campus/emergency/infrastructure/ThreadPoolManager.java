package com.campus.emergency.infrastructure;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ThreadPoolManager {
    private static final Logger LOGGER = Logger.getLogger(ThreadPoolManager.class.getName());
    private static ThreadPoolManager instance;
    private final ExecutorService executorService;

    private ThreadPoolManager() {
        int corePoolSize = Math.max(2, Runtime.getRuntime().availableProcessors());
        this.executorService = Executors.newFixedThreadPool(corePoolSize);
        LOGGER.info("ThreadPoolManager initialized with fixed thread pool size: " + corePoolSize);
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void submit(Runnable task) {
        if (!executorService.isShutdown()) {
            executorService.submit(task);
        }
    }

    public void shutdown() {
        LOGGER.info("Shutting down ThreadPoolManager...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
