package com.campus.emergency.ui.components;

import com.campus.emergency.model.Notification;
import com.campus.emergency.model.NotificationStatus;
import com.campus.emergency.security.SessionManager;
import com.campus.emergency.service.NotificationBroker;
import com.campus.emergency.service.impl.NotificationBrokerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reusable Swing Notification Panel component.
 * Displays user notification alerts, unread badges, priority indicators, and action controls.
 */
public class NotificationPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPanel.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationBroker notificationBroker;
    private final JLabel headerLabel;
    private final DefaultListModel<Notification> listModel;
    private final JList<Notification> notificationJList;
    private final JButton markReadButton;
    private final JButton markAllReadButton;
    private final JButton refreshButton;

    public NotificationPanel(NotificationBroker notificationBroker) {
        this.notificationBroker = notificationBroker != null ? notificationBroker : new NotificationBrokerImpl();
        this.listModel = new DefaultListModel<>();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(245, 247, 250));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerLabel = new JLabel("Notifications");
        headerLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionPanel.setOpaque(false);

        markReadButton = new JButton("Mark as Read");
        markReadButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        markReadButton.addActionListener(e -> markSelectedAsRead());

        markAllReadButton = new JButton("Mark All Read");
        markAllReadButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        markAllReadButton.addActionListener(e -> markAllAsRead());

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.addActionListener(e -> loadNotifications());

        actionPanel.add(markReadButton);
        actionPanel.add(markAllReadButton);
        actionPanel.add(refreshButton);
        headerPanel.add(actionPanel, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Notification List
        notificationJList = new JList<>(listModel);
        notificationJList.setCellRenderer(new NotificationCellRenderer());
        notificationJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notificationJList.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(notificationJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 230)));
        add(scrollPane, BorderLayout.CENTER);

        // Initial Load
        loadNotifications();
    }

    public NotificationPanel() {
        this(new NotificationBrokerImpl());
    }

    public void loadNotifications() {
        var currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            headerLabel.setText("Notifications (Not Logged In)");
            listModel.clear();
            return;
        }

        Long userId = currentUser.getId();
        List<Notification> userNotifs = notificationBroker.getNotificationsForUser(userId);
        int unreadCount = notificationBroker.countUnreadForUser(userId);

        listModel.clear();
        for (Notification n : userNotifs) {
            listModel.addElement(n);
        }

        if (unreadCount > 0) {
            headerLabel.setText(String.format("Notifications (🔴 %d Unread)", unreadCount));
        } else {
            headerLabel.setText("Notifications (All Read)");
        }
    }

    private void markSelectedAsRead() {
        Notification selected = notificationJList.getSelectedValue();
        var currentUser = SessionManager.getCurrentUser();
        if (selected != null && currentUser != null) {
            boolean success = notificationBroker.markAsRead(selected.getId(), currentUser.getId());
            if (success) {
                selected.setStatus(NotificationStatus.READ);
                notificationJList.repaint();
                loadNotifications();
            }
        }
    }

    private void markAllAsRead() {
        var currentUser = SessionManager.getCurrentUser();
        if (currentUser != null) {
            boolean success = notificationBroker.markAllAsRead(currentUser.getId());
            if (success) {
                loadNotifications();
            }
        }
    }

    /**
     * Custom List Cell Renderer for Notification display.
     */
    private static class NotificationCellRenderer extends JPanel implements ListCellRenderer<Notification> {
        private final JLabel titleLabel = new JLabel();
        private final JLabel messageLabel = new JLabel();
        private final JLabel metaLabel = new JLabel();

        public NotificationCellRenderer() {
            setLayout(new BorderLayout(6, 4));
            setBorder(new EmptyBorder(8, 10, 8, 10));

            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            metaLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            metaLabel.setForeground(Color.GRAY);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            topPanel.add(titleLabel, BorderLayout.WEST);
            topPanel.add(metaLabel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);
            add(messageLabel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Notification> list,
                                                      Notification notification,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            if (notification == null) return this;

            String priorityIcon = switch (notification.getPriority() != null ? notification.getPriority().toUpperCase() : "MEDIUM") {
                case "CRITICAL" -> "🔴 ";
                case "HIGH" -> "🟠 ";
                case "MEDIUM" -> "🟡 ";
                default -> "🟢 ";
            };

            boolean isUnread = notification.getStatus() != NotificationStatus.READ;
            String unreadPrefix = isUnread ? "[NEW] " : "";

            titleLabel.setText(unreadPrefix + priorityIcon + notification.getTitle());
            messageLabel.setText("<html>" + notification.getMessage() + "</html>");

            String timeStr = notification.getCreatedAt() != null ? notification.getCreatedAt().format(TIME_FORMATTER) : "";
            metaLabel.setText(timeStr + " | " + notification.getChannel());

            if (isSelected) {
                setBackground(new Color(230, 240, 255));
            } else if (isUnread) {
                setBackground(new Color(255, 248, 240));
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }
    }
}
