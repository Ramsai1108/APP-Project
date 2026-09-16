package com.campus.emergency.ui;

import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.User;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.EmergencyManager;
import com.campus.emergency.service.IdentityService;
import com.campus.emergency.util.DateFormatterUtil;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Professional Student Dashboard GUI supporting Emergency Reporting, My Reports, and Profile view.
 */
public class StudentDashboardFrame extends JFrame {

    private final EmergencyManager emergencyManager;
    private final IdentityService identityService;
    private final User currentStudent;

    private CardLayout cardLayout;
    private JPanel contentAreaPanel;

    // Dashboard Cards & Counters
    private JLabel totalCounterLabel;
    private JLabel openCounterLabel;
    private JLabel resolvedCounterLabel;
    private DefaultTableModel recentReportsTableModel;

    // Report Emergency Form Fields
    private JTextField reportTitleField;
    private JTextArea reportDescriptionArea;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> severityComboBox;
    private JTextField locationField;
    private JSpinner affectedPeopleSpinner;
    private JTextField attachmentPathField;
    private JButton submitReportButton;

    // My Reports Table
    private JTable myReportsTable;
    private DefaultTableModel myReportsTableModel;
    private List<EmergencyReport> loadedReportsCache;

    public StudentDashboardFrame(EmergencyManager emergencyManager, IdentityService identityService) {
        this.emergencyManager = emergencyManager;
        this.identityService = identityService;
        this.currentStudent = SessionManager.getCurrentUser();

        if (currentStudent == null) {
            throw new IllegalStateException("Cannot initialize Student Dashboard without authenticated session.");
        }

        initUI();
        loadDashboardDataAsync();
    }

    private void initUI() {
        FlatDarkLaf.setup();
        setTitle("Smart Campus Emergency System - Student Dashboard");
        setSize(1180, 750);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());

        // Top Navigation Header
        JPanel headerPanel = createHeaderPanel();
        mainLayout.add(headerPanel, BorderLayout.NORTH);

        // Sidebar Navigation
        JPanel sidebarPanel = createSidebarPanel();
        mainLayout.add(sidebarPanel, BorderLayout.WEST);

        // Center Content Area (CardLayout)
        cardLayout = new CardLayout();
        contentAreaPanel = new JPanel(cardLayout);

        contentAreaPanel.add(createDashboardHomeView(), "DASHBOARD");
        contentAreaPanel.add(createReportEmergencyFormView(), "REPORT_FORM");
        contentAreaPanel.add(createMyReportsView(), "MY_REPORTS");
        contentAreaPanel.add(new com.campus.emergency.ui.components.NotificationPanel(), "NOTIFICATIONS");
        contentAreaPanel.add(createProfileView(), "PROFILE");

        mainLayout.add(contentAreaPanel, BorderLayout.CENTER);
        add(mainLayout);

        switchView("DASHBOARD");
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 26, 32));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("SMART CAMPUS EMERGENCY SYSTEM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(230, 70, 70));

        JLabel userBadge = new JLabel("Logged in as: " + currentStudent.getFullName() + " (" + currentStudent.getEmail() + ")");
        userBadge.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userBadge.setForeground(new Color(180, 185, 195));

        header.add(title, BorderLayout.WEST);
        header.add(userBadge, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(32, 35, 42));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JButton dashBtn = createNavButton("Dashboard");
        JButton reportBtn = createNavButton("Report Emergency");
        JButton myReportsBtn = createNavButton("My Reports");
        JButton notifBtn = createNavButton("Notifications");
        JButton profileBtn = createNavButton("Profile");
        JButton logoutBtn = createNavButton("Logout");
        logoutBtn.setForeground(new Color(240, 90, 90));

        dashBtn.addActionListener(e -> switchView("DASHBOARD"));
        reportBtn.addActionListener(e -> switchView("REPORT_FORM"));
        myReportsBtn.addActionListener(e -> {
            switchView("MY_REPORTS");
            loadMyReportsAsync();
        });
        notifBtn.addActionListener(e -> switchView("NOTIFICATIONS"));
        profileBtn.addActionListener(e -> switchView("PROFILE"));
        logoutBtn.addActionListener(e -> handleLogout());

        sidebar.add(dashBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(reportBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(myReportsBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(notifBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(profileBtn);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutBtn);

        return sidebar;
    }

    private JButton createNavButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setMaximumSize(new Dimension(200, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        return button;
    }

    // =========================================================================
    // 1. DASHBOARD HOME VIEW
    // =========================================================================
    private JPanel createDashboardHomeView() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        JLabel welcomeLabel = new JLabel("<html><h2>Welcome, " + currentStudent.getFullName() + "!</h2><p style='color:#A0A5B0;'>Campus Safety Overview</p></html>");
        
        JButton quickReportBtn = new JButton("🚨 REPORT EMERGENCY NOW");
        quickReportBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        quickReportBtn.setBackground(new Color(220, 53, 69));
        quickReportBtn.setForeground(Color.WHITE);
        quickReportBtn.setFocusPainted(false);
        quickReportBtn.addActionListener(e -> switchView("REPORT_FORM"));

        topRow.add(welcomeLabel, BorderLayout.WEST);
        topRow.add(quickReportBtn, BorderLayout.EAST);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        
        totalCounterLabel = new JLabel("0", SwingConstants.CENTER);
        openCounterLabel = new JLabel("0", SwingConstants.CENTER);
        resolvedCounterLabel = new JLabel("0", SwingConstants.CENTER);

        cardsPanel.add(createCard("Total Reports", totalCounterLabel, new Color(40, 120, 220)));
        cardsPanel.add(createCard("Open Reports", openCounterLabel, new Color(255, 165, 0)));
        cardsPanel.add(createCard("Resolved Reports", resolvedCounterLabel, new Color(40, 167, 69)));

        JPanel recentSection = new JPanel(new BorderLayout(5, 5));
        recentSection.setBorder(BorderFactory.createTitledBorder("Recent Emergency Reports"));

        recentReportsTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Category", "Severity", "Status", "Date"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable recentTable = new JTable(recentReportsTableModel);
        recentSection.add(new JScrollPane(recentTable), BorderLayout.CENTER);

        JPanel centerContainer = new JPanel(new BorderLayout(15, 15));
        centerContainer.add(cardsPanel, BorderLayout.NORTH);
        centerContainer.add(recentSection, BorderLayout.CENTER);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(centerContainer, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                new EmptyBorder(15, 15, 15, 15)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(180, 185, 195));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // 2. REPORT EMERGENCY FORM VIEW
    // =========================================================================
    private JPanel createReportEmergencyFormView() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("SUBMIT EMERGENCY REPORT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(230, 70, 70));

        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        formGrid.add(new JLabel("Emergency Title: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        reportTitleField = new JTextField();
        formGrid.add(reportTitleField, gbc);
        row++;

        // Category
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        formGrid.add(new JLabel("Category: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        categoryComboBox = new JComboBox<>(new String[]{"MEDICAL", "FIRE", "SECURITY", "ACCIDENT", "NATURAL_DISASTER", "OTHER"});
        formGrid.add(categoryComboBox, gbc);
        row++;

        // Severity
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        formGrid.add(new JLabel("Severity Level: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        severityComboBox = new JComboBox<>(new String[]{"1 — LOW", "2 — MODERATE", "3 — SERIOUS", "4 — HIGH", "5 — CRITICAL"});
        formGrid.add(severityComboBox, gbc);
        row++;

        // Location
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        formGrid.add(new JLabel("Location: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        locationField = new JTextField();
        formGrid.add(locationField, gbc);
        row++;

        // Number of Affected People
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2;
        formGrid.add(new JLabel("Affected People: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        affectedPeopleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        formGrid.add(affectedPeopleSpinner, gbc);
        row++;

        // Description
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.NORTHWEST;
        formGrid.add(new JLabel("Description: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        reportDescriptionArea = new JTextArea(4, 30);
        reportDescriptionArea.setLineWrap(true);
        reportDescriptionArea.setWrapStyleWord(true);
        formGrid.add(new JScrollPane(reportDescriptionArea), gbc);
        row++;

        // Attachment File Selection
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.CENTER;
        formGrid.add(new JLabel("Attachment File:"), gbc);
        
        JPanel fileAttachPanel = new JPanel(new BorderLayout(5, 0));
        attachmentPathField = new JTextField();
        attachmentPathField.setEditable(false);
        
        JPanel attachButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton attachPhotoBtn = new JButton("Attach Photo");
        JButton attachVideoBtn = new JButton("Attach Video");
        
        attachPhotoBtn.addActionListener(e -> chooseFileAttachment(false));
        attachVideoBtn.addActionListener(e -> chooseFileAttachment(true));

        attachButtonsPanel.add(attachPhotoBtn);
        attachButtonsPanel.add(attachVideoBtn);
        fileAttachPanel.add(attachmentPathField, BorderLayout.CENTER);
        fileAttachPanel.add(attachButtonsPanel, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 0.8;
        formGrid.add(fileAttachPanel, gbc);
        row++;

        // Action Buttons Row
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        submitReportButton = new JButton("🚨 SUBMIT EMERGENCY");
        submitReportButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitReportButton.setBackground(new Color(220, 53, 69));
        submitReportButton.setForeground(Color.WHITE);

        JButton clearBtn = new JButton("CLEAR");
        JButton cancelBtn = new JButton("CANCEL");

        submitReportButton.addActionListener(e -> handleFormSubmissionAsync());
        clearBtn.addActionListener(e -> clearReportForm());
        cancelBtn.addActionListener(e -> switchView("DASHBOARD"));

        buttonRow.add(submitReportButton);
        buttonRow.add(clearBtn);
        buttonRow.add(cancelBtn);

        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(formGrid), BorderLayout.CENTER);
        mainPanel.add(buttonRow, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void chooseFileAttachment(boolean isVideo) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(isVideo ? "Select Video Attachment" : "Select Photo Attachment");

        if (isVideo) {
            fileChooser.setFileFilter(new FileNameExtensionFilter("Video Files (.mp4, .avi, .mov, .mkv)", "mp4", "avi", "mov", "mkv"));
        } else {
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files (.jpg, .jpeg, .png, .webp)", "jpg", "jpeg", "png", "webp"));
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                emergencyManager.validateFileAttachment(selectedFile.getAbsolutePath(), isVideo);
                attachmentPathField.setText(selectedFile.getAbsolutePath());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid File Attachment", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleFormSubmissionAsync() {
        String title = reportTitleField.getText().trim();
        String description = reportDescriptionArea.getText().trim();
        String category = (String) categoryComboBox.getSelectedItem();
        int severity = severityComboBox.getSelectedIndex() + 1;
        String location = locationField.getText().trim();
        int affectedPeople = (Integer) affectedPeopleSpinner.getValue();
        String mediaPath = attachmentPathField.getText().trim();

        EmergencyReport report = new EmergencyReport(
                currentStudent.getId(), title, description, category, severity, location, affectedPeople, mediaPath
        );

        submitReportButton.setEnabled(false);
        submitReportButton.setText("SUBMITTING...");

        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                EmergencyManager.EmergencySubmissionResult result = emergencyManager.submitReport(report);

                SwingUtilities.invokeLater(() -> {
                    submitReportButton.setEnabled(true);
                    submitReportButton.setText("🚨 SUBMIT EMERGENCY");
                    clearReportForm();

                    String successMsg = String.format(
                            "Emergency reported successfully!\n\nReport ID: #%d\nIncident ID: #%d\nCalculated Priority: %.1f / 100.0\nStatus: %s",
                            result.reportId(), result.incidentId(), result.calculatedPriority(), result.status()
                    );

                    JOptionPane.showMessageDialog(this, successMsg, "Emergency Submitted & Incident Created", JOptionPane.INFORMATION_MESSAGE);

                    switchView("MY_REPORTS");
                    loadMyReportsAsync();
                });
            } catch (DatabaseAccessException ex) {
                SwingUtilities.invokeLater(() -> {
                    submitReportButton.setEnabled(true);
                    submitReportButton.setText("🚨 SUBMIT EMERGENCY");
                    JOptionPane.showMessageDialog(this, "Unable to submit the emergency right now. Please try again.", "Database Error", JOptionPane.ERROR_MESSAGE);
                });
            } catch (IllegalArgumentException | CampusEmergencyException ex) {
                SwingUtilities.invokeLater(() -> {
                    submitReportButton.setEnabled(true);
                    submitReportButton.setText("🚨 SUBMIT EMERGENCY");
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
                });
            }
        });
    }

    private void clearReportForm() {
        reportTitleField.setText("");
        reportDescriptionArea.setText("");
        categoryComboBox.setSelectedIndex(0);
        severityComboBox.setSelectedIndex(0);
        locationField.setText("");
        affectedPeopleSpinner.setValue(0);
        attachmentPathField.setText("");
    }

    // =========================================================================
    // 3. MY REPORTS VIEW
    // =========================================================================
    private JPanel createMyReportsView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("MY EMERGENCY REPORTS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        myReportsTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Category", "Severity", "Status", "Date"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        myReportsTable = new JTable(myReportsTableModel);
        myReportsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        myReportsTable.setRowHeight(28);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton viewDetailsBtn = new JButton("VIEW DETAILS");
        JButton refreshBtn = new JButton("REFRESH");
        JButton backBtn = new JButton("BACK");

        viewDetailsBtn.addActionListener(e -> openReportDetailsDialog());
        refreshBtn.addActionListener(e -> loadMyReportsAsync());
        backBtn.addActionListener(e -> switchView("DASHBOARD"));

        buttonBar.add(viewDetailsBtn);
        buttonBar.add(refreshBtn);
        buttonBar.add(backBtn);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(myReportsTable), BorderLayout.CENTER);
        panel.add(buttonBar, BorderLayout.SOUTH);

        return panel;
    }

    private void loadMyReportsAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                List<EmergencyReport> reports = emergencyManager.getReportsForStudent(currentStudent.getId());
                loadedReportsCache = reports;

                SwingUtilities.invokeLater(() -> {
                    myReportsTableModel.setRowCount(0);
                    for (EmergencyReport r : reports) {
                        myReportsTableModel.addRow(new Object[]{
                                r.getId(),
                                r.getTitle(),
                                r.getCategory(),
                                r.getSeverity(),
                                r.getStatus(),
                                DateFormatterUtil.formatDateTime(r.getReportedAt())
                        });
                    }
                    loadDashboardDataAsync();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Failed to load your emergency reports.", "Data Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void openReportDetailsDialog() {
        int selectedRow = myReportsTable.getSelectedRow();
        if (selectedRow < 0 || loadedReportsCache == null || selectedRow >= loadedReportsCache.size()) {
            JOptionPane.showMessageDialog(this, "Please select an emergency report from the table to view details.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EmergencyReport report = loadedReportsCache.get(selectedRow);

        JDialog detailsDialog = new JDialog(this, "Emergency Report #" + report.getId() + " Details", true);
        detailsDialog.setSize(520, 520);
        detailsDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        panel.add(createDetailLine("Report ID:", "#" + report.getId()));
        panel.add(createDetailLine("Title:", report.getTitle()));
        panel.add(createDetailLine("Category:", report.getCategory()));
        panel.add(createDetailLine("Severity:", report.getSeverity() + " / 5"));
        panel.add(createDetailLine("Location:", report.getLocation()));
        panel.add(createDetailLine("Affected People:", String.valueOf(report.getAffectedPeople())));
        panel.add(createDetailLine("Status:", report.getStatus()));
        panel.add(createDetailLine("Submitted:", DateFormatterUtil.formatDateTime(report.getReportedAt())));
        panel.add(createDetailLine("Attachment:", report.getMediaPath() != null && !report.getMediaPath().isEmpty() ? report.getMediaPath() : "None"));

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(descLabel);

        JTextArea descArea = new JTextArea(report.getDescription(), 4, 30);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        panel.add(new JScrollPane(descArea));

        JButton closeBtn = new JButton("Close");
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> detailsDialog.dispose());

        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(closeBtn);

        detailsDialog.add(panel);
        detailsDialog.setVisible(true);
    }

    private JPanel createDetailLine(String label, String value) {
        JPanel line = new JPanel(new BorderLayout(5, 5));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setPreferredSize(new Dimension(140, 24));

        JLabel val = new JLabel(value != null ? value : "N/A");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        line.add(lbl, BorderLayout.WEST);
        line.add(val, BorderLayout.CENTER);
        return line;
    }

    // =========================================================================
    // 4. PROFILE VIEW (READ-ONLY)
    // =========================================================================
    private JPanel createProfileView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel titleLabel = new JLabel("STUDENT PROFILE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel infoGrid = new JPanel(new GridLayout(5, 2, 15, 15));
        infoGrid.setBorder(BorderFactory.createTitledBorder("Student Details"));

        infoGrid.add(new JLabel("Full Name:"));
        infoGrid.add(new JLabel(currentStudent.getFullName()));

        infoGrid.add(new JLabel("Username:"));
        infoGrid.add(new JLabel(currentStudent.getUsername()));

        infoGrid.add(new JLabel("Email Address:"));
        infoGrid.add(new JLabel(currentStudent.getEmail()));

        infoGrid.add(new JLabel("Phone Number:"));
        infoGrid.add(new JLabel(currentStudent.getPhone()));

        infoGrid.add(new JLabel("Account Role:"));
        infoGrid.add(new JLabel(currentStudent.getRoleName()));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(infoGrid, BorderLayout.CENTER);

        return panel;
    }

    private void loadDashboardDataAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                EmergencyManager.StudentDashboardMetrics metrics = emergencyManager.getDashboardMetrics(currentStudent.getId());
                List<EmergencyReport> recentReports = emergencyManager.getReportsForStudent(currentStudent.getId());

                SwingUtilities.invokeLater(() -> {
                    totalCounterLabel.setText(String.valueOf(metrics.totalReports()));
                    openCounterLabel.setText(String.valueOf(metrics.openReports()));
                    resolvedCounterLabel.setText(String.valueOf(metrics.resolvedReports()));

                    recentReportsTableModel.setRowCount(0);
                    int count = 0;
                    for (EmergencyReport r : recentReports) {
                        if (count++ >= 5) break;
                        recentReportsTableModel.addRow(new Object[]{
                                r.getId(), r.getTitle(), r.getCategory(), r.getSeverity(), r.getStatus(), DateFormatterUtil.formatDateTime(r.getReportedAt())
                        });
                    }
                });
            } catch (Exception ex) {
                // Background quiet refresh
            }
        });
    }

    private void switchView(String viewName) {
        cardLayout.show(contentAreaPanel, viewName);
    }

    private void handleLogout() {
        identityService.logout();
        new LoginFrame(identityService).setVisible(true);
        this.dispose();
    }
}
