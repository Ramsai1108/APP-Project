package com.campus.emergency.ui;

import com.campus.emergency.concurrency.ScheduledTaskManager;
import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.dao.IResourceDao;
import com.campus.emergency.dao.IResponderDao;
import com.campus.emergency.dao.impl.ResourceDaoImpl;
import com.campus.emergency.dao.impl.ResponderDaoImpl;
import com.campus.emergency.exception.AuthenticationException;
import com.campus.emergency.model.*;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.DispatcherDashboardService;
import com.campus.emergency.service.IdentityService;
import com.campus.emergency.service.IncidentStatusService;
import com.campus.emergency.service.ResourceDispatcher;
import com.campus.emergency.service.impl.DispatcherDashboardServiceImpl;
import com.campus.emergency.service.impl.IdentityServiceImpl;
import com.campus.emergency.service.impl.IncidentStatusServiceImpl;
import com.campus.emergency.service.impl.ResourceDispatcherImpl;
import com.campus.emergency.ui.components.NotificationPanel;
import com.campus.emergency.util.DateFormatterUtil;
import com.formdev.flatlaf.FlatDarkLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/**
 * Professional Dispatcher Control Center Frame.
 * Operational frontend for emergency dispatchers providing active incident tracking,
 * critical alert monitoring, status management, responder/resource monitoring, search/filtering, and auto-refresh.
 */
public class DispatcherDashboardFrame extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(DispatcherDashboardFrame.class);

    private final DispatcherDashboardService dashboardService;
    private final IncidentStatusService statusService;
    private final ResourceDispatcher resourceDispatcher;
    private final IdentityService identityService;
    private final User currentDispatcher;

    private final IResponderDao responderDao;
    private final IResourceDao resourceDao;

    private CardLayout cardLayout;
    private JPanel contentAreaPanel;

    // Metrics Counters
    private JLabel activeCounterLabel;
    private JLabel criticalCounterLabel;
    private JLabel responderCounterLabel;
    private JLabel resourceCounterLabel;

    // Active Incidents Table & Filter Controls
    private JTextField searchTextField;
    private JComboBox<String> categoryFilterCombo;
    private JComboBox<String> priorityFilterCombo;
    private JComboBox<String> statusFilterCombo;
    private JComboBox<String> sortCombo;
    private JCheckBox autoRefreshCheckBox;

    private JTable activeIncidentsTable;
    private DefaultTableModel activeIncidentsTableModel;
    private List<EmergencyIncident> currentIncidentCache;

    // Critical Alerts Table
    private DefaultTableModel criticalAlertsTableModel;
    private List<EmergencyIncident> criticalIncidentCache;

    // Responders Table
    private DefaultTableModel respondersTableModel;

    // Resources Table
    private DefaultTableModel resourcesTableModel;

    // Auto-refresh Scheduled Task
    private ScheduledFuture<?> autoRefreshTaskFuture;

    public DispatcherDashboardFrame(DispatcherDashboardService dashboardService,
                                   IncidentStatusService statusService,
                                   ResourceDispatcher resourceDispatcher,
                                   IdentityService identityService) {
        this.dashboardService = dashboardService != null ? dashboardService : new DispatcherDashboardServiceImpl();
        this.statusService = statusService != null ? statusService : new IncidentStatusServiceImpl();
        this.resourceDispatcher = resourceDispatcher != null ? resourceDispatcher : new ResourceDispatcherImpl();
        this.identityService = identityService != null ? identityService : new IdentityServiceImpl();
        this.responderDao = new ResponderDaoImpl();
        this.resourceDao = new ResourceDaoImpl();
        this.currentDispatcher = SessionManager.getCurrentUser();

        // 1. Strict Security Authorization Verification
        try {
            if (currentDispatcher == null) {
                throw new AuthenticationException("Access denied: User session is required.");
            }
            this.dashboardService.validateDispatcherAuthorization(currentDispatcher.getId());
        } catch (AuthenticationException ex) {
            JOptionPane.showMessageDialog(null,
                    "Access denied. You do not have permission to access the Dispatcher Control Center.",
                    "Unauthorized Access Rejection", JOptionPane.ERROR_MESSAGE);
            throw ex;
        }

        initUI();
        loadDashboardDataAsync();
        startAutoRefreshTimer();
    }

    public DispatcherDashboardFrame() {
        this(new DispatcherDashboardServiceImpl(), new IncidentStatusServiceImpl(), new ResourceDispatcherImpl(), new IdentityServiceImpl());
    }

    private void initUI() {
        FlatDarkLaf.setup();
        setTitle("Smart Campus Emergency System — Dispatcher Control Center");
        setSize(1240, 800);
        setMinimumSize(new Dimension(1050, 700));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainLayout = new JPanel(new BorderLayout());

        // Header
        mainLayout.add(createHeaderPanel(), BorderLayout.NORTH);

        // Sidebar Navigation
        mainLayout.add(createSidebarPanel(), BorderLayout.WEST);

        // Center Area (CardLayout)
        cardLayout = new CardLayout();
        contentAreaPanel = new JPanel(cardLayout);

        contentAreaPanel.add(createDashboardHomeView(), "DASHBOARD");
        contentAreaPanel.add(createCriticalAlertsView(), "CRITICAL_ALERTS");
        contentAreaPanel.add(createRespondersView(), "RESPONDERS");
        contentAreaPanel.add(createResourcesView(), "RESOURCES");
        contentAreaPanel.add(new NotificationPanel(), "NOTIFICATIONS");

        mainLayout.add(contentAreaPanel, BorderLayout.CENTER);
        add(mainLayout);

        switchView("DASHBOARD");
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 26, 32));
        header.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("🚨 DISPATCHER CONTROL CENTER");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(240, 80, 80));

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightBox.setOpaque(false);

        autoRefreshCheckBox = new JCheckBox("Auto-Refresh (5s)", true);
        autoRefreshCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        autoRefreshCheckBox.setForeground(new Color(180, 185, 195));
        autoRefreshCheckBox.setOpaque(false);
        autoRefreshCheckBox.addActionListener(e -> toggleAutoRefresh());

        JLabel userBadge = new JLabel("Dispatcher: " + currentDispatcher.getFullName() + " (" + currentDispatcher.getRoleName() + ")");
        userBadge.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userBadge.setForeground(new Color(200, 205, 215));

        rightBox.add(autoRefreshCheckBox);
        rightBox.add(userBadge);

        header.add(title, BorderLayout.WEST);
        header.add(rightBox, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(32, 35, 42));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JButton dashBtn = createNavButton("Active Incidents");
        JButton alertsBtn = createNavButton("Critical Alerts");
        JButton respondersBtn = createNavButton("Responders Monitor");
        JButton resourcesBtn = createNavButton("Resources Monitor");
        JButton notifBtn = createNavButton("Notifications");
        JButton logoutBtn = createNavButton("Logout");
        logoutBtn.setForeground(new Color(240, 90, 90));

        dashBtn.addActionListener(e -> switchView("DASHBOARD"));
        alertsBtn.addActionListener(e -> {
            switchView("CRITICAL_ALERTS");
            loadCriticalAlertsAsync();
        });
        respondersBtn.addActionListener(e -> {
            switchView("RESPONDERS");
            loadRespondersAsync();
        });
        resourcesBtn.addActionListener(e -> {
            switchView("RESOURCES");
            loadResourcesAsync();
        });
        notifBtn.addActionListener(e -> switchView("NOTIFICATIONS"));
        logoutBtn.addActionListener(e -> handleLogout());

        sidebar.add(dashBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(alertsBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(respondersBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(resourcesBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(notifBtn);
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
    // 1. DASHBOARD ACTIVE INCIDENTS VIEW
    // =========================================================================
    private JPanel createDashboardHomeView() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Top Metrics Row
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        activeCounterLabel = new JLabel("0", SwingConstants.CENTER);
        criticalCounterLabel = new JLabel("0", SwingConstants.CENTER);
        responderCounterLabel = new JLabel("0", SwingConstants.CENTER);
        resourceCounterLabel = new JLabel("0", SwingConstants.CENTER);

        cardsPanel.add(createCard("Active Incidents", activeCounterLabel, new Color(40, 120, 220)));
        cardsPanel.add(createCard("Critical Alerts", criticalCounterLabel, new Color(220, 53, 69)));
        cardsPanel.add(createCard("On-Duty Responders", responderCounterLabel, new Color(40, 167, 69)));
        cardsPanel.add(createCard("Available Resources", resourceCounterLabel, new Color(255, 165, 0)));

        // Search & Filter Toolbar
        JPanel filterToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterToolbar.setBorder(BorderFactory.createTitledBorder("Search & Filter Controls"));

        searchTextField = new JTextField(12);
        categoryFilterCombo = new JComboBox<>(new String[]{"ALL", "MEDICAL", "FIRE", "SECURITY", "ACCIDENT", "NATURAL_DISASTER", "OTHER"});
        priorityFilterCombo = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        statusFilterCombo = new JComboBox<>(new String[]{"ALL", "SUBMITTED", "OPEN", "PRIORITIZED", "DISPATCHED", "IN_PROGRESS", "RESOLVED", "CLOSED"});
        sortCombo = new JComboBox<>(new String[]{"PRIORITY", "CREATED_TIME", "STATUS"});

        JButton applyFilterBtn = new JButton("Filter / Search");
        JButton resetFilterBtn = new JButton("Reset");
        JButton refreshBtn = new JButton("Refresh");

        applyFilterBtn.addActionListener(e -> loadDashboardDataAsync());
        resetFilterBtn.addActionListener(e -> {
            searchTextField.setText("");
            categoryFilterCombo.setSelectedIndex(0);
            priorityFilterCombo.setSelectedIndex(0);
            statusFilterCombo.setSelectedIndex(0);
            sortCombo.setSelectedIndex(0);
            loadDashboardDataAsync();
        });
        refreshBtn.addActionListener(e -> loadDashboardDataAsync());

        filterToolbar.add(new JLabel("Search:"));
        filterToolbar.add(searchTextField);
        filterToolbar.add(new JLabel("Category:"));
        filterToolbar.add(categoryFilterCombo);
        filterToolbar.add(new JLabel("Priority:"));
        filterToolbar.add(priorityFilterCombo);
        filterToolbar.add(new JLabel("Status:"));
        filterToolbar.add(statusFilterCombo);
        filterToolbar.add(new JLabel("Sort By:"));
        filterToolbar.add(sortCombo);
        filterToolbar.add(applyFilterBtn);
        filterToolbar.add(resetFilterBtn);
        filterToolbar.add(refreshBtn);

        // Active Incidents JTable
        activeIncidentsTableModel = new DefaultTableModel(new String[]{"Incident ID", "Report ID", "Priority Level", "Priority Score", "Status", "Created At"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        activeIncidentsTable = new JTable(activeIncidentsTableModel);
        activeIncidentsTable.setRowHeight(28);
        activeIncidentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeIncidentsTable.setDefaultRenderer(Object.class, new IncidentTableRenderer());

        JPanel tableSection = new JPanel(new BorderLayout(5, 5));
        tableSection.setBorder(BorderFactory.createTitledBorder("Active Emergency Incidents"));

        JPanel tableBottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton viewDetailsBtn = new JButton("🔍 View & Control Incident");
        viewDetailsBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        viewDetailsBtn.setBackground(new Color(40, 120, 220));
        viewDetailsBtn.setForeground(Color.WHITE);
        viewDetailsBtn.addActionListener(e -> openIncidentDetailsDialog());

        tableBottomBar.add(viewDetailsBtn);
        tableSection.add(new JScrollPane(activeIncidentsTable), BorderLayout.CENTER);
        tableSection.add(tableBottomBar, BorderLayout.SOUTH);

        JPanel centerContainer = new JPanel(new BorderLayout(10, 10));
        centerContainer.add(filterToolbar, BorderLayout.NORTH);
        centerContainer.add(tableSection, BorderLayout.CENTER);

        panel.add(cardsPanel, BorderLayout.NORTH);
        panel.add(centerContainer, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                new EmptyBorder(12, 12, 12, 12)
        ));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(180, 185, 195));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // 2. CRITICAL ALERTS VIEW
    // =========================================================================
    private JPanel createCriticalAlertsView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("🚨 CRITICAL & HIGH PRIORITY EMERGENCY ALERTS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(230, 70, 70));

        criticalAlertsTableModel = new DefaultTableModel(new String[]{"Incident ID", "Priority Score", "Status", "Created At"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable criticalTable = new JTable(criticalAlertsTableModel);
        criticalTable.setRowHeight(28);
        criticalTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        criticalTable.setDefaultRenderer(Object.class, new IncidentTableRenderer());

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton viewAlertBtn = new JButton("Inspect Critical Incident");
        JButton refreshAlertsBtn = new JButton("Refresh");

        viewAlertBtn.addActionListener(e -> {
            int row = criticalTable.getSelectedRow();
            if (row >= 0 && criticalIncidentCache != null && row < criticalIncidentCache.size()) {
                EmergencyIncident selected = criticalIncidentCache.get(row);
                new IncidentDetailsFrame(this, selected, statusService, resourceDispatcher, this::loadDashboardDataAsync).setVisible(true);
            }
        });
        refreshAlertsBtn.addActionListener(e -> loadCriticalAlertsAsync());

        bottomBar.add(viewAlertBtn);
        bottomBar.add(refreshAlertsBtn);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(criticalTable), BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // 3. RESPONDERS MONITOR VIEW
    // =========================================================================
    private JPanel createRespondersView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("RESPONDER MONITORING & ON-DUTY STATUS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        respondersTableModel = new DefaultTableModel(new String[]{"ID", "Name", "Specialization", "Phone", "Duty Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(respondersTableModel);
        table.setRowHeight(26);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton refreshBtn = new JButton("Refresh Responders");
        refreshBtn.addActionListener(e -> loadRespondersAsync());
        bottomBar.add(refreshBtn);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // 4. RESOURCES MONITOR VIEW
    // =========================================================================
    private JPanel createResourcesView() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("EQUIPMENT & RESOURCE MONITORING");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        resourcesTableModel = new DefaultTableModel(new String[]{"ID", "Resource Name", "Type", "Status", "Location ID"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(resourcesTableModel);
        table.setRowHeight(26);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton refreshBtn = new JButton("Refresh Resources");
        refreshBtn.addActionListener(e -> loadResourcesAsync());
        bottomBar.add(refreshBtn);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // DATA LOADING & CONCURRENCY
    // =========================================================================
    private void loadDashboardDataAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                DispatcherDashboardMetrics metrics = dashboardService.getDashboardMetrics(currentDispatcher.getId());

                String query = searchTextField.getText();
                String category = (String) categoryFilterCombo.getSelectedItem();
                String priorityLevel = (String) priorityFilterCombo.getSelectedItem();
                String status = (String) statusFilterCombo.getSelectedItem();
                String sortBy = (String) sortCombo.getSelectedItem();

                List<EmergencyIncident> filtered = dashboardService.searchAndFilterIncidents(currentDispatcher.getId(), query, category, priorityLevel, status);
                List<EmergencyIncident> sorted = dashboardService.sortIncidents(filtered, sortBy);
                currentIncidentCache = sorted;

                SwingUtilities.invokeLater(() -> {
                    activeCounterLabel.setText(String.valueOf(metrics.activeIncidentCount()));
                    criticalCounterLabel.setText(String.valueOf(metrics.criticalIncidentCount()));
                    responderCounterLabel.setText(String.valueOf(metrics.availableResponderCount()));
                    resourceCounterLabel.setText(String.valueOf(metrics.availableResourceCount()));

                    activeIncidentsTableModel.setRowCount(0);
                    for (EmergencyIncident i : sorted) {
                        double p = i.getEffectivePriority();
                        String level = p >= 80.0 ? "🔴 CRITICAL" : (p >= 60.0 ? "🟠 HIGH" : (p >= 40.0 ? "🟡 MEDIUM" : "🟢 LOW"));
                        activeIncidentsTableModel.addRow(new Object[]{
                                "#" + i.getId(),
                                "#" + i.getReportId(),
                                level,
                                String.format("%.1f", p),
                                i.getStatus(),
                                DateFormatterUtil.formatDateTime(i.getCreatedAt())
                        });
                    }
                });
            } catch (Exception ex) {
                logger.warn("Background dashboard data load failed: {}", ex.getMessage());
            }
        });
    }

    private void loadCriticalAlertsAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                List<EmergencyIncident> criticals = dashboardService.getCriticalIncidents(currentDispatcher.getId());
                criticalIncidentCache = criticals;

                SwingUtilities.invokeLater(() -> {
                    criticalAlertsTableModel.setRowCount(0);
                    for (EmergencyIncident i : criticals) {
                        criticalAlertsTableModel.addRow(new Object[]{
                                "#" + i.getId(),
                                String.format("%.1f", i.getEffectivePriority()),
                                i.getStatus(),
                                DateFormatterUtil.formatDateTime(i.getCreatedAt())
                        });
                    }
                });
            } catch (Exception ex) {
                logger.warn("Background critical alerts load failed: {}", ex.getMessage());
            }
        });
    }

    private void loadRespondersAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                List<Responder> responders = responderDao.findAll();
                SwingUtilities.invokeLater(() -> {
                    respondersTableModel.setRowCount(0);
                    for (Responder r : responders) {
                        respondersTableModel.addRow(new Object[]{
                                "#" + r.getId(),
                                r.getFullName(),
                                r.getSpecialization(),
                                r.getPhone(),
                                r.getDutyStatus()
                        });
                    }
                });
            } catch (Exception ex) {
                logger.warn("Background responders load failed: {}", ex.getMessage());
            }
        });
    }

    private void loadResourcesAsync() {
        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                List<Resource> resources = resourceDao.findAll();
                SwingUtilities.invokeLater(() -> {
                    resourcesTableModel.setRowCount(0);
                    for (Resource r : resources) {
                        resourcesTableModel.addRow(new Object[]{
                                "#" + r.getId(),
                                r.getResourceName(),
                                r.getResourceType(),
                                r.getCurrentStatus(),
                                "#" + r.getLocationId()
                        });
                    }
                });
            } catch (Exception ex) {
                logger.warn("Background resources load failed: {}", ex.getMessage());
            }
        });
    }

    private boolean isAutoRefreshActive = true;

    private void startAutoRefreshTimer() {
        isAutoRefreshActive = true;
        ScheduledTaskManager.schedulePeriodicTask(() -> {
            if (isAutoRefreshActive) {
                loadDashboardDataAsync();
            }
        }, 5, 5);
    }

    private void toggleAutoRefresh() {
        isAutoRefreshActive = autoRefreshCheckBox.isSelected();
    }

    private void openIncidentDetailsDialog() {
        int selectedRow = activeIncidentsTable.getSelectedRow();
        if (selectedRow < 0 || currentIncidentCache == null || selectedRow >= currentIncidentCache.size()) {
            JOptionPane.showMessageDialog(this, "Please select an incident from the active incidents table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EmergencyIncident incident = currentIncidentCache.get(selectedRow);
        new IncidentDetailsFrame(this, incident, statusService, resourceDispatcher, this::loadDashboardDataAsync).setVisible(true);
    }

    private void switchView(String viewName) {
        cardLayout.show(contentAreaPanel, viewName);
    }

    private void handleLogout() {
        isAutoRefreshActive = false;
        identityService.logout();
        new LoginFrame(identityService).setVisible(true);
        this.dispose();
    }

    /**
     * Table Cell Renderer for highlighting critical status and priorities.
     */
    private static class IncidentTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String valStr = String.valueOf(value);
                if (valStr.contains("CRITICAL")) {
                    c.setBackground(new Color(60, 20, 20));
                    c.setForeground(new Color(255, 120, 120));
                } else if (valStr.contains("HIGH")) {
                    c.setBackground(new Color(50, 35, 15));
                    c.setForeground(new Color(255, 180, 80));
                } else {
                    c.setBackground(table.getBackground());
                    c.setForeground(table.getForeground());
                }
            }
            return c;
        }
    }
}
