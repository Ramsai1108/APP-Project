package com.campus.emergency.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global Thread Pool Manager utilizing ExecutorService.
 */
public class TaskExecutorManager {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private TaskExecutorManager() {}

    public static void submitAsyncTask(Runnable task) {
        executorService.submit(task);
    }

    public static void shutdown() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
