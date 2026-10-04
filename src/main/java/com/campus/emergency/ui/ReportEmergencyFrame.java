package com.campus.emergency.ui;

import com.campus.emergency.dao.LocationDAO;
import com.campus.emergency.infrastructure.LoggingService;
import com.campus.emergency.infrastructure.ThreadPoolManager;
import com.campus.emergency.model.EmergencyReport;
import com.campus.emergency.model.Location;
import com.campus.emergency.model.LocationResult;
import com.campus.emergency.model.User;
import com.campus.emergency.security.AuthenticationContext;
import com.campus.emergency.service.EmergencyManager;
import com.campus.emergency.service.LocationService;
import com.campus.emergency.service.ServiceRegistry;
import com.campus.emergency.ui.components.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReportEmergencyFrame extends JFrame {
    private final EmergencyManager emergencyManager;
    private final LocationDAO locationDAO;
    private final LocationService locationService;
    private final Runnable onSuccessCallback;

    private String selectedCategory = "FIRE";
    private final List<CategoryCard> categoryCards = new ArrayList<>();
    private JTextArea descriptionArea;
    private JComboBox<LocationItem> locationComboBox;
    private ModernButton autoDetectBtn;
    private JPanel detectedLocationCard;
    private JLabel detectedAddressLabel;
    private JLabel detectedCoordsLabel;
    private JLabel nearestCampusLocationLabel;
    private JLabel locationStatusBanner;
    private JSpinner peopleAffectedSpinner;
    private JPanel mediaContainerPanel;
    private File attachedMediaFile = null;
    private BufferedImage originalImage = null;
    private ModernButton submitButton;
    private ModernButton cancelButton;

    private final List<Location> cachedCampusLocations = new ArrayList<>();
    private Location lastDetectedNearestLocation = null;
    private LocationResult lastLocationResult = null;

    public static class LocationItem {
        private final Location location;

        public LocationItem(Location location) {
            this.location = location;
        }

        public Location getLocation() { return location; }

        @Override
        public String toString() {
            if (location == null) return "-- Select Campus Location --";
            return location.getBuildingName() + " (Floor " + location.getFloor() + ", Room " + location.getRoomNumber() + ")";
        }
    }

    public ReportEmergencyFrame(EmergencyManager emergencyManager) {
        this(emergencyManager, ServiceRegistry.getInstance().getLocationDAO(), ServiceRegistry.getInstance().getLocationService(), null);
    }

    public ReportEmergencyFrame(EmergencyManager emergencyManager, LocationDAO locationDAO) {
        this(emergencyManager, locationDAO, ServiceRegistry.getInstance().getLocationService(), null);
    }

    public ReportEmergencyFrame(EmergencyManager emergencyManager, LocationDAO locationDAO, Runnable onSuccessCallback) {
        this(emergencyManager, locationDAO, ServiceRegistry.getInstance().getLocationService(), onSuccessCallback);
    }

    public ReportEmergencyFrame(EmergencyManager emergencyManager, LocationDAO locationDAO, LocationService locationService, Runnable onSuccessCallback) {
        this.emergencyManager = (emergencyManager != null) ? emergencyManager : ServiceRegistry.getInstance().getEmergencyManager();
        this.locationDAO = (locationDAO != null) ? locationDAO : ServiceRegistry.getInstance().getLocationDAO();
        this.locationService = (locationService != null) ? locationService : ServiceRegistry.getInstance().getLocationService();
        this.onSuccessCallback = onSuccessCallback;
        initUI();
        loadLocations();
    }

    private void initUI() {
        setTitle("Campus Emergency Response - Report Emergency");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 860);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 14));
        mainPanel.setBackground(UITheme.BG_LIGHT);
        mainPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Report an Emergency");
        titleLabel.setFont(UITheme.FONT_TITLE_LARGE);
        titleLabel.setForeground(UITheme.EMERGENCY_RED);
        headerPanel.add(titleLabel);

        JLabel subtitleLabel = new JLabel("Provide the incident details below for immediate response center dispatch.");
        subtitleLabel.setFont(UITheme.FONT_BODY);
        subtitleLabel.setForeground(UITheme.TEXT_SECONDARY);
        headerPanel.add(subtitleLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Card Panel
        ModernCardPanel card = new ModernCardPanel(new BorderLayout(), 16);
        JPanel formContent = new JPanel();
        formContent.setLayout(new BoxLayout(formContent, BoxLayout.Y_AXIS));
        formContent.setOpaque(false);

        // 1. Category Selection
        JLabel catSectionLabel = new JLabel("1. Select Emergency Type *");
        catSectionLabel.setFont(UITheme.FONT_BODY_BOLD);
        catSectionLabel.setForeground(UITheme.TEXT_PRIMARY);
        catSectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContent.add(catSectionLabel);
        formContent.add(Box.createVerticalStrut(6));

        JPanel categoryGrid = new JPanel(new GridLayout(2, 3, 8, 8));
        categoryGrid.setOpaque(false);
        categoryGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        addCategoryOption(categoryGrid, "FIRE", VectorIcon.Type.FIRE, "Fire / Smoke", "Active flames or smoke");
        addCategoryOption(categoryGrid, "MEDICAL", VectorIcon.Type.MEDICAL, "Medical", "Injury, illness, collapse");
        addCategoryOption(categoryGrid, "HAZARD", VectorIcon.Type.HAZARD, "Hazard / Gas", "Chemical, spill, hazard");
        addCategoryOption(categoryGrid, "SECURITY", VectorIcon.Type.LOCK, "Security", "Theft, intruder, threat");
        addCategoryOption(categoryGrid, "FACILITY", VectorIcon.Type.BUILDING, "Facility", "Power, elevator, leak");
        addCategoryOption(categoryGrid, "OTHER", VectorIcon.Type.HELP, "Other Alert", "General assistance");

        formContent.add(categoryGrid);
        formContent.add(Box.createVerticalStrut(12));

        // 2. Emergency Location
        JLabel locSectionLabel = new JLabel("2. Emergency Location *");
        locSectionLabel.setFont(UITheme.FONT_BODY_BOLD);
        locSectionLabel.setForeground(UITheme.TEXT_PRIMARY);
        locSectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContent.add(locSectionLabel);
        formContent.add(Box.createVerticalStrut(6));

        // Auto-Detect Location Button (Horizontal action row: ~260x44 px)
        autoDetectBtn = new ModernButton("Use My Current Location", VectorIcon.Type.LOCATION_PIN, ModernButton.ButtonStyle.SECONDARY);
        autoDetectBtn.setPreferredSize(new Dimension(260, 44));
        autoDetectBtn.setMaximumSize(new Dimension(270, 44));
        autoDetectBtn.setFont(UITheme.FONT_BODY_BOLD);
        autoDetectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoDetectBtn.addActionListener(e -> autoDetectCurrentLocation());
        formContent.add(autoDetectBtn);
        formContent.add(Box.createVerticalStrut(6));

        // Location status banner (for loading and warning/error states)
        locationStatusBanner = new JLabel();
        locationStatusBanner.setFont(UITheme.FONT_SMALL);
        locationStatusBanner.setOpaque(true);
        locationStatusBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        locationStatusBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        locationStatusBanner.setVisible(false);
        formContent.add(locationStatusBanner);
        formContent.add(Box.createVerticalStrut(6));

        // Detected Location Card (Success state)
        detectedLocationCard = new JPanel(new BorderLayout(0, 6));
        detectedLocationCard.setBackground(new Color(240, 249, 255));
        detectedLocationCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.INFO_CYAN_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        detectedLocationCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        detectedLocationCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));
        detectedLocationCard.setVisible(false);

        JPanel detectedHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        detectedHead.setOpaque(false);
        JLabel pinIconLabel = new JLabel(new VectorIcon(VectorIcon.Type.LOCATION_PIN, 16, UITheme.ACCENT_BLUE));
        JLabel detectedBadge = new JLabel("Current Location (OpenStreetMap)");
        detectedBadge.setFont(UITheme.FONT_BODY_BOLD);
        detectedBadge.setForeground(UITheme.ACCENT_BLUE);
        detectedHead.add(pinIconLabel);
        detectedHead.add(detectedBadge);
        detectedLocationCard.add(detectedHead, BorderLayout.NORTH);

        JPanel detectedBody = new JPanel();
        detectedBody.setLayout(new BoxLayout(detectedBody, BoxLayout.Y_AXIS));
        detectedBody.setOpaque(false);

        detectedAddressLabel = new JLabel("Detected Address");
        detectedAddressLabel.setFont(UITheme.FONT_BODY_BOLD);
        detectedAddressLabel.setForeground(UITheme.TEXT_PRIMARY);
        detectedBody.add(detectedAddressLabel);
        detectedBody.add(Box.createVerticalStrut(2));

        detectedCoordsLabel = new JLabel("Lat: -- | Lon: --");
        detectedCoordsLabel.setFont(UITheme.FONT_META);
        detectedCoordsLabel.setForeground(UITheme.TEXT_SECONDARY);
        detectedBody.add(detectedCoordsLabel);
        detectedBody.add(Box.createVerticalStrut(2));

        nearestCampusLocationLabel = new JLabel("Nearest campus location: --");
        nearestCampusLocationLabel.setFont(UITheme.FONT_SMALL);
        nearestCampusLocationLabel.setForeground(UITheme.SUCCESS_GREEN);
        detectedBody.add(nearestCampusLocationLabel);

        detectedLocationCard.add(detectedBody, BorderLayout.CENTER);

        JPanel detectedActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        detectedActions.setOpaque(false);

        ModernButton useThisLocBtn = new ModernButton("Use This Location", VectorIcon.Type.CHECK, ModernButton.ButtonStyle.PRIMARY);
        useThisLocBtn.setPreferredSize(new Dimension(150, 30));
        useThisLocBtn.setFont(UITheme.FONT_SMALL);
        useThisLocBtn.addActionListener(e -> applyDetectedLocation());
        detectedActions.add(useThisLocBtn);

        ModernButton openOsmBtn = new ModernButton("Open in OpenStreetMap", VectorIcon.Type.EYE, ModernButton.ButtonStyle.SECONDARY);
        openOsmBtn.setPreferredSize(new Dimension(175, 30));
        openOsmBtn.setFont(UITheme.FONT_SMALL);
        openOsmBtn.addActionListener(e -> {
            if (lastLocationResult != null && lastLocationResult.isSuccess()) {
                openOpenStreetMapUrl(lastLocationResult.getLatitude(), lastLocationResult.getLongitude());
            }
        });
        detectedActions.add(openOsmBtn);

        detectedLocationCard.add(detectedActions, BorderLayout.SOUTH);
        formContent.add(detectedLocationCard);
        formContent.add(Box.createVerticalStrut(6));

        // Manual Campus Location Dropdown
        JLabel manualLocLabel = new JLabel("Campus Location:");
        manualLocLabel.setFont(UITheme.FONT_META);
        manualLocLabel.setForeground(UITheme.TEXT_SECONDARY);
        manualLocLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContent.add(manualLocLabel);
        formContent.add(Box.createVerticalStrut(4));

        locationComboBox = new JComboBox<>();
        locationComboBox.setFont(UITheme.FONT_BODY);
        locationComboBox.setBackground(Color.WHITE);
        locationComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        locationComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        formContent.add(locationComboBox);
        formContent.add(Box.createVerticalStrut(14));

        // 3. People Affected & Optional Media Row
        JPanel twoColRow = new JPanel(new GridLayout(1, 2, 14, 0));
        twoColRow.setOpaque(false);
        twoColRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // People affected
        JPanel peopleBox = new JPanel(new BorderLayout(0, 4));
        peopleBox.setOpaque(false);
        JLabel peopleLabel = new JLabel("3. Estimated People Affected");
        peopleLabel.setFont(UITheme.FONT_BODY_BOLD);
        peopleLabel.setForeground(UITheme.TEXT_PRIMARY);
        peopleBox.add(peopleLabel, BorderLayout.NORTH);

        peopleAffectedSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 500, 1));
        peopleAffectedSpinner.setFont(UITheme.FONT_BODY);
        peopleBox.add(peopleAffectedSpinner, BorderLayout.CENTER);
        twoColRow.add(peopleBox);

        // Media attachment (Optional)
        JPanel mediaBox = new JPanel(new BorderLayout(0, 4));
        mediaBox.setOpaque(false);
        JLabel mediaLabel = new JLabel("4. Media Evidence (Optional)");
        mediaLabel.setFont(UITheme.FONT_BODY_BOLD);
        mediaLabel.setForeground(UITheme.TEXT_PRIMARY);
        mediaBox.add(mediaLabel, BorderLayout.NORTH);

        mediaContainerPanel = new JPanel(new BorderLayout());
        mediaContainerPanel.setOpaque(false);
        renderDefaultMediaAttachUI();
        mediaBox.add(mediaContainerPanel, BorderLayout.CENTER);

        twoColRow.add(mediaBox);
        formContent.add(twoColRow);
        formContent.add(Box.createVerticalStrut(12));

        // 4. Description
        JLabel descLabel = new JLabel("5. Situation Description *");
        descLabel.setFont(UITheme.FONT_BODY_BOLD);
        descLabel.setForeground(UITheme.TEXT_PRIMARY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContent.add(descLabel);
        formContent.add(Box.createVerticalStrut(4));

        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setFont(UITheme.FONT_BODY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(new EmptyBorder(6, 8, 6, 8));

        JScrollPane descScroll = new JScrollPane(descriptionArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER));
        descScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        descScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        formContent.add(descScroll);
        formContent.add(Box.createVerticalStrut(4));

        JLabel descHelper = new JLabel("Describe what happened, where it happened, and any immediate danger.");
        descHelper.setFont(UITheme.FONT_META);
        descHelper.setForeground(UITheme.TEXT_MUTED);
        descHelper.setAlignmentX(Component.LEFT_ALIGNMENT);
        formContent.add(descHelper);
        formContent.add(Box.createVerticalStrut(10));

        // Notice banner
        JPanel noticePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        noticePanel.setBackground(UITheme.INFO_CYAN_BG);
        noticePanel.setBorder(BorderFactory.createLineBorder(UITheme.INFO_CYAN_BORDER, 1));
        noticePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel noticeIcon = new JLabel(new VectorIcon(VectorIcon.Type.INFO, 16, UITheme.INFO_CYAN));
        JLabel noticeText = new JLabel("Your emergency report will be dispatched to campus responders immediately.");
        noticeText.setFont(UITheme.FONT_SMALL);
        noticeText.setForeground(UITheme.TEXT_PRIMARY);

        noticePanel.add(noticeIcon);
        noticePanel.add(noticeText);
        formContent.add(noticePanel);
        formContent.add(Box.createVerticalGlue());

        card.add(formContent, BorderLayout.CENTER);
        mainPanel.add(card, BorderLayout.CENTER);

        // Bottom action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        cancelButton = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.addActionListener(e -> dispose());

        submitButton = new ModernButton("SUBMIT EMERGENCY", VectorIcon.Type.ALERT, ModernButton.ButtonStyle.DANGER);
        submitButton.setPreferredSize(new Dimension(210, 40));
        submitButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitButton.addActionListener(e -> submitReport());

        btnPanel.add(cancelButton);
        btnPanel.add(submitButton);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        getRootPane().setDefaultButton(submitButton);
    }

    private void autoDetectCurrentLocation() {
        autoDetectBtn.setText("Detecting Location...");
        autoDetectBtn.setButtonIcon(new VectorIcon(VectorIcon.Type.REFRESH, 16, UITheme.TEXT_SECONDARY));
        autoDetectBtn.setEnabled(false);

        detectedLocationCard.setVisible(false);

        locationStatusBanner.setText(" Detecting your location...");
        locationStatusBanner.setIcon(new VectorIcon(VectorIcon.Type.REFRESH, 16, UITheme.ACCENT_BLUE));
        locationStatusBanner.setBackground(UITheme.INFO_CYAN_BG);
        locationStatusBanner.setForeground(UITheme.ACCENT_BLUE);
        locationStatusBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.INFO_CYAN_BORDER, 1),
                new EmptyBorder(6, 10, 6, 10)
        ));
        locationStatusBanner.setVisible(true);

        ThreadPoolManager.getInstance().submit(() -> {
            LocationResult result = locationService.detectCurrentLocation();

            SwingUtilities.invokeLater(() -> {
                autoDetectBtn.setText("Use My Current Location");
                autoDetectBtn.setButtonIcon(new VectorIcon(VectorIcon.Type.LOCATION_PIN, 16, UITheme.TEXT_PRIMARY));
                autoDetectBtn.setEnabled(true);
                lastLocationResult = result;

                if (result != null && result.isSuccess()) {
                    locationStatusBanner.setVisible(false);
                    String displayName = result.getDisplayName();
                    double lat = result.getLatitude();
                    double lon = result.getLongitude();
                    Double acc = result.getAccuracy();

                    detectedAddressLabel.setText(displayName != null ? displayName : "Location Detected");
                    detectedCoordsLabel.setText(String.format(Locale.US, "Lat: %.6f | Lon: %.6f  •  Accuracy: ±%.0fm",
                            lat, lon, acc != null ? acc : 15.0));

                    // Find nearest campus location from database
                    lastDetectedNearestLocation = locationService.findNearestCampusLocation(lat, lon, cachedCampusLocations);
                    if (lastDetectedNearestLocation != null) {
                        nearestCampusLocationLabel.setText("Nearest campus location: "
                                + lastDetectedNearestLocation.getBuildingName()
                                + " (Floor " + lastDetectedNearestLocation.getFloor()
                                + ", Room " + lastDetectedNearestLocation.getRoomNumber() + ")");
                        nearestCampusLocationLabel.setVisible(true);
                    } else {
                        nearestCampusLocationLabel.setVisible(false);
                    }

                    detectedLocationCard.setVisible(true);

                    // Auto-select nearest location in dropdown if matched
                    applyDetectedLocation();

                } else {
                    detectedLocationCard.setVisible(false);
                    locationStatusBanner.setText("Location unavailable. Select your campus location manually.");
                    locationStatusBanner.setIcon(new VectorIcon(VectorIcon.Type.ALERT, 16, UITheme.WARNING_ORANGE));
                    locationStatusBanner.setBackground(new Color(254, 243, 199));
                    locationStatusBanner.setForeground(new Color(180, 83, 9));
                    locationStatusBanner.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(251, 191, 36), 1),
                            new EmptyBorder(6, 10, 6, 10)
                    ));
                    locationStatusBanner.setVisible(true);
                }
            });
        });
    }

    private void applyDetectedLocation() {
        if (lastDetectedNearestLocation != null) {
            for (int i = 0; i < locationComboBox.getItemCount(); i++) {
                LocationItem item = locationComboBox.getItemAt(i);
                if (item != null && item.getLocation() != null && item.getLocation().getId() == lastDetectedNearestLocation.getId()) {
                    locationComboBox.setSelectedIndex(i);
                    break;
                }
            }
        } else if (lastLocationResult != null && lastLocationResult.isSuccess()) {
            Location dynamicLoc = new Location(
                    1,
                    lastLocationResult.getDisplayName(),
                    "Ground",
                    "GPS",
                    lastLocationResult.getLatitude(),
                    lastLocationResult.getLongitude(),
                    "Auto-detected via OpenStreetMap Nominatim"
            );
            LocationItem item = new LocationItem(dynamicLoc);
            locationComboBox.addItem(item);
            locationComboBox.setSelectedItem(item);
        }
    }

    private void openOpenStreetMapUrl(double lat, double lon) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                String url = String.format(Locale.US, "https://www.openstreetmap.org/?mlat=%.6f&mlon=%.6f#map=17/%.6f/%.6f",
                        lat, lon, lat, lon);
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            LoggingService.warn("Failed to open OpenStreetMap in default browser: " + e.getMessage());
        }
    }

    private void renderDefaultMediaAttachUI() {
        mediaContainerPanel.removeAll();

        JPanel defaultRow = new JPanel(new BorderLayout(8, 0));
        defaultRow.setOpaque(false);

        ModernButton attachBtn = new ModernButton("Attach Photo/Video", VectorIcon.Type.EYE, ModernButton.ButtonStyle.SECONDARY);
        attachBtn.setPreferredSize(new Dimension(150, 36));
        attachBtn.addActionListener(e -> handleMediaAttach());
        defaultRow.add(attachBtn, BorderLayout.WEST);

        JLabel mediaAttachedLabel = new JLabel("No file attached");
        mediaAttachedLabel.setFont(UITheme.FONT_META);
        mediaAttachedLabel.setForeground(UITheme.TEXT_MUTED);
        defaultRow.add(mediaAttachedLabel, BorderLayout.CENTER);

        mediaContainerPanel.add(defaultRow, BorderLayout.CENTER);
        mediaContainerPanel.revalidate();
        mediaContainerPanel.repaint();
    }

    private void renderImageThumbnailUI(File file, BufferedImage image) {
        mediaContainerPanel.removeAll();

        JPanel previewCard = new JPanel(new BorderLayout(10, 0));
        previewCard.setBackground(Color.WHITE);
        previewCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        // Generate scaled thumbnail preserving aspect ratio
        int origW = image.getWidth();
        int origH = image.getHeight();
        int targetH = 75;
        int targetW = (int) Math.round((double) origW * targetH / (double) origH);
        if (targetW > 105) {
            targetW = 105;
            targetH = (int) Math.round((double) origH * targetW / (double) origW);
        }
        Image scaled = image.getScaledInstance(Math.max(1, targetW), Math.max(1, targetH), Image.SCALE_SMOOTH);
        JLabel thumbLabel = new JLabel(new ImageIcon(scaled));
        thumbLabel.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));
        thumbLabel.setPreferredSize(new Dimension(targetW, targetH));
        previewCard.add(thumbLabel, BorderLayout.WEST);

        // Details and Action Buttons
        JPanel detailsPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        detailsPanel.setOpaque(false);

        String fileName = file.getName();
        String displayFileName = fileName.length() > 22 ? fileName.substring(0, 19) + "..." : fileName;
        JLabel nameLabel = new JLabel(displayFileName);
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);
        nameLabel.setToolTipText(fileName);
        detailsPanel.add(nameLabel);

        JPanel actionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionsRow.setOpaque(false);

        ModernButton viewBtn = new ModernButton("View", VectorIcon.Type.EYE, ModernButton.ButtonStyle.PRIMARY);
        viewBtn.setPreferredSize(new Dimension(75, 28));
        viewBtn.setFont(UITheme.FONT_SMALL);
        viewBtn.addActionListener(e -> showEnlargedImageModal());
        actionsRow.add(viewBtn);

        ModernButton removeBtn = new ModernButton("Remove", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        removeBtn.setPreferredSize(new Dimension(85, 28));
        removeBtn.setFont(UITheme.FONT_SMALL);
        removeBtn.addActionListener(e -> removeMediaAttachment());
        actionsRow.add(removeBtn);

        detailsPanel.add(actionsRow);
        previewCard.add(detailsPanel, BorderLayout.CENTER);

        mediaContainerPanel.add(previewCard, BorderLayout.CENTER);
        mediaContainerPanel.revalidate();
        mediaContainerPanel.repaint();
    }

    private void renderVideoAttachmentUI(File file) {
        mediaContainerPanel.removeAll();

        JPanel videoCard = new JPanel(new BorderLayout(10, 0));
        videoCard.setBackground(Color.WHITE);
        videoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JLabel iconLabel = new JLabel(new VectorIcon(VectorIcon.Type.EYE, 22, UITheme.ACCENT_BLUE), SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(48, 48));
        videoCard.add(iconLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        infoPanel.setOpaque(false);

        String fileName = file.getName();
        String displayFileName = fileName.length() > 20 ? fileName.substring(0, 17) + "..." : fileName;
        JLabel nameLabel = new JLabel(displayFileName);
        nameLabel.setFont(UITheme.FONT_BODY_BOLD);
        nameLabel.setForeground(UITheme.TEXT_PRIMARY);
        nameLabel.setToolTipText(fileName);
        infoPanel.add(nameLabel);

        JPanel subRow = new JPanel(new BorderLayout());
        subRow.setOpaque(false);
        JLabel subLabel = new JLabel("Video attached");
        subLabel.setFont(UITheme.FONT_META);
        subLabel.setForeground(UITheme.SUCCESS_GREEN);
        subRow.add(subLabel, BorderLayout.WEST);

        ModernButton removeBtn = new ModernButton("Remove", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        removeBtn.setPreferredSize(new Dimension(80, 26));
        removeBtn.setFont(UITheme.FONT_SMALL);
        removeBtn.addActionListener(e -> removeMediaAttachment());
        subRow.add(removeBtn, BorderLayout.EAST);

        infoPanel.add(subRow);
        videoCard.add(infoPanel, BorderLayout.CENTER);

        mediaContainerPanel.add(videoCard, BorderLayout.CENTER);
        mediaContainerPanel.revalidate();
        mediaContainerPanel.repaint();
    }

    private void renderImageErrorUI(File file) {
        mediaContainerPanel.removeAll();

        JPanel errCard = new JPanel(new BorderLayout(8, 0));
        errCard.setBackground(Color.WHITE);
        errCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.WARNING_ORANGE, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        JLabel errLbl = new JLabel("Unable to preview this image. The file can still be attached.");
        errLbl.setFont(UITheme.FONT_SMALL);
        errLbl.setForeground(UITheme.WARNING_ORANGE);
        textPanel.add(errLbl);

        JLabel nameLbl = new JLabel(file.getName());
        nameLbl.setFont(UITheme.FONT_META);
        nameLbl.setForeground(UITheme.TEXT_SECONDARY);
        textPanel.add(nameLbl);

        errCard.add(textPanel, BorderLayout.CENTER);

        ModernButton removeBtn = new ModernButton("Remove", VectorIcon.Type.RESET, ModernButton.ButtonStyle.SECONDARY);
        removeBtn.setPreferredSize(new Dimension(80, 28));
        removeBtn.setFont(UITheme.FONT_SMALL);
        removeBtn.addActionListener(e -> removeMediaAttachment());
        errCard.add(removeBtn, BorderLayout.EAST);

        mediaContainerPanel.add(errCard, BorderLayout.CENTER);
        mediaContainerPanel.revalidate();
        mediaContainerPanel.repaint();
    }

    private void handleMediaAttach() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Photo or Video Evidence");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null && selected.exists()) {
                attachedMediaFile = selected;
                originalImage = null;

                String lower = selected.getName().toLowerCase();
                boolean isVideo = lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")
                        || lower.endsWith(".mkv") || lower.endsWith(".wmv") || lower.endsWith(".flv");

                if (isVideo) {
                    renderVideoAttachmentUI(selected);
                } else {
                    try {
                        BufferedImage img = ImageIO.read(selected);
                        if (img != null) {
                            originalImage = img;
                            renderImageThumbnailUI(selected, img);
                        } else {
                            renderImageErrorUI(selected);
                        }
                    } catch (Exception ex) {
                        LoggingService.warn("Failed to generate preview for image: " + ex.getMessage());
                        renderImageErrorUI(selected);
                    }
                }
            }
        }
    }

    private void showEnlargedImageModal() {
        if (originalImage == null) return;

        JDialog viewModal = new JDialog(this, "Media Evidence Preview", true);
        viewModal.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Header
        JPanel headPanel = new JPanel(new BorderLayout());
        headPanel.setOpaque(false);

        String title = attachedMediaFile != null ? attachedMediaFile.getName() : "Attached Image";
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        headPanel.add(titleLbl, BorderLayout.WEST);

        JLabel dimLbl = new JLabel(originalImage.getWidth() + " x " + originalImage.getHeight() + " px");
        dimLbl.setFont(UITheme.FONT_META);
        dimLbl.setForeground(UITheme.TEXT_MUTED);
        headPanel.add(dimLbl, BorderLayout.EAST);

        root.add(headPanel, BorderLayout.NORTH);

        // Scaled image preview to fit comfortably
        int origW = originalImage.getWidth();
        int origH = originalImage.getHeight();
        int maxW = 580;
        int maxH = 400;

        double scale = Math.min((double) maxW / origW, (double) maxH / origH);
        if (scale > 1.0) scale = 1.0;

        int fitW = Math.max(1, (int) Math.round(origW * scale));
        int fitH = Math.max(1, (int) Math.round(origH * scale));

        Image scaled = originalImage.getScaledInstance(fitW, fitH, Image.SCALE_SMOOTH);
        JLabel imgLabel = new JLabel(new ImageIcon(scaled));
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel imgWrap = new JPanel(new GridBagLayout());
        imgWrap.setBackground(UITheme.BG_LIGHT);
        imgWrap.setBorder(BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1));
        imgWrap.setPreferredSize(new Dimension(maxW + 16, maxH + 16));
        imgWrap.add(imgLabel);
        root.add(imgWrap, BorderLayout.CENTER);

        // Footer Actions
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        ModernButton closeBtn = new ModernButton("Close", ModernButton.ButtonStyle.PRIMARY);
        closeBtn.setPreferredSize(new Dimension(95, 36));
        closeBtn.addActionListener(e -> viewModal.dispose());
        btnPanel.add(closeBtn);
        root.add(btnPanel, BorderLayout.SOUTH);

        viewModal.setContentPane(root);
        viewModal.pack();
        viewModal.setLocationRelativeTo(this);
        viewModal.setVisible(true);
    }

    private void removeMediaAttachment() {
        attachedMediaFile = null;
        originalImage = null;
        renderDefaultMediaAttachUI();
    }

    private void addCategoryOption(JPanel parent, String code, VectorIcon.Type iconType, String title, String desc) {
        CategoryCard cCard = new CategoryCard(code, iconType, title, desc);
        if (code.equals(selectedCategory)) {
            cCard.setSelected(true);
        }
        cCard.setOnSelectListener(() -> {
            selectedCategory = code;
            for (CategoryCard c : categoryCards) {
                c.setSelected(c.getCategoryCode().equals(code));
            }
        });
        categoryCards.add(cCard);
        parent.add(cCard);
    }

    private void loadLocations() {
        ThreadPoolManager.getInstance().submit(() -> {
            try {
                List<Location> locations = locationDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    cachedCampusLocations.clear();
                    cachedCampusLocations.addAll(locations);

                    locationComboBox.removeAllItems();
                    locationComboBox.addItem(new LocationItem(null));
                    for (Location loc : locations) {
                        locationComboBox.addItem(new LocationItem(loc));
                    }
                    if (locationComboBox.getItemCount() > 1) {
                        locationComboBox.setSelectedIndex(1);
                    }
                });
            } catch (Exception e) {
                LoggingService.error("Failed to load locations: " + e.getMessage(), e);
            }
        });
    }

    public void submitReport() {
        User currentUser = AuthenticationContext.getCurrentUser();
        if (currentUser == null) {
            ModernDialog.showError(this, "Authentication Required", "You must be logged in to submit an emergency report.");
            return;
        }

        String category = selectedCategory;
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        LocationItem selectedLocationItem = (LocationItem) locationComboBox.getSelectedItem();

        if (selectedLocationItem == null || selectedLocationItem.getLocation() == null) {
            ModernDialog.showWarning(this, "Missing Location", "Please select an emergency location.");
            return;
        }

        int locationId = selectedLocationItem.getLocation().getId();
        int peopleAffected = (Integer) peopleAffectedSpinner.getValue();

        if (description.length() < 5) {
            ModernDialog.showWarning(this, "Missing Description", "Please provide a detailed situation description (at least 5 characters).");
            return;
        }

        submitButton.setEnabled(false);
        cancelButton.setEnabled(false);

        String photoPath = null;
        String videoPath = null;
        if (attachedMediaFile != null) {
            String lower = attachedMediaFile.getName().toLowerCase();
            if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")
                    || lower.endsWith(".mkv") || lower.endsWith(".wmv") || lower.endsWith(".flv")) {
                videoPath = attachedMediaFile.getAbsolutePath();
            } else {
                photoPath = attachedMediaFile.getAbsolutePath();
            }
        }

        final String finalPhoto = photoPath;
        final String finalVideo = videoPath;

        ThreadPoolManager.getInstance().submit(() -> {
            try {
                EmergencyReport report = emergencyManager.createReport(
                        currentUser, category, description, locationId, peopleAffected, finalPhoto, finalVideo
                );

                SwingUtilities.invokeLater(() -> showStructuredConfirmationModal(report));
            } catch (Exception ex) {
                LoggingService.error("Failed to submit emergency report: " + ex.getMessage(), ex);
                SwingUtilities.invokeLater(() -> {
                    submitButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    ModernDialog.showError(this, "Submission Error", "Failed to submit report: " + ex.getMessage());
                });
            }
        });
    }

    private void showStructuredConfirmationModal(EmergencyReport report) {
        JDialog confirmDialog = new JDialog(this, "Emergency Report Submitted", true);
        confirmDialog.setSize(460, 320);
        confirmDialog.setLocationRelativeTo(this);
        confirmDialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headPanel.setOpaque(false);
        JLabel checkIcon = new JLabel(new VectorIcon(VectorIcon.Type.CHECK, 22, UITheme.SUCCESS_GREEN));
        headPanel.add(checkIcon);

        JLabel titleLbl = new JLabel("EMERGENCY REPORT SUBMITTED");
        titleLbl.setFont(UITheme.FONT_TITLE);
        titleLbl.setForeground(UITheme.TEXT_PRIMARY);
        headPanel.add(titleLbl);
        root.add(headPanel, BorderLayout.NORTH);

        // Body Details Card
        ModernCardPanel card = new ModernCardPanel(new GridLayout(3, 2, 8, 8), 12);
        card.add(createFieldLabel("Report ID:"));
        card.add(createFieldValue("#" + report.getId()));

        card.add(createFieldLabel("Category:"));
        card.add(createFieldValue(report.getCategory()));

        card.add(createFieldLabel("Current Status:"));
        card.add(createFieldValue(report.getStatus()));

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(card, BorderLayout.NORTH);

        JLabel noteLbl = new JLabel("<html>Your report has been sent to the campus response center.<br>Responders have been notified.</html>");
        noteLbl.setFont(UITheme.FONT_BODY);
        noteLbl.setForeground(UITheme.TEXT_SECONDARY);
        centerPanel.add(noteLbl, BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);

        // Buttons: [ View Incident Status ] and [ Dismiss ]
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        ModernButton dismissBtn = new ModernButton("Dismiss", ModernButton.ButtonStyle.SECONDARY);
        dismissBtn.setPreferredSize(new Dimension(95, 36));
        dismissBtn.addActionListener(e -> {
            confirmDialog.dispose();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        });
        btnPanel.add(dismissBtn);

        ModernButton viewStatusBtn = new ModernButton("View Incident Status", VectorIcon.Type.EYE, ModernButton.ButtonStyle.PRIMARY);
        viewStatusBtn.setPreferredSize(new Dimension(170, 36));
        viewStatusBtn.addActionListener(e -> {
            confirmDialog.dispose();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        });
        btnPanel.add(viewStatusBtn);

        root.add(btnPanel, BorderLayout.SOUTH);

        confirmDialog.add(root);
        confirmDialog.setVisible(true);
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY_BOLD);
        l.setForeground(UITheme.TEXT_SECONDARY);
        return l;
    }

    private JLabel createFieldValue(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BODY);
        l.setForeground(UITheme.TEXT_PRIMARY);
        return l;
    }
}
