package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemFieldMutatorTest {

    private static final String MIN = ItemField.MIN_DAMAGE.token();
    private static final String MAX = ItemField.MAX_DAMAGE.token();
    private static final String MIN_NAME = ItemField.MIN_DAMAGE.displayName();
    private static final String MAX_NAME = ItemField.MAX_DAMAGE.displayName();
    private static final String DEFAULT = "default";
    private static final Item.BaseDamage ORIGINAL = new Item.BaseDamage(4, 9);

    private AtomicReference<Item> stored;
    private ItemFieldMutator mutator;

    @BeforeEach
    void setUp() {
        stored = new AtomicReference<>(new Item(
                "core:iron_sword",
                "Iron Sword",
                new Item.ItemAttributes('/', "weapon", "hand", ORIGINAL, List.of())));
        mutator = new ItemFieldMutator(ORIGINAL, stored::get, stored::set);
    }

    private Item.BaseDamage storedDamage() {
        Item item = stored.get();
        return item.getBaseDamage();
    }

    @Test
    void setChangesMaxDamageAndReturnsSuccess() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MAX, "12");

        assertEquals(new DevConsoleMutationResult.Success(MAX_NAME, 12), result);
        assertEquals(new Item.BaseDamage(4, 12), storedDamage());
    }

    @Test
    void setChangesMinDamageAndReturnsSuccess() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MIN, "0");

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 0), result);
        assertEquals(new Item.BaseDamage(0, 9), storedDamage());
    }

    @Test
    void addIncreasesMaxDamage() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, MAX, "3");

        assertEquals(new DevConsoleMutationResult.Success(MAX_NAME, 12), result);
        assertEquals(new Item.BaseDamage(4, 12), storedDamage());
    }

    @Test
    void subtractDecreasesMinDamage() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SUBTRACT, MIN, "1");

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 3), result);
        assertEquals(new Item.BaseDamage(3, 9), storedDamage());
    }

    @Test
    void subtractClampsToZero() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SUBTRACT, MIN, "50");

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 0), result);
        assertEquals(new Item.BaseDamage(0, 9), storedDamage());
    }

    @Test
    void setBelowZeroClampsToZero() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MIN, "-5");

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 0), result);
    }

    @Test
    void defaultRestoresOriginalMaxDamage() {
        mutator.apply(DevConsoleMutationVerb.SET, MAX, "20");

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MAX, DEFAULT);

        assertEquals(new DevConsoleMutationResult.Success(MAX_NAME, 9), result);
        assertEquals(ORIGINAL, storedDamage());
    }

    @Test
    void defaultRestoresOriginalMinDamage() {
        mutator.apply(DevConsoleMutationVerb.SET, MIN, "2");

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MIN, DEFAULT);

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 4), result);
        assertEquals(ORIGINAL, storedDamage());
    }

    @Test
    void rejectsUnknownField() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, "glyph", "1");

        assertEquals(new DevConsoleMutationResult.Failure("glyph"), result);
    }

    @Test
    void rejectsNonNumericValue() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MAX, "lots");

        assertEquals(new DevConsoleMutationResult.Failure("lots"), result);
    }

    @Test
    void rejectsNegativeAddMagnitude() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, MAX, "-2");

        assertEquals(new DevConsoleMutationResult.Failure("-2"), result);
    }

    @Test
    void acceptsZeroAddMagnitudeAsNoChange() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, MAX, "0");

        assertEquals(new DevConsoleMutationResult.Success(MAX_NAME, 9), result);
    }

    @Test
    void rejectsNegativeSubtractMagnitude() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SUBTRACT, MAX, "-2");

        assertEquals(new DevConsoleMutationResult.Failure("-2"), result);
    }

    @Test
    void rejectsMinGreaterThanMaxAndLeavesItemUnchanged() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MIN, "10");

        assertEquals(new DevConsoleMutationResult.Failure("10"), result);
        assertEquals(ORIGINAL, storedDamage());
    }

    @Test
    void rejectsMaxLessThanMinAndLeavesItemUnchanged() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MAX, "3");

        assertEquals(new DevConsoleMutationResult.Failure("3"), result);
        assertEquals(ORIGINAL, storedDamage());
    }

    @Test
    void acceptsMinEqualToMax() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.SET, MIN, "9");

        assertEquals(new DevConsoleMutationResult.Success(MIN_NAME, 9), result);
        assertEquals(new Item.BaseDamage(9, 9), storedDamage());
    }

    @Test
    void rejectsDefaultOnAddVerb() {
        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, MAX, DEFAULT);

        assertEquals(new DevConsoleMutationResult.Failure(DEFAULT), result);
    }

    @Test
    void editsAccumulateAcrossCommands() {
        mutator.apply(DevConsoleMutationVerb.SET, MAX, "20");

        DevConsoleMutationResult result = mutator.apply(DevConsoleMutationVerb.ADD, MAX, "5");

        assertEquals(new DevConsoleMutationResult.Success(MAX_NAME, 25), result);
        assertEquals(new Item.BaseDamage(4, 25), storedDamage());
    }

    @Test
    void fieldTokensListsMinAndMaxDamage() {
        assertEquals(List.of(MIN, MAX), mutator.fieldTokens());
    }

    @Test
    void hasClassDefaultOnlyForItemFields() {
        assertTrue(mutator.hasClassDefault(MIN));
        assertTrue(mutator.hasClassDefault(MAX));
        assertFalse(mutator.hasClassDefault("glyph"));
    }
}
