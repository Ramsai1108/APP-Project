package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MetricCard extends JPanel {
    private final JLabel iconLabel;
    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;
    private Color accentColor = UITheme.ACCENT_BLUE;

    public MetricCard(String title, String value, Color accentColor) {
        this((VectorIcon.Type) null, title, value, "", accentColor);
    }

    public MetricCard(VectorIcon.Type iconType, String title, String value, Color accentColor) {
        this(iconType, title, value, "", accentColor);
    }

    public MetricCard(VectorIcon.Type iconType, String title, String value, String subtitle, Color accentColor) {
        this.accentColor = accentColor != null ? accentColor : UITheme.ACCENT_BLUE;
        setLayout(new BorderLayout(8, 6));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel topRow = new JPanel(new BorderLayout(6, 0));
        topRow.setOpaque(false);

        titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(UITheme.FONT_KPI_LABEL);
        titleLabel.setForeground(UITheme.TEXT_SECONDARY);
        topRow.add(titleLabel, BorderLayout.WEST);

        iconLabel = new JLabel();
        if (iconType != null) {
            iconLabel.setIcon(new VectorIcon(iconType, 16, this.accentColor));
        }
        topRow.add(iconLabel, BorderLayout.EAST);

        add(topRow, BorderLayout.NORTH);

        JPanel centerRow = new JPanel(new BorderLayout(0, 2));
        centerRow.setOpaque(false);

        valueLabel = new JLabel(value);
        valueLabel.setFont(UITheme.FONT_KPI_VALUE);
        valueLabel.setForeground(UITheme.TEXT_PRIMARY);
        centerRow.add(valueLabel, BorderLayout.CENTER);

        subtitleLabel = new JLabel(subtitle != null ? subtitle : "");
        subtitleLabel.setFont(UITheme.FONT_META);
        subtitleLabel.setForeground(UITheme.TEXT_MUTED);
        if (subtitle != null && !subtitle.isEmpty()) {
            centerRow.add(subtitleLabel, BorderLayout.SOUTH);
        }

        add(centerRow, BorderLayout.CENTER);
    }

    // Overload for backward compatibility with old String icon calls, mapping them safely to VectorIcons
    public MetricCard(String iconStr, String title, String value, Color accentColor) {
        this(mapStringToVectorType(iconStr), title, value, "", accentColor);
    }

    public MetricCard(String iconStr, String title, String value, String subtitle, Color accentColor) {
        this(mapStringToVectorType(iconStr), title, value, subtitle, accentColor);
    }

    private static VectorIcon.Type mapStringToVectorType(String str) {
        if (str == null) return null;
        String s = str.toUpperCase().trim();
        if (s.contains("ALERT") || s.contains("CRITICAL") || s.contains("EMERGENCY")) return VectorIcon.Type.ALERT;
        if (s.contains("ACTIVITY") || s.contains("ACTIVE") || s.contains("QUEUE")) return VectorIcon.Type.ACTIVITY;
        if (s.contains("CHECK") || s.contains("RESOLVED")) return VectorIcon.Type.CHECK;
        if (s.contains("LIST") || s.contains("TOTAL") || s.contains("REPORT")) return VectorIcon.Type.LIST;
        if (s.contains("USER") || s.contains("RESPONDER")) return VectorIcon.Type.USER;
        if (s.contains("MEDICAL") || s.contains("RESOURCE") || s.contains("AMBULANCE")) return VectorIcon.Type.BUILDING;
        if (s.contains("FIRE")) return VectorIcon.Type.FIRE;
        return VectorIcon.Type.ACTIVITY;
    }

    public void setValue(String value) {
        valueLabel.setText(value);
        repaint();
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        repaint();
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        int arc = 10;
        // Background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        // Top Accent Stripe
        g2.setColor(accentColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, 4, arc, arc);
        g2.fillRect(0, 2, getWidth() - 1, 3);

        // Subtle Border
        g2.setColor(UITheme.CARD_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}

