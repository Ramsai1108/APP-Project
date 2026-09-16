package com.campus.emergency.ui;

import javax.swing.JLabel;
import java.awt.Font;

/**
 * Dispatcher Control Room Dashboard Panel.
 */
public class DispatcherDashboardPanel extends BasePanel {

    @Override
    protected void initComponents() {
        JLabel titleLabel = new JLabel("Emergency Dispatcher Command Console");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        add(titleLabel);
    }
}
