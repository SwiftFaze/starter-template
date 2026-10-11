package com.swiftfaze.veil.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldSeedsTest {

    private static final long FRESH = 99L;

    @Test
    void anUnsetOverrideDrawsAFreshSeed() {
        assertEquals(FRESH, WorldSeeds.resolve(null, () -> FRESH));
    }

    @Test
    void aNumericOverrideIsUsedInPlaceOfTheFreshSeed() {
        assertEquals(-7L, WorldSeeds.resolve("-7", () -> FRESH));
    }

    @Test
    void surroundingWhitespaceIsIgnored() {
        assertEquals(42L, WorldSeeds.resolve("  42 ", () -> FRESH));
    }

    @Test
    void aMalformedOverrideFallsBackToTheFreshSeed() {
        assertEquals(FRESH, WorldSeeds.resolve("not-a-number", () -> FRESH));
    }
}
