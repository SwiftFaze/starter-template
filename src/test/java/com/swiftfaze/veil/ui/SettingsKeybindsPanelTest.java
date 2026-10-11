package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsKeybindsPanelTest {

    @Test
    void constructorInitializes(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsKeybindsPanel panel = new SettingsKeybindsPanel(screen -> {}, new ControlsHintBarWidget(), store);

        assertEquals("Move up", panel.getHighlightedActionName());
    }

    @Test
    void moveUpWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsKeybindsPanel panel = new SettingsKeybindsPanel(screen -> {}, new ControlsHintBarWidget(), store);
        panel.moveDown();
        assertEquals("Move down", panel.getHighlightedActionName());

        panel.moveUp();

        assertEquals("Move up", panel.getHighlightedActionName());
    }

    @Test
    void moveDownWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsKeybindsPanel panel = new SettingsKeybindsPanel(screen -> {}, new ControlsHintBarWidget(), store);

        panel.moveDown();

        assertEquals("Move down", panel.getHighlightedActionName());
    }

    @Test
    void confirmWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsKeybindsPanel panel = new SettingsKeybindsPanel(screen -> {}, new ControlsHintBarWidget(), store);
        assertFalse(panel.isPopupOpen());

        panel.confirm();

        assertTrue(panel.isPopupOpen());
    }
}
