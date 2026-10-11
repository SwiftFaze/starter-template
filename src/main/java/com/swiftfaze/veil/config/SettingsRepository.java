package com.swiftfaze.veil.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes settings.json from/to the install directory.
 * Handles missing files, corrupt JSON, and partial configs by falling
 * back to defaults gracefully.
 */
public class SettingsRepository {
    private static final Logger logger = LoggerFactory.getLogger(SettingsRepository.class);
    private final Path installDir;

    public SettingsRepository(Path installDir) {
        this.installDir = installDir;
    }

    public SettingsConfig load() {
        Path settingsFile = installDir.resolve("settings.json");

        if (!Files.exists(settingsFile)) {
            return new SettingsConfig();
        }

        try (var reader = Files.newBufferedReader(settingsFile)) {
            SettingsConfig loaded = new Gson().fromJson(reader, SettingsConfig.class);
            if (loaded == null) {
                return new SettingsConfig();
            }
            mergeKeybindDefaults(loaded);
            return loaded;
        } catch (Exception e) {
            logger.warn("Failed to load settings.json, using defaults", e);
            return new SettingsConfig();
        }
    }

    /**
     * Merges in missing keybind defaults. Gson's default deserialization
     * replaces nested Maps entirely; this fills in any missing actions
     * from the hardcoded defaults.
     */
    private static void mergeKeybindDefaults(SettingsConfig loaded) {
        SettingsConfig defaults = new SettingsConfig();
        for (String action : defaults.getKeybinds().keySet()) {
            if (!loaded.getKeybinds().containsKey(action)) {
                loaded.getKeybinds().put(action, defaults.getKeybinds().get(action));
            }
        }
    }

    public void save(SettingsConfig config) {
        Path settingsFile = installDir.resolve("settings.json");

        try (var writer = Files.newBufferedWriter(settingsFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(config, writer);
        } catch (IOException e) {
            logger.warn("Failed to save settings.json", e);
        }
    }
}
