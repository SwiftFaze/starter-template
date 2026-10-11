package com.swiftfaze.veil.entities.items;

import com.swiftfaze.veil.component.DetailTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemTest {

    @Test
    void getDetailTablesWithoutEffects() {
        Item item = new Item(
                "test:sword",
                "Iron Sword",
                new Item.ItemAttributes(
                        '/',
                        "weapon",
                        "hand",
                        new Item.BaseDamage(5, 10),
                        List.of()
                )
        );

        List<DetailTable> tables = item.getDetailTables();

        assertEquals(1, tables.size());
        assertEquals("", tables.get(0).label());
        assertEquals(List.of("Field", "Value"), tables.get(0).columnHeaders());
        assertEquals(List.of(
                List.of("ID", "test:sword"),
                List.of("Name", "Iron Sword"),
                List.of("Glyph", "/"),
                List.of("Type", "weapon"),
                List.of("Slot", "hand"),
                List.of("Base Damage (Min)", "5"),
                List.of("Base Damage (Max)", "10")
        ), tables.get(0).rows());
    }

    @Test
    void getDetailTablesWithEffects() {
        List<Item.Effect> effects = List.of(
                new Item.Effect("bonus", "strength", "+2"),
                new Item.Effect("penalty", "speed", "-1")
        );
        Item item = new Item(
                "test:staff",
                "Arcane Staff",
                new Item.ItemAttributes(
                        '|',
                        "weapon",
                        "hand",
                        new Item.BaseDamage(3, 8),
                        effects
                )
        );

        List<DetailTable> tables = item.getDetailTables();

        assertEquals(2, tables.size());
        assertEquals("", tables.get(0).label());
        assertEquals(7, tables.get(0).rows().size());
        assertEquals("Effects:", tables.get(1).label());
        assertEquals(List.of("Type", "Stat", "Calc"), tables.get(1).columnHeaders());
        assertEquals(2, tables.get(1).rows().size());
        assertEquals(List.of("bonus", "strength", "+2"), tables.get(1).rows().get(0));
        assertEquals(List.of("penalty", "speed", "-1"), tables.get(1).rows().get(1));
    }

    @Test
    void noDamageFieldsWhenBaseDamageIsZero() {
        Item item = new Item(
                "test:armour",
                "Chain Mail",
                new Item.ItemAttributes(
                        '[',
                        "armour",
                        "body",
                        new Item.BaseDamage(0, 0),
                        List.of()
                )
        );

        List<DetailTable> tables = item.getDetailTables();

        assertEquals(1, tables.size());
        assertEquals(List.of(
                List.of("ID", "test:armour"),
                List.of("Name", "Chain Mail"),
                List.of("Glyph", "["),
                List.of("Type", "armour"),
                List.of("Slot", "body")
        ), tables.get(0).rows());
    }

    private static Item swordWithDamage4To9() {
        return new Item(
                "test:sword",
                "Iron Sword",
                new Item.ItemAttributes('/', "weapon", "hand", new Item.BaseDamage(4, 9), List.of())
        );
    }

    @Test
    void withBaseDamageReturnsNewItemWithUpdatedDamage() {
        Item updated = swordWithDamage4To9().withBaseDamage(5, 12);

        assertEquals(new Item.BaseDamage(5, 12), updated.getBaseDamage());
    }

    @Test
    void withBaseDamagePreservesIdAndName() {
        Item original = swordWithDamage4To9();

        Item updated = original.withBaseDamage(5, 12);

        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getName(), updated.getName());
    }

    @Test
    void withBaseDamagePreservesGlyphTypeAndSlot() {
        Item original = swordWithDamage4To9();

        Item updated = original.withBaseDamage(5, 12);

        assertEquals(original.getGlyph(), updated.getGlyph());
        assertEquals(original.getType(), updated.getType());
        assertEquals(original.getSlot(), updated.getSlot());
    }

    @Test
    void withBaseDamagePreservesEffects() {
        List<Item.Effect> effects = List.of(
                new Item.Effect("bonus", "strength", "+2")
        );
        Item original = new Item(
                "test:sword",
                "Iron Sword",
                new Item.ItemAttributes('/', "weapon", "hand", new Item.BaseDamage(4, 9), effects)
        );

        Item updated = original.withBaseDamage(5, 12);

        assertEquals(effects, updated.getEffects());
    }
}
