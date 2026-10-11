package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

/**
 * Applies the dev-console command bar's set/add/subtract verbs to the live running player's
 * {@link Stats}, reusing the same field floors/clamping rules as {@link PlayerDetailPanel}'s
 * arm+Left/Right editing: attributes floor at 0, Max HP/Max Mana floor at 1 and clamp Current
 * HP/Mana down when lowered below it, Current HP/Current Mana floor at 0. Uses a
 * {@link Supplier}, not a direct {@link Player} reference, for the same reason
 * {@link PlayerSandboxProvider} does - always mutate whichever player is currently live.
 */
public class PlayerFieldMutator implements DevConsoleFieldMutator {

    private static final String DEFAULT_VALUE_TOKEN = "default";

    private final Supplier<Player> playerSupplier;

    public PlayerFieldMutator(Supplier<Player> playerSupplier) {
        this.playerSupplier = playerSupplier;
    }

    @Override
    public DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue) {
        Optional<PlayerField> field = PlayerField.fromToken(fieldToken);
        if (field.isEmpty()) {
            return new DevConsoleMutationResult.Failure(fieldToken);
        }

        Stats stats = playerSupplier.get().getPlayerInfo().getStats();
        OptionalInt resolvedValue = resolveValue(field.get(), verb, rawValue, stats);
        if (resolvedValue.isEmpty()) {
            return new DevConsoleMutationResult.Failure(rawValue);
        }

        int newValue = applyClamped(field.get(), stats, resolvedValue.getAsInt());
        return new DevConsoleMutationResult.Success(field.get().displayName(), newValue);
    }

    private OptionalInt resolveValue(PlayerField field, DevConsoleMutationVerb verb, String rawValue, Stats stats) {
        if (DEFAULT_VALUE_TOKEN.equals(rawValue)) {
            return verb == DevConsoleMutationVerb.SET ? defaultValue(field) : OptionalInt.empty();
        }
        OptionalInt parsed = parseInt(rawValue);
        if (parsed.isEmpty() || (verb != DevConsoleMutationVerb.SET && parsed.getAsInt() < 0)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(combine(field, verb, parsed.getAsInt(), stats));
    }

    private static int combine(PlayerField field, DevConsoleMutationVerb verb, int value, Stats stats) {
        int current = field.get(stats);
        return switch (verb) {
            case SET -> value;
            case ADD -> current + value;
            case SUBTRACT -> current - value;
        };
    }

    private OptionalInt defaultValue(PlayerField field) {
        if (!field.hasClassDefault()) {
            return OptionalInt.empty();
        }
        PlayerClass playerClass = playerSupplier.get().getPlayerInfo().getPlayerClass();
        Stats baseStats = new Stats();
        playerClass.applyStatsAtLevel(baseStats, 0);
        return OptionalInt.of(field.get(baseStats));
    }

    private static int applyClamped(PlayerField field, Stats stats, int rawNewValue) {
        int floored = Math.max(field.floor(), rawNewValue);
        field.set(stats, floored);
        clampDependentCurrent(field, stats, floored);
        return floored;
    }

    private static void clampDependentCurrent(PlayerField field, Stats stats, int newMax) {
        if (field == PlayerField.MAX_HP && stats.getCurrentHp() > newMax) {
            stats.setCurrentHp(newMax);
        } else if (field == PlayerField.MAX_MANA && stats.getCurrentMana() > newMax) {
            stats.setCurrentMana(newMax);
        }
    }

    private static OptionalInt parseInt(String rawValue) {
        try {
            return OptionalInt.of(Integer.parseInt(rawValue));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    @Override
    public List<String> fieldTokens() {
        return Arrays.stream(PlayerField.values()).map(PlayerField::token).toList();
    }

    @Override
    public boolean hasClassDefault(String fieldToken) {
        return PlayerField.fromToken(fieldToken).map(PlayerField::hasClassDefault).orElse(false);
    }
}
