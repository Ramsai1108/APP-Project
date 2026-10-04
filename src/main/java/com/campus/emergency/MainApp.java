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

        // Start Localhost Web Application Server (http://localhost:3000)
        com.campus.emergency.infrastructure.web.LocalWebServer.startServer(3000, registry);

        // Shutdown hook for connection pools, thread pools, and web server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggingService.info("Shutting down Campus Emergency System...");
            com.campus.emergency.infrastructure.web.LocalWebServer.stopServer();
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
