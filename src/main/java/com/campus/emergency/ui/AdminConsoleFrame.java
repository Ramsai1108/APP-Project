package com.campus.emergency.ui;

import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.dao.LocationDAO;
import com.campus.emergency.dao.SystemLogDAO;
import com.campus.emergency.dao.UserDAO;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.*;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.*;
import com.campus.emergency.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminConsoleFrame extends JFrame {
    private final TrackingService trackingService;
    private final DataAggregator dataAggregator;
    private final SystemLogDAO logDAO;
    private final EmergencyManager emergencyManager;
    private final ResourceDispatcher resourceDispatcher;
    private final EmergencyReportDAO reportDAO;
    private final LocationDAO locationDAO;
    private final UserDAO userDAO;

    // Header Health Strip
    private JLabel lastRefreshLabel;

    // 6 Top KPI Metric Cards
    private MetricCard totalCard;
    private MetricCard activeCard;
    private MetricCard criticalCard;
    private MetricCard resolvedCard;
    private MetricCard responderCard;
    private MetricCard resourceCard;

    // Executive Overview Visualizations
    private SimpleBarChart priorityChart;
    private SimpleBarChart statusChart;
    private ModernTable overviewIncidentsTable;
    private DefaultTableModel overviewIncidentsModel;
    private ModernTable overviewLogsTable;
    private DefaultTableModel overviewLogsModel;

    // Incident Management Tab
    private ModernTable incidentsTable;
    private DefaultTableModel incidentsModel;
    private JComboBox<String> statusFilterBox;
    private JComboBox<String> priorityFilterBox;
    private List<EmergencyIncident> cachedAllIncidents = new ArrayList<>();

    // Audit Logs Tab
    private ModernTable logsTable;
    private DefaultTableModel logsModel;
    private JComboBox<String> logEventTypeFilterBox;
    private ModernTextField logSearchField;
    private List<SystemLog> cachedAllLogs = new ArrayList<>();

    // Broadcasts Tab
    private NotificationPanel notificationPanel;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public AdminConsoleFrame() {
        this(ServiceRegistry.getInstance().getTrackingService(),
                ServiceRegistry.getInstance().getDataAggregator(),
                ServiceRegistry.getInstance().getLogDAO(),
                ServiceRegistry.getInstance().getEmergencyManager(),
                ServiceRegistry.getInstance().getResourceDispatcher());
    }

    public AdminConsoleFrame(TrackingService trackingService, DataAggregator dataAggregator,
                             SystemLogDAO logDAO, EmergencyManager emergencyManager,
                             ResourceDispatcher resourceDispatcher) {
        this.trackingService = (trackingService != null) ? trackingService : ServiceRegistry.getInstance().getTrackingService();
        this.dataAggregator = (dataAggregator != null) ? dataAggregator : ServiceRegistry.getInstance().getDataAggregator();
        this.logDAO = (logDAO != null) ? logDAO : ServiceRegistry.getInstance().getLogDAO();
        this.emergencyManager = (emergencyManager != null) ? emergencyManager : ServiceRegistry.getInstance().getEmergencyManager();
        this.resourceDispatcher = (resourceDispatcher != null) ? resourceDispatcher : ServiceRegistry.getInstance().getResourceDispatcher();
        this.reportDAO = ServiceRegistry.getInstance().getReportDAO();
        this.locationDAO = ServiceRegistry.getInstance().getLocationDAO();
        this.userDAO = ServiceRegistry.getInstance().getUserDAO();

        initUI();
        refreshConsole();
    }

    private void initUI() {
        User admin = AuthenticationContext.getCurrentUser();
        String name = admin != null ? admin.getFullName() : "Administrator";

        setTitle("Campus Emergency Response - System Administrator Console [" + name + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 860);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1040, 720));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UITheme.BG_MAIN);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ==========================================
        // 1. ADMIN CONSOLE HEADER & HEALTH STRIP
        // ==========================================
        JPanel topHeaderContainer = new JPanel(new BorderLayout(0, 10));
        topHeaderContainer.setOpaque(false);

        // Header Top Row
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel topSuperTitle = new JLabel("SYSTEM ADMINISTRATOR");
        topSuperTitle.setFont(UITheme.FONT_KPI_LABEL);
        topSuperTitle.setForeground(UITheme.ACCENT_BLUE);
        titleBlock.add(topSuperTitle);
        titleBlock.add(Box.createVerticalStrut(2));

        JLabel title = new JLabel("Emergency Operations Center");
        title.setFont(UITheme.FONT_APP_TITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        titleBlock.add(title);
        titleBlock.add(Box.createVerticalStrut(2));

        JLabel sub = new JLabel("Real-time incident monitoring, response coordination & system oversight");
        sub.setFont(UITheme.FONT_BODY);
        sub.setForeground(UITheme.TEXT_SECONDARY);
        titleBlock.add(sub);
        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        topActions.setOpaque(false);

        ModernButton refreshBtn = new ModernButton("Refresh Console", VectorIcon.Type.REFRESH, ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(150, 36));
        refreshBtn.addActionListener(e -> refreshConsole());

        ModernButton logoutBtn = new ModernButton("Logout", VectorIcon.Type.LOGOUT, ModernButton.ButtonStyle.DARK);
        logoutBtn.setPreferredSize(new Dimension(110, 36));
        logoutBtn.addActionListener(e -> handleLogout());

        topActions.add(refreshBtn);
        topActions.add(logoutBtn);
        headerBar.add(topActions, BorderLayout.EAST);
        topHeaderContainer.add(headerBar, BorderLayout.NORTH);

        // System Health Strip
        JPanel healthStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        healthStrip.setBackground(Color.WHITE);
        healthStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(2, 8, 2, 8)
        ));

        healthStrip.add(createHealthBadge("SYSTEM STATUS", "OPERATIONAL", UITheme.SUCCESS_GREEN));
        healthStrip.add(createHealthBadge("DATABASE", "CONNECTED", UITheme.SUCCESS_GREEN));
        healthStrip.add(createHealthBadge("DISPATCH SERVICE", "ACTIVE", UITheme.ACCENT_BLUE));

        lastRefreshLabel = new JLabel("LAST REFRESH: " + LocalDateTime.now().format(timeOnlyFormatter));
        lastRefreshLabel.setFont(UITheme.FONT_META);
        lastRefreshLabel.setForeground(UITheme.TEXT_MUTED);
        healthStrip.add(lastRefreshLabel);

        topHeaderContainer.add(healthStrip, BorderLayout.SOUTH);
        mainPanel.add(topHeaderContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. MODERN TABBED NAVIGATION
        // ==========================================
        ModernTabbedPane tabbedPane = new ModernTabbedPane();

        // ------------------------------------------
        // TAB 1: EXECUTIVE OVERVIEW
        // ------------------------------------------
        JPanel overviewTab = new JPanel(new BorderLayout(0, 14));
        overviewTab.setOpaque(false);
        overviewTab.setBorder(new EmptyBorder(8, 0, 8, 0));

        // TOP: 6 KPI Cards
        JPanel metricsGrid = new JPanel(new GridLayout(1, 6, 10, 0));
        metricsGrid.setOpaque(false);

        totalCard = new MetricCard(VectorIcon.Type.LIST, "TOTAL INCIDENTS", "0", UITheme.ACCENT_BLUE);
        activeCard = new MetricCard(VectorIcon.Type.ACTIVITY, "ACTIVE INCIDENTS", "0", UITheme.WARNING_ORANGE);
        criticalCard = new MetricCard(VectorIcon.Type.ALERT, "CRITICAL", "0", UITheme.EMERGENCY_RED);
        resolvedCard = new MetricCard(VectorIcon.Type.CHECK, "RESOLVED", "0", UITheme.SUCCESS_GREEN);
        responderCard = new MetricCard(VectorIcon.Type.USER, "RESPONDERS", "0 / 0", UITheme.INFO_CYAN);
        resourceCard = new MetricCard(VectorIcon.Type.BUILDING, "RESOURCES", "0 / 0", UITheme.ACCENT_BLUE);

        metricsGrid.add(totalCard);
        metricsGrid.add(activeCard);
        metricsGrid.add(criticalCard);
        metricsGrid.add(resolvedCard);
        metricsGrid.add(responderCard);
        metricsGrid.add(resourceCard);
        overviewTab.add(metricsGrid, BorderLayout.NORTH);

        // MIDDLE & BOTTOM: Charts & Activity Snapshot
        JPanel overviewCenterPanel = new JPanel(new GridLayout(2, 1, 0, 14));
        overviewCenterPanel.setOpaque(false);

        // MIDDLE: Horizontal Charts
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 14, 0));
        chartsRow.setOpaque(false);

        priorityChart = new SimpleBarChart("Incidents by Priority Severity");
        statusChart = new SimpleBarChart("Incidents by Lifecycle Status");

        chartsRow.add(priorityChart);
        chartsRow.add(statusChart);
        overviewCenterPanel.add(chartsRow);

        // BOTTOM: Recent Critical Incidents | System Activity
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 14, 0));
        bottomRow.setOpaque(false);

        // Recent Critical Incidents Card
        ModernCardPanel recentIncCard = new ModernCardPanel(new BorderLayout(0, 8), 12);
        JPanel incHeaderRow = new JPanel(new BorderLayout());
        incHeaderRow.setOpaque(false);

        JLabel recentIncTitle = new JLabel("Recent Critical Incidents");
        recentIncTitle.setFont(UITheme.FONT_SUBTITLE);
        recentIncTitle.setForeground(UITheme.TEXT_PRIMARY);
        incHeaderRow.add(recentIncTitle, BorderLayout.WEST);

        ModernButton ovViewBtn = new ModernButton("View Details", VectorIcon.Type.EYE, ModernButton.ButtonStyle.SECONDARY);
        ovViewBtn.setPreferredSize(new Dimension(120, 28));
        ovViewBtn.addActionListener(e -> {
            int row = overviewIncidentsTable.getSelectedRow();
            if (row >= 0) {
                int incId = (Integer) overviewIncidentsModel.getValueAt(row, 0);
                inspectIncidentDetailsById(incId);
            } else {
                ModernDialog.showWarning(this, "Select Incident", "Please select an incident row to inspect details.");
            }
        });
        incHeaderRow.add(ovViewBtn, BorderLayout.EAST);
        recentIncCard.add(incHeaderRow, BorderLayout.NORTH);

        String[] ovIncCols = {"ID", "Priority", "Status", "Dispatcher", "Created"};
        overviewIncidentsModel = new DefaultTableModel(ovIncCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        overviewIncidentsTable = new ModernTable(overviewIncidentsModel);
        overviewIncidentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = overviewIncidentsTable.getSelectedRow();
                    if (row >= 0) {
                        int incId = (Integer) overviewIncidentsModel.getValueAt(row, 0);
                        inspectIncidentDetailsById(incId);
                    }
                }
            }
        });
        JScrollPane ovIncScroll = new JScrollPane(overviewIncidentsTable);
        ovIncScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        recentIncCard.add(ovIncScroll, BorderLayout.CENTER);

        // System Activity Snapshot Card
        ModernCardPanel recentLogsCard = new ModernCardPanel(new BorderLayout(0, 8), 12);
        JLabel recentLogsTitle = new JLabel("Recent System Activity");
        recentLogsTitle.setFont(UITheme.FONT_SUBTITLE);
        recentLogsTitle.setForeground(UITheme.TEXT_PRIMARY);
        recentLogsCard.add(recentLogsTitle, BorderLayout.NORTH);

        String[] ovLogCols = {"Time", "Event", "User", "Details"};
        overviewLogsModel = new DefaultTableModel(ovLogCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        overviewLogsTable = new ModernTable(overviewLogsModel);
        overviewLogsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        overviewLogsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        overviewLogsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        overviewLogsTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        JScrollPane ovLogScroll = new JScrollPane(overviewLogsTable);
        ovLogScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        recentLogsCard.add(ovLogScroll, BorderLayout.CENTER);

        bottomRow.add(recentIncCard);
        bottomRow.add(recentLogsCard);
        overviewCenterPanel.add(bottomRow);

        overviewTab.add(overviewCenterPanel, BorderLayout.CENTER);
        tabbedPane.addTab("Executive Overview", overviewTab);

        // ------------------------------------------
        // TAB 2: INCIDENT MANAGEMENT
        // ------------------------------------------
        ModernCardPanel incTabCard = new ModernCardPanel(new BorderLayout(0, 12), 16);

        // Filter Toolbar
        JPanel incFilterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        incFilterBar.setOpaque(false);

        JLabel stLabel = new JLabel("Status:");
        stLabel.setFont(UITheme.FONT_BODY_BOLD);
        stLabel.setForeground(UITheme.TEXT_SECONDARY);
        incFilterBar.add(stLabel);

        statusFilterBox = new JComboBox<>(new String[]{"ALL", "OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "CLOSED"});
        statusFilterBox.setFont(UITheme.FONT_BODY);
        statusFilterBox.setBackground(Color.WHITE);
        statusFilterBox.setPreferredSize(new Dimension(140, 32));
        incFilterBar.add(statusFilterBox);

        JLabel prLabel = new JLabel("Priority:");
        prLabel.setFont(UITheme.FONT_BODY_BOLD);
        prLabel.setForeground(UITheme.TEXT_SECONDARY);
        incFilterBar.add(prLabel);

        priorityFilterBox = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        priorityFilterBox.setFont(UITheme.FONT_BODY);
        priorityFilterBox.setBackground(Color.WHITE);
        priorityFilterBox.setPreferredSize(new Dimension(130, 32));
        incFilterBar.add(priorityFilterBox);

        ModernButton applyFilterBtn = new ModernButton("Apply Filters", VectorIcon.Type.FILTER, ModernButton.ButtonStyle.PRIMARY);
        applyFilterBtn.setPreferredSize(new Dimension(130, 32));
        applyFilterBtn.addActionListener(e -> applyIncidentFilters());
        incFilterBar.add(applyFilterBtn);

        ModernButton resetFilterBtn = new ModernButton("Reset", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        resetFilterBtn.setPreferredSize(new Dimension(95, 32));
        resetFilterBtn.addActionListener(e -> resetIncidentFilters());
        incFilterBar.add(resetFilterBtn);

        ModernButton viewDetailsBtn = new ModernButton("View Incident Details", VectorIcon.Type.EYE, ModernButton.ButtonStyle.OUTLINE);
        viewDetailsBtn.setPreferredSize(new Dimension(175, 32));
        viewDetailsBtn.addActionListener(e -> openSelectedIncidentDetails());
        incFilterBar.add(viewDetailsBtn);

        incTabCard.add(incFilterBar, BorderLayout.NORTH);

        String[] incCols = {"Incident ID", "Report ID", "Priority", "Status", "Dispatcher ID", "Created Timestamp"};
        incidentsModel = new DefaultTableModel(incCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        incidentsTable = new ModernTable(incidentsModel);
        incidentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedIncidentDetails();
                }
            }
        });

        JScrollPane incScroll = new JScrollPane(incidentsTable);
        incScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        incTabCard.add(incScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Incident Management", incTabCard);

        // ------------------------------------------
        // TAB 3: SYSTEM AUDIT LOGS
        // ------------------------------------------
        ModernCardPanel logsTabCard = new ModernCardPanel(new BorderLayout(0, 12), 16);

        JPanel logsHeaderBar = new JPanel(new BorderLayout(10, 0));
        logsHeaderBar.setOpaque(false);

        JLabel logsTitle = new JLabel("Security & Operational Audit Log Ledger");
        logsTitle.setFont(UITheme.FONT_TITLE);
        logsTitle.setForeground(UITheme.TEXT_PRIMARY);
        logsHeaderBar.add(logsTitle, BorderLayout.WEST);

        JPanel logFilterToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        logFilterToolbar.setOpaque(false);

        JLabel evLabel = new JLabel("Event Type:");
        evLabel.setFont(UITheme.FONT_BODY_BOLD);
        evLabel.setForeground(UITheme.TEXT_SECONDARY);
        logFilterToolbar.add(evLabel);

        logEventTypeFilterBox = new JComboBox<>(new String[]{
                "ALL", "LOGIN", "LOGOUT", "USER_REGISTERED", "EMERGENCY_REPORTED", "STATUS_CHANGED",
                "DISPATCH_UPDATED", "RESPONDER_ASSIGNED", "RESOURCE_ASSIGNED", "LOCATION_UPDATED"
        });
        logEventTypeFilterBox.setFont(UITheme.FONT_BODY);
        logEventTypeFilterBox.setBackground(Color.WHITE);
        logEventTypeFilterBox.setPreferredSize(new Dimension(150, 32));
        logEventTypeFilterBox.addActionListener(e -> applyLogFilter());
        logFilterToolbar.add(logEventTypeFilterBox);

        logSearchField = new ModernTextField("Search user or details...");
        logSearchField.setPreferredSize(new Dimension(180, 32));
        logFilterToolbar.add(logSearchField);

        ModernButton logSearchBtn = new ModernButton("Search", VectorIcon.Type.SEARCH, ModernButton.ButtonStyle.PRIMARY);
        logSearchBtn.setPreferredSize(new Dimension(95, 32));
        logSearchBtn.addActionListener(e -> applyLogFilter());
        logFilterToolbar.add(logSearchBtn);

        ModernButton logClearBtn = new ModernButton("Clear", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        logClearBtn.setPreferredSize(new Dimension(85, 32));
        logClearBtn.addActionListener(e -> {
            logSearchField.setText("");
            logEventTypeFilterBox.setSelectedIndex(0);
            applyLogFilter();
        });
        logFilterToolbar.add(logClearBtn);

        logsHeaderBar.add(logFilterToolbar, BorderLayout.EAST);
        logsTabCard.add(logsHeaderBar, BorderLayout.NORTH);

        String[] logCols = {"Log ID", "Timestamp", "Event Type", "User ID", "Details"};
        logsModel = new DefaultTableModel(logCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        logsTable = new ModernTable(logsModel);
        logsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        logsTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        logsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        logsTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        logsTable.getColumnModel().getColumn(4).setPreferredWidth(450);

        JScrollPane logScroll = new JScrollPane(logsTable);
        logScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        logsTabCard.add(logScroll, BorderLayout.CENTER);

        tabbedPane.addTab("Audit Logs", logsTabCard);

        // ------------------------------------------
        // TAB 4: BROADCASTS & NOTIFICATIONS
        // ------------------------------------------
        notificationPanel = new NotificationPanel(
                ServiceRegistry.getInstance().getNotificationBroker(),
                trackingService
        );
        tabbedPane.addTab("Broadcasts & Notifications", notificationPanel);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    private JPanel createHealthBadge(String label, String value, Color statusColor) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dot.setForeground(statusColor);
        p.add(dot);

        JLabel lbl = new JLabel(label + ": " + value);
        lbl.setFont(UITheme.FONT_META);
        lbl.setForeground(UITheme.TEXT_PRIMARY);
        p.add(lbl);

        return p;
    }

    public void refreshConsole() {
        User admin = AuthenticationContext.getCurrentUser();
        if (admin == null) return;

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                int total = dataAggregator.getTotalIncidentsCount();
                int active = dataAggregator.getActiveIncidentsCount();
                int resolved = dataAggregator.getResolvedIncidentsCount();
                int critical = dataAggregator.getIncidentsCountByPriority("CRITICAL");
                int high = dataAggregator.getIncidentsCountByPriority("HIGH");
                int medium = dataAggregator.getIncidentsCountByPriority("MEDIUM");
                int low = dataAggregator.getIncidentsCountByPriority("LOW");

                int openSt = dataAggregator.getIncidentsCountByStatus("OPEN");
                int ackSt = dataAggregator.getIncidentsCountByStatus("ACKNOWLEDGED");
                int inProgSt = dataAggregator.getIncidentsCountByStatus("IN_PROGRESS");
                int resSt = dataAggregator.getIncidentsCountByStatus("RESOLVED");
                int closedSt = dataAggregator.getIncidentsCountByStatus("CLOSED");

                int availResp = dataAggregator.getAvailableRespondersCount();
                int busyResp = dataAggregator.getBusyRespondersCount();

                int availRes = dataAggregator.getAvailableResourcesCount();
                int dispRes = dataAggregator.getDispatchedResourcesCount();

                List<SystemLog> logs = logDAO.findAll();
                List<EmergencyIncident> incidents = dataAggregator.getAllIncidents();

                cachedAllIncidents = new ArrayList<>(incidents);
                cachedAllLogs = new ArrayList<>(logs);

                SwingUtilities.invokeLater(() -> {
                    // Update Health Strip
                    lastRefreshLabel.setText("LAST REFRESH: " + LocalDateTime.now().format(timeOnlyFormatter));

                    // Update 6 KPI Cards
                    totalCard.setValue(String.valueOf(total));
                    activeCard.setValue(String.valueOf(active));
                    criticalCard.setValue(String.valueOf(critical));
                    resolvedCard.setValue(String.valueOf(resolved));
                    responderCard.setValue(availResp + " / " + (availResp + busyResp));
                    resourceCard.setValue(availRes + " / " + (availRes + dispRes));

                    // Refresh Charts with exact real values and restrained colors
                    priorityChart.clearData();
                    priorityChart.addBar("CRITICAL", critical, UITheme.CRITICAL);
                    priorityChart.addBar("HIGH", high, UITheme.HIGH);
                    priorityChart.addBar("MEDIUM", medium, UITheme.MEDIUM);
                    priorityChart.addBar("LOW", low, UITheme.SUCCESS);

                    statusChart.clearData();
                    statusChart.addBar("OPEN", openSt, UITheme.INFO);
                    if (ackSt > 0) {
                        statusChart.addBar("ACKNOWLEDGED", ackSt, new Color(0x7E, 0x22, 0xCE));
                    }
                    statusChart.addBar("IN PROGRESS", inProgSt, UITheme.HIGH);
                    statusChart.addBar("RESOLVED", resSt, UITheme.SUCCESS);
                    statusChart.addBar("CLOSED", closedSt, UITheme.TEXT_MUTED);

                    // Refresh Overview Recent Critical Incidents Table
                    overviewIncidentsModel.setRowCount(0);
                    int shownInc = 0;
                    for (int i = incidents.size() - 1; i >= 0 && shownInc < 6; i--) {
                        EmergencyIncident inc = incidents.get(i);
                        String timeStr = inc.getCreatedAt() != null ? inc.getCreatedAt().format(formatter) : "";
                        String dispStr = inc.getAssignedDispatcherId() != null ? "Disp #" + inc.getAssignedDispatcherId() : "Unassigned";
                        overviewIncidentsModel.addRow(new Object[]{
                                inc.getId(),
                                inc.getPriority(),
                                inc.getStatus(),
                                dispStr,
                                timeStr
                        });
                        shownInc++;
                    }

                    // Refresh Overview Recent System Activity Table
                    overviewLogsModel.setRowCount(0);
                    int shownLogs = 0;
                    for (int i = logs.size() - 1; i >= 0 && shownLogs < 6; i--) {
                        SystemLog l = logs.get(i);
                        String timeStr = l.getCreatedAt() != null ? l.getCreatedAt().format(formatter) : "";
                        overviewLogsModel.addRow(new Object[]{
                                timeStr,
                                l.getEventType(),
                                l.getUserId() != null ? "User #" + l.getUserId() : "System",
                                l.getDetails()
                        });
                        shownLogs++;
                    }

                    // Refresh Full Logs Tab
                    applyLogFilter();

                    // Refresh Incidents Tab
                    populateIncidentsTable(cachedAllIncidents);

                    if (notificationPanel != null) {
                        notificationPanel.refreshNotifications();
                    }
                });
            } catch (Exception e) {
                LoggingService.error("Failed to refresh admin console: " + e.getMessage(), e);
            }
        });
    }

    private void populateIncidentsTable(List<EmergencyIncident> incidents) {
        incidentsModel.setRowCount(0);
        for (EmergencyIncident inc : incidents) {
            String timeStr = inc.getCreatedAt() != null ? inc.getCreatedAt().format(formatter) : "";
            incidentsModel.addRow(new Object[]{
                    inc.getId(),
                    inc.getReportId(),
                    inc.getPriority(),
                    inc.getStatus(),
                    inc.getAssignedDispatcherId() != null ? "Disp #" + inc.getAssignedDispatcherId() : "Unassigned",
                    timeStr
            });
        }
    }

    private void populateLogsTable(List<SystemLog> logs) {
        logsModel.setRowCount(0);
        for (int i = logs.size() - 1; i >= 0; i--) {
            SystemLog l = logs.get(i);
            String timeStr = l.getCreatedAt() != null ? l.getCreatedAt().format(formatter) : "";
            logsModel.addRow(new Object[]{
                    l.getId(),
                    timeStr,
                    l.getEventType(),
                    l.getUserId() != null ? "User #" + l.getUserId() : "System",
                    l.getDetails()
            });
        }
    }

    private void applyIncidentFilters() {
        String stFilter = (String) statusFilterBox.getSelectedItem();
        String prFilter = (String) priorityFilterBox.getSelectedItem();

        List<EmergencyIncident> filtered = cachedAllIncidents.stream()
                .filter(i -> "ALL".equals(stFilter) || stFilter.equalsIgnoreCase(i.getStatus()))
                .filter(i -> "ALL".equals(prFilter) || prFilter.equalsIgnoreCase(i.getPriority()))
                .collect(Collectors.toList());

        populateIncidentsTable(filtered);
    }

    private void resetIncidentFilters() {
        statusFilterBox.setSelectedIndex(0);
        priorityFilterBox.setSelectedIndex(0);
        populateIncidentsTable(cachedAllIncidents);
    }

    private void applyLogFilter() {
        String query = logSearchField != null && logSearchField.getText() != null ? logSearchField.getText().trim().toLowerCase() : "";
        String evType = logEventTypeFilterBox != null ? (String) logEventTypeFilterBox.getSelectedItem() : "ALL";

        List<SystemLog> filtered = cachedAllLogs.stream()
                .filter(l -> {
                    if ("ALL".equalsIgnoreCase(evType)) return true;
                    return l.getEventType() != null && l.getEventType().equalsIgnoreCase(evType);
                })
                .filter(l -> {
                    if (query.isEmpty()) return true;
                    return (l.getEventType() != null && l.getEventType().toLowerCase().contains(query)) ||
                            (l.getDetails() != null && l.getDetails().toLowerCase().contains(query)) ||
                            (l.getUserId() != null && String.valueOf(l.getUserId()).contains(query));
                })
                .collect(Collectors.toList());

        populateLogsTable(filtered);
    }

    private void openSelectedIncidentDetails() {
        int selectedRow = incidentsTable.getSelectedRow();
        if (selectedRow < 0) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident row to inspect details.");
            return;
        }

        int incidentId = (Integer) incidentsModel.getValueAt(selectedRow, 0);
        inspectIncidentDetailsById(incidentId);
    }

    private void inspectIncidentDetailsById(int incidentId) {
        User admin = AuthenticationContext.getCurrentUser();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                Optional<EmergencyIncident> incOpt = trackingService.getIncidentDetails(admin, incidentId);
                if (incOpt.isPresent()) {
                    EmergencyIncident inc = incOpt.get();
                    Optional<Location> locOpt = trackingService.getLocation(inc.getReportId());
                    Optional<EmergencyReport> repOpt = reportDAO != null ? reportDAO.findById(inc.getReportId()) : Optional.empty();

                    User reporter = null;
                    if (repOpt.isPresent() && userDAO != null) {
                        reporter = userDAO.findById(repOpt.get().getReporterId()).orElse(null);
                    }

                    final User finalReporter = reporter;
                    final EmergencyReport finalRep = repOpt.orElse(null);
                    SwingUtilities.invokeLater(() -> showIncidentDetailsModal(inc, locOpt.orElse(null), finalRep, finalReporter));
                } else {
                    SwingUtilities.invokeLater(() ->
                            ModernDialog.showError(this, "Incident Not Found", "Could not retrieve details for incident #" + incidentId));
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        ModernDialog.showError(this, "Details Error", ex.getMessage()));
            }
        });
    }

    private void showIncidentDetailsModal(EmergencyIncident inc, Location loc, EmergencyReport rep, User reporter) {
        JDialog modal = new JDialog(this, "Incident Details - #" + inc.getId(), true);
        modal.setSize(540, 520);
        modal.setLocationRelativeTo(this);
        modal.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Modal Header
        JPanel headPanel = new JPanel(new BorderLayout());
        headPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("Incident Record #" + inc.getId());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        headPanel.add(titleLbl, BorderLayout.WEST);

        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badgesPanel.setOpaque(false);
        badgesPanel.add(new PriorityBadge(inc.getPriority()));
        badgesPanel.add(new StatusBadge(inc.getStatus()));
        headPanel.add(badgesPanel, BorderLayout.EAST);

        root.add(headPanel, BorderLayout.NORTH);

        // Modal Body Fields Grid
        ModernCardPanel infoCard = new ModernCardPanel(new GridLayout(7, 2, 8, 8), 12);

        infoCard.add(createModalFieldLabel("Report ID:"));
        infoCard.add(createModalFieldValue("#" + inc.getReportId() + (rep != null ? " (" + rep.getCategory() + ")" : "")));

        infoCard.add(createModalFieldLabel("Reporter:"));
        String reporterStr = reporter != null ? reporter.getFullName() + " (" + reporter.getUsername() + ")" : (rep != null ? "User #" + rep.getReporterId() : "--");
        infoCard.add(createModalFieldValue(reporterStr));

        infoCard.add(createModalFieldLabel("Campus Location:"));
        String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "Campus Main";
        infoCard.add(createModalFieldValue(locStr));

        infoCard.add(createModalFieldLabel("Assigned Dispatcher:"));
        infoCard.add(createModalFieldValue(inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "Unassigned"));

        infoCard.add(createModalFieldLabel("Created Timestamp:"));
        infoCard.add(createModalFieldValue(inc.getCreatedAt() != null ? inc.getCreatedAt().format(formatter) : "--"));

        infoCard.add(createModalFieldLabel("Resolution Timestamp:"));
        infoCard.add(createModalFieldValue(inc.getResolvedAt() != null ? inc.getResolvedAt().format(formatter) : "Active (Unresolved)"));

        infoCard.add(createModalFieldLabel("Current Status:"));
        infoCard.add(createModalFieldValue(inc.getStatus()));

        JPanel centerContainer = new JPanel(new BorderLayout(0, 10));
        centerContainer.setOpaque(false);
        centerContainer.add(infoCard, BorderLayout.NORTH);

        if (rep != null && rep.getDescription() != null && !rep.getDescription().isEmpty()) {
            JPanel descPanel = new JPanel(new BorderLayout(0, 4));
            descPanel.setOpaque(false);
            JLabel descLbl = new JLabel("Report Description:");
            descLbl.setFont(UITheme.FONT_BODY_BOLD);
            descLbl.setForeground(UITheme.TEXT_SECONDARY);
            descPanel.add(descLbl, BorderLayout.NORTH);

            JTextArea descArea = new JTextArea(rep.getDescription());
            descArea.setFont(UITheme.FONT_BODY);
            descArea.setEditable(false);
            descArea.setLineWrap(true);
            descArea.setWrapStyleWord(true);
            descArea.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UITheme.CARD_BORDER),
                    new EmptyBorder(6, 8, 6, 8)
            ));
            JScrollPane descScroll = new JScrollPane(descArea);
            descPanel.add(descScroll, BorderLayout.CENTER);
            centerContainer.add(descPanel, BorderLayout.CENTER);
        }

        root.add(centerContainer, BorderLayout.CENTER);

        // Modal Bottom Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(100, 36));
        closeBtn.addActionListener(e -> modal.dispose());
        btnPanel.add(closeBtn);

        root.add(btnPanel, BorderLayout.SOUTH);

        modal.add(root);
        modal.setVisible(true);
    }

    private JLabel createModalFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    private JLabel createModalFieldValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    public void handleLogout() {
        AuthenticationContext.clear();
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
        dispose();
    }
}
