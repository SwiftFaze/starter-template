package com.swiftfaze.veil.ui.widget;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Rectangle;

/**
 * Stretches every child to the parent's full bounds, stacked on top of each
 * other — the layout half of a {@link javax.swing.JLayeredPane} overlay,
 * where z-order (not position) is what separates the layers. A {@link PopupWidget}
 * instead follows its {@link PopupWidget#placement()}.
 */
public class FillLayout implements LayoutManager {

    /** Gap between a bottom-anchored popup and the parent's bottom edge (the outer padding token). */
    private static final int BOTTOM_MARGIN = 10;
    private static final double BOTTOM_WIDTH_FRACTION = 0.8;
    private static final int BOTTOM_MIN_WIDTH = 480;
    private static final int BOTTOM_MAX_WIDTH = 960;
    private static final double BOTTOM_HEIGHT_FRACTION = 0.45;
    private static final int BOTTOM_MIN_HEIGHT = 260;
    private static final int BOTTOM_MAX_HEIGHT = 480;

    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return parent.getPreferredSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof PopupWidget popup) {
                child.setBounds(popupBounds(popup, parent.getSize()));
            } else {
                // Stretch non-popup children to fill parent
                child.setBounds(0, 0, parent.getWidth(), parent.getHeight());
            }
        }
    }

    private static Rectangle popupBounds(PopupWidget popup, Dimension parent) {
        return switch (popup.placement()) {
            case FULL_SCREEN -> new Rectangle(0, 0, parent.width, parent.height);
            case CENTERED -> centred(popup.getPreferredSize(), parent);
            case BOTTOM_CENTER -> bottomCentred(parent);
        };
    }

    /** Centres a popup at its preferred size. */
    private static Rectangle centred(Dimension prefSize, Dimension parent) {
        int x = (parent.width - prefSize.width) / 2;
        int y = (parent.height - prefSize.height) / 2;
        return new Rectangle(x, y, prefSize.width, prefSize.height);
    }

    /**
     * The RPG dialogue box: horizontally centred, along the bottom edge with a margin. Centring
     * vertically would cover the player, which the camera keeps in the middle of the view.
     */
    private static Rectangle bottomCentred(Dimension parent) {
        int width = clamp(Math.round(parent.width * BOTTOM_WIDTH_FRACTION), BOTTOM_MIN_WIDTH, BOTTOM_MAX_WIDTH);
        int height = clamp(Math.round(parent.height * BOTTOM_HEIGHT_FRACTION), BOTTOM_MIN_HEIGHT, BOTTOM_MAX_HEIGHT);
        int x = (parent.width - width) / 2;
        int y = parent.height - height - BOTTOM_MARGIN;
        return new Rectangle(x, y, width, height);
    }

    private static int clamp(long value, int min, int max) {
        return (int) Math.max(min, Math.min(max, value));
    }
}
