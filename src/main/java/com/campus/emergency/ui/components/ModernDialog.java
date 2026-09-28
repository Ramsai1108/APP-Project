package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ModernDialog extends JDialog {

    public enum DialogType {
        SUCCESS, ERROR, WARNING, INFO
    }

    public ModernDialog(Window owner, String title, String heading, String message, DialogType type) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        initUI(heading, message, type);
    }

    private void initUI(String heading, String message, DialogType type) {
        setUndecorated(true);
        setSize(420, 220);
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.CARD_BORDER, 1),
                new EmptyBorder(20, 24, 20, 24)
        ));

        // Top Icon and Heading
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topPanel.setOpaque(false);

        VectorIcon.Type iconType;
        Color iconBgColor;
        switch (type) {
            case SUCCESS:
                iconType = VectorIcon.Type.CHECK;
                iconBgColor = UITheme.SUCCESS_GREEN;
                break;
            case ERROR:
                iconType = VectorIcon.Type.ALERT;
                iconBgColor = UITheme.EMERGENCY_RED;
                break;
            case WARNING:
                iconType = VectorIcon.Type.ALERT;
                iconBgColor = UITheme.WARNING_ORANGE;
                break;
            case INFO:
            default:
                iconType = VectorIcon.Type.INFO;
                iconBgColor = UITheme.ACCENT_BLUE;
                break;
        }

        JLabel iconLabel = new JLabel(new VectorIcon(iconType, 18, Color.WHITE), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.applyQualityRenderingHints(g2);
                g2.setColor(iconBgColor);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLabel.setPreferredSize(new Dimension(36, 36));
        topPanel.add(iconLabel);

        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(UITheme.FONT_TITLE);
        headingLabel.setForeground(UITheme.TEXT_PRIMARY);
        topPanel.add(headingLabel);

        root.add(topPanel, BorderLayout.NORTH);

        // Body Message
        JTextArea msgArea = new JTextArea(message);
        msgArea.setFont(UITheme.FONT_BODY);
        msgArea.setForeground(UITheme.TEXT_SECONDARY);
        msgArea.setEditable(false);
        msgArea.setLineWrap(true);
        msgArea.setWrapStyleWord(true);
        msgArea.setOpaque(false);
        msgArea.setBorder(new EmptyBorder(12, 8, 12, 8));
        root.add(msgArea, BorderLayout.CENTER);

        // Action Button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);

        ModernButton okBtn = new ModernButton("Dismiss", ModernButton.ButtonStyle.PRIMARY);
        okBtn.setPreferredSize(new Dimension(100, 36));
        okBtn.addActionListener(e -> dispose());
        btnPanel.add(okBtn);

        root.add(btnPanel, BorderLayout.SOUTH);

        add(root);
    }

    public static void showSuccess(Window owner, String heading, String message) {
        ModernDialog dialog = new ModernDialog(owner, "Success", heading, message, DialogType.SUCCESS);
        dialog.setVisible(true);
    }

    public static void showError(Window owner, String heading, String message) {
        ModernDialog dialog = new ModernDialog(owner, "Error", heading, message, DialogType.ERROR);
        dialog.setVisible(true);
    }

    public static void showWarning(Window owner, String heading, String message) {
        ModernDialog dialog = new ModernDialog(owner, "Warning", heading, message, DialogType.WARNING);
        dialog.setVisible(true);
    }

    public static void showInfo(Window owner, String heading, String message) {
        ModernDialog dialog = new ModernDialog(owner, "Information", heading, message, DialogType.INFO);
        dialog.setVisible(true);
    }
}
