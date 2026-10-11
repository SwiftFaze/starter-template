package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldRegeneratorTest {

    private static final long DRAWN_SEED = 777L;
    private static final long REGENERATE_SEED = 12_345L;

    @Test
    void noGameDrawsFreshSeedsFromItsSource() {
        WorldRegenerator regenerator = WorldRegenerator.noGame(() -> DRAWN_SEED);

        assertEquals(DRAWN_SEED, regenerator.freshSeed());
    }

    @Test
    void noGameReportsThatNoGameIsRunning() {
        WorldRegenerator regenerator = WorldRegenerator.noGame(() -> 1L);

        assertEquals(Optional.of(WorldRegenerator.NO_GAME_MESSAGE), regenerator.regenerate(REGENERATE_SEED));
    }
}
