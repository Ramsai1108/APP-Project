package com.campus.emergency.ui;

import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.IdentityService;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final IdentityService identityService;
    private final ServiceRegistry serviceRegistry;

    private ModernTextField usernameField;
    private ModernPasswordField passwordField;
    private ModernButton loginButton;
    private ModernButton registerButton;
    private JCheckBox rememberMeBox;
    private JLabel statusLabel;

    public LoginFrame() {
        this(ServiceRegistry.getInstance().getIdentityService(), ServiceRegistry.getInstance());
    }

    public LoginFrame(IdentityService identityService) {
        this(identityService, ServiceRegistry.getInstance());
    }

    public LoginFrame(IdentityService identityService, ServiceRegistry serviceRegistry) {
        this.identityService = (identityService != null) ? identityService : ServiceRegistry.getInstance().getIdentityService();
        this.serviceRegistry = (serviceRegistry != null) ? serviceRegistry : ServiceRegistry.getInstance();
        initUI();
    }

    private void initUI() {
        setTitle("Campus Emergency Response System - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(Color.WHITE);

        // LEFT HERO PANEL (Deep Navy Branding)
        JPanel leftHero = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.applyQualityRenderingHints(g2);
                GradientPaint gp = new GradientPaint(0, 0, UITheme.PRIMARY_DARK, getWidth(), getHeight(), new Color(30, 41, 59));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        leftHero.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel brandContent = new JPanel();
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));
        brandContent.setOpaque(false);

        JLabel badge = new JLabel("EMERGENCY OPS PORTAL");
        badge.setFont(UITheme.FONT_BADGE);
        badge.setForeground(new Color(147, 197, 253));
        brandContent.add(badge);
        brandContent.add(Box.createVerticalStrut(12));

        JLabel title1 = new JLabel("Smart Campus");
        title1.setFont(UITheme.FONT_TITLE_XL);
        title1.setForeground(Color.WHITE);
        brandContent.add(title1);

        JLabel title2 = new JLabel("Emergency Response");
        title2.setFont(UITheme.FONT_TITLE_XL);
        title2.setForeground(Color.WHITE);
        brandContent.add(title2);
        brandContent.add(Box.createVerticalStrut(16));

        JLabel tagline = new JLabel("<html>Fast reporting. Coordinated response.<br>Safer campus for everyone.</html>");
        tagline.setFont(UITheme.FONT_BODY);
        tagline.setForeground(new Color(203, 213, 225));
        brandContent.add(tagline);
        brandContent.add(Box.createVerticalStrut(30));

        JLabel f1 = new JLabel("●  24/7 Dispatch Center Coordination");
        f1.setFont(UITheme.FONT_SMALL);
        f1.setForeground(new Color(148, 163, 184));
        brandContent.add(f1);
        brandContent.add(Box.createVerticalStrut(8));

        JLabel f2 = new JLabel("●  Real-time Priority & Status Tracking");
        f2.setFont(UITheme.FONT_SMALL);
        f2.setForeground(new Color(148, 163, 184));
        brandContent.add(f2);
        brandContent.add(Box.createVerticalStrut(8));

        JLabel f3 = new JLabel("●  Secure Role-Based Authorization");
        f3.setFont(UITheme.FONT_SMALL);
        f3.setForeground(new Color(148, 163, 184));
        brandContent.add(f3);

        leftHero.add(brandContent, BorderLayout.CENTER);

        JLabel versionLabel = new JLabel("Campus Emergency System v1.0.0");
        versionLabel.setFont(UITheme.FONT_SMALL);
        versionLabel.setForeground(new Color(100, 116, 139));
        leftHero.add(versionLabel, BorderLayout.SOUTH);

        root.add(leftHero);

        // RIGHT LOGIN CARD
        JPanel rightCard = new JPanel(new BorderLayout(0, 16));
        rightCard.setBackground(Color.WHITE);
        rightCard.setBorder(new EmptyBorder(36, 40, 36, 40));

        JPanel formHeader = new JPanel(new GridLayout(2, 1, 0, 4));
        formHeader.setOpaque(false);
        JLabel signInTitle = new JLabel("Sign In to Portal");
        signInTitle.setFont(UITheme.FONT_TITLE_LARGE);
        signInTitle.setForeground(UITheme.TEXT_PRIMARY);
        formHeader.add(signInTitle);

        JLabel signInSubtitle = new JLabel("Enter your campus credentials to continue");
        signInSubtitle.setFont(UITheme.FONT_BODY);
        signInSubtitle.setForeground(UITheme.TEXT_SECONDARY);
        formHeader.add(signInSubtitle);

        rightCard.add(formHeader, BorderLayout.NORTH);

        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        JLabel userLabel = new JLabel("Username");
        userLabel.setFont(UITheme.FONT_BODY_BOLD);
        userLabel.setForeground(UITheme.TEXT_SECONDARY);
        formBody.add(userLabel);
        formBody.add(Box.createVerticalStrut(6));

        usernameField = new ModernTextField("e.g. student_john");
        formBody.add(usernameField);
        formBody.add(Box.createVerticalStrut(14));

        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(UITheme.FONT_BODY_BOLD);
        passLabel.setForeground(UITheme.TEXT_SECONDARY);
        formBody.add(passLabel);
        formBody.add(Box.createVerticalStrut(6));

        passwordField = new ModernPasswordField("Enter your password");
        formBody.add(passwordField);
        formBody.add(Box.createVerticalStrut(10));

        JPanel optionsRow = new JPanel(new BorderLayout());
        optionsRow.setOpaque(false);
        rememberMeBox = new JCheckBox("Remember me");
        rememberMeBox.setFont(UITheme.FONT_SMALL);
        rememberMeBox.setForeground(UITheme.TEXT_SECONDARY);
        rememberMeBox.setOpaque(false);
        rememberMeBox.setSelected(true);
        optionsRow.add(rememberMeBox, BorderLayout.WEST);

        JLabel forgotLabel = new JLabel("Need help?");
        forgotLabel.setFont(UITheme.FONT_SMALL);
        forgotLabel.setForeground(UITheme.ACCENT_BLUE);
        forgotLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgotLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ModernDialog.showInfo(LoginFrame.this, "Campus IT Helpdesk", "Please contact Campus Security or IT support to reset your password.");
            }
        });
        optionsRow.add(forgotLabel, BorderLayout.EAST);
        formBody.add(optionsRow);
        formBody.add(Box.createVerticalStrut(16));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(UITheme.FONT_SMALL);
        statusLabel.setForeground(UITheme.EMERGENCY_RED);
        formBody.add(statusLabel);
        formBody.add(Box.createVerticalStrut(10));

        loginButton = new ModernButton("Sign In", ModernButton.ButtonStyle.PRIMARY);
        loginButton.setPreferredSize(new Dimension(300, 40));
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formBody.add(loginButton);
        formBody.add(Box.createVerticalStrut(10));

        registerButton = new ModernButton("Create New Account", ModernButton.ButtonStyle.OUTLINE);
        registerButton.setPreferredSize(new Dimension(300, 38));
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        formBody.add(registerButton);

        rightCard.add(formBody, BorderLayout.CENTER);
        root.add(rightCard);

        add(root);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> openRegistration());
        getRootPane().setDefaultButton(loginButton);
    }

    public void handleLogin() {
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);

        if (username.isEmpty() || password.isEmpty()) {
            setStatus("Username and password are required.", true);
            ModernDialog.showWarning(this, "Missing Input", "Please enter both your username and password.");
            return;
        }

        loginButton.setEnabled(false);
        registerButton.setEnabled(false);
        setStatus("Authenticating...", false);

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                User user = identityService.login(username, password);
                SwingUtilities.invokeLater(() -> {
                    AuthenticationContext.setCurrentUser(user);
                    setStatus("Login successful!", false);
                    openRoleDashboard(user);
                    dispose();
                });
            } catch (Exception ex) {
                LoggingService.warn("GUI Login failed for user: " + username + " - " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    registerButton.setEnabled(true);
                    setStatus("Authentication failed: " + ex.getMessage(), true);
                    ModernDialog.showError(this, "Authentication Failed", ex.getMessage());
                });
            }
        });
    }

    public void openRegistration() {
        RegistrationFrame regFrame = new RegistrationFrame(identityService, serviceRegistry);
        regFrame.setVisible(true);
    }

    private void openRoleDashboard(User user) {
        if (user != null && user.getRole().isAdmin()) {
            AdminConsoleFrame adminFrame = new AdminConsoleFrame(
                    serviceRegistry.getTrackingService(),
                    serviceRegistry.getDataAggregator(),
                    serviceRegistry.getLogDAO(),
                    serviceRegistry.getEmergencyManager(),
                    serviceRegistry.getResourceDispatcher()
            );
            adminFrame.setVisible(true);
        } else {
            UserDashboardFrame userFrame = new UserDashboardFrame(
                    serviceRegistry.getEmergencyManager(),
                    serviceRegistry.getTrackingService(),
                    serviceRegistry.getReportDAO(),
                    serviceRegistry.getLocationDAO()
            );
            userFrame.setVisible(true);
        }
    }

    private void setStatus(String message, boolean isError) {
        statusLabel.setText(message != null ? message : " ");
        statusLabel.setForeground(isError ? UITheme.EMERGENCY_RED : UITheme.ACCENT_BLUE);
    }
}
