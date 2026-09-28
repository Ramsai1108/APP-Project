package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleBarChart extends JPanel {
    private final String chartTitle;
    private final Map<String, Integer> data = new LinkedHashMap<>();
    private final Map<String, Color> barColors = new LinkedHashMap<>();

    public SimpleBarChart(String chartTitle) {
        this.chartTitle = chartTitle;
        setOpaque(false);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setPreferredSize(new Dimension(getPreferredSize().width, 210));
    }

    public void addBar(String label, int value, Color color) {
        data.put(label, value);
        barColors.put(label, color != null ? color : UITheme.ACCENT_BLUE);
        repaint();
    }

    public void clearData() {
        data.clear();
        barColors.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        int width = getWidth();
        int height = getHeight();

        // Card Background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, width - 1, height - 1, 10, 10);
        g2.setColor(UITheme.CARD_BORDER);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);

        // Title
        g2.setFont(UITheme.FONT_SUBTITLE);
        g2.setColor(UITheme.TEXT_PRIMARY);
        g2.drawString(chartTitle, 16, 24);

        if (data.isEmpty()) {
            g2.setFont(UITheme.FONT_BODY);
            g2.setColor(UITheme.TEXT_MUTED);
            g2.drawString("No data recorded", 16, height / 2);
            g2.dispose();
            return;
        }

        int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        if (maxVal == 0) maxVal = 1;

        int startY = 44;
        int barHeight = 18;
        int gap = 12;
        int labelWidth = 110;
        int rightValueSpace = 55;
        int maxBarWidth = Math.max(60, width - labelWidth - rightValueSpace - 32);

        int currentY = startY;
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String label = entry.getKey();
            int val = entry.getValue();
            Color color = barColors.getOrDefault(label, UITheme.ACCENT_BLUE);

            // Draw label
            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.setColor(UITheme.TEXT_SECONDARY);
            g2.drawString(label, 16, currentY + 14);

            // Draw bar background track
            int barX = 16 + labelWidth;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(barX, currentY, maxBarWidth, barHeight, 4, 4);

            // Draw filled bar
            int filledWidth = (int) (((double) val / maxVal) * maxBarWidth);
            if (val > 0 && filledWidth < 6) filledWidth = 6;

            if (filledWidth > 0) {
                g2.setColor(color);
                g2.fillRoundRect(barX, currentY, filledWidth, barHeight, 4, 4);
            }

            // Draw value
            g2.setColor(UITheme.TEXT_PRIMARY);
            g2.setFont(UITheme.FONT_BODY_BOLD);
            g2.drawString(String.valueOf(val), barX + maxBarWidth + 10, currentY + 14);

            currentY += barHeight + gap;
        }

        g2.dispose();
    }
}

