package com.swiftfaze.veil.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SettingsConfigTest {

    @Test
    void defaultConstructorInitializesWithHardcodedDefaults() {
        SettingsConfig config = new SettingsConfig();

        assertEquals(5, config.getBrightness());
        assertEquals("Windowed", config.getFullscreen());
        assertEquals("Monospaced", config.getFont());
        assertEquals("Default", config.getTheme());
        assertEquals(5, config.getVolume());
        assertNotNull(config.getKeybinds());
    }

    @Test
    void defaultKeybindsMapHasAllFiveActions() {
        SettingsConfig config = new SettingsConfig();

        assertEquals("Up", config.getKeybinds().get("Move up"));
        assertEquals("Down", config.getKeybinds().get("Move down"));
        assertEquals("Left", config.getKeybinds().get("Move left"));
        assertEquals("Right", config.getKeybinds().get("Move right"));
        assertEquals("I", config.getKeybinds().get("Toggle inventory"));
    }

    @Test
    void brightnessCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        config.setBrightness(8);

        assertEquals(8, config.getBrightness());
    }

    @Test
    void fullscreenCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        config.setFullscreen("Fullscreen");

        assertEquals("Fullscreen", config.getFullscreen());
    }

    @Test
    void fontCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        config.setFont("Serif");

        assertEquals("Serif", config.getFont());
    }

    @Test
    void themeCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        config.setTheme("Midnight");

        assertEquals("Midnight", config.getTheme());
    }

    @Test
    void volumeCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        config.setVolume(2);

        assertEquals(2, config.getVolume());
    }

    @Test
    void keybindsCanBeSetAndRetrieved() {
        SettingsConfig config = new SettingsConfig();
        Map<String, String> newBindings = Map.of("Move up", "W", "Toggle inventory", "O");
        config.setKeybinds(newBindings);

        assertEquals("W", config.getKeybinds().get("Move up"));
        assertEquals("O", config.getKeybinds().get("Toggle inventory"));
    }

    @Test
    void setKeybindsMakesDefensiveCopyOfInput() {
        SettingsConfig config = new SettingsConfig();
        Map<String, String> original = new LinkedHashMap<>(Map.of("Move up", "W"));
        config.setKeybinds(original);

        original.put("Move up", "A");

        assertEquals("W", config.getKeybinds().get("Move up"));
    }

    @Test
    void resetToDefaultsResetsMainScreenSettingsOnly() {
        SettingsConfig config = new SettingsConfig();
        config.setBrightness(8);
        config.setFullscreen("Fullscreen");
        config.setVolume(2);
        config.setKeybinds(Map.of("Move up", "W"));

        config.resetToDefaults();

        assertEquals(5, config.getBrightness());
        assertEquals("Windowed", config.getFullscreen());
        assertEquals("Monospaced", config.getFont());
        assertEquals("Default", config.getTheme());
        assertEquals(5, config.getVolume());
        assertEquals("W", config.getKeybinds().get("Move up"));
    }
}
