package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatusBadge extends JLabel {
    private Color bgColor = UITheme.INFO_BG;
    private Color fgColor = UITheme.INFO;
    private Color borderColor = UITheme.INFO_BORDER;

    public StatusBadge(String status) {
        super(status != null ? status.toUpperCase().trim() : "UNKNOWN", SwingConstants.CENTER);
        init();
        setStatus(status);
    }

    private void init() {
        setFont(UITheme.FONT_BADGE);
        setOpaque(false);
        setBorder(new EmptyBorder(3, 8, 3, 8));
    }

    public void setStatus(String status) {
        String st = status != null ? status.toUpperCase().trim() : "UNKNOWN";
        setText(st);

        switch (st) {
            case "OPEN":
            case "NEW":
                bgColor = UITheme.INFO_BG;
                fgColor = UITheme.INFO;
                borderColor = UITheme.INFO_BORDER;
                break;
            case "ACKNOWLEDGED":
                bgColor = new Color(0xF3, 0xE8, 0xFF); // Purple/Blue
                fgColor = new Color(0x7E, 0x22, 0xCE);
                borderColor = new Color(0xE9, 0xD5, 0xFF);
                break;
            case "IN_PROGRESS":
            case "DISPATCHED":
            case "IN_USE":
                bgColor = UITheme.HIGH_BG;
                fgColor = UITheme.HIGH;
                borderColor = UITheme.HIGH_BORDER;
                break;
            case "RESOLVED":
            case "AVAILABLE":
                bgColor = UITheme.SUCCESS_BG;
                fgColor = UITheme.SUCCESS;
                borderColor = UITheme.SUCCESS_BORDER;
                break;
            case "CLOSED":
                bgColor = new Color(0xF1, 0xF5, 0xF9);
                fgColor = UITheme.TEXT_SECONDARY;
                borderColor = UITheme.CARD_BORDER;
                break;
            case "CANCELLED":
            case "OFFLINE":
                bgColor = UITheme.CRITICAL_BG;
                fgColor = UITheme.CRITICAL;
                borderColor = UITheme.CRITICAL_BORDER;
                break;
            default:
                bgColor = new Color(0xF1, 0xF5, 0xF9);
                fgColor = UITheme.TEXT_SECONDARY;
                borderColor = UITheme.CARD_BORDER;
                break;
        }
        setForeground(fgColor);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        int arc = 10;
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
