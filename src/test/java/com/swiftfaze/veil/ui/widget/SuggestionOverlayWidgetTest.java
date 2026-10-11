package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SuggestionOverlayWidgetTest {

    private static final int OWNER_X = 10;
    private static final int OWNER_Y = 200;
    private static final int OWNER_WIDTH = 300;
    private static final int OWNER_HEIGHT = 40;
    private static final int TOP_OFFSET = 16;
    private static final int HORIZONTAL_INSET = 2;
    private static final int SECOND_OWNER_Y = 100;
    private static final int SECOND_OWNER_WIDTH = 50;
    private static final int SECOND_OWNER_HEIGHT = 20;

    @Test
    void positionsFlushAboveOwnerUsingTheGivenTopOffsetAndOwnersFullBounds() throws Exception {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel owner = new JPanel();
        layeredPane.add(owner);
        owner.setBounds(OWNER_X, OWNER_Y, OWNER_WIDTH, OWNER_HEIGHT);

        SuggestionOverlayWidget overlay = new SuggestionOverlayWidget();
        overlay.show(List.of("a", "b"), owner, TOP_OFFSET);

        Rectangle bounds = contentBounds(overlay);
        assertEquals(OWNER_X, bounds.x, "x must match owner's own left edge exactly, not an inset-derived value");
        assertEquals(OWNER_WIDTH, bounds.width, "width must match owner's own full width exactly, not an inset-narrowed value");
        assertEquals(OWNER_Y + TOP_OFFSET - bounds.height, bounds.y, "y must sit ownerTopOffset below owner's top, then height above it");
    }

    @Test
    void zeroTopOffsetPositionsFlushWithOwnersOwnTopEdge() throws Exception {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel owner = new JPanel();
        layeredPane.add(owner);
        owner.setBounds(0, SECOND_OWNER_Y, SECOND_OWNER_WIDTH, SECOND_OWNER_HEIGHT);

        SuggestionOverlayWidget overlay = new SuggestionOverlayWidget();
        overlay.show(List.of("only"), owner);

        Rectangle bounds = contentBounds(overlay);
        assertEquals(SECOND_OWNER_Y - bounds.height, bounds.y);
    }

    @Test
    void horizontalInsetShrinksWidthAndShiftsXInFromBothOwnerEdgesEqually() throws Exception {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel owner = new JPanel();
        layeredPane.add(owner);
        owner.setBounds(OWNER_X, OWNER_Y, OWNER_WIDTH, OWNER_HEIGHT);

        SuggestionOverlayWidget overlay = new SuggestionOverlayWidget();
        overlay.show(List.of("a", "b"), owner, TOP_OFFSET, HORIZONTAL_INSET);

        Rectangle bounds = contentBounds(overlay);
        assertEquals(OWNER_X + HORIZONTAL_INSET, bounds.x, "x must move in from owner's left edge by the inset");
        assertEquals(OWNER_WIDTH - 2 * HORIZONTAL_INSET, bounds.width, "width must shrink by the inset on both sides");
    }

    private static Rectangle contentBounds(SuggestionOverlayWidget overlay)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = SuggestionOverlayWidget.class.getDeclaredField("content");
        field.setAccessible(true);
        JPanel content = (JPanel) field.get(overlay);
        return content.getBounds();
    }
}
