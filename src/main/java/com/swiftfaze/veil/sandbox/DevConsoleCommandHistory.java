package com.swiftfaze.veil.sandbox;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory command history for the dev console. Implements shell-style Up/Down navigation:
 * Up walks backward through history, most recent first; Down walks forward; pressing Down past
 * the newest entry restores whatever draft was being typed before history navigation started.
 * History records only on Enter for non-empty, non-duplicate-adjacent commands (shell-style
 * adjacent dedup only, no global move-to-top reordering).
 */
public class DevConsoleCommandHistory {

    private final List<String> entries = new ArrayList<>();
    private int cursor;
    private String savedDraft = "";

    public DevConsoleCommandHistory() {
        this.cursor = 0;
    }

    /**
     * Record a command in history if it's non-empty and differs from the immediately
     * previous entry (adjacent dedup only).
     */
    public void record(String command) {
        String trimmed = command.strip();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!entries.isEmpty() && entries.get(entries.size() - 1).equals(trimmed)) {
            return;
        }
        entries.add(trimmed);
        cursor = entries.size();
        savedDraft = "";
    }

    /**
     * Navigate backward through history, most recent first. On the first Up from the live
     * state (cursor == entries.size()), captures the current draft so Down past the newest
     * entry can restore it.
     *
     * @param currentDraft the text currently in the command field (before Up is pressed)
     * @return the history entry at the new cursor position
     */
    public String navigateUp(String currentDraft) {
        if (entries.isEmpty()) {
            return currentDraft;
        }
        if (cursor == entries.size()) {
            savedDraft = currentDraft;
        }
        if (cursor > 0) {
            cursor--;
        }
        return entries.get(cursor);
    }

    /**
     * Navigate forward through history. Returns null if not currently navigating (cursor ==
     * entries.size()), a signal to the caller to leave the field untouched. Otherwise returns
     * the next entry, or the saved draft if cursor reaches entries.size().
     *
     * @return the history entry at the new cursor position, or the saved draft when pressing
     *         Down past the newest entry, or null if not navigating
     */
    public @Nullable String navigateDown() {
        if (entries.isEmpty() || cursor == entries.size()) {
            return null;
        }
        cursor++;
        return cursor == entries.size() ? savedDraft : entries.get(cursor);
    }

    /**
     * Returns the total number of entries in history.
     *
     * @return the total number of entries in history
     */
    public int size() {
        return entries.size();
    }
}
