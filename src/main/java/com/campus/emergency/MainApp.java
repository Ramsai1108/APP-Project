package com.campus.emergency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.SwingUtilities;
import com.campus.emergency.ui.MainFrame;

/**
 * Main Entry Point for Smart Campus Emergency & Resource Co-ordination System.
 */
public class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        logger.info("Initializing Smart Campus Emergency System...");
        
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                logger.info("Swing GUI initialized successfully.");
            } catch (Exception e) {
                logger.error("Failed to start Swing GUI", e);
            }
        });
    }
}
