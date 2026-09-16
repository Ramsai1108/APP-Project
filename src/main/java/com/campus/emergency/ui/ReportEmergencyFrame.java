package com.campus.emergency.ui;

import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.exception.CampusEmergencyException;
import com.campus.emergency.exception.DatabaseAccessException;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.User;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.EmergencyManager;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Standalone Swing Frame for Emergency Reporting.
 */
public class ReportEmergencyFrame extends JFrame {

    private final EmergencyManager emergencyManager;
    private final User currentUser;

    private JTextField titleField;
    private JTextArea descriptionArea;
    private JComboBox<String> categoryComboBox;
    private JComboBox<String> severityComboBox;
    private JTextField locationField;
    private JSpinner affectedPeopleSpinner;
    private JTextField attachmentPathField;
    private JButton submitButton;

    public ReportEmergencyFrame(EmergencyManager emergencyManager) {
        this.emergencyManager = emergencyManager;
        this.currentUser = SessionManager.getCurrentUser();

        if (currentUser == null) {
            throw new IllegalStateException("Cannot open ReportEmergencyFrame without an active user session.");
        }

        initUI();
    }

    private void initUI() {
        FlatDarkLaf.setup();
        setTitle("Report Campus Emergency - SOS Portal");
        setSize(650, 680);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Header
        JLabel headerLabel = new JLabel("🚨 EMERGENCY REPORTING FORM");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        headerLabel.setForeground(new Color(230, 70, 70));

        // Form Fields
        JPanel formGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Title
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formGrid.add(new JLabel("Title: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        titleField = new JTextField();
        formGrid.add(titleField, gbc);
        row++;

        // Category
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formGrid.add(new JLabel("Category: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        categoryComboBox = new JComboBox<>(new String[]{"MEDICAL", "FIRE", "SECURITY", "ACCIDENT", "NATURAL_DISASTER", "OTHER"});
        formGrid.add(categoryComboBox, gbc);
        row++;

        // Severity
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formGrid.add(new JLabel("Severity: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        severityComboBox = new JComboBox<>(new String[]{"1 — LOW", "2 — MODERATE", "3 — SERIOUS", "4 — HIGH", "5 — CRITICAL"});
        formGrid.add(severityComboBox, gbc);
        row++;

        // Location
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formGrid.add(new JLabel("Location: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        locationField = new JTextField();
        formGrid.add(locationField, gbc);
        row++;

        // Affected People
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        formGrid.add(new JLabel("Affected People:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        affectedPeopleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        formGrid.add(affectedPeopleSpinner, gbc);
        row++;

        // Description
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.NORTHWEST;
        formGrid.add(new JLabel("Description: *"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.75;
        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        formGrid.add(new JScrollPane(descriptionArea), gbc);
        row++;

        // File Attachments
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25; gbc.anchor = GridBagConstraints.CENTER;
        formGrid.add(new JLabel("Media Attachment:"), gbc);

        JPanel attachPanel = new JPanel(new BorderLayout(5, 0));
        attachmentPathField = new JTextField();
        attachmentPathField.setEditable(false);

        JPanel attachButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton photoBtn = new JButton("Attach Photo");
        JButton videoBtn = new JButton("Attach Video");

        photoBtn.addActionListener(e -> chooseAttachment(false));
        videoBtn.addActionListener(e -> chooseAttachment(true));

        attachButtons.add(photoBtn);
        attachButtons.add(videoBtn);
        attachPanel.add(attachmentPathField, BorderLayout.CENTER);
        attachPanel.add(attachButtons, BorderLayout.EAST);

        gbc.gridx = 1; gbc.weightx = 0.75;
        formGrid.add(attachPanel, gbc);
        row++;

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        submitButton = new JButton("SUBMIT EMERGENCY");
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitButton.setBackground(new Color(220, 53, 69));
        submitButton.setForeground(Color.WHITE);

        JButton cancelButton = new JButton("CANCEL");

        submitButton.addActionListener(e -> handleSubmitAsync());
        cancelButton.addActionListener(e -> dispose());

        buttonRow.add(submitButton);
        buttonRow.add(cancelButton);

        mainPanel.add(headerLabel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(formGrid), BorderLayout.CENTER);
        mainPanel.add(buttonRow, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void chooseAttachment(boolean isVideo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(isVideo ? "Select Video File" : "Select Image File");

        if (isVideo) {
            chooser.setFileFilter(new FileNameExtensionFilter("Video Files (.mp4, .avi, .mov, .mkv)", "mp4", "avi", "mov", "mkv"));
        } else {
            chooser.setFileFilter(new FileNameExtensionFilter("Image Files (.jpg, .jpeg, .png, .webp)", "jpg", "jpeg", "png", "webp"));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                emergencyManager.validateFileAttachment(file.getAbsolutePath(), isVideo);
                attachmentPathField.setText(file.getAbsolutePath());
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "File Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleSubmitAsync() {
        String title = titleField.getText().trim();
        String description = descriptionArea.getText().trim();
        String category = (String) categoryComboBox.getSelectedItem();
        int severity = severityComboBox.getSelectedIndex() + 1;
        String location = locationField.getText().trim();
        int affectedPeople = (Integer) affectedPeopleSpinner.getValue();
        String mediaPath = attachmentPathField.getText().trim();

        EmergencyReport report = new EmergencyReport(
                currentUser.getId(), title, description, category, severity, location, affectedPeople, mediaPath
        );

        submitButton.setEnabled(false);
        submitButton.setText("SUBMITTING...");

        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                EmergencyManager.EmergencySubmissionResult result = emergencyManager.submitReport(report);

                SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("SUBMIT EMERGENCY");

                    String msg = String.format(
                            "Emergency Reported Successfully!\n\nReport ID: #%d\nIncident ID: #%d\nPriority Score: %.1f",
                            result.reportId(), result.incidentId(), result.calculatedPriority()
                    );
                    JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                });
            } catch (DatabaseAccessException ex) {
                SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("SUBMIT EMERGENCY");
                    JOptionPane.showMessageDialog(this, "Unable to submit the emergency right now. Please try again.", "Database Error", JOptionPane.ERROR_MESSAGE);
                });
            } catch (IllegalArgumentException | CampusEmergencyException ex) {
                SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    submitButton.setText("SUBMIT EMERGENCY");
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
                });
            }
        });
    }
}
