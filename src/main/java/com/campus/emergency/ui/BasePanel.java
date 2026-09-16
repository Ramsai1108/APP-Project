package com.campus.emergency.ui;

import javax.swing.JPanel;
import java.awt.LayoutManager;

/**
 * Base Swing Panel enforcing consistent styling and layout helpers.
 */
public abstract class BasePanel extends JPanel {

    public BasePanel() {
        initComponents();
    }

    public BasePanel(LayoutManager layout) {
        super(layout);
        initComponents();
    }

    protected abstract void initComponents();
}
