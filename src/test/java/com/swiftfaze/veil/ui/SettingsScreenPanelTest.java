package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsScreenPanelTest {

    @Test
    void constructorInitializes(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsScreenPanel panel = new SettingsScreenPanel(
            screen -> {},
            path -> {},
            new ControlsHintBarWidget(),
            store
        );

        assertEquals("Brightness", panel.getHighlightedItemName());
    }

    @Test
    void moveLeftWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsScreenPanel panel = new SettingsScreenPanel(screen -> {}, path -> {}, new ControlsHintBarWidget(), store);
        int before = panel.getSliderValue("Brightness");

        panel.moveLeft();

        assertEquals(Math.max(0, before - 1), panel.getSliderValue("Brightness"));
    }

    @Test
    void moveRightWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsScreenPanel panel = new SettingsScreenPanel(screen -> {}, path -> {}, new ControlsHintBarWidget(), store);
        int before = panel.getSliderValue("Brightness");

        panel.moveRight();

        assertEquals(Math.min(10, before + 1), panel.getSliderValue("Brightness"));
    }

    @Test
    void confirmWorks(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        List<String> openedFolders = new ArrayList<>();
        SettingsScreenPanel panel = new SettingsScreenPanel(
            screen -> {},
            openedFolders::add,
            new ControlsHintBarWidget(),
            store
        );
        for (int i = 0; i < 6; i++) {
            panel.moveDown();
        }
        assertEquals("Open Game Folder", panel.getHighlightedItemName());

        panel.confirm();

        assertTrue(openedFolders.contains("game"));
    }

    @Test
    void backDefaultsToTitle(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        List<String> capturedScreen = new ArrayList<>();
        SettingsScreenPanel panel = new SettingsScreenPanel(
            screen -> capturedScreen.add(screen),
            path -> {},
            new ControlsHintBarWidget(),
            store
        );

        panel.back();

        assertTrue(capturedScreen.contains("title"));
    }

    @Test
    void backUsesSetBackTarget(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        List<String> capturedScreen = new ArrayList<>();
        SettingsScreenPanel panel = new SettingsScreenPanel(
            screen -> capturedScreen.add(screen),
            path -> {},
            new ControlsHintBarWidget(),
            store
        );

        panel.setBackTarget("pause");
        panel.back();

        assertTrue(capturedScreen.contains("pause"));
    }
}
