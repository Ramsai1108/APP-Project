package com.campus.emergency.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class VectorIcon implements Icon {

    public enum Type {
        SHIELD, REFRESH, LOGOUT, ALERT, CHECK, CLOCK, USER, SEARCH, FILTER, BELL, CHART, LIST,
        FIRE, MEDICAL, HAZARD, LOCK, BUILDING, HELP, RESET, INFO, EYE, SERVER, ACTIVITY, ARROW_RIGHT,
        LOCATION_PIN
    }

    private final Type type;
    private final int size;
    private final Color color;

    public VectorIcon(Type type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color != null ? color : UITheme.TEXT_SECONDARY;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        UITheme.applyQualityRenderingHints(g2);
        g2.setColor(color);
        g2.translate(x, y);

        double s = size / 16.0; // standard 16x16 grid
        g2.scale(s, s);

        switch (type) {
            case SHIELD:
                Path2D shield = new Path2D.Double();
                shield.moveTo(8, 1.5);
                shield.lineTo(14, 4);
                shield.curveTo(14, 10.5, 8, 14.5, 8, 15);
                shield.curveTo(8, 14.5, 2, 10.5, 2, 4);
                shield.closePath();
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(shield);
                break;

            case REFRESH:
            case RESET:
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Arc2D arc = new Arc2D.Double(2.5, 2.5, 11, 11, 45, 270, Arc2D.OPEN);
                g2.draw(arc);
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(9.5, 1.5);
                arrow.lineTo(13.5, 3.5);
                arrow.lineTo(9.5, 6.5);
                g2.draw(arrow);
                break;

            case LOGOUT:
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D door = new Path2D.Double();
                door.moveTo(7, 2.5);
                door.lineTo(3, 2.5);
                door.lineTo(3, 13.5);
                door.lineTo(7, 13.5);
                g2.draw(door);
                g2.drawLine(6, 8, 14, 8);
                g2.drawLine(11, 5, 14, 8);
                g2.drawLine(11, 11, 14, 8);
                break;

            case ALERT:
                Path2D tri = new Path2D.Double();
                tri.moveTo(8, 2);
                tri.lineTo(15, 14);
                tri.lineTo(1, 14);
                tri.closePath();
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(tri);
                g2.drawLine(8, 6, 8, 10);
                g2.fillOval(7, 11, 2, 2);
                break;

            case CHECK:
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(2, 8, 6, 12);
                g2.drawLine(6, 12, 14, 4);
                break;

            case CLOCK:
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(2, 2, 12, 12);
                g2.drawLine(8, 4, 8, 8);
                g2.drawLine(8, 8, 11, 8);
                break;

            case USER:
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(5, 2, 6, 6);
                Path2D body = new Path2D.Double();
                body.moveTo(2, 14);
                body.curveTo(2, 10.5, 5, 10, 8, 10);
                body.curveTo(11, 10, 14, 10.5, 14, 14);
                g2.draw(body);
                break;

            case SEARCH:
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(2, 2, 8, 8);
                g2.drawLine(8, 8, 14, 14);
                break;

            case FILTER:
                Path2D filter = new Path2D.Double();
                filter.moveTo(2, 3);
                filter.lineTo(14, 3);
                filter.lineTo(9.5, 8.5);
                filter.lineTo(9.5, 13);
                filter.lineTo(6.5, 14);
                filter.lineTo(6.5, 8.5);
                filter.closePath();
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(filter);
                break;

            case BELL:
                Path2D bell = new Path2D.Double();
                bell.moveTo(8, 2);
                bell.curveTo(5.5, 2, 4, 4.5, 4, 9);
                bell.lineTo(2, 11.5);
                bell.lineTo(14, 11.5);
                bell.lineTo(12, 9);
                bell.curveTo(12, 4.5, 10.5, 2, 8, 2);
                bell.closePath();
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(bell);
                g2.drawArc(6, 12, 4, 3, 0, -180);
                break;

            case CHART:
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(2, 14, 14, 14);
                g2.fillRect(3, 8, 2, 5);
                g2.fillRect(7, 4, 2, 9);
                g2.fillRect(11, 6, 2, 7);
                break;

            case LIST:
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(3, 4, 13, 4);
                g2.drawLine(3, 8, 13, 8);
                g2.drawLine(3, 12, 13, 12);
                break;

            case FIRE:
                Path2D flame = new Path2D.Double();
                flame.moveTo(8, 1.5);
                flame.curveTo(9, 4, 13, 7, 13, 11);
                flame.curveTo(13, 14, 10.5, 15, 8, 15);
                flame.curveTo(5.5, 15, 3, 14, 3, 11);
                flame.curveTo(3, 8, 6, 5, 8, 1.5);
                flame.closePath();
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(flame);
                g2.drawOval(6, 9, 4, 4);
                break;

            case MEDICAL:
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(2, 2, 12, 12, 3, 3);
                g2.fillRect(6, 4, 4, 8);
                g2.fillRect(4, 6, 8, 4);
                break;

            case HAZARD:
                Path2D hazard = new Path2D.Double();
                hazard.moveTo(8, 1.5);
                hazard.lineTo(15, 14.5);
                hazard.lineTo(1, 14.5);
                hazard.closePath();
                g2.setStroke(new BasicStroke(1.4f));
                g2.draw(hazard);
                g2.drawLine(8, 5, 8, 9);
                g2.fillOval(7, 11, 2, 2);
                break;

            case LOCK:
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(3, 7, 10, 8, 2, 2);
                g2.drawArc(5, 2, 6, 8, 0, 180);
                g2.fillOval(7, 10, 2, 2);
                break;

            case BUILDING:
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRect(3, 2, 10, 13);
                g2.fillRect(5, 4, 2, 2);
                g2.fillRect(9, 4, 2, 2);
                g2.fillRect(5, 7, 2, 2);
                g2.fillRect(9, 7, 2, 2);
                g2.fillRect(7, 11, 2, 4);
                break;

            case HELP:
            case INFO:
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(2, 2, 12, 12);
                g2.drawLine(8, 7, 8, 11);
                g2.fillOval(7, 4, 2, 2);
                break;

            case EYE:
                g2.setStroke(new BasicStroke(1.4f));
                Path2D eye = new Path2D.Double();
                eye.moveTo(2, 8);
                eye.curveTo(4, 4, 12, 4, 14, 8);
                eye.curveTo(12, 12, 4, 12, 2, 8);
                eye.closePath();
                g2.draw(eye);
                g2.fillOval(6, 6, 4, 4);
                break;

            case SERVER:
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(2, 2, 12, 3, 2, 2);
                g2.drawRoundRect(2, 6, 12, 3, 2, 2);
                g2.drawRoundRect(2, 10, 12, 3, 2, 2);
                g2.fillOval(11, 3, 1, 1);
                g2.fillOval(11, 7, 1, 1);
                g2.fillOval(11, 11, 1, 1);
                break;

            case ACTIVITY:
                Path2D pulse = new Path2D.Double();
                pulse.moveTo(1, 8);
                pulse.lineTo(5, 8);
                pulse.lineTo(7, 3);
                pulse.lineTo(9, 13);
                pulse.lineTo(11, 8);
                pulse.lineTo(15, 8);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(pulse);
                break;

            case ARROW_RIGHT:
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(3, 8, 13, 8);
                g2.drawLine(9, 4, 13, 8);
                g2.drawLine(9, 12, 13, 8);
                break;

            case LOCATION_PIN:
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Path2D pin = new Path2D.Double();
                pin.moveTo(8, 1.5);
                pin.curveTo(4.5, 1.5, 2.5, 3.5, 2.5, 7.0);
                pin.curveTo(2.5, 11.0, 8.0, 15.0, 8.0, 15.0);
                pin.curveTo(8.0, 15.0, 13.5, 11.0, 13.5, 7.0);
                pin.curveTo(13.5, 3.5, 11.5, 1.5, 8.0, 1.5);
                pin.closePath();
                g2.draw(pin);
                g2.drawOval(6, 5, 4, 4);
                break;
        }

        g2.dispose();
    }
}

