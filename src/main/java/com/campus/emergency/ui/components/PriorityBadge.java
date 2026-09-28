package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PriorityBadge extends JLabel {
    private Color bgColor = UITheme.INFO_CYAN_BG;
    private Color fgColor = UITheme.INFO_CYAN;
    private Color borderColor = UITheme.INFO_CYAN_BORDER;

    public PriorityBadge(String priority) {
        super(priority != null ? priority.toUpperCase() : "NORMAL", SwingConstants.CENTER);
        init();
        setPriority(priority);
    }

    private void init() {
        setFont(UITheme.FONT_BADGE);
        setOpaque(false);
        setBorder(new EmptyBorder(4, 10, 4, 10));
    }

    public void setPriority(String priority) {
        String pr = priority != null ? priority.toUpperCase().trim() : "NORMAL";
        setText(pr);

        switch (pr) {
            case "CRITICAL":
                bgColor = UITheme.EMERGENCY_RED_BG;
                fgColor = UITheme.EMERGENCY_RED;
                borderColor = UITheme.EMERGENCY_RED_BORDER;
                break;
            case "HIGH":
                bgColor = UITheme.WARNING_ORANGE_BG;
                fgColor = UITheme.WARNING_ORANGE;
                borderColor = UITheme.WARNING_ORANGE_BORDER;
                break;
            case "MEDIUM":
                bgColor = UITheme.AMBER_YELLOW_BG;
                fgColor = UITheme.AMBER_YELLOW;
                borderColor = UITheme.AMBER_YELLOW_BORDER;
                break;
            case "LOW":
            default:
                bgColor = UITheme.SUCCESS_GREEN_BG;
                fgColor = UITheme.SUCCESS_GREEN;
                borderColor = UITheme.SUCCESS_GREEN_BORDER;
                break;
        }
        setForeground(fgColor);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        int arc = 12;
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
