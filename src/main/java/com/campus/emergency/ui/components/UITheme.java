package com.campus.emergency.ui.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UITheme {

    // 8px Spacing System
    public static final int SPACING_8 = 8;
    public static final int SPACING_16 = 16;
    public static final int SPACING_24 = 24;
    public static final int SPACING_32 = 32;

    // Color Palette: Emergency Operations Center / Security Operations
    public static final Color BG_MAIN = new Color(0xF5, 0xF7, 0xFA);           // #F5F7FA
    public static final Color BG_LIGHT = new Color(0xF8, 0xFA, 0xFC);          // Slate 50
    public static final Color PRIMARY_DARK = new Color(0x0F, 0x17, 0x2A);      // #0F172A (Slate 900)
    public static final Color NAV_BG = new Color(0x1E, 0x29, 0x3B);            // Slate 800
    public static final Color NAV_HOVER = new Color(0x33, 0x41, 0x55);         // Slate 700
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(0xE2, 0xE8, 0xF0);       // #E2E8F0 (Slate 200)
    public static final Color CARD_BORDER_HOVER = new Color(0x94, 0xA3, 0xB8);

    public static final Color TEXT_PRIMARY = new Color(0x0F, 0x17, 0x2A);      // #0F172A
    public static final Color TEXT_SECONDARY = new Color(0x64, 0x74, 0x8B);    // #64748B
    public static final Color TEXT_MUTED = new Color(0x94, 0xA3, 0xB8);        // Slate 400
    public static final Color TEXT_LIGHT = new Color(0xF1, 0xF5, 0xF9);

    public static final Color ACCENT_BLUE = new Color(0x02, 0x84, 0xC7);       // #0284C7 (Sky/Blue 600)
    public static final Color ACCENT_BLUE_HOVER = new Color(0x03, 0x69, 0xA1);
    public static final Color ACCENT_BLUE_BG = new Color(0xF0, 0xF9, 0xFF);

    // Emergency Severity Colors (Restrained Professional Palette)
    public static final Color CRITICAL = new Color(0xDC, 0x26, 0x26);          // #DC2626
    public static final Color CRITICAL_BG = new Color(0xFE, 0xF2, 0xF2);
    public static final Color CRITICAL_BORDER = new Color(0xFE, 0xCA, 0xCA);

    public static final Color HIGH = new Color(0xEA, 0x58, 0x0C);              // #EA580C
    public static final Color HIGH_BG = new Color(0xFF, 0xF7, 0xED);
    public static final Color HIGH_BORDER = new Color(0xFE, 0xD7, 0xAA);

    public static final Color MEDIUM = new Color(0xD9, 0x77, 0x06);            // #D97706
    public static final Color MEDIUM_BG = new Color(0xFE, 0xFC, 0xE8);
    public static final Color MEDIUM_BORDER = new Color(0xFE, 0xF0, 0x8A);

    public static final Color SUCCESS = new Color(0x16, 0xA3, 0x4A);           // #16A34A
    public static final Color SUCCESS_BG = new Color(0xF0, 0xFD, 0xF4);
    public static final Color SUCCESS_BORDER = new Color(0xBB, 0xF7, 0xD0);

    public static final Color INFO = new Color(0x02, 0x84, 0xC7);              // #0284C7
    public static final Color INFO_BG = new Color(0xF0, 0xF9, 0xFF);
    public static final Color INFO_BORDER = new Color(0xBA, 0xE6, 0xFD);

    // Aliases for compatibility
    public static final Color EMERGENCY_RED = CRITICAL;
    public static final Color EMERGENCY_RED_BG = CRITICAL_BG;
    public static final Color EMERGENCY_RED_BORDER = CRITICAL_BORDER;

    public static final Color WARNING_ORANGE = HIGH;
    public static final Color WARNING_ORANGE_BG = HIGH_BG;
    public static final Color WARNING_ORANGE_BORDER = HIGH_BORDER;

    public static final Color AMBER_YELLOW = MEDIUM;
    public static final Color AMBER_YELLOW_BG = MEDIUM_BG;
    public static final Color AMBER_YELLOW_BORDER = MEDIUM_BORDER;

    public static final Color SUCCESS_GREEN = SUCCESS;
    public static final Color SUCCESS_GREEN_BG = SUCCESS_BG;
    public static final Color SUCCESS_GREEN_BORDER = SUCCESS_BORDER;

    public static final Color INFO_CYAN = INFO;
    public static final Color INFO_CYAN_BG = INFO_BG;
    public static final Color INFO_CYAN_BORDER = INFO_BORDER;

    // Typography: Segoe UI (Standard on Windows)
    public static final Font FONT_APP_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE_LARGE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font FONT_TITLE_XL = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SECTION_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_KPI_VALUE = new Font("Segoe UI", Font.BOLD, 28);
    public static final Font FONT_KPI_LABEL = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_META = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 11);

    public static void applyQualityRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static Border createCardPadding() {
        return new EmptyBorder(16, 16, 16, 16);
    }

    public static Border createSectionPadding() {
        return new EmptyBorder(16, 24, 16, 24);
    }
}

