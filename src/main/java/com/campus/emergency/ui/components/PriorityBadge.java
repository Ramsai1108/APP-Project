package com.campus.emergency.ui.components;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Font;

/**
 * Custom UI Component badge displaying colored emergency priority levels.
 */
public class PriorityBadge extends JLabel {

    public PriorityBadge(double priorityScore) {
        setText(String.format("PRIORITY: %.1f", priorityScore));
        setFont(new Font("SansSerif", Font.BOLD, 12));
        setOpaque(true);

        if (priorityScore >= 80.0) {
            setBackground(new Color(220, 53, 69)); // High Alert Red
            setForeground(Color.WHITE);
        } else if (priorityScore >= 50.0) {
            setBackground(new Color(255, 193, 7)); // Moderate Alert Yellow/Orange
            setForeground(Color.BLACK);
        } else {
            setBackground(new Color(40, 167, 69)); // Standard Green
            setForeground(Color.WHITE);
        }
    }
}
