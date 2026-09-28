package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class ModernTable extends JTable {

    public ModernTable(DefaultTableModel model) {
        super(model);
        initStyle();
    }

    private void initStyle() {
        setFont(UITheme.FONT_TABLE);
        setRowHeight(40);
        setShowGrid(false);
        setShowHorizontalLines(false);
        setShowVerticalLines(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionBackground(UITheme.ACCENT_BLUE_BG);
        setSelectionForeground(UITheme.TEXT_PRIMARY);
        setFillsViewportHeight(true);
        setBackground(Color.WHITE);

        // Header Styling
        JTableHeader header = getTableHeader();
        header.setFont(UITheme.FONT_BADGE);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(UITheme.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 36));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.CARD_BORDER));

        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);

        // Striped and padded cell renderer
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                        BorderFactory.createEmptyBorder(0, 12, 0, 12)
                ));

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_PRIMARY);
                } else {
                    c.setBackground(UITheme.ACCENT_BLUE_BG);
                    c.setForeground(UITheme.ACCENT_BLUE);
                }

                // Check for Priority or Status values to render as Badges
                if (value instanceof String) {
                    String str = (String) value;
                    String upper = str.toUpperCase().trim();
                    if ("CRITICAL".equals(upper) || "HIGH".equals(upper) ||
                            "MEDIUM".equals(upper) || "LOW".equals(upper)) {
                        return new PriorityBadge(upper);
                    }
                    if ("OPEN".equals(upper) || "IN_PROGRESS".equals(upper) || "IN PROGRESS".equals(upper) ||
                            "RESOLVED".equals(upper) || "CLOSED".equals(upper) ||
                            "ACKNOWLEDGED".equals(upper) ||
                            "AVAILABLE".equals(upper) || "DISPATCHED".equals(upper) ||
                            "IN_USE".equals(upper) || "OFFLINE".equals(upper) || "BUSY".equals(upper)) {
                        return new StatusBadge(upper);
                    }
                }
                return c;
            }
        });
    }
}

