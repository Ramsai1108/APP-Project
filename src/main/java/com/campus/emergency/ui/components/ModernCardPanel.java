package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ModernCardPanel extends JPanel {
    private Color backgroundColor = Color.WHITE;
    private Color borderColor = UITheme.CARD_BORDER;
    private int cornerRadius = 10;
    private boolean showBorder = true;

    public ModernCardPanel() {
        this(new BorderLayout());
    }

    public ModernCardPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(16, 16, 16, 16));
    }

    public ModernCardPanel(LayoutManager layout, int padding) {
        super(layout);
        setOpaque(false);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public void setShowBorder(boolean show) {
        this.showBorder = show;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        // Fill rounded card background
        g2.setColor(backgroundColor != null ? backgroundColor : Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        // Draw border
        if (showBorder && borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
