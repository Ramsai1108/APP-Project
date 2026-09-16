package com.campus.emergency.ui.components;

import javax.swing.JLabel;

/**
 * Custom UI Component badge displaying resource and incident status.
 */
public class StatusIndicator extends JLabel {

    public StatusIndicator(String status) {
        setText(status != null ? status.toUpperCase() : "UNKNOWN");
    }
}
