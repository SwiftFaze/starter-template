package com.swiftfaze.veil.sandbox;

import java.util.Optional;

/**
 * Captures and restores in-memory snapshots of entity state (e.g. position and stats) via the
 * dev-console command bar's snapshot/restore verbs. Snapshots are named, free-form tokens,
 * unbounded in count; re-snapshotting an existing name overwrites it. Snapshots live for the
 * dev-console session only.
 */
public interface DevConsoleSnapshotter {

    /**
     * Captures the current state of the entity into the named slot, overwriting any existing
     * snapshot with that name. Returns a success message to be written to the transcript.
     */
    String takeSnapshot(String name);

    /**
     * Restores the entity's state from the named slot, if it exists. Returns either a success
     * message or an error message (slot not found) to be written to the transcript.
     */
    Optional<String> restoreSnapshot(String name);
}
