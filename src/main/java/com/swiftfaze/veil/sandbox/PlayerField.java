package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.player.Stats;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * The ten {@link Stats} fields addressable by the dev console's set/add/subtract verbs, keyed by
 * the abbreviation token issue #170's clarification pass settled on. Class and the derived Attack
 * Power/Defense rows are deliberately not here - {@link PlayerFieldMutator#apply} rejects any
 * other token as an unknown field.
 */
enum PlayerField {
    STRENGTH("str", "Strength", 0, /* hasClassDefault= */ true),
    DEXTERITY("dex", "Dexterity", 0, /* hasClassDefault= */ true),
    CONSTITUTION("con", "Constitution", 0, /* hasClassDefault= */ true),
    INTELLIGENCE("int", "Intelligence", 0, /* hasClassDefault= */ true),
    WISDOM("wis", "Wisdom", 0, /* hasClassDefault= */ true),
    LUCK("luck", "Luck", 0, /* hasClassDefault= */ true),
    MAX_HP("maxhp", "Max HP", 1, /* hasClassDefault= */ true),
    MAX_MANA("maxmana", "Max Mana", 1, /* hasClassDefault= */ true),
    CURRENT_HP("hp", "Current HP", 0, /* hasClassDefault= */ false),
    CURRENT_MANA("mana", "Current Mana", 0, /* hasClassDefault= */ false);

    // Shared across all constants (not per-instance state), so ImmutableEnumChecker's
    // per-constant immutability check doesn't apply - only the enum's own instance
    // fields (token/displayName/floor/hasClassDefault, all primitives/String) do.
    private static final Map<PlayerField, ToIntFunction<Stats>> GETTERS = new EnumMap<>(PlayerField.class);
    private static final Map<PlayerField, ObjIntConsumer<Stats>> SETTERS = new EnumMap<>(PlayerField.class);

    static {
        GETTERS.put(STRENGTH, Stats::getStrength);
        GETTERS.put(DEXTERITY, Stats::getDexterity);
        GETTERS.put(CONSTITUTION, Stats::getConstitution);
        GETTERS.put(INTELLIGENCE, Stats::getIntelligence);
        GETTERS.put(WISDOM, Stats::getWisdom);
        GETTERS.put(LUCK, Stats::getLuck);
        GETTERS.put(MAX_HP, Stats::getMaxHp);
        GETTERS.put(MAX_MANA, Stats::getMaxMana);
        GETTERS.put(CURRENT_HP, Stats::getCurrentHp);
        GETTERS.put(CURRENT_MANA, Stats::getCurrentMana);

        SETTERS.put(STRENGTH, Stats::setStrength);
        SETTERS.put(DEXTERITY, Stats::setDexterity);
        SETTERS.put(CONSTITUTION, Stats::setConstitution);
        SETTERS.put(INTELLIGENCE, Stats::setIntelligence);
        SETTERS.put(WISDOM, Stats::setWisdom);
        SETTERS.put(LUCK, Stats::setLuck);
        SETTERS.put(MAX_HP, Stats::setMaxHp);
        SETTERS.put(MAX_MANA, Stats::setMaxMana);
        SETTERS.put(CURRENT_HP, Stats::setCurrentHp);
        SETTERS.put(CURRENT_MANA, Stats::setCurrentMana);
    }

    private final String token;
    private final String displayName;
    private final int floor;
    private final boolean hasClassDefault;

    PlayerField(String token, String displayName, int floor, boolean hasClassDefault) {
        this.token = token;
        this.displayName = displayName;
        this.floor = floor;
        this.hasClassDefault = hasClassDefault;
    }

    static Optional<PlayerField> fromToken(String token) {
        return Arrays.stream(values()).filter(field -> field.token.equals(token)).findFirst();
    }

    String displayName() {
        return displayName;
    }

    String token() {
        return token;
    }

    int floor() {
        return floor;
    }

    boolean hasClassDefault() {
        return hasClassDefault;
    }

    int get(Stats stats) {
        return GETTERS.get(this).applyAsInt(stats);
    }

    void set(Stats stats, int value) {
        SETTERS.get(this).accept(stats, value);
    }
}
