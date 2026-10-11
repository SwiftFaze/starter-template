package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.GamePanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PauseToggleListenerTest {

    @Test
    void togglePauseOpensThePopup() {
        GamePanel gamePanel = new GamePanel();
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);

        listener.togglePause();

        assertTrue(pauseMenuPopup.isVisible());
    }

    @Test
    void togglePauseSetsGamePanelPausedFlag() {
        GamePanel gamePanel = new GamePanel();
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);

        listener.togglePause();

        assertTrue(gamePanel.isPaused());
    }

    @Test
    void togglingPauseTwiceClosesThePopup() {
        GamePanel gamePanel = new GamePanel();
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);

        listener.togglePause();
        listener.togglePause();

        assertFalse(pauseMenuPopup.isVisible());
    }

    @Test
    void dismissingThePauseMenuResumesTheWorld() {
        GamePanel gamePanel = new GamePanel();
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);
        listener.togglePause();

        pauseMenuPopup.dismiss();

        assertFalse(gamePanel.isPaused());
    }

    @Test
    void updatePlayerDoesNotThrow() {
        GamePanel gamePanel = new GamePanel();
        PauseMenuPopup pauseMenuPopup = new PauseMenuPopup();
        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);

        assertDoesNotThrow(() -> listener.updatePlayer(null));
    }
}
