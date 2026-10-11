package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Applies the dev-console command bar's set/add/subtract verbs to one item's base damage fields.
 * Values never go below {@link #FLOOR}, and an edit that would leave min above max is rejected.
 * The mutator reads the item through a supplier and hands the edited immutable copy to a consumer,
 * so it knows nothing about where the provider keeps items. It also keeps the original mod-loaded
 * damage, which the {@code default} keyword restores.
 */
public class ItemFieldMutator implements DevConsoleFieldMutator {

    private static final String DEFAULT_VALUE_TOKEN = "default";
    private static final int FLOOR = 0;

    private final Item.BaseDamage originalBaseDamage;
    private final Supplier<Item> currentItem;
    private final Consumer<Item> itemReplacer;

    public ItemFieldMutator(Item.BaseDamage originalBaseDamage, Supplier<Item> currentItem,
                            Consumer<Item> itemReplacer) {
        this.originalBaseDamage = originalBaseDamage;
        this.currentItem = currentItem;
        this.itemReplacer = itemReplacer;
    }

    @Override
    public DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue) {
        Optional<ItemField> field = ItemField.fromToken(fieldToken);
        if (field.isEmpty()) {
            return new DevConsoleMutationResult.Failure(fieldToken);
        }

        Item item = currentItem.get();
        Item.BaseDamage currentDamage = item.getBaseDamage();
        OptionalInt resolvedValue = resolveValue(field.get(), verb, rawValue, currentDamage);
        if (resolvedValue.isEmpty()) {
            return new DevConsoleMutationResult.Failure(rawValue);
        }

        int newValue = Math.max(FLOOR, resolvedValue.getAsInt());
        int newMin = field.get().minWith(currentDamage, newValue);
        int newMax = field.get().maxWith(currentDamage, newValue);
        if (newMin > newMax) {
            return new DevConsoleMutationResult.Failure(rawValue);
        }

        itemReplacer.accept(item.withBaseDamage(newMin, newMax));
        return new DevConsoleMutationResult.Success(field.get().displayName(), newValue);
    }

    private OptionalInt resolveValue(ItemField field, DevConsoleMutationVerb verb, String rawValue,
                                     Item.BaseDamage currentDamage) {
        if (DEFAULT_VALUE_TOKEN.equals(rawValue)) {
            return verb == DevConsoleMutationVerb.SET
                    ? OptionalInt.of(field.valueIn(originalBaseDamage))
                    : OptionalInt.empty();
        }
        OptionalInt parsed = parseInt(rawValue);
        if (parsed.isEmpty() || (verb != DevConsoleMutationVerb.SET && parsed.getAsInt() < 0)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(combine(verb, field.valueIn(currentDamage), parsed.getAsInt()));
    }

    private static int combine(DevConsoleMutationVerb verb, int current, int value) {
        return switch (verb) {
            case SET -> value;
            case ADD -> current + value;
            case SUBTRACT -> current - value;
        };
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
        return List.of(ItemField.MIN_DAMAGE.token(), ItemField.MAX_DAMAGE.token());
    }

    @Override
    public boolean hasClassDefault(String fieldToken) {
        return ItemField.fromToken(fieldToken).isPresent();
    }
}
