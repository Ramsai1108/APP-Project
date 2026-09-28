package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class ModernTabbedPane extends JTabbedPane {

    public ModernTabbedPane() {
        super(TOP, SCROLL_TAB_LAYOUT);
        setFont(UITheme.FONT_BODY_BOLD);
        setBackground(UITheme.BG_MAIN);
        setForeground(UITheme.TEXT_SECONDARY);
        setFocusable(false);
        setUI(new ModernTabbedPaneUI());
    }

    private static class ModernTabbedPaneUI extends BasicTabbedPaneUI {

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(10, 20, 10, 20);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(8, 0, 0, 0);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            // No default 3D border
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // Clean minimal top divider
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(UITheme.CARD_BORDER);
            g2.drawLine(0, 0, tabPane.getWidth(), 0);
            g2.dispose();
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.applyQualityRenderingHints(g2);

            if (isSelected) {
                g2.setColor(Color.WHITE);
                g2.fillRect(x, y, w, h);

                // Bottom active indicator line
                g2.setColor(UITheme.ACCENT_BLUE);
                g2.fillRect(x, y + h - 3, w, 3);
            } else {
                g2.setColor(UITheme.BG_MAIN);
                g2.fillRect(x, y, w, h);
            }

            g2.dispose();
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.applyQualityRenderingHints(g2);

            g2.setFont(isSelected ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
            g2.setColor(isSelected ? UITheme.ACCENT_BLUE : UITheme.TEXT_SECONDARY);

            int x = textRect.x;
            int y = textRect.y + metrics.getAscent();
            g2.drawString(title, x, y);
            g2.dispose();
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
            // No dashed focus outline
        }
    }
}
