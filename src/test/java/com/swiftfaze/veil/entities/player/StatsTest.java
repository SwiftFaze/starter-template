package com.swiftfaze.veil.entities.player;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsTest {

    @Test
    void defaultStatsAreZero() {
        Stats stats = new Stats();

        assertEquals(0, stats.getStrength());
        assertEquals(0, stats.getDexterity());
        assertEquals(0, stats.getConstitution());
    }

    @Test
    void attackPowerUsesFormula() {
        Stats stats = new Stats();
        stats.setStrength(10);
        stats.setDexterity(6);

        assertEquals(23, stats.getAttackPower()); // 10*2 + 6/2 = 20 + 3 = 23
    }

    @Test
    void defenseUsesFormula() {
        Stats stats = new Stats();
        stats.setConstitution(20);
        stats.setDexterity(9);

        assertEquals(23, stats.getDefense()); // 20 + 9/3 = 20 + 3 = 23
    }

    // ========== Property-based tests ==========

    @Provide
    Arbitrary<Integer> nonNegativeStats() {
        // Cap at 1000 to represent realistic game stat values and avoid integer overflow.
        return Arbitraries.integers().between(0, 1000);
    }

    @Property
    void attackPowerAndDefenseAreNonNegative(
            @ForAll("nonNegativeStats") int strength,
            @ForAll("nonNegativeStats") int dexterity,
            @ForAll("nonNegativeStats") int constitution) {
        Stats stats = new Stats();
        stats.setStrength(strength);
        stats.setDexterity(dexterity);
        stats.setConstitution(constitution);

        // Both formulas should produce non-negative values for non-negative inputs.
        assertTrue(stats.getAttackPower() >= 0,
                "Attack power must be non-negative with strength=" + strength + ", dexterity=" + dexterity);
        assertTrue(stats.getDefense() >= 0,
                "Defense must be non-negative with constitution=" + constitution + ", dexterity=" + dexterity);
    }
}
