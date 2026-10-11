package com.swiftfaze.veil.ui.widget;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A flat, black-track/solid-thumb scrollbar with no arrow buttons, matching
 * the terminal aesthetic instead of the platform look-and-feel's default.
 */
public class TerminalScrollBarUi extends BasicScrollBarUI {

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = WidgetTheme.scrollbarThumb();
        this.trackColor = WidgetTheme.background();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return zeroSizeButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return zeroSizeButton();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(WidgetTheme.background());
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty()) {
            return;
        }
        g.setColor(WidgetTheme.scrollbarThumb());
        g.fillRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height);
    }

    private static JButton zeroSizeButton() {
        JButton button = new JButton();
        Dimension zero = new Dimension(0, 0);
        button.setPreferredSize(zero);
        button.setMinimumSize(zero);
        button.setMaximumSize(zero);
        return button;
    }
}