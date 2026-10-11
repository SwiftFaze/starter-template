package com.swiftfaze.veil.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldPauseTest {

    @Test
    void startsUnpaused() {
        assertFalse(new WorldPause().isPaused());
    }

    @Test
    void holdingAScreenPausesTheWorld() {
        WorldPause pause = new WorldPause();

        pause.hold(WorldPause.INVENTORY);

        assertTrue(pause.isPaused());
    }

    @Test
    void releasingTheOnlyHeldScreenResumesTheWorld() {
        WorldPause pause = new WorldPause();
        pause.hold(WorldPause.INVENTORY);

        pause.release(WorldPause.INVENTORY);

        assertFalse(pause.isPaused());
    }

    @Test
    void worldStaysPausedWhileAnotherScreenIsStillHeld() {
        WorldPause pause = new WorldPause();
        pause.hold(WorldPause.INVENTORY);
        pause.hold("codex");

        pause.release(WorldPause.INVENTORY);
        assertTrue(pause.isPaused());

        pause.release("codex");
        assertFalse(pause.isPaused());
    }

    @Test
    void releasingAScreenThatWasNeverHeldIsHarmless() {
        WorldPause pause = new WorldPause();
        pause.hold("pause");

        pause.release("codex");

        assertTrue(pause.isPaused());
    }

    @Test
    void isHeldByReportsOnlyTheScreenThatHoldsIt() {
        WorldPause pause = new WorldPause();
        pause.hold(WorldPause.DIALOGUE);

        assertTrue(pause.isHeldBy(WorldPause.DIALOGUE));
        assertFalse(pause.isHeldBy(WorldPause.CHEST));
    }

    @Test
    void clearResumesTheWorldWhateverIsHeld() {
        WorldPause pause = new WorldPause();
        pause.hold(WorldPause.INVENTORY);
        pause.hold("pause");

        pause.releaseAll();

        assertFalse(pause.isPaused());
    }
}
