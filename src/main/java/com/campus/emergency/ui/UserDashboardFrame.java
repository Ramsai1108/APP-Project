package com.campus.emergency.ui;

import com.campus.emergency.dao.EmergencyReportDAO;
import com.campus.emergency.dao.LocationDAO;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.EmergencyIncident;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.Location;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.EmergencyManager;
import com.campus.emergency.service.IncidentUpdateService;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.service.TrackingService;
import com.campus.emergency.ui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserDashboardFrame extends JFrame {
    private final EmergencyManager emergencyManager;
    private final TrackingService trackingService;
    private final EmergencyReportDAO reportDAO;
    private final LocationDAO locationDAO;
    private final IncidentUpdateService incidentUpdateService;

    private ModernTable reportsTable;
    private DefaultTableModel tableModel;
    private NotificationPanel notificationPanel;
    private JLabel welcomeLabel;
    private MetricCard activeCard;
    private MetricCard resolvedCard;
    private MetricCard totalCard;
    private JPanel emptyStatePanel;
    private JPanel tableContainerPanel;
    private JPanel centerCardContainer;
    private CardLayout cardLayout;
    private StatusTimelinePanel activeTimelinePanel;
    private JLabel selectedReportSummaryLabel;

    private List<EmergencyReport> cachedMyReports = new ArrayList<>();
    private List<EmergencyIncident> cachedIncidents = new ArrayList<>();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserDashboardFrame() {
        this(ServiceRegistry.getInstance().getEmergencyManager(),
                ServiceRegistry.getInstance().getTrackingService(),
                ServiceRegistry.getInstance().getReportDAO(),
                ServiceRegistry.getInstance().getLocationDAO(),
                ServiceRegistry.getInstance().getIncidentUpdateService());
    }

    public UserDashboardFrame(EmergencyManager emergencyManager, TrackingService trackingService,
                              EmergencyReportDAO reportDAO, LocationDAO locationDAO) {
        this(emergencyManager, trackingService, reportDAO, locationDAO, ServiceRegistry.getInstance().getIncidentUpdateService());
    }

    public UserDashboardFrame(EmergencyManager emergencyManager, TrackingService trackingService,
                              EmergencyReportDAO reportDAO, LocationDAO locationDAO,
                              IncidentUpdateService incidentUpdateService) {
        this.emergencyManager = (emergencyManager != null) ? emergencyManager : ServiceRegistry.getInstance().getEmergencyManager();
        this.trackingService = (trackingService != null) ? trackingService : ServiceRegistry.getInstance().getTrackingService();
        this.reportDAO = (reportDAO != null) ? reportDAO : ServiceRegistry.getInstance().getReportDAO();
        this.locationDAO = (locationDAO != null) ? locationDAO : ServiceRegistry.getInstance().getLocationDAO();
        this.incidentUpdateService = (incidentUpdateService != null) ? incidentUpdateService : ServiceRegistry.getInstance().getIncidentUpdateService();

        initUI();
        setupRealTimePolling();
        refreshReports();
    }

    private void setupRealTimePolling() {
        if (incidentUpdateService != null) {
            incidentUpdateService.addListener(new IncidentUpdateService.IncidentUpdateListener() {
                @Override
                public void onNewIncidents(List<EmergencyIncident> newIncidents) {
                    refreshReports();
                }

                @Override
                public void onIncidentsChanged(List<EmergencyIncident> currentIncidents) {
                    refreshReports();
                }
            });
            incidentUpdateService.start(2);
        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopRealTimePolling();
            }
        });
    }

    private void stopRealTimePolling() {
        if (incidentUpdateService != null) {
            incidentUpdateService.stop();
        }
    }

    private void initUI() {
        User currentUser = AuthenticationContext.getCurrentUser();
        String userName = currentUser != null ? currentUser.getFullName() : "Student/Staff";
        String userRole = currentUser != null ? currentUser.getRole().name() : "STUDENT";

        setTitle("Campus Emergency Response - User Portal [" + userName + " (" + userRole + ")]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1140, 760);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(960, 640));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UITheme.BG_LIGHT);

        // ==========================================
        // 1. LEFT DARK SIDEBAR
        // ==========================================
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBackground(UITheme.PRIMARY_DARK);
        sidebar.setBorder(new EmptyBorder(24, 20, 24, 20));

        JPanel sideTop = new JPanel();
        sideTop.setLayout(new BoxLayout(sideTop, BoxLayout.Y_AXIS));
        sideTop.setOpaque(false);

        JLabel logoBadge = new JLabel("CAMPUS SAFETY");
        logoBadge.setFont(UITheme.FONT_BADGE);
        logoBadge.setForeground(new Color(147, 197, 253));
        sideTop.add(logoBadge);
        sideTop.add(Box.createVerticalStrut(6));

        JLabel logoTitle = new JLabel("Emergency Portal");
        logoTitle.setFont(UITheme.FONT_TITLE);
        logoTitle.setForeground(Color.WHITE);
        sideTop.add(logoTitle);
        sideTop.add(Box.createVerticalStrut(24));

        // Navigation Items
        ModernButton navDashboard = new ModernButton("Dashboard", VectorIcon.Type.CHART, ModernButton.ButtonStyle.DARK);
        navDashboard.setHorizontalAlignment(SwingConstants.LEFT);
        navDashboard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        ModernButton navSos = new ModernButton("ONE-TOUCH SOS", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.DANGER);
        navSos.setHorizontalAlignment(SwingConstants.LEFT);
        navSos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        navSos.setFont(new Font("Segoe UI", Font.BOLD, 12));
        navSos.addActionListener(e -> triggerSosEmergency());

        ModernButton navReport = new ModernButton("Report Incident", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.SECONDARY);
        navReport.setHorizontalAlignment(SwingConstants.LEFT);
        navReport.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        navReport.addActionListener(e -> openReportEmergency());

        sideTop.add(navDashboard);
        sideTop.add(Box.createVerticalStrut(8));
        sideTop.add(navSos);
        sideTop.add(Box.createVerticalStrut(8));
        sideTop.add(navReport);

        sidebar.add(sideTop, BorderLayout.NORTH);

        // Sidebar Bottom Profile & Logout
        JPanel sideBottom = new JPanel(new GridLayout(2, 1, 0, 8));
        sideBottom.setOpaque(false);

        JLabel userProfile = new JLabel(userName + " (" + userRole + ")");
        userProfile.setIcon(new VectorIcon(VectorIcon.Type.USER, 14, new Color(148, 163, 184)));
        userProfile.setFont(UITheme.FONT_SMALL);
        userProfile.setForeground(new Color(148, 163, 184));
        sideBottom.add(userProfile);

        ModernButton logoutBtn = new ModernButton("Log Out", VectorIcon.Type.LOGOUT, ModernButton.ButtonStyle.DARK);
        logoutBtn.addActionListener(e -> handleLogout());
        sideBottom.add(logoutBtn);

        sidebar.add(sideBottom, BorderLayout.SOUTH);
        root.add(sidebar, BorderLayout.WEST);

        // ==========================================
        // 2. MAIN CONTENT AREA
        // ==========================================
        JPanel mainContent = new JPanel(new BorderLayout(0, 16));
        mainContent.setBackground(UITheme.BG_LIGHT);
        mainContent.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Top Banner & Big CTA
        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel greetingPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        greetingPanel.setOpaque(false);

        welcomeLabel = new JLabel("Welcome, " + userName);
        welcomeLabel.setFont(UITheme.FONT_TITLE_LARGE);
        welcomeLabel.setForeground(UITheme.TEXT_PRIMARY);
        greetingPanel.add(welcomeLabel);

        JLabel subtitle = new JLabel("Campus Safety & Emergency Dispatch System");
        subtitle.setFont(UITheme.FONT_BODY);
        subtitle.setForeground(UITheme.TEXT_SECONDARY);
        greetingPanel.add(subtitle);
        topHeader.add(greetingPanel, BorderLayout.WEST);

        JPanel ctaGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        ctaGroup.setOpaque(false);

        ModernButton refreshBtn = new ModernButton("Refresh", VectorIcon.Type.REFRESH, ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.addActionListener(e -> refreshReports());

        ModernButton sosBtn = new ModernButton("ONE-TOUCH SOS", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.DANGER);
        sosBtn.setPreferredSize(new Dimension(170, 42));
        sosBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sosBtn.addActionListener(e -> triggerSosEmergency());

        ModernButton bigEmergencyBtn = new ModernButton("REPORT EMERGENCY", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.PRIMARY);
        bigEmergencyBtn.setPreferredSize(new Dimension(200, 42));
        bigEmergencyBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bigEmergencyBtn.addActionListener(e -> openReportEmergency());

        ctaGroup.add(refreshBtn);
        ctaGroup.add(sosBtn);
        ctaGroup.add(bigEmergencyBtn);
        topHeader.add(ctaGroup, BorderLayout.EAST);

        mainContent.add(topHeader, BorderLayout.NORTH);

        // Center Content: Metric Cards + Incidents Section + Notifications Tab
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // Metric Stat Cards Row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRow.setOpaque(false);

        activeCard = new MetricCard(VectorIcon.Type.ALERT, "Active Emergencies", "0", "Awaiting or in progress", UITheme.EMERGENCY_RED);
        resolvedCard = new MetricCard(VectorIcon.Type.CHECK, "Resolved Cases", "0", "Successfully closed", UITheme.SUCCESS_GREEN);
        totalCard = new MetricCard(VectorIcon.Type.LIST, "My Total Reports", "0", "Lifetime submissions", UITheme.ACCENT_BLUE);

        statsRow.add(activeCard);
        statsRow.add(resolvedCard);
        statsRow.add(totalCard);
        centerPanel.add(statsRow, BorderLayout.NORTH);

        // Tabs for Incidents and Notifications
        ModernTabbedPane tabbedPane = new ModernTabbedPane();

        // Tab 1: My Emergencies
        ModernCardPanel incidentsCard = new ModernCardPanel(new BorderLayout(0, 10), 14);

        cardLayout = new CardLayout();
        centerCardContainer = new JPanel(cardLayout);
        centerCardContainer.setOpaque(false);

        // Container when reports exist
        tableContainerPanel = new JPanel(new BorderLayout(0, 10));
        tableContainerPanel.setOpaque(false);

        // Active Status Progression Header Strip
        JPanel activeStatusStrip = new JPanel(new BorderLayout(10, 0));
        activeStatusStrip.setBackground(new Color(248, 250, 252));
        activeStatusStrip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel summaryLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        summaryLeft.setOpaque(false);
        selectedReportSummaryLabel = new JLabel("Selected Incident Progression:");
        selectedReportSummaryLabel.setFont(UITheme.FONT_BODY_BOLD);
        selectedReportSummaryLabel.setForeground(UITheme.TEXT_PRIMARY);
        summaryLeft.add(selectedReportSummaryLabel);
        activeStatusStrip.add(summaryLeft, BorderLayout.WEST);

        activeTimelinePanel = new StatusTimelinePanel("OPEN");
        activeStatusStrip.add(activeTimelinePanel, BorderLayout.CENTER);

        ModernButton viewDetailsActionBtn = new ModernButton("View Full Details", VectorIcon.Type.EYE, ModernButton.ButtonStyle.PRIMARY);
        viewDetailsActionBtn.setPreferredSize(new Dimension(150, 32));
        viewDetailsActionBtn.addActionListener(e -> openSelectedReportDetails());
        activeStatusStrip.add(viewDetailsActionBtn, BorderLayout.EAST);

        tableContainerPanel.add(activeStatusStrip, BorderLayout.NORTH);

        String[] columns = {"Report ID", "Category", "Description", "Location", "Status", "Priority"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        reportsTable = new ModernTable(tableModel);
        reportsTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        reportsTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        reportsTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        reportsTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        reportsTable.getColumnModel().getColumn(4).setPreferredWidth(110);
        reportsTable.getColumnModel().getColumn(5).setPreferredWidth(100);

        reportsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleReportSelection();
            }
        });

        reportsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && reportsTable.getSelectedRow() != -1) {
                    openSelectedReportDetails();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(reportsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        tableContainerPanel.add(tableScroll, BorderLayout.CENTER);

        // Useful Empty State Panel
        emptyStatePanel = new JPanel(new GridBagLayout());
        emptyStatePanel.setBackground(Color.WHITE);
        JPanel emptyInner = new JPanel();
        emptyInner.setLayout(new BoxLayout(emptyInner, BoxLayout.Y_AXIS));
        emptyInner.setOpaque(false);

        JLabel emptyIcon = new JLabel(new VectorIcon(VectorIcon.Type.SHIELD, 48, UITheme.TEXT_MUTED), SwingConstants.CENTER);
        emptyIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyIcon);
        emptyInner.add(Box.createVerticalStrut(12));

        JLabel emptyTitle = new JLabel("No Emergency Reports Filed", SwingConstants.CENTER);
        emptyTitle.setFont(UITheme.FONT_TITLE);
        emptyTitle.setForeground(UITheme.TEXT_PRIMARY);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptyTitle);
        emptyInner.add(Box.createVerticalStrut(6));

        JLabel emptySub = new JLabel("You have not submitted any emergency alerts yet.", SwingConstants.CENTER);
        emptySub.setFont(UITheme.FONT_BODY);
        emptySub.setForeground(UITheme.TEXT_MUTED);
        emptySub.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyInner.add(emptySub);
        emptyInner.add(Box.createVerticalStrut(16));

        ModernButton reportNowBtn = new ModernButton("Report an Emergency", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.DANGER);
        reportNowBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        reportNowBtn.addActionListener(e -> openReportEmergency());
        emptyInner.add(reportNowBtn);

        emptyStatePanel.add(emptyInner);

        centerCardContainer.add(tableContainerPanel, "TABLE");
        centerCardContainer.add(emptyStatePanel, "EMPTY");
        incidentsCard.add(centerCardContainer, BorderLayout.CENTER);

        tabbedPane.addTab("My Emergency Reports", incidentsCard);

        // Tab 2: Notifications Panel
        notificationPanel = new NotificationPanel(
                ServiceRegistry.getInstance().getNotificationBroker(),
                trackingService
        );
        tabbedPane.addTab("Campus Alerts & Notifications", notificationPanel);

        centerPanel.add(tabbedPane, BorderLayout.CENTER);
        mainContent.add(centerPanel, BorderLayout.CENTER);

        root.add(mainContent, BorderLayout.CENTER);
        add(root);
    }

    public void refreshReports() {
        User currentUser = AuthenticationContext.getCurrentUser();
        if (currentUser == null) return;

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<EmergencyReport> allReports = reportDAO.findAll();
                List<EmergencyReport> myReports = allReports.stream()
                        .filter(r -> r.getReporterId() == currentUser.getId())
                        .collect(Collectors.toList());

                List<EmergencyIncident> incidents = trackingService.getActiveIncidents(currentUser);

                cachedMyReports = new ArrayList<>(myReports);
                cachedIncidents = new ArrayList<>(incidents);

                int activeCount = 0;
                int resolvedCount = 0;

                for (EmergencyReport r : myReports) {
                    if ("RESOLVED".equalsIgnoreCase(r.getStatus()) || "CLOSED".equalsIgnoreCase(r.getStatus())) {
                        resolvedCount++;
                    } else {
                        activeCount++;
                    }
                }

                final int finalActive = activeCount;
                final int finalResolved = resolvedCount;
                final int finalTotal = myReports.size();

                SwingUtilities.invokeLater(() -> {
                    activeCard.setValue(String.valueOf(finalActive));
                    resolvedCard.setValue(String.valueOf(finalResolved));
                    totalCard.setValue(String.valueOf(finalTotal));

                    tableModel.setRowCount(0);

                    if (myReports.isEmpty()) {
                        cardLayout.show(centerCardContainer, "EMPTY");
                    } else {
                        cardLayout.show(centerCardContainer, "TABLE");

                        for (EmergencyReport r : myReports) {
                            Optional<EmergencyIncident> matchedInc = incidents.stream()
                                    .filter(inc -> inc.getReportId() == r.getId())
                                    .findFirst();

                            String priority = matchedInc.map(EmergencyIncident::getPriority).orElse("NORMAL");
                            String status = matchedInc.map(EmergencyIncident::getStatus).orElse(r.getStatus());

                            Location loc = locationDAO.findById(r.getLocationId()).orElse(null);
                            String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "Location #" + r.getLocationId();

                            tableModel.addRow(new Object[]{
                                    "#" + r.getId(),
                                    r.getCategory(),
                                    r.getDescription(),
                                    locStr,
                                    status,
                                    priority
                            });
                        }

                        if (reportsTable.getRowCount() > 0) {
                            reportsTable.setRowSelectionInterval(0, 0);
                        }
                    }
                });
            } catch (Exception e) {
                LoggingService.error("Failed to refresh user dashboard reports: " + e.getMessage(), e);
            }
        });
    }

    public void triggerSosEmergency() {
        User currentUser = AuthenticationContext.getCurrentUser();
        if (currentUser == null) {
            ModernDialog.showError(this, "Authentication Required", "You must be logged in to trigger an SOS alert.");
            return;
        }

        JDialog sosDialog = new JDialog(this, "SOS Emergency Distress Signal", true);
        sosDialog.setSize(480, 360);
        sosDialog.setLocationRelativeTo(this);
        sosDialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        JLabel alertIcon = new JLabel(new VectorIcon(VectorIcon.Type.ALERT, 24, UITheme.EMERGENCY_RED));
        head.add(alertIcon);

        JLabel titleLbl = new JLabel("ONE-TOUCH SOS EMERGENCY");
        titleLbl.setFont(UITheme.FONT_TITLE_LARGE);
        titleLbl.setForeground(UITheme.EMERGENCY_RED);
        head.add(titleLbl);
        root.add(head, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JLabel descLbl = new JLabel("<html><b>TRANSMIT IMMEDIATE HIGH-PRIORITY DISTRESS SIGNAL</b><br>This will alert Campus Police, Emergency Dispatch, and First Responders with maximum priority.</html>");
        descLbl.setFont(UITheme.FONT_BODY);
        descLbl.setForeground(UITheme.TEXT_PRIMARY);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(descLbl);
        body.add(Box.createVerticalStrut(14));

        JLabel locPrompt = new JLabel("Confirm Your Current Location:");
        locPrompt.setFont(UITheme.FONT_BODY_BOLD);
        locPrompt.setForeground(UITheme.TEXT_SECONDARY);
        locPrompt.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(locPrompt);
        body.add(Box.createVerticalStrut(4));

        JComboBox<ReportEmergencyFrame.LocationItem> locBox = new JComboBox<>();
        locBox.setFont(UITheme.FONT_BODY);
        locBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        locBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<Location> locs = locationDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    for (Location l : locs) {
                        locBox.addItem(new ReportEmergencyFrame.LocationItem(l));
                    }
                    if (locBox.getItemCount() > 0) {
                        locBox.setSelectedIndex(0);
                    }
                });
            } catch (Exception ex) {
                LoggingService.error("Failed to load locations for SOS: " + ex.getMessage(), ex);
            }
        });
        body.add(locBox);
        body.add(Box.createVerticalStrut(12));

        JLabel warningBadge = new JLabel("Priority: CRITICAL | Automatic Notification: ALL RESPONDERS");
        warningBadge.setFont(UITheme.FONT_META);
        warningBadge.setForeground(UITheme.EMERGENCY_RED);
        warningBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(warningBadge);

        root.add(body, BorderLayout.CENTER);

        // Footer Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelBtn.setPreferredSize(new Dimension(95, 38));
        cancelBtn.addActionListener(e -> sosDialog.dispose());
        btnPanel.add(cancelBtn);

        ModernButton transmitBtn = new ModernButton("BROADCAST SOS NOW", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.DANGER);
        transmitBtn.setPreferredSize(new Dimension(200, 38));
        transmitBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        transmitBtn.addActionListener(e -> {
            ReportEmergencyFrame.LocationItem selLoc = (ReportEmergencyFrame.LocationItem) locBox.getSelectedItem();
            int locId = (selLoc != null && selLoc.getLocation() != null) ? selLoc.getLocation().getId() : 1;
            String locName = (selLoc != null && selLoc.getLocation() != null) ? selLoc.getLocation().getBuildingName() : "Campus Main Hall";

            transmitBtn.setEnabled(false);
            cancelBtn.setEnabled(false);

            ThreadPoolManager.getInstance().submit(() -> {
                try {
                    String desc = "INSTANT SOS PANIC ALERT: User " + currentUser.getFullName() + " (" + currentUser.getUsername()
                            + ") triggered emergency SOS distress beacon at " + locName + ". Immediate on-scene dispatch requested!";
                    EmergencyReport report = emergencyManager.createReport(
                            currentUser, "SECURITY", desc, locId, 1, null, null
                    );

                    SwingUtilities.invokeLater(() -> {
                        sosDialog.dispose();
                        refreshReports();
                        showSosSuccessDialog(report);
                    });
                } catch (Exception ex) {
                    LoggingService.error("Failed to broadcast SOS alert: " + ex.getMessage(), ex);
                    SwingUtilities.invokeLater(() -> {
                        transmitBtn.setEnabled(true);
                        cancelBtn.setEnabled(true);
                        ModernDialog.showError(sosDialog, "SOS Broadcast Error", "Failed to broadcast SOS: " + ex.getMessage());
                    });
                }
            });
        });
        btnPanel.add(transmitBtn);

        root.add(btnPanel, BorderLayout.SOUTH);
        sosDialog.setContentPane(root);
        sosDialog.setVisible(true);
    }

    private void showSosSuccessDialog(EmergencyReport report) {
        JDialog successDialog = new JDialog(this, "SOS Alert Broadcasted", true);
        successDialog.setSize(460, 300);
        successDialog.setLocationRelativeTo(this);
        successDialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        head.setOpaque(false);
        head.add(new JLabel(new VectorIcon(VectorIcon.Type.CHECK, 24, UITheme.SUCCESS_GREEN)));

        JLabel title = new JLabel("SOS DISTRESS BEACON ACTIVE");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.EMERGENCY_RED);
        head.add(title);
        root.add(head, BorderLayout.NORTH);

        ModernCardPanel card = new ModernCardPanel(new GridLayout(3, 2, 8, 8), 12);
        card.add(createModalFieldLabel("Emergency ID:"));
        card.add(createModalFieldValue("#" + report.getId()));
        card.add(createModalFieldLabel("Priority:"));
        card.add(createModalFieldValue("CRITICAL (HIGHEST)"));
        card.add(createModalFieldLabel("Dispatch Status:"));
        card.add(createModalFieldValue("DISPATCHED TO FIELD TEAMS"));

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(card, BorderLayout.NORTH);

        JLabel note = new JLabel("<html>Campus Security and Medical First Responders have been notified.<br><b>Stay in a safe location until help arrives.</b></html>");
        note.setFont(UITheme.FONT_BODY);
        note.setForeground(UITheme.TEXT_PRIMARY);
        center.add(note, BorderLayout.CENTER);

        root.add(center, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        ModernButton okBtn = new ModernButton("Acknowledge & Dismiss", ModernButton.ButtonStyle.PRIMARY);
        okBtn.setPreferredSize(new Dimension(180, 36));
        okBtn.addActionListener(e -> successDialog.dispose());
        btnPanel.add(okBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        successDialog.setContentPane(root);
        successDialog.setVisible(true);
    }

    private void handleReportSelection() {
        int selectedRow = reportsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= cachedMyReports.size()) {
            return;
        }

        EmergencyReport report = cachedMyReports.get(selectedRow);
        Optional<EmergencyIncident> matchedInc = cachedIncidents.stream()
                .filter(inc -> inc.getReportId() == report.getId())
                .findFirst();

        String status = matchedInc.map(EmergencyIncident::getStatus).orElse(report.getStatus());
        selectedReportSummaryLabel.setText("Selected Report #" + report.getId() + " (" + report.getCategory() + "):");
        activeTimelinePanel.setStatus(status);
    }

    private void openReportEmergency() {
        ReportEmergencyFrame reportFrame = new ReportEmergencyFrame(
                emergencyManager,
                locationDAO,
                this::refreshReports
        );
        reportFrame.setVisible(true);
    }

    private void openSelectedReportDetails() {
        int selectedRow = reportsTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= cachedMyReports.size()) {
            ModernDialog.showWarning(this, "Select Report", "Please select a report from the table to view its full details.");
            return;
        }

        EmergencyReport report = cachedMyReports.get(selectedRow);
        EmergencyIncident inc = cachedIncidents.stream()
                .filter(i -> i.getReportId() == report.getId())
                .findFirst()
                .orElse(null);

        Location loc = locationDAO.findById(report.getLocationId()).orElse(null);

        showReportDetailsModal(report, inc, loc);
    }

    private void showReportDetailsModal(EmergencyReport report, EmergencyIncident inc, Location loc) {
        JDialog modal = new JDialog(this, "Emergency Report Details - #" + report.getId(), true);
        modal.setSize(520, 480);
        modal.setLocationRelativeTo(this);
        modal.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headPanel = new JPanel(new BorderLayout());
        headPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("Emergency Report #" + report.getId());
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        headPanel.add(titleLbl, BorderLayout.WEST);

        JPanel badgesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badgesPanel.setOpaque(false);
        String priorityStr = inc != null && inc.getPriority() != null ? inc.getPriority() : "NORMAL";
        badgesPanel.add(new PriorityBadge(priorityStr));
        badgesPanel.add(new StatusBadge(report.getStatus()));
        headPanel.add(badgesPanel, BorderLayout.EAST);

        root.add(headPanel, BorderLayout.NORTH);

        // Body Info Card
        JPanel centerBody = new JPanel(new BorderLayout(0, 10));
        centerBody.setOpaque(false);

        ModernCardPanel infoCard = new ModernCardPanel(new GridLayout(6, 2, 8, 8), 12);
        infoCard.add(createModalFieldLabel("Category:"));
        infoCard.add(createModalFieldValue(report.getCategory()));

        infoCard.add(createModalFieldLabel("Location:"));
        String locStr = loc != null ? loc.getBuildingName() + " (Room " + loc.getRoomNumber() + ")" : "Location #" + report.getLocationId();
        infoCard.add(createModalFieldValue(locStr));

        infoCard.add(createModalFieldLabel("Created Time:"));
        infoCard.add(createModalFieldValue(report.getCreatedAt() != null ? report.getCreatedAt().format(formatter) : "--"));

        infoCard.add(createModalFieldLabel("Last Update:"));
        infoCard.add(createModalFieldValue(report.getUpdatedAt() != null ? report.getUpdatedAt().format(formatter) : "--"));

        infoCard.add(createModalFieldLabel("Assigned Dispatcher:"));
        String dispStr = (inc != null && inc.getAssignedDispatcherId() != null) ? "Dispatcher #" + inc.getAssignedDispatcherId() : "Response Center Queue";
        infoCard.add(createModalFieldValue(dispStr));

        infoCard.add(createModalFieldLabel("Current Status:"));
        infoCard.add(createModalFieldValue(report.getStatus()));

        centerBody.add(infoCard, BorderLayout.NORTH);

        // Description Box
        JPanel descPanel = new JPanel(new BorderLayout(0, 4));
        descPanel.setOpaque(false);
        JLabel descHeader = new JLabel("Situation Description:");
        descHeader.setFont(UITheme.FONT_BODY_BOLD);
        descHeader.setForeground(UITheme.TEXT_SECONDARY);
        descPanel.add(descHeader, BorderLayout.NORTH);

        JTextArea descArea = new JTextArea(report.getDescription());
        descArea.setFont(UITheme.FONT_BODY);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER),
                new EmptyBorder(8, 8, 8, 8)
        ));
        JScrollPane descScroll = new JScrollPane(descArea);
        descPanel.add(descScroll, BorderLayout.CENTER);

        centerBody.add(descPanel, BorderLayout.CENTER);
        root.add(centerBody, BorderLayout.CENTER);

        // Bottom Actions
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

    private void handleLogout() {
        stopRealTimePolling();
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
