package com.swiftfaze.veil.sandbox;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * How the dev console's `regen` verb reaches the running game. The sandbox cannot construct
 * engine classes, so Main supplies the implementation that talks to the game panel.
 */
public interface WorldRegenerator {

    /** Shown by `regen` when no game is attached, e.g. in the class/stats sandbox. */
    String NO_GAME_MESSAGE = "No game is running to regenerate";

    /** A fresh random seed, drawn for a bare `regen`. */
    long freshSeed();

    /** Regenerates the running world from {@code seed}. Empty on success, else the error to show. */
    Optional<String> regenerate(long seed);

    /** A regenerator for a console with no game attached: every regeneration reports {@link #NO_GAME_MESSAGE}. */
    static WorldRegenerator noGame(LongSupplier freshSeeds) {
        return new WorldRegenerator() {
            @Override
            public long freshSeed() {
                return freshSeeds.getAsLong();
            }

            @Override
            public Optional<String> regenerate(long seed) {
                return Optional.of(NO_GAME_MESSAGE);
            }
        };
    }
}
