package com.swiftfaze.veil.sandbox;

import javax.swing.JComponent;
import java.util.List;
import java.util.Optional;

/**
 * A source of individually-searchable {@link DevConsoleEntry} items the dev
 * console's top-level results table lists directly (e.g. every player class,
 * not a single "Classes" row). A future provider (items, monsters, quests,
 * ...) only needs to implement this and be registered - no changes to the
 * search/results/keybinding shell in {@link DevConsolePanel}.
 */
public interface DevConsoleProvider {

    List<DevConsoleEntry> entries();

    /**
     * Opens the detail panel for one entry, pre-selected to it (e.g. jumping
     * straight to a specific class's stats, not a generic "browse everything"
     * view).
     *
     * @param id the {@link DevConsoleEntry#id()} - fully-qualified id (e.g. "core:mage")
     */
    JComponent createPanel(String id);

    /**
     * Opt-in hook for a provider whose entries support live field mutation via the command bar's
     * set/add/subtract verbs. Empty by default - overridden by {@link PlayerSandboxProvider}
     * and {@link ItemSandboxProvider}.
     *
     * @param id the {@link DevConsoleEntry#id()} the mutation targets
     */
    default Optional<DevConsoleFieldMutator> fieldMutator(String id) {
        return Optional.empty();
    }

    /**
     * Opt-in hook for a provider whose entries support snapshot/restore of position and stats
     * via the command bar's snapshot/restore verbs. Empty by default - only
     * {@link PlayerSandboxProvider} overrides it for v1.
     *
     * @param id the {@link DevConsoleEntry#id()} the snapshots target
     */
    default Optional<DevConsoleSnapshotter> snapshotter(String id) {
        return Optional.empty();
    }

    /**
     * Opt-in hook for providers that cache mod-loaded data and need to refresh from disk
     * when {@link DevConsoleModel#reload()} is called. No-op by default. Providers that
     * already re-read mods/ on every {@link #entries()} call need not override this.
     * May throw {@link com.swiftfaze.veil.exceptions.ModLoadException} if the reload fails.
     */
    default void reload() {
        // Intentionally empty: providers without cached mod data have nothing to refresh.
    }
}
