package com.campus.emergency.ui;

import com.campus.emergency.concurrency.TaskExecutorManager;
import com.campus.emergency.dao.IEmergencyReportDao;
import com.campus.emergency.dao.IIncidentTimelineDao;
import com.campus.emergency.dao.IResourceAllocationDao;
import com.campus.emergency.dao.impl.EmergencyReportDaoImpl;
import com.campus.emergency.dao.impl.IncidentTimelineDaoImpl;
import com.campus.emergency.dao.impl.ResourceAllocationDaoImpl;
import com.campus.emergency.model.*;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.DispatchResult;
import com.campus.emergency.service.IncidentStatusService;
import com.campus.emergency.service.ResourceDispatcher;
import com.campus.emergency.service.impl.IncidentStatusServiceImpl;
import com.campus.emergency.service.impl.ResourceDispatcherImpl;
import com.campus.emergency.util.DateFormatterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Detailed Inspector & Action Frame for an Emergency Incident.
 * Displays Incident info, Location, Reporter details, Assigned Responders/Resources, Timeline,
 * and executes transacted dispatch and status updates.
 */
public class IncidentDetailsFrame extends JDialog {

    private static final Logger logger = LoggerFactory.getLogger(IncidentDetailsFrame.class);

    private final EmergencyIncident incident;
    private final IncidentStatusService statusService;
    private final ResourceDispatcher resourceDispatcher;
    private final IEmergencyReportDao reportDao;
    private final IIncidentTimelineDao timelineDao;
    private final IResourceAllocationDao allocationDao;
    private final Runnable onRefreshCallback;

    private EmergencyReport report;

    // Action Buttons
    private JButton dispatchBtn;
    private JButton manualDispatchBtn;
    private JButton inProgressBtn;
    private JButton resolveBtn;
    private JButton closeBtn;
    private JButton cancelBtn;

    private JLabel statusValueLabel;
    private DefaultTableModel timelineTableModel;
    private JTextArea allocationDetailsArea;

    public IncidentDetailsFrame(Window owner,
                                EmergencyIncident incident,
                                IncidentStatusService statusService,
                                ResourceDispatcher resourceDispatcher,
                                Runnable onRefreshCallback) {
        super(owner, "Incident #" + (incident != null ? incident.getId() : "") + " Control Details", ModalityType.APPLICATION_MODAL);
        this.incident = incident;
        this.statusService = statusService != null ? statusService : new IncidentStatusServiceImpl();
        this.resourceDispatcher = resourceDispatcher != null ? resourceDispatcher : new ResourceDispatcherImpl();
        this.reportDao = new EmergencyReportDaoImpl();
        this.timelineDao = new IncidentTimelineDaoImpl();
        this.allocationDao = new ResourceAllocationDaoImpl();
        this.onRefreshCallback = onRefreshCallback;

        if (incident != null && incident.getReportId() != null) {
            this.report = reportDao.findById(incident.getReportId()).orElse(null);
        }

        initUI();
        loadTimelineAndAllocationsAsync();
    }

    private void initUI() {
        setSize(880, 680);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 26, 32));
        headerPanel.setBorder(new EmptyBorder(12, 20, 12, 20));

        double priorityScore = incident != null ? incident.getEffectivePriority() : 0.0;
        String priorityIcon = priorityScore >= 80.0 ? "🔴 CRITICAL" : (priorityScore >= 60.0 ? "🟠 HIGH" : (priorityScore >= 40.0 ? "🟡 MEDIUM" : "🟢 LOW"));

        JLabel title = new JLabel("INCIDENT CONTROL ROOM — INCIDENT #" + (incident != null ? incident.getId() : ""));
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(Color.WHITE);

        JLabel priorityLabel = new JLabel("Priority: " + priorityIcon + String.format(" (%.1f / 100)", priorityScore));
        priorityLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        priorityLabel.setForeground(new Color(240, 200, 80));

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(priorityLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Center Content Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));

        tabbedPane.addTab("General & Location", createGeneralInfoPanel());
        tabbedPane.addTab("Assigned Resources & Responders", createResourceAllocationPanel());
        tabbedPane.addTab("Event Timeline", createTimelinePanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Action Control Bar
        JPanel actionControlBar = createActionControlBar();
        add(actionControlBar, BorderLayout.SOUTH);
    }

    private JPanel createGeneralInfoPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Left Panel: Incident Details
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createTitledBorder("Incident Summary"));

        statusValueLabel = new JLabel(incident != null ? incident.getStatus() : "N/A");
        statusValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        left.add(createPair("Incident ID:", "#" + (incident != null ? incident.getId() : "")));
        left.add(createPair("Report ID:", "#" + (incident != null ? incident.getReportId() : "")));
        left.add(createPair("Title:", report != null ? report.getTitle() : "N/A"));
        left.add(createPair("Category:", report != null ? report.getCategory() : "N/A"));
        left.add(createPair("Severity:", (report != null ? report.getSeverity() : 1) + " / 5"));
        left.add(createPair("Status:", statusValueLabel.getText()));
        left.add(createPair("Created At:", incident != null ? DateFormatterUtil.formatDateTime(incident.getCreatedAt()) : "N/A"));

        // Right Panel: Location & Reporter
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createTitledBorder("Location & Reporter Information"));

        right.add(createPair("Location:", report != null ? report.getLocation() : "N/A"));
        right.add(createPair("Affected People:", String.valueOf(report != null ? report.getAffectedPeople() : 0)));
        right.add(createPair("Reporter ID:", "#" + (report != null ? report.getReporterId() : "")));
        right.add(createPair("Media Attachment:", report != null && report.getMediaPath() != null ? report.getMediaPath() : "None"));

        right.add(Box.createRigidArea(new Dimension(0, 10)));
        JLabel descHeader = new JLabel("Description:");
        descHeader.setFont(new Font("Segoe UI", Font.BOLD, 12));
        right.add(descHeader);

        JTextArea descArea = new JTextArea(report != null ? report.getDescription() : "", 4, 25);
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        right.add(new JScrollPane(descArea));

        panel.add(left);
        panel.add(right);
        return panel;
    }

    private JPanel createResourceAllocationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        allocationDetailsArea = new JTextArea("Loading active resource allocations...");
        allocationDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        allocationDetailsArea.setEditable(false);

        panel.add(new JLabel("Current Active Responders & Resource Allocations:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(allocationDetailsArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTimelinePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        timelineTableModel = new DefaultTableModel(new String[]{"ID", "Action", "Description", "Timestamp"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(timelineTableModel);
        table.setRowHeight(24);

        panel.add(new JLabel("Incident Action & Audit Event History:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createActionControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 224, 230)));

        dispatchBtn = new JButton("⚡ Auto Dispatch");
        manualDispatchBtn = new JButton("📋 Manual Dispatch");
        inProgressBtn = new JButton("▶ Mark In Progress");
        resolveBtn = new JButton("✔ Mark Resolved");
        closeBtn = new JButton("🔒 Close Incident");
        cancelBtn = new JButton("❌ Cancel Incident");

        dispatchBtn.setBackground(new Color(220, 53, 69));
        dispatchBtn.setForeground(Color.WHITE);
        manualDispatchBtn.setBackground(new Color(40, 120, 220));
        manualDispatchBtn.setForeground(Color.WHITE);

        dispatchBtn.addActionListener(e -> executeAutoDispatch());
        manualDispatchBtn.addActionListener(e -> executeManualDispatch());
        inProgressBtn.addActionListener(e -> updateStatus("IN_PROGRESS", "Responder reached scene"));
        resolveBtn.addActionListener(e -> updateStatus("RESOLVED", "Emergency incident resolved successfully"));
        closeBtn.addActionListener(e -> updateStatus("CLOSED", "Incident closed after operational review"));
        cancelBtn.addActionListener(e -> updateStatus("CANCELLED", "Emergency incident cancelled by dispatcher"));

        JButton closeWindowBtn = new JButton("Close Window");
        closeWindowBtn.addActionListener(e -> dispose());

        bar.add(dispatchBtn);
        bar.add(manualDispatchBtn);
        bar.add(inProgressBtn);
        bar.add(resolveBtn);
        bar.add(closeBtn);
        bar.add(cancelBtn);
        bar.add(closeWindowBtn);

        updateButtonStates();
        return bar;
    }

    private void updateButtonStates() {
        String status = incident != null && incident.getStatus() != null ? incident.getStatus().toUpperCase() : "REPORTED";

        boolean isClosedOrCancelled = "CLOSED".equals(status) || "CANCELLED".equals(status);

        dispatchBtn.setEnabled(!isClosedOrCancelled && !"DISPATCHED".equals(status) && !"IN_PROGRESS".equals(status) && !"RESOLVED".equals(status));
        manualDispatchBtn.setEnabled(!isClosedOrCancelled && !"DISPATCHED".equals(status) && !"IN_PROGRESS".equals(status) && !"RESOLVED".equals(status));
        inProgressBtn.setEnabled("DISPATCHED".equals(status));
        resolveBtn.setEnabled("DISPATCHED".equals(status) || "IN_PROGRESS".equals(status));
        closeBtn.setEnabled("RESOLVED".equals(status));
        cancelBtn.setEnabled(!isClosedOrCancelled && !"RESOLVED".equals(status));
    }

    private void executeAutoDispatch() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to automatically dispatch optimal responders and resources for Incident #" + incident.getId() + "?",
                "Confirm Auto Dispatch", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        var user = SessionManager.getCurrentUser();
        Long dispatcherId = user != null ? user.getId() : 2L;

        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                String category = report != null ? report.getCategory() : "OTHER";
                DispatchResult result = resourceDispatcher.dispatchForIncident(incident.getId(), category, dispatcherId);

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Auto Dispatch Successful!\n\n" + result.dispatchSummary(), "Dispatch Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    incident.setStatus("DISPATCHED");
                    statusValueLabel.setText("DISPATCHED");
                    updateButtonStates();
                    loadTimelineAndAllocationsAsync();
                    if (onRefreshCallback != null) onRefreshCallback.run();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Auto Dispatch Failed: " + ex.getMessage(), "Dispatch Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void executeManualDispatch() {
        var user = SessionManager.getCurrentUser();
        Long dispatcherId = user != null ? user.getId() : 2L;

        String responderIdStr = JOptionPane.showInputDialog(this, "Enter Responder ID to assign (or leave blank):", "Manual Responder Dispatch", JOptionPane.QUESTION_MESSAGE);
        Long responderId = (responderIdStr != null && !responderIdStr.trim().isEmpty()) ? Long.parseLong(responderIdStr.trim()) : null;

        String resourceIdStr = JOptionPane.showInputDialog(this, "Enter Resource ID to assign (or leave blank):", "Manual Resource Dispatch", JOptionPane.QUESTION_MESSAGE);
        List<Long> resourceIds = (resourceIdStr != null && !resourceIdStr.trim().isEmpty()) ? List.of(Long.parseLong(resourceIdStr.trim())) : List.of();

        if (responderId == null && resourceIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "At least one responder or resource must be selected for dispatch.", "Manual Dispatch Cancelled", JOptionPane.WARNING_MESSAGE);
            return;
        }

        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                DispatchResult result = resourceDispatcher.dispatchManual(incident.getId(), responderId, resourceIds, dispatcherId);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Manual Dispatch Successful!\n\n" + result.dispatchSummary(), "Dispatch Confirmed", JOptionPane.INFORMATION_MESSAGE);
                    incident.setStatus("DISPATCHED");
                    statusValueLabel.setText("DISPATCHED");
                    updateButtonStates();
                    loadTimelineAndAllocationsAsync();
                    if (onRefreshCallback != null) onRefreshCallback.run();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Manual Dispatch Failed: " + ex.getMessage(), "Dispatch Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void updateStatus(String targetStatus, String defaultReason) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Update Incident #" + incident.getId() + " status to " + targetStatus + "?",
                "Confirm Status Change", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        var user = SessionManager.getCurrentUser();
        Long actingUserId = user != null ? user.getId() : 2L;

        TaskExecutorManager.submitAsyncTask(() -> {
            try {
                statusService.updateIncidentStatus(incident.getId(), targetStatus, actingUserId, defaultReason);

                SwingUtilities.invokeLater(() -> {
                    incident.setStatus(targetStatus);
                    statusValueLabel.setText(targetStatus);
                    updateButtonStates();
                    loadTimelineAndAllocationsAsync();
                    if (onRefreshCallback != null) onRefreshCallback.run();
                    JOptionPane.showMessageDialog(this, "Incident status updated to " + targetStatus, "Status Updated", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Status Update Failed: " + ex.getMessage(), "Status Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void loadTimelineAndAllocationsAsync() {
        if (incident == null) return;

        TaskExecutorManager.submitAsyncTask(() -> {
            List<IncidentTimeline> timelineList = timelineDao.findByIncidentId(incident.getId());
            List<ResourceAllocation> allocations = allocationDao.findByIncidentId(incident.getId());

            SwingUtilities.invokeLater(() -> {
                // Populate Timeline Table
                timelineTableModel.setRowCount(0);
                for (IncidentTimeline t : timelineList) {
                    timelineTableModel.addRow(new Object[]{
                            "#" + t.getId(),
                            t.getActionType(),
                            t.getDescription(),
                            DateFormatterUtil.formatDateTime(t.getCreatedAt())
                    });
                }

                // Populate Resource Allocation Summary
                if (allocations.isEmpty()) {
                    allocationDetailsArea.setText("No active responders or resources allocated to this incident yet.");
                } else {
                    StringBuilder sb = new StringBuilder("ACTIVE ALLOCATIONS:\n==============================\n");
                    for (ResourceAllocation a : allocations) {
                        sb.append(String.format("Allocation ID: #%d | Responder ID: %s | Resource ID: %s | Status: %s | Allocated At: %s\n",
                                a.getId(),
                                a.getResponderId() != null ? "#" + a.getResponderId() : "N/A",
                                a.getResourceId() != null ? "#" + a.getResourceId() : "N/A",
                                a.getStatus(),
                                DateFormatterUtil.formatDateTime(a.getAllocatedAt())
                        ));
                    }
                    allocationDetailsArea.setText(sb.toString());
                }
            });
        });
    }

    private JPanel createPair(String label, String value) {
        JPanel line = new JPanel(new BorderLayout(5, 5));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setPreferredSize(new Dimension(140, 22));

        JLabel val = new JLabel(value != null ? value : "");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        line.add(lbl, BorderLayout.WEST);
        line.add(val, BorderLayout.CENTER);
        return line;
    }
}
