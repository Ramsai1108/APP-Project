package com.campus.emergency.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Authentication Login View Panel.
 */
public class LoginPanel extends BasePanel {

    private final MainFrame mainFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    protected void initComponents() {
        setLayout(new GridBagLayout());
        JLabel titleLabel = new JLabel("Smart Campus Emergency Portal");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(titleLabel);
    }
}
