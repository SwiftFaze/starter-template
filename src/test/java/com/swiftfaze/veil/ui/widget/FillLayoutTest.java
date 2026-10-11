package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FillLayoutTest {

    @Test
    void layoutContainerStretchesEveryChildToTheParentsFullBounds() {
        JPanel parent = new JPanel(new FillLayout());
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        parent.add(first);
        parent.add(second);
        parent.setBounds(0, 0, 300, 200);

        parent.doLayout();

        assertEquals(new Rectangle(0, 0, 300, 200), first.getBounds());
        assertEquals(new Rectangle(0, 0, 300, 200), second.getBounds());
    }

    @Test
    void centredPopupSitsAtItsPreferredSizeInTheMiddle() {
        PopupWidget popup = popup(PopupWidget.Placement.CENTERED, new Dimension(200, 100));

        assertEquals(new Rectangle(50, 50, 200, 100), layoutInside(300, 200, popup));
    }

    @Test
    void bottomCentrePopupIsAlmostHalfTallAndEightTenthsWideWithATenPixelBottomMargin() {
        assertEquals(new Rectangle(100, 320, 800, 270), bottomPopupIn(1000, 600));
    }

    @Test
    void bottomCentrePopupWidthIsCappedAtTheMaximum() {
        assertEquals(new Rectangle(520, 485, 960, 405), bottomPopupIn(2000, 900));
    }

    @Test
    void bottomCentrePopupWidthIsRaisedToTheMinimum() {
        assertEquals(new Rectangle(10, 320, 480, 270), bottomPopupIn(500, 600));
    }

    @Test
    void bottomCentrePopupHeightIsRaisedToTheMinimum() {
        assertEquals(new Rectangle(100, 30, 800, 260), bottomPopupIn(1000, 300));
    }

    @Test
    void bottomCentrePopupIsStillAnchoredToTheBottomWhenTheWindowIsResized() {
        Rectangle larger = bottomPopupIn(1000, 900);
        Rectangle smaller = bottomPopupIn(1000, 600);

        assertEquals(900 - 10, larger.y + larger.height);
        assertEquals(600 - 10, smaller.y + smaller.height);
    }

    private static Rectangle bottomPopupIn(int width, int height) {
        return layoutInside(width, height, popup(PopupWidget.Placement.BOTTOM_CENTER, new Dimension(1, 1)));
    }

    /** Lays the popup out as the only child of a parent of the given size, and returns its bounds. */
    private static Rectangle layoutInside(int width, int height, PopupWidget popup) {
        JPanel parent = new JPanel(new FillLayout());
        parent.add(popup);
        parent.setBounds(0, 0, width, height);
        parent.doLayout();
        return popup.getBounds();
    }

    private static PopupWidget popup(PopupWidget.Placement placement, Dimension preferred) {
        return new PopupWidget() {
            @Override
            public PopupWidget.Placement placement() {
                return placement;
            }

            @Override
            public Dimension getPreferredSize() {
                return preferred;
            }
        };
    }
}
