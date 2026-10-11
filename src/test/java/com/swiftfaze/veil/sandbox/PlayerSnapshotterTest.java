package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.Stats;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerSnapshotterTest {

    private static final String SLOT = "boss";
    private static final int SEED = 10;
    private static final int OTHER_SEED = 500;
    private static final int HOME_X = 4;
    private static final int HOME_Y = 7;

    private final Player player = new Player(HOME_X, HOME_Y);
    private final Player replacement = new Player(0, 0);
    private final PlayerSnapshotter snapshotter = new PlayerSnapshotter(() -> player);

    @Test
    void takeSnapshotReportsTheSavedSlot() {
        assertEquals("Snapshot boss saved", snapshotter.takeSnapshot(SLOT));
    }

    @Test
    void restoreWritesPositionBackToPlayer() {
        snapshotter.takeSnapshot(SLOT);
        player.setPosition(0, 0);

        Optional<String> result = snapshotter.restoreSnapshot(SLOT);

        assertEquals(Optional.of("Snapshot boss restored"), result);
        assertEquals(HOME_X, player.getX());
        assertEquals(HOME_Y, player.getY());
    }

    @Test
    void restoreWritesEveryStatFieldBackToPlayer() {
        fill(playerStats(), SEED);
        snapshotter.takeSnapshot(SLOT);
        fill(playerStats(), OTHER_SEED);

        snapshotter.restoreSnapshot(SLOT);

        assertFilled(playerStats(), SEED);
    }

    @Test
    void restoreReturnsEmptyForUnknownName() {
        assertFalse(snapshotter.restoreSnapshot("nosuchslot").isPresent());
    }

    @Test
    void resnapshottingAnExistingNameOverwritesIt() {
        fill(playerStats(), SEED);
        snapshotter.takeSnapshot(SLOT);
        fill(playerStats(), OTHER_SEED);
        snapshotter.takeSnapshot(SLOT);
        fill(playerStats(), SEED);

        snapshotter.restoreSnapshot(SLOT);

        assertFilled(playerStats(), OTHER_SEED);
    }

    @Test
    void restoreTargetsTheSupplierPlayerNotACachedReference() {
        Player[] current = {player};
        PlayerSnapshotter switching = new PlayerSnapshotter(() -> current[0]);
        fill(playerStats(), SEED);
        switching.takeSnapshot(SLOT);
        current[0] = replacement;

        switching.restoreSnapshot(SLOT);

        assertFilled(replacementStats(), SEED);
    }

    @Test
    void separateNamedSlotsAreKeptIndependently() {
        fill(playerStats(), SEED);
        snapshotter.takeSnapshot("strong");
        fill(playerStats(), OTHER_SEED);
        snapshotter.takeSnapshot("weak");

        snapshotter.restoreSnapshot("strong");

        assertFilled(playerStats(), SEED);
    }

    private Stats playerStats() {
        return player.getStats();
    }

    private Stats replacementStats() {
        return replacement.getStats();
    }

    /** Gives each of the ten editable fields a distinct value derived from the seed. */
    private static void fill(Stats stats, int seed) {
        stats.setStrength(seed);
        stats.setDexterity(seed + 1);
        stats.setConstitution(seed + 2);
        stats.setIntelligence(seed + 3);
        stats.setWisdom(seed + 4);
        stats.setLuck(seed + 5);
        stats.setMaxHp(seed + 6);
        stats.setMaxMana(seed + 7);
        stats.setCurrentHp(seed + 8);
        stats.setCurrentMana(seed + 9);
    }

    private static void assertFilled(Stats stats, int seed) {
        int[] actual = {stats.getStrength(), stats.getDexterity(), stats.getConstitution(),
                stats.getIntelligence(), stats.getWisdom(), stats.getLuck(), stats.getMaxHp(),
                stats.getMaxMana(), stats.getCurrentHp(), stats.getCurrentMana()};
        int[] expected = new int[actual.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = seed + i;
        }
        assertArrayEquals(expected, actual);
    }
}
