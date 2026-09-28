package com.campus.emergency.ui;

import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.NotificationBroker;
import com.campus.emergency.service.TrackingService;
import com.campus.emergency.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationPanel extends JPanel {
    private final NotificationBroker notificationBroker;
    private final TrackingService trackingService;
    private final ModernTable notificationTable;
    private final DefaultTableModel tableModel;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public NotificationPanel(NotificationBroker notificationBroker) {
        this(notificationBroker, null);
    }

    public NotificationPanel(NotificationBroker notificationBroker, TrackingService trackingService) {
        this.notificationBroker = notificationBroker;
        this.trackingService = trackingService;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 12, 12, 12));

        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 12), 16);

        // Header with Title and Actions (Create Broadcast + Refresh)
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);

        JLabel bellIcon = new JLabel(new VectorIcon(VectorIcon.Type.BELL, 18, UITheme.ACCENT_BLUE));
        titlePanel.add(bellIcon);

        JLabel title = new JLabel("Live Campus Alerts & Broadcast Feed");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        titlePanel.add(title);
        header.add(titlePanel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        ModernButton broadcastBtn = new ModernButton("Create Broadcast", VectorIcon.Type.BELL, ModernButton.ButtonStyle.PRIMARY);
        broadcastBtn.setPreferredSize(new Dimension(160, 34));
        broadcastBtn.addActionListener(e -> showCreateBroadcastDialog());
        actionPanel.add(broadcastBtn);

        ModernButton refreshBtn = new ModernButton("Refresh Alerts", VectorIcon.Type.REFRESH, ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(140, 34));
        refreshBtn.addActionListener(e -> refreshNotifications());
        actionPanel.add(refreshBtn);

        header.add(actionPanel, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        String[] columns = {"Timestamp", "Event Type", "Recipient Target", "Channel", "Alert Content"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        notificationTable = new ModernTable(tableModel);
        notificationTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        notificationTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        notificationTable.getColumnModel().getColumn(2).setPreferredWidth(130);
        notificationTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        notificationTable.getColumnModel().getColumn(4).setPreferredWidth(350);

        JScrollPane scrollPane = new JScrollPane(notificationTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        refreshNotifications();
    }

    private void showCreateBroadcastDialog() {
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, "Create Campus Broadcast Alert", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(window);
        dialog.setResizable(false);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel dialogTitle = new JLabel("Dispatch Campus Broadcast");
        dialogTitle.setFont(UITheme.FONT_TITLE);
        dialogTitle.setForeground(UITheme.TEXT_PRIMARY);
        content.add(dialogTitle, BorderLayout.NORTH);

        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        // Audience Selector
        JLabel audLabel = new JLabel("Target Audience:");
        audLabel.setFont(UITheme.FONT_BODY_BOLD);
        audLabel.setForeground(UITheme.TEXT_SECONDARY);
        formBody.add(audLabel);
        formBody.add(Box.createVerticalStrut(4));

        String[] audiences = {"Entire Campus", "Students", "Staff", "Responders"};
        JComboBox<String> audienceCombo = new JComboBox<>(audiences);
        audienceCombo.setFont(UITheme.FONT_BODY);
        audienceCombo.setBackground(Color.WHITE);
        audienceCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        formBody.add(audienceCombo);
        formBody.add(Box.createVerticalStrut(12));

        // Severity Selector
        JLabel sevLabel = new JLabel("Broadcast Severity Level:");
        sevLabel.setFont(UITheme.FONT_BODY_BOLD);
        sevLabel.setForeground(UITheme.TEXT_SECONDARY);
        formBody.add(sevLabel);
        formBody.add(Box.createVerticalStrut(4));

        String[] severities = {"INFO", "WARNING", "CRITICAL"};
        JComboBox<String> severityCombo = new JComboBox<>(severities);
        severityCombo.setFont(UITheme.FONT_BODY);
        severityCombo.setBackground(Color.WHITE);
        severityCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        formBody.add(severityCombo);
        formBody.add(Box.createVerticalStrut(12));

        // Message text area
        JLabel msgLabel = new JLabel("Broadcast Message Content *:");
        msgLabel.setFont(UITheme.FONT_BODY_BOLD);
        msgLabel.setForeground(UITheme.TEXT_PRIMARY);
        formBody.add(msgLabel);
        formBody.add(Box.createVerticalStrut(4));

        JTextArea msgArea = new JTextArea(4, 20);
        msgArea.setFont(UITheme.FONT_BODY);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane msgScroll = new JScrollPane(msgArea);
        msgScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        msgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        formBody.add(msgScroll);

        content.add(formBody, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(95, 36));
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(cancelBtn);

        ModernButton sendBtn = new ModernButton("Send Broadcast", VectorIcon.Type.BELL, ModernButton.ButtonStyle.PRIMARY);
        sendBtn.setPreferredSize(new Dimension(150, 36));
        sendBtn.addActionListener(e -> {
            String text = msgArea.getText() != null ? msgArea.getText().trim() : "";
            if (text.isEmpty()) {
                ModernDialog.showWarning(dialog, "Missing Content", "Please enter a broadcast message to dispatch.");
                return;
            }

            String selectedAud = (String) audienceCombo.getSelectedItem();
            String recipientCode;
            if ("Students".equalsIgnoreCase(selectedAud)) {
                recipientCode = "STUDENT";
            } else if ("Staff".equalsIgnoreCase(selectedAud)) {
                recipientCode = "STAFF";
            } else if ("Responders".equalsIgnoreCase(selectedAud)) {
                recipientCode = "DISPATCH_CENTER";
            } else {
                recipientCode = "ALL_STAKEHOLDERS";
            }

            String selectedSev = (String) severityCombo.getSelectedItem();
            NotificationBroker.EventType eventType;
            if ("CRITICAL".equalsIgnoreCase(selectedSev)) {
                eventType = NotificationBroker.EventType.EMERGENCY_REPORTED;
            } else if ("WARNING".equalsIgnoreCase(selectedSev)) {
                eventType = NotificationBroker.EventType.STATUS_CHANGED;
            } else {
                eventType = NotificationBroker.EventType.DISPATCH_UPDATED;
            }

            if (notificationBroker != null) {
                notificationBroker.notifyEvent(eventType, recipientCode, text, NotificationBroker.NotificationType.IN_APP);
            }

            dialog.dispose();
            ModernDialog.showSuccess(window, "Broadcast Sent", "Campus broadcast alert has been sent to " + selectedAud + ".");
            refreshNotifications();
        });
        btnRow.add(sendBtn);

        content.add(btnRow, BorderLayout.SOUTH);
        dialog.add(content);
        dialog.setVisible(true);
    }

    public void refreshNotifications() {
        User currentUser = AuthenticationContext.getCurrentUser();
        tableModel.setRowCount(0);

        List<NotificationBroker.NotificationMessage> messages;
        try {
            if (trackingService != null && currentUser != null) {
                messages = trackingService.getNotifications(currentUser);
            } else if (notificationBroker != null && currentUser != null) {
                messages = notificationBroker.getNotificationsForUser(currentUser);
            } else if (notificationBroker != null) {
                messages = notificationBroker.getSentNotifications();
            } else {
                return;
            }

            for (int i = messages.size() - 1; i >= 0; i--) {
                NotificationBroker.NotificationMessage msg = messages.get(i);
                String timeStr = msg.getTimestamp() != null ? msg.getTimestamp().format(formatter) : "";
                tableModel.addRow(new Object[]{
                        timeStr,
                        msg.getEventType() != null ? msg.getEventType().name() : "",
                        msg.getRecipient(),
                        msg.getType() != null ? msg.getType().name() : "",
                        msg.getContent()
                });
            }
        } catch (Exception e) {
            // Handle notification fetch failure gracefully
        }
    }
}
