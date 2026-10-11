package com.swiftfaze.veil.world;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The expected values are the port's own output, pinned so any change to the noise math, the
 * seeded shuffle or the table indexing shows up: the terrain a seed gives is part of the product.
 */
class PerlinNoiseTest {

    private static final double TOLERANCE = 1e-12;

    @ParameterizedTest(name = "seed {0} at ({1}, {2}) is {3}")
    @CsvSource({
        "0,     0.5,   0.5,   0.06865822532492256",
        "0,     37.25, 12.75, -0.24277661816647017",
        "0,     5.0,   9.0,   0.3559690290841851",
        "0,     100.0, 100.0, -0.09774075196938774",
        "0,     -8.5,  260.3, -0.16138884096943962",
        "0,     300.1, -2.0,  0.28553060456699725",
        "12345, 0.5,   0.5,   -0.09149471416934947",
        "12345, 37.25, 12.75, -0.19430359858246113",
        "12345, 300.1, -2.0,  -0.7617595164835453",
        "12345, 100.0, 100.0, -0.28109278377620195",
        "12345, 5.0,   9.0,   -0.24494237115534126",
    })
    void terrainIsPinned(long seed, double x, double y, double expected) {
        assertEquals(expected, new PerlinNoise(seed).noise(x, y), TOLERANCE);
    }

    @Test
    void differentSeedsGiveDifferentTerrain() {
        assertNotEquals(new PerlinNoise(0L).noise(5.0, 9.0), new PerlinNoise(1L).noise(5.0, 9.0));
    }

    @Test
    void theSameSeedGivesTheSameField() {
        assertEquals(new PerlinNoise(7L).noise(42.5, 17.5), new PerlinNoise(7L).noise(42.5, 17.5));
    }
}
