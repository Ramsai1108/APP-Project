package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatusTimelinePanel extends JPanel {
    private static final String[] STEPS = {"OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "CLOSED"};
    private static final String[] LABELS = {"1. Open", "2. Acknowledged", "3. In Progress", "4. Resolved", "5. Closed"};
    private String currentStatus = "OPEN";

    public StatusTimelinePanel() {
        this("OPEN");
    }

    public StatusTimelinePanel(String currentStatus) {
        this.currentStatus = (currentStatus != null) ? currentStatus.toUpperCase().trim() : "OPEN";
        setLayout(new GridLayout(1, STEPS.length, 6, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 0, 8, 0));
        updateView();
    }

    public void setStatus(String status) {
        this.currentStatus = (status != null) ? status.toUpperCase().trim() : "OPEN";
        updateView();
    }

    private void updateView() {
        removeAll();
        int activeIndex = getStepIndex(currentStatus);

        for (int i = 0; i < STEPS.length; i++) {
            boolean isCompleted = i < activeIndex;
            boolean isCurrent = i == activeIndex;

            JPanel stepBox = new JPanel(new BorderLayout());
            stepBox.setOpaque(false);
            stepBox.setBorder(new EmptyBorder(6, 4, 6, 4));

            JLabel stepLabel = new JLabel(LABELS[i], SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.applyQualityRenderingHints(g2);

                    int arc = 6;
                    Color bg;
                    Color fg;
                    Color border;

                    if (isCurrent) {
                        bg = currentStatus.equals("RESOLVED") || currentStatus.equals("CLOSED") ? UITheme.SUCCESS_GREEN : UITheme.ACCENT_BLUE;
                        fg = Color.WHITE;
                        border = bg;
                    } else if (isCompleted) {
                        bg = UITheme.SUCCESS_GREEN_BG;
                        fg = UITheme.SUCCESS_GREEN;
                        border = UITheme.SUCCESS_GREEN_BORDER;
                    } else {
                        bg = new Color(241, 245, 249);
                        fg = UITheme.TEXT_MUTED;
                        border = UITheme.CARD_BORDER;
                    }

                    g2.setColor(bg);
                    g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                    g2.setColor(border);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                    g2.dispose();

                    setForeground(fg);
                    setFont(isCurrent ? UITheme.FONT_BADGE : UITheme.FONT_SMALL);
                    super.paintComponent(g);
                }
            };
            stepLabel.setPreferredSize(new Dimension(100, 28));
            stepBox.add(stepLabel, BorderLayout.CENTER);
            add(stepBox);
        }
        revalidate();
        repaint();
    }

    private int getStepIndex(String status) {
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i].equalsIgnoreCase(status)) {
                return i;
            }
        }
        return 0;
    }
}
