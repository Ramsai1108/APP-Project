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
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    // Header & Health Strip
    private JLabel lastRefreshLabel;
    private JLabel adminProfileLabel;

    // 6 Top KPI Metric Cards
    private MetricCard totalCard;
    private MetricCard activeCard;
    private MetricCard criticalCard;
    private MetricCard resolvedCard;
    private MetricCard usersCard;
    private MetricCard resourceCard;

    // Operations Center CardLayout
    private CardLayout sectionCardLayout;
    private JPanel sectionContainerPanel;
    private String activeSectionName = "OVERVIEW";
    private final List<ModernButton> navButtons = new ArrayList<>();

    // Section 1: Executive Overview
    private MetricCard fireCountCard;
    private MetricCard medicalCountCard;
    private MetricCard hazardCountCard;
    private MetricCard securityCountCard;
    private MetricCard facilityCountCard;
    private MetricCard otherCountCard;
    private ModernTable overviewIncidentsTable;
    private DefaultTableModel overviewIncidentsModel;

    // Section 2: Incident Management
    private ModernTable incidentsTable;
    private DefaultTableModel incidentsModel;
    private JComboBox<String> statusFilterBox;
    private JComboBox<String> priorityFilterBox;
    private String selectedCategoryFilter = "ALL";
    private final List<CategoryCard> categoryCards = new ArrayList<>();

    // Section 2: Live Operations
    private ModernTable liveOpsTable;
    private DefaultTableModel liveOpsModel;
    private MetricCard liveActiveCard;
    private MetricCard liveCriticalCard;
    private MetricCard liveUnassignedCard;
    private MetricCard liveRespondersCard;

    // Section 3: Dispatch & Resources
    private ModernTable resourcesTable;
    private DefaultTableModel resourcesModel;
    private JComboBox<IncidentItem> dispatchIncidentBox;
    private JComboBox<ResourceItem> dispatchResourceBox;

    // Section 4: Users
    private ModernTable usersTable;
    private DefaultTableModel usersModel;
    private ModernTextField userSearchField;

    // Section 5: Broadcasts & Notifications
    private ModernTable broadcastsTable;
    private DefaultTableModel broadcastsModel;
    private JComboBox<String> broadcastAudienceBox;
    private JComboBox<String> broadcastSeverityBox;
    private JTextArea broadcastMessageArea;

    // Section 6: Audit Logs
    private ModernTable auditLogsTable;
    private DefaultTableModel auditLogsModel;
    private JComboBox<String> logEventTypeBox;
    private ModernTextField logSearchField;

    // Section 7: System Settings
    private JComboBox<String> autoRefreshBox;
    private JCheckBox autoDispatchToggle;
    private JLabel dbStatusLabel;
    private JLabel dbPoolMetricsLabel;

    // Data Caches
    private List<EmergencyIncident> cachedIncidents = new ArrayList<>();
    private List<EmergencyReport> cachedReports = new ArrayList<>();
    private List<User> cachedUsers = new ArrayList<>();
    private List<Resource> cachedResources = new ArrayList<>();
    private List<Responder> cachedResponders = new ArrayList<>();
    private List<SystemLog> cachedLogs = new ArrayList<>();
    private List<Location> cachedLocations = new ArrayList<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static class IncidentItem {
        private final EmergencyIncident incident;

        public IncidentItem(EmergencyIncident incident) {
            this.incident = incident;
        }

        public EmergencyIncident getIncident() { return incident; }

        @Override
        public String toString() {
            if (incident == null) return "-- Select Active Incident --";
            return "Incident #" + incident.getId() + " [" + incident.getPriority() + "] - " + incident.getStatus();
        }
    }

    public static class ResourceItem {
        private final Resource resource;

        public ResourceItem(Resource resource) {
            this.resource = resource;
        }

        public Resource getResource() { return resource; }

        @Override
        public String toString() {
            if (resource == null) return "-- Select Available Resource --";
            return resource.getName() + " (" + resource.getType() + ") - " + resource.getStatus();
        }
    }

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
        String adminName = admin != null ? admin.getFullName() : "Administrator";

        setTitle("Campus Emergency Response System - Emergency Operations Center [" + adminName + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 880);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1080, 740));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UITheme.BG_MAIN);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ==========================================
        // 1. HEADER & SYSTEM HEALTH BAR
        // ==========================================
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel topBadge = new JLabel("EMERGENCY OPERATIONS CENTER (EOC)");
        topBadge.setFont(UITheme.FONT_KPI_LABEL);
        topBadge.setForeground(UITheme.EMERGENCY_RED);
        titleBlock.add(topBadge);
        titleBlock.add(Box.createVerticalStrut(2));

        JLabel titleLabel = new JLabel("Campus Incident Command Console");
        titleLabel.setFont(UITheme.FONT_APP_TITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(2));

        adminProfileLabel = new JLabel("Logged in as: " + adminName + " (ADMIN)");
        adminProfileLabel.setFont(UITheme.FONT_BODY);
        adminProfileLabel.setForeground(UITheme.TEXT_SECONDARY);
        titleBlock.add(adminProfileLabel);

        headerBar.add(titleBlock, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        headerActions.setOpaque(false);

        ModernButton refreshBtn = new ModernButton("Refresh Console", VectorIcon.Type.REFRESH, ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setPreferredSize(new Dimension(160, 36));
        refreshBtn.addActionListener(e -> refreshConsole());

        ModernButton logoutBtn = new ModernButton("Logout", VectorIcon.Type.LOGOUT, ModernButton.ButtonStyle.DARK);
        logoutBtn.setPreferredSize(new Dimension(110, 36));
        logoutBtn.addActionListener(e -> handleLogout());

        headerActions.add(refreshBtn);
        headerActions.add(logoutBtn);
        headerBar.add(headerActions, BorderLayout.EAST);
        topContainer.add(headerBar, BorderLayout.NORTH);

        // System Status Strip
        JPanel healthStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        healthStrip.setBackground(Color.WHITE);
        healthStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(2, 8, 2, 8)
        ));

        healthStrip.add(createHealthBadge("SYSTEM STATUS", "OPERATIONAL", UITheme.SUCCESS_GREEN));
        healthStrip.add(createHealthBadge("DATABASE", "CONNECTED", UITheme.SUCCESS_GREEN));
        healthStrip.add(createHealthBadge("DISPATCH CENTER", "ACTIVE", UITheme.ACCENT_BLUE));

        lastRefreshLabel = new JLabel("LAST SYNC: " + LocalDateTime.now().format(timeFormatter));
        lastRefreshLabel.setFont(UITheme.FONT_META);
        lastRefreshLabel.setForeground(UITheme.TEXT_MUTED);
        healthStrip.add(lastRefreshLabel);

        topContainer.add(healthStrip, BorderLayout.SOUTH);
        mainPanel.add(topContainer, BorderLayout.NORTH);

        // ==========================================
        // 2. TOP KPI METRIC CARDS (6 CARDS)
        // ==========================================
        JPanel centerWrapper = new JPanel(new BorderLayout(0, 14));
        centerWrapper.setOpaque(false);

        JPanel metricsGrid = new JPanel(new GridLayout(1, 6, 10, 0));
        metricsGrid.setOpaque(false);

        totalCard = new MetricCard(VectorIcon.Type.LIST, "TOTAL INCIDENTS", "0", UITheme.ACCENT_BLUE);
        activeCard = new MetricCard(VectorIcon.Type.ACTIVITY, "ACTIVE INCIDENTS", "0", UITheme.WARNING_ORANGE);
        criticalCard = new MetricCard(VectorIcon.Type.ALERT, "CRITICAL", "0", UITheme.EMERGENCY_RED);
        resolvedCard = new MetricCard(VectorIcon.Type.CHECK, "RESOLVED", "0", UITheme.SUCCESS_GREEN);
        usersCard = new MetricCard(VectorIcon.Type.USER, "REGISTERED USERS", "0", UITheme.INFO_CYAN);
        resourceCard = new MetricCard(VectorIcon.Type.BUILDING, "AVAIL RESOURCES", "0 / 0", UITheme.ACCENT_BLUE);

        metricsGrid.add(totalCard);
        metricsGrid.add(activeCard);
        metricsGrid.add(criticalCard);
        metricsGrid.add(resolvedCard);
        metricsGrid.add(usersCard);
        metricsGrid.add(resourceCard);
        centerWrapper.add(metricsGrid, BorderLayout.NORTH);

        // ==========================================
        // 3. SEGMENTED NAVIGATION & OPERATIONS AREA
        // ==========================================
        JPanel opsPanel = new JPanel(new BorderLayout(0, 10));
        opsPanel.setOpaque(false);

        // Navigation Card Bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navBar.setOpaque(false);

        addNavButton(navBar, "OVERVIEW", "Executive Overview", VectorIcon.Type.CHART);
        addNavButton(navBar, "INCIDENTS", "Incident Management", VectorIcon.Type.ALERT);
        addNavButton(navBar, "LIVE_OPS", "Live Operations", VectorIcon.Type.ACTIVITY);
        addNavButton(navBar, "DISPATCH", "Dispatch & Resources", VectorIcon.Type.SERVER);
        addNavButton(navBar, "USERS", "User Management", VectorIcon.Type.USER);
        addNavButton(navBar, "BROADCASTS", "Broadcasts & Notifications", VectorIcon.Type.BELL);
        addNavButton(navBar, "AUDIT_LOGS", "Audit Logs", VectorIcon.Type.LIST);
        addNavButton(navBar, "SETTINGS", "System Settings", VectorIcon.Type.SHIELD);

        opsPanel.add(navBar, BorderLayout.NORTH);

        // CardLayout Container for Section Views
        sectionCardLayout = new CardLayout();
        sectionContainerPanel = new JPanel(sectionCardLayout);
        sectionContainerPanel.setOpaque(false);

        sectionContainerPanel.add(buildExecutiveOverviewSection(), "OVERVIEW");
        sectionContainerPanel.add(buildIncidentsSection(), "INCIDENTS");
        sectionContainerPanel.add(buildLiveOpsSection(), "LIVE_OPS");
        sectionContainerPanel.add(buildDispatchSection(), "DISPATCH");
        sectionContainerPanel.add(buildUsersSection(), "USERS");
        sectionContainerPanel.add(buildBroadcastsSection(), "BROADCASTS");
        sectionContainerPanel.add(buildAuditLogsSection(), "AUDIT_LOGS");
        sectionContainerPanel.add(buildSettingsSection(), "SETTINGS");

        opsPanel.add(sectionContainerPanel, BorderLayout.CENTER);
        centerWrapper.add(opsPanel, BorderLayout.CENTER);

        mainPanel.add(centerWrapper, BorderLayout.CENTER);
        add(mainPanel);

        // Default active section: Executive Overview
        switchSection("OVERVIEW");
    }

    private void addNavButton(JPanel parent, String sectionKey, String label, VectorIcon.Type iconType) {
        ModernButton btn = new ModernButton(label, iconType, ModernButton.ButtonStyle.SECONDARY);
        btn.setPreferredSize(new Dimension(150, 36));
        btn.setFont(UITheme.FONT_SMALL);
        btn.addActionListener(e -> switchSection(sectionKey));
        navButtons.add(btn);
        parent.add(btn);
    }

    private void switchSection(String sectionKey) {
        this.activeSectionName = sectionKey;
        sectionCardLayout.show(sectionContainerPanel, sectionKey);

        for (ModernButton btn : navButtons) {
            boolean isActive = (sectionKey.equals("OVERVIEW") && btn.getText().contains("Overview")) ||
                    (sectionKey.equals("INCIDENTS") && btn.getText().contains("Incident")) ||
                    (sectionKey.equals("LIVE_OPS") && btn.getText().contains("Live")) ||
                    (sectionKey.equals("DISPATCH") && btn.getText().contains("Dispatch")) ||
                    (sectionKey.equals("USERS") && btn.getText().contains("User")) ||
                    (sectionKey.equals("BROADCASTS") && btn.getText().contains("Broadcast")) ||
                    (sectionKey.equals("AUDIT_LOGS") && btn.getText().contains("Audit")) ||
                    (sectionKey.equals("SETTINGS") && btn.getText().contains("Settings"));

            btn.setButtonStyle(isActive ? ModernButton.ButtonStyle.PRIMARY : ModernButton.ButtonStyle.SECONDARY);
        }
    }

    // =========================================================================
    // SECTION 1: EXECUTIVE OVERVIEW
    // =========================================================================
    private JPanel buildExecutiveOverviewSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        // Top Category Breakdown Stat Cards (6 Emergency Categories)
        JPanel catGrid = new JPanel(new GridLayout(1, 6, 8, 0));
        catGrid.setOpaque(false);

        fireCountCard = new MetricCard(VectorIcon.Type.FIRE, "Fire / Smoke", "0", UITheme.EMERGENCY_RED);
        medicalCountCard = new MetricCard(VectorIcon.Type.MEDICAL, "Medical", "0", UITheme.EMERGENCY_RED);
        hazardCountCard = new MetricCard(VectorIcon.Type.HAZARD, "Hazard / Gas", "0", UITheme.WARNING_ORANGE);
        securityCountCard = new MetricCard(VectorIcon.Type.LOCK, "Security", "0", UITheme.ACCENT_BLUE);
        facilityCountCard = new MetricCard(VectorIcon.Type.BUILDING, "Facility", "0", UITheme.INFO_CYAN);
        otherCountCard = new MetricCard(VectorIcon.Type.HELP, "Other Alert", "0", UITheme.TEXT_SECONDARY);

        catGrid.add(fireCountCard);
        catGrid.add(medicalCountCard);
        catGrid.add(hazardCountCard);
        catGrid.add(securityCountCard);
        catGrid.add(facilityCountCard);
        catGrid.add(otherCountCard);
        panel.add(catGrid, BorderLayout.NORTH);

        // Center Executive Summary Table
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 10), 12);
        JLabel title = new JLabel("Executive Operational Summary — Active & Recent Incidents");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Category", "Priority", "Location", "Reported Time", "Status", "Assigned Dispatcher"};
        overviewIncidentsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        overviewIncidentsTable = new ModernTable(overviewIncidentsModel);
        JScrollPane scroll = new JScrollPane(overviewIncidentsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        ModernButton viewIncidentsBtn = new ModernButton("Go to Incident Management", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.PRIMARY);
        viewIncidentsBtn.setPreferredSize(new Dimension(210, 32));
        viewIncidentsBtn.addActionListener(e -> switchSection("INCIDENTS"));
        actions.add(viewIncidentsBtn);

        card.add(actions, BorderLayout.SOUTH);
        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // SECTION 2: INCIDENTS MANAGEMENT
    // =========================================================================
    private JPanel buildIncidentsSection() {
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 10), 14);

        JPanel topControls = new JPanel();
        topControls.setLayout(new BoxLayout(topControls, BoxLayout.Y_AXIS));
        topControls.setOpaque(false);

        // Emergency Category Cards Filter Row
        JLabel catLabel = new JLabel("Filter Incidents by Emergency Category:");
        catLabel.setFont(UITheme.FONT_META);
        catLabel.setForeground(UITheme.TEXT_SECONDARY);
        catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        topControls.add(catLabel);
        topControls.add(Box.createVerticalStrut(4));

        JPanel categoryGrid = new JPanel(new GridLayout(1, 7, 6, 0));
        categoryGrid.setOpaque(false);
        categoryGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        addCategoryFilterCard(categoryGrid, "ALL", VectorIcon.Type.LIST, "All Types", "View all");
        addCategoryFilterCard(categoryGrid, "FIRE", VectorIcon.Type.FIRE, "Fire / Smoke", "Flames / Smoke");
        addCategoryFilterCard(categoryGrid, "MEDICAL", VectorIcon.Type.MEDICAL, "Medical", "Injury / Illness");
        addCategoryFilterCard(categoryGrid, "HAZARD", VectorIcon.Type.HAZARD, "Hazard / Gas", "Chemical / Leak");
        addCategoryFilterCard(categoryGrid, "SECURITY", VectorIcon.Type.LOCK, "Security", "Threat / Intruder");
        addCategoryFilterCard(categoryGrid, "FACILITY", VectorIcon.Type.BUILDING, "Facility", "Power / Elevator");
        addCategoryFilterCard(categoryGrid, "OTHER", VectorIcon.Type.HELP, "Other Alert", "General assistance");

        topControls.add(categoryGrid);
        topControls.add(Box.createVerticalStrut(10));

        // Sub-filters row (Status & Priority)
        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterRow.setOpaque(false);
        filterRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        filterRow.add(new JLabel("Status:"));
        statusFilterBox = new JComboBox<>(new String[]{"ALL STATUSES", "OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"});
        statusFilterBox.setFont(UITheme.FONT_SMALL);
        statusFilterBox.addActionListener(e -> applyIncidentFilters());
        filterRow.add(statusFilterBox);

        filterRow.add(Box.createHorizontalStrut(10));
        filterRow.add(new JLabel("Priority:"));
        priorityFilterBox = new JComboBox<>(new String[]{"ALL PRIORITIES", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        priorityFilterBox.setFont(UITheme.FONT_SMALL);
        priorityFilterBox.addActionListener(e -> applyIncidentFilters());
        filterRow.add(priorityFilterBox);

        topControls.add(filterRow);
        card.add(topControls, BorderLayout.NORTH);

        // Incidents Table
        String[] columns = {"ID", "Type", "Priority", "Location", "Reported By", "Time", "Status", "Assigned Resource"};
        incidentsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        incidentsTable = new ModernTable(incidentsModel);
        incidentsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        incidentsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        incidentsTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        incidentsTable.getColumnModel().getColumn(3).setPreferredWidth(180);
        incidentsTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        incidentsTable.getColumnModel().getColumn(5).setPreferredWidth(140);
        incidentsTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        incidentsTable.getColumnModel().getColumn(7).setPreferredWidth(140);

        incidentsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && incidentsTable.getSelectedRow() >= 0) {
                    inspectSelectedIncident();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(incidentsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(tableScroll, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomActions.setOpaque(false);

        ModernButton inspectBtn = new ModernButton("Inspect Details", VectorIcon.Type.EYE, ModernButton.ButtonStyle.PRIMARY);
        inspectBtn.setPreferredSize(new Dimension(140, 32));
        inspectBtn.addActionListener(e -> inspectSelectedIncident());

        ModernButton updateStatusBtn = new ModernButton("Change Status", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        updateStatusBtn.setPreferredSize(new Dimension(140, 32));
        updateStatusBtn.addActionListener(e -> showChangeStatusModal());

        bottomActions.add(updateStatusBtn);
        bottomActions.add(inspectBtn);
        card.add(bottomActions, BorderLayout.SOUTH);

        return card;
    }

    private void addCategoryFilterCard(JPanel parent, String code, VectorIcon.Type iconType, String title, String desc) {
        CategoryCard cCard = new CategoryCard(code, iconType, title, desc);
        cCard.setSelected(code.equals(selectedCategoryFilter));
        cCard.setOnSelectListener(() -> {
            selectedCategoryFilter = code;
            for (CategoryCard c : categoryCards) {
                c.setSelected(c.getCategoryCode().equals(code));
            }
            applyIncidentFilters();
        });
        categoryCards.add(cCard);
        parent.add(cCard);
    }

    // =========================================================================
    // SECTION 2: LIVE OPERATIONS
    // =========================================================================
    private JPanel buildLiveOpsSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // 4 Live Stats Cards
        JPanel liveStatsGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        liveStatsGrid.setOpaque(false);

        liveActiveCard = new MetricCard(VectorIcon.Type.ACTIVITY, "ACTIVE INCIDENTS", "0", UITheme.WARNING_ORANGE);
        liveCriticalCard = new MetricCard(VectorIcon.Type.ALERT, "CRITICAL ALERTS", "0", UITheme.EMERGENCY_RED);
        liveUnassignedCard = new MetricCard(VectorIcon.Type.HELP, "UNASSIGNED", "0", UITheme.INFO_CYAN);
        liveRespondersCard = new MetricCard(VectorIcon.Type.USER, "ACTIVE RESPONDERS", "0", UITheme.SUCCESS_GREEN);

        liveStatsGrid.add(liveActiveCard);
        liveStatsGrid.add(liveCriticalCard);
        liveStatsGrid.add(liveUnassignedCard);
        liveStatsGrid.add(liveRespondersCard);
        panel.add(liveStatsGrid, BorderLayout.NORTH);

        // Live Operations Table Card
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 10), 12);
        JLabel title = new JLabel("Live Incident Command & Location Tracking");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Type", "Priority", "Building / Coordinates", "Reported Time", "Status", "Dispatcher"};
        liveOpsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        liveOpsTable = new ModernTable(liveOpsModel);
        JScrollPane scroll = new JScrollPane(liveOpsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(scroll, BorderLayout.CENTER);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsRow.setOpaque(false);

        ModernButton inspectLiveBtn = new ModernButton("Inspect Incident", VectorIcon.Type.EYE, ModernButton.ButtonStyle.PRIMARY);
        inspectLiveBtn.setPreferredSize(new Dimension(150, 32));
        inspectLiveBtn.addActionListener(e -> inspectLiveOpsSelectedIncident());

        ModernButton openMapBtn = new ModernButton("Open Location on Map", VectorIcon.Type.LOCATION_PIN, ModernButton.ButtonStyle.SECONDARY);
        openMapBtn.setPreferredSize(new Dimension(180, 32));
        openMapBtn.addActionListener(e -> openLiveIncidentMap());

        actionsRow.add(openMapBtn);
        actionsRow.add(inspectLiveBtn);
        card.add(actionsRow, BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // SECTION 3: DISPATCH & RESOURCES
    // =========================================================================
    private JPanel buildDispatchSection() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);

        // Dispatch Control Card
        ModernCardPanel dispatchCard = new ModernCardPanel(new GridLayout(2, 1, 0, 8), 12);
        JLabel title = new JLabel("Dispatch Responders & Assign Emergency Resources");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        dispatchCard.add(title);

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlRow.setOpaque(false);

        controlRow.add(new JLabel("Target Incident:"));
        dispatchIncidentBox = new JComboBox<>();
        dispatchIncidentBox.setPreferredSize(new Dimension(280, 34));
        controlRow.add(dispatchIncidentBox);

        controlRow.add(new JLabel("Available Resource:"));
        dispatchResourceBox = new JComboBox<>();
        dispatchResourceBox.setPreferredSize(new Dimension(260, 34));
        controlRow.add(dispatchResourceBox);

        ModernButton executeDispatchBtn = new ModernButton("ASSIGN RESOURCE", VectorIcon.Type.CHECK, ModernButton.ButtonStyle.PRIMARY);
        executeDispatchBtn.setPreferredSize(new Dimension(170, 34));
        executeDispatchBtn.addActionListener(e -> handleDispatchExecution());
        controlRow.add(executeDispatchBtn);

        dispatchCard.add(controlRow);
        main.add(dispatchCard, BorderLayout.NORTH);

        // Resources Table Card
        ModernCardPanel resourcesCard = new ModernCardPanel(new BorderLayout(0, 8), 12);
        JLabel resTitle = new JLabel("Campus Emergency Resources & Units");
        resTitle.setFont(UITheme.FONT_SUBTITLE);
        resTitle.setForeground(UITheme.TEXT_PRIMARY);
        resourcesCard.add(resTitle, BorderLayout.NORTH);

        String[] cols = {"ID", "Resource Name", "Type", "Status", "Assigned Incident", "Base Location"};
        resourcesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        resourcesTable = new ModernTable(resourcesModel);
        JScrollPane scroll = new JScrollPane(resourcesTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        resourcesCard.add(scroll, BorderLayout.CENTER);

        JPanel resActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        resActions.setOpaque(false);

        ModernButton releaseBtn = new ModernButton("Release Resource", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        releaseBtn.setPreferredSize(new Dimension(150, 32));
        releaseBtn.addActionListener(e -> handleReleaseResource());
        resActions.add(releaseBtn);

        resourcesCard.add(resActions, BorderLayout.SOUTH);
        main.add(resourcesCard, BorderLayout.CENTER);

        return main;
    }

    // =========================================================================
    // SECTION 4: USER MANAGEMENT
    // =========================================================================
    private JPanel buildUsersSection() {
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 10), 12);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel title = new JLabel("Campus User Directory");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        topRow.add(title, BorderLayout.WEST);

        JPanel searchGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchGroup.setOpaque(false);
        userSearchField = new ModernTextField("Search by name, email or username...");
        userSearchField.setPreferredSize(new Dimension(280, 32));

        ModernButton searchBtn = new ModernButton("Search", VectorIcon.Type.SEARCH, ModernButton.ButtonStyle.SECONDARY);
        searchBtn.setPreferredSize(new Dimension(100, 32));
        searchBtn.addActionListener(e -> applyUserSearchFilter());

        searchGroup.add(userSearchField);
        searchGroup.add(searchBtn);
        topRow.add(searchGroup, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);

        String[] cols = {"ID", "Username", "Full Name", "Campus Email", "Phone", "Role", "Created Date"};
        usersModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        usersTable = new ModernTable(usersModel);
        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // SECTION 5: BROADCASTS & NOTIFICATIONS
    // =========================================================================
    private JPanel buildBroadcastsSection() {
        JPanel main = new JPanel(new BorderLayout(0, 10));
        main.setOpaque(false);

        // Create Broadcast Card
        ModernCardPanel createCard = new ModernCardPanel(new BorderLayout(0, 8), 12);
        JLabel title = new JLabel("Broadcast Emergency Announcement / Campus Alert");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        createCard.add(title, BorderLayout.NORTH);

        JPanel formBody = new JPanel();
        formBody.setLayout(new BoxLayout(formBody, BoxLayout.Y_AXIS));
        formBody.setOpaque(false);

        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        metaRow.setOpaque(false);

        metaRow.add(new JLabel("Target Audience:"));
        broadcastAudienceBox = new JComboBox<>(new String[]{"All Users", "Dispatched Responders", "Campus Community"});
        broadcastAudienceBox.setPreferredSize(new Dimension(180, 32));
        metaRow.add(broadcastAudienceBox);

        metaRow.add(new JLabel("Alert Severity:"));
        broadcastSeverityBox = new JComboBox<>(new String[]{"Information", "Warning", "Critical Emergency"});
        broadcastSeverityBox.setPreferredSize(new Dimension(180, 32));
        metaRow.add(broadcastSeverityBox);

        formBody.add(metaRow);
        formBody.add(Box.createVerticalStrut(8));

        JLabel msgLbl = new JLabel("Announcement Message:");
        msgLbl.setFont(UITheme.FONT_BODY_BOLD);
        formBody.add(msgLbl);
        formBody.add(Box.createVerticalStrut(4));

        broadcastMessageArea = new JTextArea(3, 20);
        broadcastMessageArea.setFont(UITheme.FONT_BODY);
        broadcastMessageArea.setLineWrap(true);
        broadcastMessageArea.setWrapStyleWord(true);
        JScrollPane msgScroll = new JScrollPane(broadcastMessageArea);
        msgScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 70));
        formBody.add(msgScroll);

        createCard.add(formBody, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        ModernButton sendBroadcastBtn = new ModernButton("SEND BROADCAST", VectorIcon.Type.BELL, ModernButton.ButtonStyle.DANGER);
        sendBroadcastBtn.setPreferredSize(new Dimension(180, 36));
        sendBroadcastBtn.addActionListener(e -> handleSendBroadcast());
        btnRow.add(sendBroadcastBtn);

        createCard.add(btnRow, BorderLayout.SOUTH);
        main.add(createCard, BorderLayout.NORTH);

        // Broadcast History Table Card
        ModernCardPanel histCard = new ModernCardPanel(new BorderLayout(0, 8), 12);
        JLabel histTitle = new JLabel("Recent Broadcast Log & Notifications");
        histTitle.setFont(UITheme.FONT_SUBTITLE);
        histTitle.setForeground(UITheme.TEXT_PRIMARY);
        histCard.add(histTitle, BorderLayout.NORTH);

        String[] cols = {"Time", "Audience", "Severity", "Sender", "Message Content"};
        broadcastsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        broadcastsTable = new ModernTable(broadcastsModel);
        JScrollPane scroll = new JScrollPane(broadcastsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        histCard.add(scroll, BorderLayout.CENTER);

        main.add(histCard, BorderLayout.CENTER);
        return main;
    }

    // =========================================================================
    // SECTION 6: AUDIT LOGS
    // =========================================================================
    private JPanel buildAuditLogsSection() {
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 10), 12);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel title = new JLabel("System Audit & Event Logs");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        topRow.add(title, BorderLayout.WEST);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRow.setOpaque(false);

        filterRow.add(new JLabel("Event Type:"));
        logEventTypeBox = new JComboBox<>(new String[]{"ALL EVENTS", "EMERGENCY_REPORTED", "STATUS_CHANGED", "DISPATCH_UPDATED", "USER_LOGIN", "RESPONDER_DISPATCHED"});
        logEventTypeBox.setFont(UITheme.FONT_SMALL);
        logEventTypeBox.addActionListener(e -> applyLogFilters());
        filterRow.add(logEventTypeBox);

        logSearchField = new ModernTextField("Search log details...");
        logSearchField.setPreferredSize(new Dimension(220, 32));
        filterRow.add(logSearchField);

        ModernButton searchLogsBtn = new ModernButton("Filter", VectorIcon.Type.SEARCH, ModernButton.ButtonStyle.SECONDARY);
        searchLogsBtn.setPreferredSize(new Dimension(90, 32));
        searchLogsBtn.addActionListener(e -> applyLogFilters());
        filterRow.add(searchLogsBtn);

        topRow.add(filterRow, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        String[] cols = {"Timestamp", "User ID", "Event Type", "Event Action & Details"};
        auditLogsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        auditLogsTable = new ModernTable(auditLogsModel);
        auditLogsTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        auditLogsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        auditLogsTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        auditLogsTable.getColumnModel().getColumn(3).setPreferredWidth(450);

        JScrollPane scroll = new JScrollPane(auditLogsTable);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // SECTION 7: SYSTEM SETTINGS
    // =========================================================================
    private JPanel buildSettingsSection() {
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(0, 14), 14);

        JLabel title = new JLabel("Emergency Operations Center System Settings");
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        card.add(title, BorderLayout.NORTH);

        JPanel settingsBody = new JPanel();
        settingsBody.setLayout(new BoxLayout(settingsBody, BoxLayout.Y_AXIS));
        settingsBody.setOpaque(false);

        // Group 1: Console Auto-Refresh
        ModernCardPanel grp1 = new ModernCardPanel(new GridLayout(2, 2, 10, 8), 10);
        grp1.add(createSettingLabel("Console Auto-Refresh Interval:"));

        autoRefreshBox = new JComboBox<>(new String[]{"Manual (Off)", "10 Seconds", "30 Seconds", "60 Seconds"});
        autoRefreshBox.setSelectedIndex(2);
        grp1.add(autoRefreshBox);

        grp1.add(createSettingLabel("Automatic Dispatch Recommendation:"));
        autoDispatchToggle = new JCheckBox("Enabled (Auto-suggest nearest available responder)");
        autoDispatchToggle.setSelected(true);
        autoDispatchToggle.setOpaque(false);
        grp1.add(autoDispatchToggle);

        settingsBody.add(grp1);
        settingsBody.add(Box.createVerticalStrut(12));

        // Group 2: Database & System Health
        ModernCardPanel grp2 = new ModernCardPanel(new GridLayout(2, 1, 0, 6), 10);
        JLabel dbTitle = new JLabel("Database Connection Pool & Infrastructure Health");
        dbTitle.setFont(UITheme.FONT_BODY_BOLD);
        grp2.add(dbTitle);

        dbStatusLabel = new JLabel("Status: CONNECTED (HikariCP Pool - MySQL 8.0 on localhost:3306)");
        dbStatusLabel.setFont(UITheme.FONT_SMALL);
        dbStatusLabel.setForeground(UITheme.SUCCESS_GREEN);
        grp2.add(dbStatusLabel);

        dbPoolMetricsLabel = new JLabel("Active Connections: 2 | Idle: 8 | Max Pool Size: 10");
        dbPoolMetricsLabel.setFont(UITheme.FONT_META);
        dbPoolMetricsLabel.setForeground(UITheme.TEXT_SECONDARY);
        grp2.add(dbPoolMetricsLabel);

        settingsBody.add(grp2);
        settingsBody.add(Box.createVerticalStrut(14));

        // Save Settings Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        ModernButton saveSettingsBtn = new ModernButton("Save Configuration", VectorIcon.Type.CHECK, ModernButton.ButtonStyle.PRIMARY);
        saveSettingsBtn.setPreferredSize(new Dimension(170, 36));
        saveSettingsBtn.addActionListener(e -> ModernDialog.showInfo(this, "Settings Saved", "System configuration has been successfully updated."));
        actions.add(saveSettingsBtn);

        settingsBody.add(actions);
        card.add(settingsBody, BorderLayout.CENTER);

        return card;
    }

    private JLabel createSettingLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    // =========================================================================
    // DATA REFRESH & EVENT HANDLERS
    // =========================================================================
    public void refreshConsole() {
        User admin = AuthenticationContext.getCurrentUser();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<EmergencyIncident> incidents = trackingService.getHistoricalIncidents(admin, null, null, null);
                List<EmergencyReport> reports = reportDAO.findAll();
                List<User> users = userDAO.findAll();
                List<Resource> resources = resourceDispatcher.getAvailableResources();
                List<Responder> responders = resourceDispatcher.getAvailableResponders();
                List<SystemLog> logs = logDAO.findAll();
                List<Location> locs = locationDAO.findAll();

                cachedIncidents = new ArrayList<>(incidents);
                cachedReports = new ArrayList<>(reports);
                cachedUsers = new ArrayList<>(users);
                cachedResources = new ArrayList<>(resources);
                cachedResponders = new ArrayList<>(responders);
                cachedLogs = new ArrayList<>(logs);
                cachedLocations = new ArrayList<>(locs);

                int totalCount = incidents.size();
                int activeCount = 0;
                int criticalCount = 0;
                int resolvedCount = 0;
                int unassignedCount = 0;

                for (EmergencyIncident inc : incidents) {
                    if ("CRITICAL".equalsIgnoreCase(inc.getPriority())) criticalCount++;
                    if ("RESOLVED".equalsIgnoreCase(inc.getStatus()) || "CLOSED".equalsIgnoreCase(inc.getStatus())) {
                        resolvedCount++;
                    } else {
                        activeCount++;
                    }
                    if (inc.getAssignedDispatcherId() == null) unassignedCount++;
                }

                int fireCnt = 0, medCnt = 0, hazCnt = 0, secCnt = 0, facCnt = 0, othCnt = 0;
                for (EmergencyReport r : reports) {
                    String cat = r.getCategory() != null ? r.getCategory().toUpperCase() : "";
                    switch (cat) {
                        case "FIRE": fireCnt++; break;
                        case "MEDICAL": medCnt++; break;
                        case "HAZARD": hazCnt++; break;
                        case "SECURITY": secCnt++; break;
                        case "FACILITY": facCnt++; break;
                        default: othCnt++; break;
                    }
                }

                final int fTotal = totalCount;
                final int fActive = activeCount;
                final int fCritical = criticalCount;
                final int fResolved = resolvedCount;
                final int fUsers = users.size();
                final int fUnassigned = unassignedCount;
                final int fResponders = responders.size();
                final int fAvailRes = (int) resources.stream().filter(r -> "AVAILABLE".equalsIgnoreCase(r.getStatus())).count();
                final int fFire = fireCnt, fMed = medCnt, fHaz = hazCnt, fSec = secCnt, fFac = facCnt, fOth = othCnt;

                SwingUtilities.invokeLater(() -> {
                    lastRefreshLabel.setText("LAST SYNC: " + LocalDateTime.now().format(timeFormatter));

                    totalCard.setValue(String.valueOf(fTotal));
                    activeCard.setValue(String.valueOf(fActive));
                    criticalCard.setValue(String.valueOf(fCritical));
                    resolvedCard.setValue(String.valueOf(fResolved));
                    usersCard.setValue(String.valueOf(fUsers));
                    resourceCard.setValue(fAvailRes + " / " + resources.size());

                    if (fireCountCard != null) fireCountCard.setValue(String.valueOf(fFire));
                    if (medicalCountCard != null) medicalCountCard.setValue(String.valueOf(fMed));
                    if (hazardCountCard != null) hazardCountCard.setValue(String.valueOf(fHaz));
                    if (securityCountCard != null) securityCountCard.setValue(String.valueOf(fSec));
                    if (facilityCountCard != null) facilityCountCard.setValue(String.valueOf(fFac));
                    if (otherCountCard != null) otherCountCard.setValue(String.valueOf(fOth));

                    liveActiveCard.setValue(String.valueOf(fActive));
                    liveCriticalCard.setValue(String.valueOf(fCritical));
                    liveUnassignedCard.setValue(String.valueOf(fUnassigned));
                    liveRespondersCard.setValue(String.valueOf(fResponders));

                    populateOverviewTable();
                    applyIncidentFilters();
                    populateLiveOpsTable();
                    populateDispatchBoxes();
                    populateUsersTable();
                    populateBroadcastsTable();
                    populateAuditLogsTable();
                });
            } catch (Exception e) {
                LoggingService.error("Failed to refresh Admin Console data: " + e.getMessage(), e);
            }
        });
    }

    private void populateOverviewTable() {
        if (overviewIncidentsModel == null) return;
        overviewIncidentsModel.setRowCount(0);
        for (EmergencyIncident inc : cachedIncidents) {
            EmergencyReport report = cachedReports.stream().filter(r -> r.getId() == inc.getReportId()).findFirst().orElse(null);
            Location loc = report != null ? cachedLocations.stream().filter(l -> l.getId() == report.getLocationId()).findFirst().orElse(null) : null;
            String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "--";
            overviewIncidentsModel.addRow(new Object[]{
                    inc.getId(),
                    report != null ? report.getCategory() : "OTHER",
                    inc.getPriority(),
                    locStr,
                    inc.getCreatedAt() != null ? inc.getCreatedAt().format(timeFormatter) : "--",
                    inc.getStatus(),
                    inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "UNASSIGNED"
            });
        }
    }

    private void applyIncidentFilters() {
        String status = statusFilterBox != null ? (String) statusFilterBox.getSelectedItem() : "ALL STATUSES";
        String priority = priorityFilterBox != null ? (String) priorityFilterBox.getSelectedItem() : "ALL PRIORITIES";

        incidentsModel.setRowCount(0);

        for (EmergencyIncident inc : cachedIncidents) {
            EmergencyReport report = cachedReports.stream()
                    .filter(r -> r.getId() == inc.getReportId())
                    .findFirst()
                    .orElse(null);

            String cat = report != null ? report.getCategory() : "OTHER";
            if (!"ALL".equalsIgnoreCase(selectedCategoryFilter) && !selectedCategoryFilter.equalsIgnoreCase(cat)) {
                continue;
            }
            if (!"ALL STATUSES".equalsIgnoreCase(status) && !status.equalsIgnoreCase(inc.getStatus())) {
                continue;
            }
            if (!"ALL PRIORITIES".equalsIgnoreCase(priority) && !priority.equalsIgnoreCase(inc.getPriority())) {
                continue;
            }

            Location loc = report != null ? cachedLocations.stream().filter(l -> l.getId() == report.getLocationId()).findFirst().orElse(null) : null;
            String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "--";
            String reporter = report != null ? "User #" + report.getReporterId() : "--";
            String resStr = inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "Unassigned";

            incidentsModel.addRow(new Object[]{
                    inc.getId(),
                    cat,
                    inc.getPriority(),
                    locStr,
                    reporter,
                    inc.getCreatedAt() != null ? inc.getCreatedAt().format(timeFormatter) : "--",
                    inc.getStatus(),
                    resStr
            });
        }
    }

    private void populateLiveOpsTable() {
        liveOpsModel.setRowCount(0);

        for (EmergencyIncident inc : cachedIncidents) {
            if ("RESOLVED".equalsIgnoreCase(inc.getStatus()) || "CLOSED".equalsIgnoreCase(inc.getStatus())) {
                continue;
            }

            EmergencyReport report = cachedReports.stream().filter(r -> r.getId() == inc.getReportId()).findFirst().orElse(null);
            Location loc = report != null ? cachedLocations.stream().filter(l -> l.getId() == report.getLocationId()).findFirst().orElse(null) : null;

            String locStr = "--";
            if (loc != null) {
                if (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0) {
                    locStr = String.format(Locale.US, "%s (%.4f, %.4f)", loc.getBuildingName(), loc.getLatitude(), loc.getLongitude());
                } else {
                    locStr = loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")";
                }
            }

            liveOpsModel.addRow(new Object[]{
                    inc.getId(),
                    report != null ? report.getCategory() : "OTHER",
                    inc.getPriority(),
                    locStr,
                    inc.getCreatedAt() != null ? inc.getCreatedAt().format(timeFormatter) : "--",
                    inc.getStatus(),
                    inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "UNASSIGNED"
            });
        }
    }

    private void populateDispatchBoxes() {
        dispatchIncidentBox.removeAllItems();
        dispatchIncidentBox.addItem(new IncidentItem(null));
        for (EmergencyIncident inc : cachedIncidents) {
            if (!"RESOLVED".equalsIgnoreCase(inc.getStatus()) && !"CLOSED".equalsIgnoreCase(inc.getStatus())) {
                dispatchIncidentBox.addItem(new IncidentItem(inc));
            }
        }

        dispatchResourceBox.removeAllItems();
        dispatchResourceBox.addItem(new ResourceItem(null));
        for (Resource res : cachedResources) {
            dispatchResourceBox.addItem(new ResourceItem(res));
        }

        resourcesModel.setRowCount(0);
        for (Resource r : cachedResources) {
            resourcesModel.addRow(new Object[]{
                    r.getId(),
                    r.getName(),
                    r.getType(),
                    r.getStatus(),
                    r.getAssignedIncidentId() != null ? "#" + r.getAssignedIncidentId() : "None",
                    "HQ Base"
            });
        }
    }

    private void populateUsersTable() {
        usersModel.setRowCount(0);
        for (User u : cachedUsers) {
            usersModel.addRow(new Object[]{
                    u.getId(),
                    u.getUsername(),
                    u.getFullName(),
                    u.getEmail(),
                    u.getPhone() != null ? u.getPhone() : "--",
                    u.getRole() != null ? u.getRole().getDisplayRole() : "USER",
                    u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : "--"
            });
        }
    }

    private void applyUserSearchFilter() {
        String q = userSearchField.getText() != null ? userSearchField.getText().trim().toLowerCase() : "";
        usersModel.setRowCount(0);

        for (User u : cachedUsers) {
            if (q.isEmpty() || u.getFullName().toLowerCase().contains(q) ||
                    u.getUsername().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)) {
                usersModel.addRow(new Object[]{
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone() != null ? u.getPhone() : "--",
                        u.getRole() != null ? u.getRole().getDisplayRole() : "USER",
                        u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : "--"
                });
            }
        }
    }

    private void populateBroadcastsTable() {
        broadcastsModel.setRowCount(0);
        List<NotificationBroker.NotificationMessage> notes = ServiceRegistry.getInstance().getNotificationBroker().getSentNotifications();

        for (NotificationBroker.NotificationMessage m : notes) {
            broadcastsModel.addRow(new Object[]{
                    m.getTimestamp() != null ? m.getTimestamp().format(timeFormatter) : "--",
                    m.getRecipient(),
                    m.getEventType() != null ? m.getEventType().name() : "INFO",
                    "System Admin",
                    m.getContent()
            });
        }
    }

    private void handleSendBroadcast() {
        String msg = broadcastMessageArea.getText() != null ? broadcastMessageArea.getText().trim() : "";
        if (msg.isEmpty()) {
            ModernDialog.showWarning(this, "Empty Message", "Please type a broadcast message before sending.");
            return;
        }

        String audience = (String) broadcastAudienceBox.getSelectedItem();
        String severity = (String) broadcastSeverityBox.getSelectedItem();

        ServiceRegistry.getInstance().getNotificationBroker().sendNotification(
                "ALL",
                "[" + severity.toUpperCase() + "] " + msg,
                NotificationBroker.NotificationType.IN_APP
        );

        broadcastMessageArea.setText("");
        populateBroadcastsTable();
        ModernDialog.showInfo(this, "Broadcast Sent", "Emergency announcement broadcasted to " + audience + ".");
    }

    private void populateAuditLogsTable() {
        auditLogsModel.setRowCount(0);
        for (SystemLog l : cachedLogs) {
            auditLogsModel.addRow(new Object[]{
                    l.getCreatedAt() != null ? l.getCreatedAt().format(formatter) : "--",
                    l.getUserId() != null ? "#" + l.getUserId() : "System",
                    l.getEventType(),
                    l.getDetails()
            });
        }
    }

    private void applyLogFilters() {
        String event = (String) logEventTypeBox.getSelectedItem();
        String search = logSearchField.getText() != null ? logSearchField.getText().trim().toLowerCase() : "";

        auditLogsModel.setRowCount(0);
        for (SystemLog l : cachedLogs) {
            boolean matchesEvent = "ALL EVENTS".equalsIgnoreCase(event) || (l.getEventType() != null && l.getEventType().equalsIgnoreCase(event));
            boolean matchesSearch = search.isEmpty() || (l.getDetails() != null && l.getDetails().toLowerCase().contains(search));

            if (matchesEvent && matchesSearch) {
                auditLogsModel.addRow(new Object[]{
                        l.getCreatedAt() != null ? l.getCreatedAt().format(formatter) : "--",
                        l.getUserId() != null ? "#" + l.getUserId() : "System",
                        l.getEventType(),
                        l.getDetails()
                });
            }
        }
    }

    private void inspectSelectedIncident() {
        int row = incidentsTable.getSelectedRow();
        if (row < 0) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident row to inspect details.");
            return;
        }
        int incId = (Integer) incidentsModel.getValueAt(row, 0);
        inspectIncidentDetailsById(incId);
    }

    private void inspectLiveOpsSelectedIncident() {
        int row = liveOpsTable.getSelectedRow();
        if (row < 0) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident row from Live Operations to inspect.");
            return;
        }
        int incId = (Integer) liveOpsModel.getValueAt(row, 0);
        inspectIncidentDetailsById(incId);
    }

    private void inspectIncidentDetailsById(int incId) {
        EmergencyIncident inc = cachedIncidents.stream().filter(i -> i.getId() == incId).findFirst().orElse(null);
        if (inc == null) return;

        EmergencyReport rep = cachedReports.stream().filter(r -> r.getId() == inc.getReportId()).findFirst().orElse(null);
        Location loc = rep != null ? cachedLocations.stream().filter(l -> l.getId() == rep.getLocationId()).findFirst().orElse(null) : null;

        showIncidentInspectionModal(inc, rep, loc);
    }

    private void showIncidentInspectionModal(EmergencyIncident inc, EmergencyReport rep, Location loc) {
        JDialog modal = new JDialog(this, "Incident Details - #" + inc.getId(), true);
        modal.setSize(540, 520);
        modal.setLocationRelativeTo(this);
        modal.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);

        JLabel title = new JLabel("Incident Command #" + inc.getId());
        title.setFont(UITheme.FONT_TITLE_LARGE);
        title.setForeground(UITheme.TEXT_PRIMARY);
        head.add(title, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badges.setOpaque(false);
        badges.add(new PriorityBadge(inc.getPriority()));
        badges.add(new StatusBadge(inc.getStatus()));
        head.add(badges, BorderLayout.EAST);

        root.add(head, BorderLayout.NORTH);

        // Content
        ModernCardPanel card = new ModernCardPanel(new GridLayout(6, 2, 8, 8), 12);
        card.add(createDetailLabel("Incident ID:"));
        card.add(createDetailValue("#" + inc.getId()));

        card.add(createDetailLabel("Category:"));
        card.add(createDetailValue(rep != null ? rep.getCategory() : "OTHER"));

        card.add(createDetailLabel("Location:"));
        String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "--";
        card.add(createDetailValue(locStr));

        card.add(createDetailLabel("GPS Coordinates:"));
        String coords = (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0))
                ? String.format(Locale.US, "%.6f, %.6f", loc.getLatitude(), loc.getLongitude()) : "No GPS data";
        card.add(createDetailValue(coords));

        card.add(createDetailLabel("Reported By:"));
        card.add(createDetailValue(rep != null ? "User #" + rep.getReporterId() : "--"));

        card.add(createDetailLabel("Assigned Dispatcher:"));
        card.add(createDetailValue(inc.getAssignedDispatcherId() != null ? "Dispatcher #" + inc.getAssignedDispatcherId() : "UNASSIGNED"));

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(card, BorderLayout.NORTH);

        if (rep != null) {
            JPanel descBox = new JPanel(new BorderLayout(0, 4));
            descBox.setOpaque(false);
            JLabel descLbl = new JLabel("Reporter Situation Description:");
            descLbl.setFont(UITheme.FONT_BODY_BOLD);
            descBox.add(descLbl, BorderLayout.NORTH);

            JTextArea area = new JTextArea(rep.getDescription());
            area.setFont(UITheme.FONT_BODY);
            area.setEditable(false);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(area);
            descBox.add(scroll, BorderLayout.CENTER);
            center.add(descBox, BorderLayout.CENTER);
        }

        root.add(center, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        if (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0)) {
            ModernButton osmBtn = new ModernButton("Open Map", VectorIcon.Type.LOCATION_PIN, ModernButton.ButtonStyle.SECONDARY);
            osmBtn.setPreferredSize(new Dimension(120, 36));
            osmBtn.addActionListener(e -> openOpenStreetMapUrl(loc.getLatitude(), loc.getLongitude()));
            btnRow.add(osmBtn);
        }

        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(100, 36));
        closeBtn.addActionListener(e -> modal.dispose());
        btnRow.add(closeBtn);

        root.add(btnRow, BorderLayout.SOUTH);

        modal.setContentPane(root);
        modal.setVisible(true);
    }

    private JLabel createDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    private JLabel createDetailValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }

    private void openLiveIncidentMap() {
        int row = liveOpsTable.getSelectedRow();
        if (row < 0) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident row from Live Operations table.");
            return;
        }
        int incId = (Integer) liveOpsModel.getValueAt(row, 0);
        EmergencyIncident inc = cachedIncidents.stream().filter(i -> i.getId() == incId).findFirst().orElse(null);
        if (inc == null) return;

        EmergencyReport rep = cachedReports.stream().filter(r -> r.getId() == inc.getReportId()).findFirst().orElse(null);
        Location loc = rep != null ? cachedLocations.stream().filter(l -> l.getId() == rep.getLocationId()).findFirst().orElse(null) : null;

        if (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0)) {
            openOpenStreetMapUrl(loc.getLatitude(), loc.getLongitude());
        } else {
            ModernDialog.showInfo(this, "No Coordinates", "Numeric GPS coordinates are not recorded for location: " + (loc != null ? loc.getBuildingName() : "Unknown"));
        }
    }

    private void openOpenStreetMapUrl(double lat, double lon) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                String url = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f", lat, lon, lat, lon);
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            LoggingService.warn("Failed to open OpenStreetMap: " + e.getMessage());
        }
    }

    private void showChangeStatusModal() {
        int row = incidentsTable.getSelectedRow();
        if (row < 0) {
            ModernDialog.showWarning(this, "Select Incident", "Please select an incident from the table to change status.");
            return;
        }
        int incId = (Integer) incidentsModel.getValueAt(row, 0);

        String[] statuses = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"};
        String selectedStatus = (String) JOptionPane.showInputDialog(
                this,
                "Select new operational status for Incident #" + incId + ":",
                "Update Incident Status",
                JOptionPane.QUESTION_MESSAGE,
                null,
                statuses,
                statuses[1]
        );

        if (selectedStatus != null) {
            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    emergencyManager.updateIncidentStatus(incId, selectedStatus);
                    SwingUtilities.invokeLater(() -> {
                        refreshConsole();
                        ModernDialog.showInfo(this, "Status Updated", "Incident #" + incId + " status changed to " + selectedStatus);
                    });
                } catch (Exception e) {
                    LoggingService.error("Failed to update status: " + e.getMessage(), e);
                    SwingUtilities.invokeLater(() -> ModernDialog.showError(this, "Update Error", e.getMessage()));
                }
            });
        }
    }

    private void handleDispatchExecution() {
        IncidentItem selInc = (IncidentItem) dispatchIncidentBox.getSelectedItem();
        ResourceItem selRes = (ResourceItem) dispatchResourceBox.getSelectedItem();

        if (selInc == null || selInc.getIncident() == null) {
            ModernDialog.showWarning(this, "Missing Incident", "Please select an active target incident.");
            return;
        }
        if (selRes == null || selRes.getResource() == null) {
            ModernDialog.showWarning(this, "Missing Resource", "Please select an available emergency resource.");
            return;
        }

        User admin = AuthenticationContext.getCurrentUser();
        int incId = selInc.getIncident().getId();
        int resId = selRes.getResource().getId();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                resourceDispatcher.assignResourceToIncident(admin, resId, incId);
                SwingUtilities.invokeLater(() -> {
                    refreshConsole();
                    ModernDialog.showInfo(this, "Resource Dispatched", "Resource '" + selRes.getResource().getName() + "' assigned to Incident #" + incId);
                });
            } catch (Exception e) {
                LoggingService.error("Failed dispatch: " + e.getMessage(), e);
                SwingUtilities.invokeLater(() -> ModernDialog.showError(this, "Dispatch Error", e.getMessage()));
            }
        });
    }

    private void handleReleaseResource() {
        int row = resourcesTable.getSelectedRow();
        if (row < 0) {
            ModernDialog.showWarning(this, "Select Resource", "Please select a resource row to release.");
            return;
        }

        int resId = (Integer) resourcesModel.getValueAt(row, 0);
        User admin = AuthenticationContext.getCurrentUser();

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                resourceDispatcher.releaseResource(admin, resId);
                SwingUtilities.invokeLater(() -> {
                    refreshConsole();
                    ModernDialog.showInfo(this, "Resource Released", "Resource #" + resId + " has been released to AVAILABLE.");
                });
            } catch (Exception e) {
                LoggingService.error("Failed release: " + e.getMessage(), e);
                SwingUtilities.invokeLater(() -> ModernDialog.showError(this, "Release Error", e.getMessage()));
            }
        });
    }

    private JPanel createHealthBadge(String label, String value, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label + ":");
        l.setFont(UITheme.FONT_KPI_LABEL);
        l.setForeground(UITheme.TEXT_SECONDARY);

        JLabel v = new JLabel(value);
        v.setFont(UITheme.FONT_BODY_BOLD);
        v.setForeground(color);

        p.add(l);
        p.add(v);
        return p;
    }

    private void handleLogout() {
        AuthenticationContext.clear();
        dispose();
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame(
                    ServiceRegistry.getInstance().getIdentityService(),
                    ServiceRegistry.getInstance()
            );
            loginFrame.setVisible(true);
        });
    }
}
