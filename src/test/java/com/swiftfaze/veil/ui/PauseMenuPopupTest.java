package com.swiftfaze.veil.ui;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseMenuPopupTest {

    @Test
    void constructorInitializes() {
        PauseMenuPopup popup = new PauseMenuPopup();
        assertNotNull(popup);
    }

    @Test
    void menuItemsAreInCorrectOrder() {
        PauseMenuPopup popup = new PauseMenuPopup();

        assertEquals(PauseMenuPopup.RESUME, popup.getSelectedItem());
        popup.onDown();
        assertEquals(PauseMenuPopup.SETTINGS, popup.getSelectedItem());
        popup.onDown();
        assertEquals(PauseMenuPopup.EXIT_TO_MAIN_MENU, popup.getSelectedItem());
    }

    @Test
    void onUpNavigatesMenuBackward() {
        PauseMenuPopup popup = new PauseMenuPopup();
        popup.onDown();
        popup.onDown();
        assertEquals(PauseMenuPopup.EXIT_TO_MAIN_MENU, popup.getSelectedItem());

        popup.onUp();
        assertEquals(PauseMenuPopup.SETTINGS, popup.getSelectedItem());
    }

    @Test
    void confirmingResumeClosesPopup() {
        PauseMenuPopup popup = new PauseMenuPopup();
        popup.open();
        assertTrue(popup.isVisible());

        fireAction(popup, "pause-confirm");

        assertFalse(popup.isVisible());
    }

    @Test
    void confirmingSettingsInvokesCallback() {
        PauseMenuPopup popup = new PauseMenuPopup();
        List<String> selectedItems = new ArrayList<>();
        popup.setOnMenuSelect(item -> selectedItems.add(item));

        popup.onDown();
        assertEquals(PauseMenuPopup.SETTINGS, popup.getSelectedItem());
        fireAction(popup, "pause-confirm");

        assertTrue(selectedItems.contains(PauseMenuPopup.SETTINGS));
    }

    @Test
    void confirmingExitToMainMenuInvokesCallback() {
        PauseMenuPopup popup = new PauseMenuPopup();
        List<String> selectedItems = new ArrayList<>();
        popup.setOnMenuSelect(item -> selectedItems.add(item));

        popup.onDown();
        popup.onDown();
        assertEquals(PauseMenuPopup.EXIT_TO_MAIN_MENU, popup.getSelectedItem());
        fireAction(popup, "pause-confirm");

        assertTrue(selectedItems.contains(PauseMenuPopup.EXIT_TO_MAIN_MENU));
    }

    private static void fireAction(PauseMenuPopup popup, String actionName) {
        Action action = popup.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(popup, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
