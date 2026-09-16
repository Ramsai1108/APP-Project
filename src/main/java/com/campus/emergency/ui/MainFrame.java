package com.campus.emergency.ui;

import com.campus.emergency.config.AppConfig;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

/**
 * Main Application Swing Window Frame.
 */
public class MainFrame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel mainContainer;

    public MainFrame() {
        FlatDarkLaf.setup();

        setTitle(AppConfig.APP_TITLE);
        setSize(1280, 800);
        setMinimumSize(new java.awt.Dimension(1024, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        mainContainer.add(new LoginPanel(this), "LOGIN");
        mainContainer.add(new EmergencyReportPanel(), "REPORT");
        mainContainer.add(new DispatcherDashboardPanel(), "DISPATCHER");
        mainContainer.add(new AdminDashboardPanel(), "ADMIN");

        add(mainContainer, BorderLayout.CENTER);
        showView("LOGIN");
    }

    public void showView(String viewName) {
        cardLayout.show(mainContainer, viewName);
    }
}
