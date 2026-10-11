package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.Stats;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Manages named snapshots of a live player's position and editable Stats fields, using a
 * {@link Supplier} to always read the currently-running player (the same pattern as
 * {@link PlayerFieldMutator}).
 */
public class PlayerSnapshotter implements DevConsoleSnapshotter {

    private final Supplier<Player> playerSupplier;
    private final Map<String, Snapshot> snapshots = new HashMap<>();

    public PlayerSnapshotter(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    @Override
    public String takeSnapshot(String name) {
        Player player = playerSupplier.get();
        snapshots.put(name, Snapshot.capture(player.getX(), player.getY(), currentStats()));
        return "Snapshot " + name + " saved";
    }

    @Override
    public Optional<String> restoreSnapshot(String name) {
        Snapshot snapshot = snapshots.get(name);
        if (snapshot == null) {
            return Optional.empty();
        }
        Player player = playerSupplier.get();
        player.setPosition(snapshot.x(), snapshot.y());
        snapshot.applyTo(currentStats());
        return Optional.of("Snapshot " + name + " restored");
    }

    private Stats currentStats() {
        return playerSupplier.get().getStats();
    }

    /**
     * Immutable snapshot of player position and stats.
     */
    private record Snapshot(int x, int y, int strength, int dexterity, int constitution,
                            int intelligence, int wisdom, int luck, int maxHp, int maxMana,
                            int currentHp, int currentMana) {

        static Snapshot capture(int x, int y, Stats stats) {
            return new Snapshot(x, y,
                    stats.getStrength(), stats.getDexterity(), stats.getConstitution(),
                    stats.getIntelligence(), stats.getWisdom(), stats.getLuck(),
                    stats.getMaxHp(), stats.getMaxMana(), stats.getCurrentHp(), stats.getCurrentMana());
        }

        void applyTo(Stats stats) {
            stats.setStrength(strength);
            stats.setDexterity(dexterity);
            stats.setConstitution(constitution);
            stats.setIntelligence(intelligence);
            stats.setWisdom(wisdom);
            stats.setLuck(luck);
            stats.setMaxHp(maxHp);
            stats.setMaxMana(maxMana);
            stats.setCurrentHp(currentHp);
            stats.setCurrentMana(currentMana);
        }
    }
}
