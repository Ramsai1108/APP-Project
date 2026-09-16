package com.campus.emergency.ui.components;

import javax.swing.JButton;

/**
 * Custom styled button component.
 */
public class CustomButton extends JButton {

    public CustomButton(String text) {
        super(text);
        setFocusPainted(false);
    }
}
