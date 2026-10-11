package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactPopupWidgetTest {

    @Test
    void isFullScreenReturnsFalse() {
        CompactPopupWidget popup = new CompactPopupWidget("Title");
        assertFalse(popup.isFullScreen());
    }

    @Test
    void constructorInitializes() {
        CompactPopupWidget popup = new CompactPopupWidget("Title");
        assertFalse(popup.isVisible());
    }

    @Test
    void openWorks() {
        CompactPopupWidget popup = new CompactPopupWidget("Title");
        popup.open();
        assertTrue(popup.isVisible());
    }

    @Test
    void dismissWorks() {
        CompactPopupWidget popup = new CompactPopupWidget("Title");
        popup.open();
        popup.dismiss();
        assertFalse(popup.isVisible());
    }
}
