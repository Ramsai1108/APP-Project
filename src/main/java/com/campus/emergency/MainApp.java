package com.campus.emergency;

import com.campus.emergency.config.AppConfig;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.ui.LoginFrame;

import javax.swing.SwingUtilities;

public class MainApp {

    public static void main(String[] args) {
        LoggingService.info("Starting Campus Emergency Response System v" + AppConfig.getInstance().getAppVersion());

        // Initialize ServiceRegistry and infrastructure
        ServiceRegistry registry = ServiceRegistry.getInstance();

        // Shutdown hook for connection pools and thread pools
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggingService.info("Shutting down Campus Emergency System...");
            ThreadPoolManager.getInstance().shutdown();
            if (registry.getPoolManager() != null) {
                registry.getPoolManager().shutdown();
            }
        }));

        // Launch Login GUI on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(registry.getIdentityService(), registry);
            loginFrame.setVisible(true);
        });
    }
}
