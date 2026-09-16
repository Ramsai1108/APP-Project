package com.campus.emergency.ui;

import javax.swing.JLabel;
import java.awt.Font;

/**
 * Administrator Master System Dashboard Panel.
 */
public class AdminDashboardPanel extends BasePanel {

    @Override
    protected void initComponents() {
        JLabel titleLabel = new JLabel("System Administration & Master Control");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(titleLabel);
    }
}
