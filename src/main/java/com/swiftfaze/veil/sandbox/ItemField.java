package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;

import java.util.Arrays;
import java.util.Optional;

/**
 * The two {@link Item.BaseDamage} fields addressable by the dev console's set/add/subtract verbs.
 * Other fields (glyph, type, slot, effects) are deliberately not here -
 * {@link ItemFieldMutator#apply} rejects any other token as an unknown field.
 */
enum ItemField {
    MIN_DAMAGE("mindmg", "Base Damage (Min)"),
    MAX_DAMAGE("maxdmg", "Base Damage (Max)");

    private final String token;
    private final String displayName;

    ItemField(String token, String displayName) {
        this.token = token;
        this.displayName = displayName;
    }

    static Optional<ItemField> fromToken(String token) {
        return Arrays.stream(values()).filter(field -> field.token.equals(token)).findFirst();
    }

    /** The field whose detail-panel row is labelled {@code displayName}, e.g. "Base Damage (Max)". */
    static Optional<ItemField> fromDisplayName(String displayName) {
        return Arrays.stream(values()).filter(field -> field.displayName.equals(displayName)).findFirst();
    }

    String displayName() {
        return displayName;
    }

    String token() {
        return token;
    }

    /** This field's current value within {@code damage}. */
    int valueIn(Item.BaseDamage damage) {
        return switch (this) {
            case MIN_DAMAGE -> damage.min();
            case MAX_DAMAGE -> damage.max();
        };
    }

    /** The minimum damage after setting this field to {@code value} within {@code damage}. */
    int minWith(Item.BaseDamage damage, int value) {
        return this == MIN_DAMAGE ? value : damage.min();
    }

    /** The maximum damage after setting this field to {@code value} within {@code damage}. */
    int maxWith(Item.BaseDamage damage, int value) {
        return this == MAX_DAMAGE ? value : damage.max();
    }
}
