package com.swiftfaze.veil.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropConfirmationPopupTest {

    @Test
    void constructorInitializes() {
        DropConfirmationPopup popup = new DropConfirmationPopup();

        assertFalse(popup.isVisible());
    }

    @Test
    void dismissWorks() {
        DropConfirmationPopup popup = new DropConfirmationPopup();
        popup.open();
        assertTrue(popup.isVisible());

        popup.dismiss();

        assertFalse(popup.isVisible());
    }
}
