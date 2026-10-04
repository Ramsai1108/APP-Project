package com.campus.emergency.ui;

import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.Role;
import com.campus.emergency.model.User;
import com.campus.emergency.service.IdentityService;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.ui.components.*;
import com.campus.emergency.util.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RegistrationFrame extends JFrame {
    private final IdentityService identityService;
    private final ServiceRegistry serviceRegistry;

    private ModernTextField fullNameField;
    private ModernTextField usernameField;
    private ModernTextField emailField;
    private ModernTextField phoneField;
    private JComboBox<RoleOption> roleComboBox;
    private ModernPasswordField passwordField;
    private ModernPasswordField confirmPasswordField;
    private ModernButton submitButton;
    private ModernButton cancelButton;

    private static class RoleOption {
        private final String label;
        private final Role role;

        public RoleOption(String label, Role role) {
            this.label = label;
            this.role = role;
        }

        public Role getRole() {
            return role;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public RegistrationFrame(IdentityService identityService) {
        this(identityService, ServiceRegistry.getInstance());
    }

    public RegistrationFrame(IdentityService identityService, ServiceRegistry serviceRegistry) {
        this.identityService = (identityService != null) ? identityService : ServiceRegistry.getInstance().getIdentityService();
        this.serviceRegistry = (serviceRegistry != null) ? serviceRegistry : ServiceRegistry.getInstance();
        initUI();
    }

    private void initUI() {
        setTitle("Campus Emergency Response System - User Registration");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(540, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(24, 28, 24, 28));

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Create Campus Account");
        titleLabel.setFont(UITheme.FONT_TITLE_LARGE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        headerPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Register to report emergencies and receive campus safety alerts");
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Card Panel
        ModernCardPanel card = new ModernCardPanel(new GridLayout(7, 2, 12, 12), 20);

        card.add(createFieldLabel("Full Name *"));
        fullNameField = new ModernTextField("e.g. John Doe");
        card.add(fullNameField);

        card.add(createFieldLabel("Username *"));
        usernameField = new ModernTextField("e.g. jdoe24");
        card.add(usernameField);

        card.add(createFieldLabel("Campus Email *"));
        emailField = new ModernTextField("e.g. jdoe@campus.edu");
        card.add(emailField);

        card.add(createFieldLabel("Phone Number"));
        phoneField = new ModernTextField("e.g. 555-0199");
        card.add(phoneField);

        card.add(createFieldLabel("Account Role *"));
        RoleOption[] roleOptions = new RoleOption[]{
                new RoleOption("Student", Role.STUDENT),
                new RoleOption("Admin", Role.ADMIN)
        };
        roleComboBox = new JComboBox<>(roleOptions);
        roleComboBox.setSelectedIndex(0);
        roleComboBox.setFont(UITheme.FONT_BODY);
        roleComboBox.setBackground(Color.WHITE);
        card.add(roleComboBox);

        card.add(createFieldLabel("Password * (min 6 chars)"));
        passwordField = new ModernPasswordField("Enter secure password");
        card.add(passwordField);

        card.add(createFieldLabel("Confirm Password *"));
        confirmPasswordField = new ModernPasswordField("Re-enter password");
        card.add(confirmPasswordField);

        mainPanel.add(card, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        cancelButton = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelButton.setPreferredSize(new Dimension(100, 38));
        cancelButton.addActionListener(e -> dispose());

        submitButton = new ModernButton("Submit Registration", ModernButton.ButtonStyle.PRIMARY);
        submitButton.setPreferredSize(new Dimension(170, 38));
        submitButton.addActionListener(e -> handleRegistration());

        btnPanel.add(cancelButton);
        btnPanel.add(submitButton);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        getRootPane().setDefaultButton(submitButton);
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UITheme.FONT_BODY_BOLD);
        label.setForeground(UITheme.TEXT_SECONDARY);
        return label;
    }

    public void handleRegistration() {
        String fullName = fullNameField.getText() != null ? fullNameField.getText().trim() : "";
        String username = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        
        RoleOption selectedOption = (RoleOption) roleComboBox.getSelectedItem();
        if (selectedOption == null) {
            ModernDialog.showWarning(this, "Validation Error", "Please select an Account Role.");
            return;
        }
        Role role = selectedOption.getRole();

        char[] passChars = passwordField.getPassword();
        char[] confirmChars = confirmPasswordField.getPassword();
        String password = new String(passChars);
        String confirmPassword = new String(confirmChars);

        if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            ModernDialog.showWarning(this, "Validation Error", "Please fill in all required fields (Name, Username, Email, Password).");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ModernDialog.showWarning(this, "Invalid Email", "Please enter a valid email address format (e.g., user@campus.edu).");
            return;
        }

        if (password.length() < 6) {
            ModernDialog.showWarning(this, "Weak Password", "Password must be at least 6 characters long.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            ModernDialog.showWarning(this, "Password Mismatch", "Passwords do not match. Please verify and try again.");
            return;
        }

        submitButton.setEnabled(false);
        cancelButton.setEnabled(false);

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                User newUser = identityService.register(username, password, fullName, email, phone, role);
                SwingUtilities.invokeLater(() -> {
                    ModernDialog.showSuccess(this, "Registration Successful", "Account created successfully for " + newUser.getUsername() + "! You can now log in.");
                    dispose();
                });
            } catch (Exception ex) {
                LoggingService.warn("Registration failed for username: " + username + " - " + ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    ModernDialog.showError(this, "Registration Error", ex.getMessage());
                });
            }
        });
    }
}
