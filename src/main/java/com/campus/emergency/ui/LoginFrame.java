package com.campus.emergency.ui;

import com.campus.emergency.dao.impl.EmergencyReportDaoImpl;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.User;
import com.campus.emergency.service.EmergencyManager;
import com.campus.emergency.service.IdentityService;
import com.campus.emergency.service.impl.EmergencyManagerImpl;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modern Swing GUI Login Window for Smart Campus Emergency System.
 */
public class LoginFrame extends JFrame {

    private final IdentityService identityService;
    private final EmergencyManager emergencyManager;

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    public LoginFrame(IdentityService identityService) {
        this.identityService = identityService;
        this.emergencyManager = new EmergencyManagerImpl(new EmergencyReportDaoImpl());
        initUI();
    }

    private void initUI() {
        FlatDarkLaf.setup();
        setTitle("Smart Campus Emergency System - Login");
        setSize(480, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(new Color(30, 33, 40));

        // Header Title
        JLabel titleLabel = new JLabel("CAMPUS EMERGENCY");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(230, 70, 70));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Authentication & Resource Portal");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(170, 175, 185));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Input Form Panel
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        formPanel.setOpaque(false);
        formPanel.setMaximumSize(new Dimension(380, 180));

        JLabel emailLabel = new JLabel("Email Address:");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailField = new JTextField();
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
        };
        emailField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);

        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        loginButton = new JButton("LOGIN TO PORTAL");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(40, 120, 220));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setMaximumSize(new Dimension(380, 45));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.addActionListener(e -> performLogin());

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(new Color(240, 90, 90));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        mainPanel.add(formPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(statusLabel);

        add(mainPanel);
    }

    private void performLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        statusLabel.setText("Authenticating...");
        statusLabel.setForeground(new Color(150, 200, 255));

        try {
            User user = identityService.authenticate(email, password);
            statusLabel.setText("Login Successful! Redirecting...");
            statusLabel.setForeground(new Color(80, 200, 120));

            redirectToDashboard(user);

        } catch (AuthenticationException e) {
            statusLabel.setText(e.getMessage());
            statusLabel.setForeground(new Color(240, 90, 90));
            JOptionPane.showMessageDialog(this, e.getMessage(), "Authentication Failure", JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseAccessException e) {
            statusLabel.setText("Database Connection Error. Try again.");
            statusLabel.setForeground(new Color(240, 90, 90));
            JOptionPane.showMessageDialog(this, "Database Connection Error: " + e.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            statusLabel.setText("An unexpected error occurred.");
            statusLabel.setForeground(new Color(240, 90, 90));
        }
    }

    private void redirectToDashboard(User user) {
        String roleName = user.getRoleName().toUpperCase();

        if ("STUDENT".equals(roleName)) {
            StudentDashboardFrame studentFrame = new StudentDashboardFrame(emergencyManager, identityService);
            studentFrame.setVisible(true);
            this.dispose();
            return;
        }

        JFrame dashboard;
        switch (roleName) {
            case "ADMINISTRATOR":
            case "ADMIN":
            case "DISPATCHER":
                try {
                    dashboard = new DispatcherDashboardFrame();
                } catch (Exception ex) {
                    dashboard = createPlaceholderDashboard("DISPATCH CONTROL ROOM", user);
                }
                break;
            case "STAFF":
                dashboard = createPlaceholderDashboard("STAFF FACILITY EMERGENCY PORTAL", user);
                break;
            case "EXTERNAL_AGENCY":
            case "AGENCY":
                dashboard = createPlaceholderDashboard("EXTERNAL AGENCY ESCALATION MONITOR", user);
                break;
            default:
                dashboard = createPlaceholderDashboard("GENERIC CAMPUS USER PORTAL", user);
                break;
        }

        dashboard.setVisible(true);
        this.dispose();
    }

    private JFrame createPlaceholderDashboard(String title, User user) {
        JFrame frame = new JFrame(title + " - Welcome " + user.getFullName());
        frame.setSize(1024, 600);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><h2 style='color:#4A90E2;'>" + title + "</h2>" +
                "<p>Logged in as: <b>" + user.getFullName() + "</b> (" + user.getEmail() + ")</p>" +
                "<p>Role: <b>" + user.getRoleName() + "</b></p></html>", SwingConstants.CENTER);

        JButton logoutButton = new JButton("LOGOUT");
        logoutButton.addActionListener(e -> {
            identityService.logout();
            new LoginFrame(identityService).setVisible(true);
            frame.dispose();
        });

        panel.add(welcomeLabel, BorderLayout.CENTER);
        panel.add(logoutButton, BorderLayout.SOUTH);
        frame.add(panel);
        return frame;
    }
}
