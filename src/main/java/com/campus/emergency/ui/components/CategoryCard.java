package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CategoryCard extends JPanel {
    private final String categoryCode;
    private final String title;
    private final String description;
    private final VectorIcon.Type iconType;
    private boolean isSelected = false;
    private boolean isHovered = false;
    private Runnable onSelectListener;

    public CategoryCard(String categoryCode, VectorIcon.Type iconType, String title, String description) {
        this.categoryCode = categoryCode;
        this.iconType = iconType;
        this.title = title;
        this.description = description;

        setLayout(new BorderLayout(10, 6));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 14, 12, 14));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel();
        if (iconType != null) {
            iconLabel.setIcon(new VectorIcon(iconType, 22, UITheme.ACCENT_BLUE));
        }
        add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UITheme.FONT_SUBTITLE);
        titleLabel.setForeground(UITheme.TEXT_PRIMARY);
        textPanel.add(titleLabel);

        JLabel descLabel = new JLabel(description);
        descLabel.setFont(UITheme.FONT_META);
        descLabel.setForeground(UITheme.TEXT_MUTED);
        textPanel.add(descLabel);

        add(textPanel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (onSelectListener != null) {
                    onSelectListener.run();
                }
            }
        });
    }

    // Overload for backward compatibility with String icons
    public CategoryCard(String categoryCode, String iconStr, String title, String description) {
        this(categoryCode, mapStringToVectorType(categoryCode, iconStr), title, description);
    }

    private static VectorIcon.Type mapStringToVectorType(String code, String iconStr) {
        if ("FIRE".equalsIgnoreCase(code)) return VectorIcon.Type.FIRE;
        if ("MEDICAL".equalsIgnoreCase(code)) return VectorIcon.Type.MEDICAL;
        if ("HAZARD".equalsIgnoreCase(code)) return VectorIcon.Type.HAZARD;
        if ("SECURITY".equalsIgnoreCase(code)) return VectorIcon.Type.LOCK;
        if ("FACILITY".equalsIgnoreCase(code)) return VectorIcon.Type.BUILDING;
        return VectorIcon.Type.HELP;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        repaint();
    }

    public void setOnSelectListener(Runnable listener) {
        this.onSelectListener = listener;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        int arc = 8;
        Color bg = isSelected ? UITheme.ACCENT_BLUE_BG : (isHovered ? new Color(248, 250, 252) : Color.WHITE);
        Color border = isSelected ? UITheme.ACCENT_BLUE : (isHovered ? UITheme.CARD_BORDER_HOVER : UITheme.CARD_BORDER);
        float strokeWidth = isSelected ? 2.0f : 1.0f;

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.setColor(border);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}

