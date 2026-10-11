package com.swiftfaze.veil.world;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.LongSupplier;

/** Picks the seed for each new game: the {@link #PROPERTY} override, else a fresh random seed. */
public final class WorldSeeds {

    /** System property that fixes the world seed, e.g. {@code -Dveil.worldSeed=12345}. */
    public static final String PROPERTY = "veil.worldSeed";

    private static final Logger logger = LoggerFactory.getLogger(WorldSeeds.class);

    private WorldSeeds() {
    }

    /** A seed source reading {@link #PROPERTY} on every call, drawing from {@code freshSeeds} when unset or malformed. */
    public static LongSupplier fromSystemProperty(LongSupplier freshSeeds) {
        return () -> resolve(System.getProperty(PROPERTY), freshSeeds);
    }

    static long resolve(@Nullable String override, LongSupplier freshSeeds) {
        if (override == null) {
            return freshSeeds.getAsLong();
        }
        try {
            return Long.parseLong(override.trim());
        } catch (NumberFormatException e) {
            logger.warn("Ignoring malformed {}=\"{}\"; generating the world from a random seed", PROPERTY, override);
            return freshSeeds.getAsLong();
        }
    }
}
