package com.campus.emergency.ui;

import com.campus.emergency.dao.LocationDAO;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.EmergencyManager;
import com.campus.emergency.service.ResourceDispatcher;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.service.TrackingService;
import com.campus.emergency.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Optional;

public class TrackingDashboardFrame extends JFrame {
    private final TrackingService trackingService;
    private final ResourceDispatcher resourceDispatcher;
    private final EmergencyManager emergencyManager;
    private final LocationDAO locationDAO;

    private ModernTable incidentsTable;
    private DefaultTableModel incidentsModel;
    private JComboBox<ResponderItem> responderComboBox;
    private JComboBox<ResourceItem> resourceComboBox;
    private JComboBox<String> statusComboBox;
    private NotificationPanel notificationPanel;

    private MetricCard criticalCard;
    private MetricCard activeCard;
    private MetricCard responderCard;
    private MetricCard resourceCard;

    private JLabel selectedIncidentLabel;
    private JLabel detailsCategoryLabel;
    private JLabel detailsPriorityLabel;
    private JLabel detailsStatusLabel;
    private JLabel detailsLocationLabel;
    private JTextArea detailsDescArea;
    private StatusTimelinePanel timelinePanel;

    private EmergencyIncident currentSelectedIncident = null;

    public static class ResponderItem {
        private final Responder responder;
        public ResponderItem(Responder responder) { this.responder = responder; }
        public Responder getResponder() { return responder; }
        @Override
        public String toString() {
            if (responder == null) return "-- Select Available Responder --";
            return "Responder #" + responder.getId() + " [" + responder.getSpecialization() + "] (" + responder.getStatus() + ")";
        }
    }

    public static class ResourceItem {
        private final Resource resource;
        public ResourceItem(Resource resource) { this.resource = resource; }
        public Resource getResource() { return resource; }
        @Override
        public String toString() {
            if (resource == null) return "-- Select Available Resource --";
            return resource.getName() + " [" + resource.getType() + "] (" + resource.getStatus() + ")";
        }
    }

    public TrackingDashboardFrame() {
        this(ServiceRegistry.getInstance().getTrackingService(),
                ServiceRegistry.getInstance().getResourceDispatcher(),
                ServiceRegistry.getInstance().getEmergencyManager(),
                ServiceRegistry.getInstance().getLocationDAO());
    }

    public TrackingDashboardFrame(TrackingService trackingService, ResourceDispatcher resourceDispatcher,
                                  EmergencyManager emergencyManager, LocationDAO locationDAO) {
        this.trackingService = (trackingService != null) ? trackingService : ServiceRegistry.getInstance().getTrackingService();
        this.resourceDispatcher = (resourceDispatcher != null) ? resourceDispatcher : ServiceRegistry.getInstance().getResourceDispatcher();
        this.emergencyManager = (emergencyManager != null) ? emergencyManager : ServiceRegistry.getInstance().getEmergencyManager();
        this.locationDAO = (locationDAO != null) ? locationDAO : ServiceRegistry.getInstance().getLocationDAO();

        initUI();
        refreshDashboard();
    }

    private void initUI() {
        User user = AuthenticationContext.getCurrentUser();
        String name = user != null ? user.getFullName() : "Dispatcher";

        setTitle("Campus Emergency Response - Dispatch & Operations Center [" + name + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 700));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 16));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ==========================================
        // 1. TOP HEADER & METRICS ROW
        // ==========================================
        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);

        // Header Title Bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("Emergency Dispatch & Operations Center");
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        titleBlock.add(title);

        JLabel sub = new JLabel("Real-time campus incident queue, responder deployment & resource allocation");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titleBlock.add(sub);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topActions.setOpaque(false);

        ModernButton refreshBtn = new ModernButton("Refresh", VectorIcon.Type.REFRESH, ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.addActionListener(e -> refreshDashboard());

        ModernButton logoutBtn = new ModernButton("Logout", VectorIcon.Type.LOGOUT, ModernButton.ButtonStyle.DARK);
        logoutBtn.addActionListener(e -> handleLogout());

        topActions.add(refreshBtn);
        topActions.add(logoutBtn);
        headerBar.add(topActions, BorderLayout.EAST);

        topContainer.add(headerBar, BorderLayout.NORTH);

        // 4 Metric Stat Cards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 14, 0));
        statsRow.setOpaque(false);

        criticalCard = new MetricCard(VectorIcon.Type.ALERT, "Critical Incidents", "0", "Requires immediate dispatch", UITheme.EMERGENCY_RED);
        activeCard = new MetricCard(VectorIcon.Type.ACTIVITY, "Active Queue", "0", "Open or in progress", UITheme.WARNING_ORANGE);
        responderCard = new MetricCard(VectorIcon.Type.USER, "Available Responders", "0", "Ready for deployment", UITheme.ACCENT_BLUE);
        resourceCard = new MetricCard(VectorIcon.Type.BUILDING, "Available Resources", "0", "Vehicles, kits & gear", UITheme.SUCCESS_GREEN);

        statsRow.add(criticalCard);
        statsRow.add(activeCard);
        statsRow.add(responderCard);
        statsRow.add(resourceCard);
        topContainer.add(statsRow, BorderLayout.SOUTH);

        mainPanel.add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. TABS: QUEUE & DISPATCH / NOTIFICATIONS
        // ==========================================
        ModernTabbedPane tabbedPane = new ModernTabbedPane();

        // Tab 1: Dispatch Operations 3-Column Center
        JPanel opsPanel = new JPanel(new BorderLayout(14, 0));
        opsPanel.setOpaque(false);
        opsPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // LEFT: Incidents Queue Table Card
        ModernCardPanel leftQueueCard = new ModernCardPanel(new BorderLayout(0, 10), 14);
        leftQueueCard.setPreferredSize(new Dimension(540, opsPanel.getHeight()));

        JLabel queueTitle = new JLabel("Active Incident Queue");
        queueTitle.setFont(UITheme.FONT_TITLE);
        queueTitle.setForeground(UITheme.TEXT_PRIMARY);
        leftQueueCard.add(queueTitle, BorderLayout.NORTH);

        String[] columns = {"ID", "Priority", "Status", "Dispatcher"};
        incidentsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        incidentsTable = new ModernTable(incidentsModel);
        incidentsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        incidentsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        incidentsTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        incidentsTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        incidentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleIncidentSelection();
            }
        });

        JScrollPane queueScroll = new JScrollPane(incidentsTable);
        queueScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        leftQueueCard.add(queueScroll, BorderLayout.CENTER);

        opsPanel.add(leftQueueCard, BorderLayout.WEST);

        // RIGHT: Selected Details & Action Dispatch Panel
        ModernCardPanel rightActionCard = new ModernCardPanel(new BorderLayout(0, 14), 16);

        // Details Top
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);

        selectedIncidentLabel = new JLabel("Select an incident from the queue");
        selectedIncidentLabel.setFont(UITheme.FONT_TITLE);
        selectedIncidentLabel.setForeground(UITheme.TEXT_PRIMARY);
        detailsPanel.add(selectedIncidentLabel);
        detailsPanel.add(Box.createVerticalStrut(10));

        // Timeline Step Indicator
        timelinePanel = new StatusTimelinePanel("OPEN");
        detailsPanel.add(timelinePanel);
        detailsPanel.add(Box.createVerticalStrut(12));

        // Meta info grid
        JPanel metaGrid = new JPanel(new GridLayout(2, 2, 8, 8));
        metaGrid.setOpaque(false);

        detailsPriorityLabel = new JLabel("Priority: --");
        detailsPriorityLabel.setFont(UITheme.FONT_BODY_BOLD);
        detailsPriorityLabel.setForeground(UITheme.TEXT_SECONDARY);

        detailsStatusLabel = new JLabel("Status: --");
        detailsStatusLabel.setFont(UITheme.FONT_BODY_BOLD);
        detailsStatusLabel.setForeground(UITheme.TEXT_SECONDARY);

        detailsLocationLabel = new JLabel("Location: --");
        detailsLocationLabel.setFont(UITheme.FONT_BODY);
        detailsLocationLabel.setForeground(UITheme.TEXT_SECONDARY);

        detailsCategoryLabel = new JLabel("Category: --");
        detailsCategoryLabel.setFont(UITheme.FONT_BODY);
        detailsCategoryLabel.setForeground(UITheme.TEXT_SECONDARY);

        metaGrid.add(detailsPriorityLabel);
        metaGrid.add(detailsStatusLabel);
        metaGrid.add(detailsLocationLabel);
        metaGrid.add(detailsCategoryLabel);
        detailsPanel.add(metaGrid);
        detailsPanel.add(Box.createVerticalStrut(10));

        JLabel descHeader = new JLabel("Incident Description:");
        descHeader.setFont(UITheme.FONT_BODY_BOLD);
        descHeader.setForeground(UITheme.TEXT_SECONDARY);
        detailsPanel.add(descHeader);
        detailsPanel.add(Box.createVerticalStrut(4));

        detailsDescArea = new JTextArea(3, 20);
        detailsDescArea.setFont(UITheme.FONT_BODY);
        detailsDescArea.setLineWrap(true);
        detailsDescArea.setWrapStyleWord(true);
        detailsDescArea.setEditable(false);
        detailsDescArea.setBackground(new Color(248, 250, 252));
        detailsDescArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER),
                new EmptyBorder(6, 8, 6, 8)
        ));
        detailsPanel.add(detailsDescArea);

        rightActionCard.add(detailsPanel, BorderLayout.NORTH);

        // Dispatch Controls Center / Bottom
        JPanel dispatchForm = new JPanel(new GridLayout(3, 1, 0, 12));
        dispatchForm.setOpaque(false);
        dispatchForm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.CARD_BORDER),
                new EmptyBorder(12, 0, 0, 0)
        ));

        // 1. Responder Assignment Row
        JPanel respRow = new JPanel(new BorderLayout(10, 0));
        respRow.setOpaque(false);
        responderComboBox = new JComboBox<>();
        responderComboBox.setFont(UITheme.FONT_BODY);
        responderComboBox.setBackground(Color.WHITE);

        ModernButton assignRespBtn = new ModernButton("Assign Responder", ModernButton.ButtonStyle.PRIMARY);
        assignRespBtn.setPreferredSize(new Dimension(160, 36));
        assignRespBtn.addActionListener(e -> handleAssignResponder());

        respRow.add(responderComboBox, BorderLayout.CENTER);
        respRow.add(assignRespBtn, BorderLayout.EAST);
        dispatchForm.add(respRow);

        // 2. Resource Assignment Row
        JPanel resRow = new JPanel(new BorderLayout(10, 0));
        resRow.setOpaque(false);
        resourceComboBox = new JComboBox<>();
        resourceComboBox.setFont(UITheme.FONT_BODY);
        resourceComboBox.setBackground(Color.WHITE);

        ModernButton assignResBtn = new ModernButton("Assign Resource", ModernButton.ButtonStyle.PRIMARY);
        assignResBtn.setPreferredSize(new Dimension(160, 36));
        assignResBtn.addActionListener(e -> handleAssignResource());

        resRow.add(resourceComboBox, BorderLayout.CENTER);
        resRow.add(assignResBtn, BorderLayout.EAST);
        dispatchForm.add(resRow);

        // 3. Status Transition Row
        JPanel stRow = new JPanel(new BorderLayout(10, 0));
        stRow.setOpaque(false);
        String[] statuses = {"OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "CLOSED"};
        statusComboBox = new JComboBox<>(statuses);
        statusComboBox.setFont(UITheme.FONT_BODY);
        statusComboBox.setBackground(Color.WHITE);

        ModernButton updateStBtn = new ModernButton("Update Status", ModernButton.ButtonStyle.SUCCESS);
        updateStBtn.setPreferredSize(new Dimension(160, 36));
        updateStBtn.addActionListener(e -> handleUpdateStatus());

        stRow.add(statusComboBox, BorderLayout.CENTER);
        stRow.add(updateStBtn, BorderLayout.EAST);
        dispatchForm.add(stRow);

        rightActionCard.add(dispatchForm, BorderLayout.CENTER);
        opsPanel.add(rightActionCard, BorderLayout.CENTER);

        tabbedPane.addTab("Operational Dispatch Queue", opsPanel);

        // Tab 2: Notification Center
        notificationPanel = new NotificationPanel(
                ServiceRegistry.getInstance().getNotificationBroker(),
                trackingService
        );
        tabbedPane.addTab("Live Alerts & Broadcasts", notificationPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    public void refreshDashboard() {
        User user = AuthenticationContext.getCurrentUser();
        if (user == null) return;

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<EmergencyIncident> activeIncidents = trackingService.getActiveIncidents(user);
                List<Responder> availableResponders = resourceDispatcher.getAvailableResponders();
                List<Resource> availableResources = resourceDispatcher.getAvailableResources();

                long criticalCount = activeIncidents.stream()
                        .filter(i -> "CRITICAL".equalsIgnoreCase(i.getPriority()))
                        .count();

                SwingUtilities.invokeLater(() -> {
                    criticalCard.setValue(String.valueOf(criticalCount));
                    activeCard.setValue(String.valueOf(activeIncidents.size()));
                    responderCard.setValue(String.valueOf(availableResponders.size()));
                    resourceCard.setValue(String.valueOf(availableResources.size()));

                    incidentsModel.setRowCount(0);
                    for (EmergencyIncident inc : activeIncidents) {
                        String dispStr = inc.getAssignedDispatcherId() != null ? "Disp #" + inc.getAssignedDispatcherId() : "Unassigned";
                        incidentsModel.addRow(new Object[]{
                                inc.getId(),
                                inc.getPriority(),
                                inc.getStatus(),
                                dispStr
                        });
                    }

                    responderComboBox.removeAllItems();
                    responderComboBox.addItem(new ResponderItem(null));
                    for (Responder resp : availableResponders) {
                        responderComboBox.addItem(new ResponderItem(resp));
                    }

                    resourceComboBox.removeAllItems();
                    resourceComboBox.addItem(new ResourceItem(null));
                    for (Resource res : availableResources) {
                        resourceComboBox.addItem(new ResourceItem(res));
                    }

                    if (notificationPanel != null) {
                        notificationPanel.refreshNotifications();
                    }
                });
            } catch (Exception e) {
                LoggingService.error("Failed to refresh tracking dashboard: " + e.getMessage(), e);
            }
        });
    }

    private void handleIncidentSelection() {
        int row = incidentsTable.getSelectedRow();
        if (row < 0) return;

        int incidentId = (Integer) incidentsModel.getValueAt(row, 0);
        User user = AuthenticationContext.getCurrentUser();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                Optional<EmergencyIncident> incOpt = trackingService.getIncidentDetails(user, incidentId);
                if (incOpt.isPresent()) {
                    EmergencyIncident inc = incOpt.get();
                    currentSelectedIncident = inc;
                    Optional<Location> locOpt = trackingService.getLocation(inc.getReportId());

                    SwingUtilities.invokeLater(() -> {
                        selectedIncidentLabel.setText("Incident #" + inc.getId());
                        timelinePanel.setStatus(inc.getStatus());
                        detailsPriorityLabel.setText("Priority: " + inc.getPriority());
                        detailsStatusLabel.setText("Status: " + inc.getStatus());
                        detailsLocationLabel.setText("Location: " + locOpt.map(Location::getBuildingName).orElse("Campus Main"));
                        detailsCategoryLabel.setText("Report ID: #" + inc.getReportId());
                        detailsDescArea.setText("Incident #" + inc.getId() + " is currently " + inc.getStatus() + ". Assigned to: " +
                                (inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "Unassigned"));
                        statusComboBox.setSelectedItem(inc.getStatus());
                    });
                }
            } catch (Exception e) {
                LoggingService.error("Failed to load incident details: " + e.getMessage(), e);
            }
        });
    }

    public void handleAssignResponder() {
        if (currentSelectedIncident == null) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident from the queue first.");
            return;
        }

        ResponderItem item = (ResponderItem) responderComboBox.getSelectedItem();
        if (item == null || item.getResponder() == null) {
            ModernDialog.showWarning(this, "Select Responder", "Please select an available responder to assign.");
            return;
        }

        User user = AuthenticationContext.getCurrentUser();
        int incidentId = currentSelectedIncident.getId();
        int responderId = item.getResponder().getId();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                boolean success = resourceDispatcher.assignResponderToIncident(user, incidentId, responderId);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        ModernDialog.showSuccess(this, "Responder Dispatched", "Responder #" + responderId + " assigned to Incident #" + incidentId);
                        refreshDashboard();
                    } else {
                        ModernDialog.showError(this, "Assignment Failed", "Could not assign responder. Please check availability.");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        ModernDialog.showError(this, "Assignment Error", e.getMessage())
                );
            }
        });
    }

    public void handleAssignResource() {
        if (currentSelectedIncident == null) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident from the queue first.");
            return;
        }

        ResourceItem item = (ResourceItem) resourceComboBox.getSelectedItem();
        if (item == null || item.getResource() == null) {
            ModernDialog.showWarning(this, "Select Resource", "Please select an available resource to assign.");
            return;
        }

        User user = AuthenticationContext.getCurrentUser();
        int incidentId = currentSelectedIncident.getId();
        int resourceId = item.getResource().getId();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                boolean success = resourceDispatcher.assignResourceToIncident(user, resourceId, incidentId);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        ModernDialog.showSuccess(this, "Resource Dispatched", "Resource #" + resourceId + " assigned to Incident #" + incidentId);
                        refreshDashboard();
                    } else {
                        ModernDialog.showError(this, "Assignment Failed", "Could not assign resource. Please check availability.");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        ModernDialog.showError(this, "Assignment Error", e.getMessage())
                );
            }
        });
    }

    public void handleUpdateStatus() {
        if (currentSelectedIncident == null) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident from the queue first.");
            return;
        }

        String newStatus = (String) statusComboBox.getSelectedItem();
        int incidentId = currentSelectedIncident.getId();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                boolean success = emergencyManager.updateIncidentStatus(incidentId, newStatus);
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        ModernDialog.showSuccess(this, "Status Updated", "Incident #" + incidentId + " status updated to " + newStatus);
                        refreshDashboard();
                    } else {
                        ModernDialog.showError(this, "Update Failed", "Invalid status transition.");
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        ModernDialog.showError(this, "Status Error", e.getMessage())
                );
            }
        });
    }

    public void handleLogout() {
        AuthenticationContext.clear();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
        dispose();
    }
}
