package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class ModernPasswordField extends JPasswordField {
    private String placeholder = "";
    private boolean isFocused = false;
    private int cornerRadius = 8;

    public ModernPasswordField() {
        this("");
    }

    public ModernPasswordField(String placeholder) {
        this.placeholder = placeholder;
        init();
    }

    private void init() {
        setFont(UITheme.FONT_BODY);
        setForeground(UITheme.TEXT_PRIMARY);
        setCaretColor(UITheme.ACCENT_BLUE);
        setOpaque(false);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setPreferredSize(new Dimension(getPreferredSize().width, 38));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                isFocused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                isFocused = false;
                repaint();
            }
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        // Background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        // Border
        if (isFocused) {
            g2.setColor(UITheme.ACCENT_BLUE);
            g2.setStroke(new BasicStroke(1.5f));
        } else {
            g2.setColor(UITheme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
        }
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

        g2.dispose();
        super.paintComponent(g);

        // Draw Placeholder if empty
        if ((getPassword() == null || getPassword().length == 0) && placeholder != null && !placeholder.isEmpty() && !isFocused) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            UITheme.applyQualityRenderingHints(gPlaceholder);
            gPlaceholder.setFont(getFont());
            gPlaceholder.setColor(UITheme.TEXT_MUTED);
            Insets insets = getInsets();
            FontMetrics fm = gPlaceholder.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            gPlaceholder.drawString(placeholder, insets.left, y);
            gPlaceholder.dispose();
        }
    }
}
