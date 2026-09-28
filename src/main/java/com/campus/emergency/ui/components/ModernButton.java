package com.campus.emergency.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY, DANGER, SECONDARY, SUCCESS, DARK, OUTLINE
    }

    private ButtonStyle style = ButtonStyle.PRIMARY;
    private Icon buttonIcon = null;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 8;

    public ModernButton(String text) {
        this(text, (Icon) null, ButtonStyle.PRIMARY);
    }

    public ModernButton(String text, ButtonStyle style) {
        this(text, (Icon) null, style);
    }

    public ModernButton(String text, VectorIcon.Type iconType, ButtonStyle style) {
        this(text, iconType != null ? new VectorIcon(iconType, 16, getIconColorForStyle(style)) : null, style);
    }

    public ModernButton(String text, Icon icon, ButtonStyle style) {
        super(text);
        this.buttonIcon = icon;
        this.style = style;
        init();
    }

    private static Color getIconColorForStyle(ButtonStyle style) {
        if (style == ButtonStyle.SECONDARY || style == ButtonStyle.OUTLINE) {
            return UITheme.TEXT_PRIMARY;
        }
        return Color.WHITE;
    }

    private void init() {
        setFont(UITheme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 16, 8, 16));
        setPreferredSize(new Dimension(getPreferredSize().width, 38));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setButtonStyle(ButtonStyle style) {
        this.style = style;
        repaint();
    }

    public void setButtonIcon(Icon icon) {
        this.buttonIcon = icon;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);

        Color bg;
        Color fg = Color.WHITE;
        Color border = null;

        if (!isEnabled()) {
            bg = new Color(226, 232, 240);
            fg = new Color(148, 163, 184);
        } else {
            switch (style) {
                case DANGER:
                    bg = isPressed ? new Color(185, 28, 28) : (isHovered ? new Color(239, 68, 68) : UITheme.EMERGENCY_RED);
                    break;
                case SUCCESS:
                    bg = isPressed ? new Color(21, 128, 61) : (isHovered ? new Color(34, 197, 94) : UITheme.SUCCESS_GREEN);
                    break;
                case DARK:
                    bg = isPressed ? UITheme.PRIMARY_DARK : (isHovered ? UITheme.NAV_HOVER : UITheme.NAV_BG);
                    break;
                case SECONDARY:
                    bg = isPressed ? new Color(203, 213, 225) : (isHovered ? new Color(226, 232, 240) : new Color(241, 245, 249));
                    fg = UITheme.TEXT_PRIMARY;
                    border = UITheme.CARD_BORDER;
                    break;
                case OUTLINE:
                    bg = isPressed ? new Color(241, 245, 249) : (isHovered ? new Color(248, 250, 252) : Color.WHITE);
                    fg = UITheme.TEXT_PRIMARY;
                    border = isHovered ? UITheme.ACCENT_BLUE : UITheme.CARD_BORDER;
                    break;
                case PRIMARY:
                default:
                    bg = isPressed ? UITheme.ACCENT_BLUE_HOVER : (isHovered ? new Color(59, 130, 246) : UITheme.ACCENT_BLUE);
                    break;
            }
        }

        // Fill background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw border if outline/secondary
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        // Measure text & icon
        String text = getText() != null ? getText() : "";
        FontMetrics fm = g2.getFontMetrics(getFont());
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent() - fm.getDescent();

        int iconWidth = buttonIcon != null ? buttonIcon.getIconWidth() : 0;
        int iconHeight = buttonIcon != null ? buttonIcon.getIconHeight() : 0;
        int gap = (buttonIcon != null && !text.isEmpty()) ? 8 : 0;
        int totalContentWidth = iconWidth + gap + textWidth;

        int startX;
        if (getHorizontalAlignment() == SwingConstants.LEFT) {
            startX = 16;
        } else if (getHorizontalAlignment() == SwingConstants.RIGHT) {
            startX = getWidth() - totalContentWidth - 16;
        } else {
            startX = (getWidth() - totalContentWidth) / 2;
        }

        // Paint Icon
        if (buttonIcon != null) {
            int iconY = (getHeight() - iconHeight) / 2;
            buttonIcon.paintIcon(this, g2, startX, iconY);
        }

        // Paint Text
        if (!text.isEmpty()) {
            int textX = startX + iconWidth + gap;
            int textY = (getHeight() + textHeight) / 2;
            g2.setFont(getFont());
            g2.setColor(fg);
            g2.drawString(text, textX, textY);
        }

        g2.dispose();
    }
}

