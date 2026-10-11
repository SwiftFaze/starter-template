package com.swiftfaze.veil.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SettingsStoreTest {

    @Test
    void constructorLoadsConfigFromRepository(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);

        assertNotNull(store.config());
        assertEquals(5, store.config().getBrightness());
    }

    @Test
    void configReturnsTheSameInstanceOnMultipleCalls(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);

        SettingsConfig first = store.config();
        SettingsConfig second = store.config();

        assertSame(first, second);
    }

    @Test
    void persistWritesTheCurrentConfigState(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        store.config().setVolume(8);

        store.persist();

        SettingsStore reloaded = new SettingsStore(tempDir);
        assertEquals(8, reloaded.config().getVolume());
    }

    @Test
    void bothPanelsShareTheSameConfigInstance(@TempDir Path tempDir) {
        SettingsStore store = new SettingsStore(tempDir);
        SettingsConfig configRef1 = store.config();

        configRef1.setVolume(7);
        SettingsConfig configRef2 = store.config();

        assertEquals(7, configRef2.getVolume());
        assertSame(configRef1, configRef2);
    }
}
