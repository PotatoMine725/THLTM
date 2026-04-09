package com.wifichat.shared.ui;

import javax.swing.border.AbstractBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * A custom border that paints a rounded rectangle outline.
 * Shared across client and admin modules.
 */
public class RoundedBorder extends AbstractBorder {
    private final Color borderColor;
    private final int radius;
    private final int thickness;

    public RoundedBorder(Color borderColor, int radius, int thickness) {
        this.borderColor = borderColor;
        this.radius = radius;
        this.thickness = thickness;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(borderColor);
        g2d.setStroke(new BasicStroke(thickness));
        g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
        g2d.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(8, 10, 8, 10);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 10;
        insets.right = 10;
        insets.top = 8;
        insets.bottom = 8;
        return insets;
    }
}
