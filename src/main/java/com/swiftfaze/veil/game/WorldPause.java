package com.swiftfaze.veil.game;

import java.util.HashSet;
import java.util.Set;

/**
 * The one source of truth for "the world is paused": the world is paused while any
 * screen id is held. Screens hold their id when they open and release it when they
 * close, so closing one screen cannot unpause the world while another is still open.
 */
public final class WorldPause {

    public static final String INVENTORY = "inventory";
    public static final String CODEX = "codex";
    public static final String PAUSE_MENU = "pause";
    public static final String CHEST = "chest";
    public static final String DIALOGUE = "dialogue";

    private final Set<String> heldBy = new HashSet<>();

    public void hold(String screenId) {
        heldBy.add(screenId);
    }

    public void release(String screenId) {
        heldBy.remove(screenId);
    }

    public boolean isPaused() {
        return !heldBy.isEmpty();
    }

    public boolean isHeldBy(String screenId) {
        return heldBy.contains(screenId);
    }

    public void releaseAll() {
        heldBy.clear();
    }
}
