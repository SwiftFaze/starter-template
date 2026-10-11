package com.swiftfaze.veil.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsRepositoryTest {

    @Test
    void loadReturnsDefaultsWhenFileDoesNotExist(@TempDir Path tempDir) {
        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertNotNull(config);
        assertEquals(5, config.getBrightness());
        assertEquals("Windowed", config.getFullscreen());
    }

    @Test
    void loadReturnsDefaultsWhenFileIsEmpty(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        Files.writeString(settingsFile, "");

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertNotNull(config);
        assertEquals(5, config.getBrightness());
    }

    @Test
    void loadReturnsDefaultsWhenFileHasNullJson(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        Files.writeString(settingsFile, "null");

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertNotNull(config);
        assertEquals(5, config.getBrightness());
    }

    @Test
    void loadParsesValidJsonWithAllFields(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        String json = """
                {
                  "brightness": 8,
                  "fullscreen": "Fullscreen",
                  "font": "Serif",
                  "theme": "Midnight",
                  "volume": 2
                }
                """;
        Files.writeString(settingsFile, json);

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals(8, config.getBrightness());
        assertEquals("Fullscreen", config.getFullscreen());
        assertEquals("Serif", config.getFont());
        assertEquals("Midnight", config.getTheme());
        assertEquals(2, config.getVolume());
    }

    @Test
    void loadFallsBackToDefaultsForMissingTopLevelFields(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        String json = """
                {
                  "volume": 8
                }
                """;
        Files.writeString(settingsFile, json);

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals(5, config.getBrightness());
        assertEquals(8, config.getVolume());
    }

    @Test
    void loadReturnsBothDefaultsAndLoadedKeybinds(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        String json = """
                {
                  "keybinds": {
                    "Toggle inventory": "O"
                  }
                }
                """;
        Files.writeString(settingsFile, json);

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals("Up", config.getKeybinds().get("Move up"));
        assertEquals("O", config.getKeybinds().get("Toggle inventory"));
    }

    @Test
    void loadMergesKeybindDefaultsForMissingActions(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        String json = """
                {
                  "keybinds": {
                    "Move up": "W"
                  }
                }
                """;
        Files.writeString(settingsFile, json);

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals("W", config.getKeybinds().get("Move up"));
        assertEquals("Down", config.getKeybinds().get("Move down"));
        assertEquals("I", config.getKeybinds().get("Toggle inventory"));
    }

    @Test
    void loadReturnsDefaultsOnMalformedJson(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        Files.writeString(settingsFile, "{not valid json");

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertNotNull(config);
        assertEquals(5, config.getBrightness());
    }

    @Test
    void savePersistsConfigToJsonFile(@TempDir Path tempDir) throws Exception {
        SettingsConfig config = new SettingsConfig();
        config.setBrightness(8);
        config.setVolume(2);

        SettingsRepository repo = new SettingsRepository(tempDir);
        repo.save(config);

        Path settingsFile = tempDir.resolve("settings.json");
        String saved = Files.readString(settingsFile);
        assertTrue(saved.contains("\"brightness\": 8"), saved);
        assertTrue(saved.contains("\"volume\": 2"), saved);
    }

    @Test
    void roundTripPreservesAllFields(@TempDir Path tempDir) throws Exception {
        SettingsConfig original = new SettingsConfig();
        original.setBrightness(7);
        original.setFullscreen("Fullscreen");
        original.setFont("Serif");
        original.setTheme("Sunrise");
        original.setVolume(3);

        SettingsRepository repo = new SettingsRepository(tempDir);
        repo.save(original);
        SettingsConfig loaded = repo.load();

        assertEquals(7, loaded.getBrightness());
        assertEquals("Fullscreen", loaded.getFullscreen());
        assertEquals("Serif", loaded.getFont());
        assertEquals("Sunrise", loaded.getTheme());
        assertEquals(3, loaded.getVolume());
    }

    @Test
    void roundTripPreservesKeybinds(@TempDir Path tempDir) throws Exception {
        SettingsConfig original = new SettingsConfig();
        original.getKeybinds().put("Move up", "W");
        original.getKeybinds().put("Toggle inventory", "O");

        SettingsRepository repo = new SettingsRepository(tempDir);
        repo.save(original);
        SettingsConfig loaded = repo.load();

        assertEquals("W", loaded.getKeybinds().get("Move up"));
        assertEquals("O", loaded.getKeybinds().get("Toggle inventory"));
        assertEquals("Down", loaded.getKeybinds().get("Move down"));
    }

    @Test
    void loadReturnsZeroWindowSizeWhenFileDoesNotExist(@TempDir Path tempDir) {
        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals(0, config.getWindowWidth());
        assertEquals(0, config.getWindowHeight());
    }

    @Test
    void roundTripPreservesWindowSize(@TempDir Path tempDir) throws Exception {
        SettingsConfig original = new SettingsConfig();
        original.setWindowWidth(1024);
        original.setWindowHeight(768);

        SettingsRepository repo = new SettingsRepository(tempDir);
        repo.save(original);
        SettingsConfig loaded = repo.load();

        assertEquals(1024, loaded.getWindowWidth());
        assertEquals(768, loaded.getWindowHeight());
    }

    @Test
    void loadFallsBackToZeroWindowSizeWhenMissingFromFile(@TempDir Path tempDir) throws Exception {
        Path settingsFile = tempDir.resolve("settings.json");
        String json = """
                {
                  "volume": 8
                }
                """;
        Files.writeString(settingsFile, json);

        SettingsRepository repo = new SettingsRepository(tempDir);
        SettingsConfig config = repo.load();

        assertEquals(8, config.getVolume());
        assertEquals(0, config.getWindowWidth());
        assertEquals(0, config.getWindowHeight());
    }
}
