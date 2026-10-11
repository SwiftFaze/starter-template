package com.swiftfaze.veil.config;

import java.nio.file.Path;

/**
 * Single collaborator for both SettingsScreenPanel and SettingsKeybindsPanel.
 * Ensures they share the same SettingsConfig instance, preventing stale-write
 * bugs where one screen's save could clobber another's already-persisted changes.
 * Passed by reference to both panels at construction time.
 */
public class SettingsStore {
    private final SettingsConfig config;
    private final SettingsRepository repository;

    public SettingsStore(Path installDir) {
        this.repository = new SettingsRepository(installDir);
        this.config = repository.load();
    }

    public SettingsConfig config() {
        return config;
    }

    public void persist() {
        repository.save(config);
    }
}
