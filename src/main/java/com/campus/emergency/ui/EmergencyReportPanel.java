package com.campus.emergency.ui;

import javax.swing.JLabel;
import java.awt.Font;

/**
 * Emergency SOS Reporting Panel.
 */
public class EmergencyReportPanel extends BasePanel {

    @Override
    protected void initComponents() {
        JLabel titleLabel = new JLabel("Emergency SOS Reporting Center");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(titleLabel);
    }
}
