package com.swiftfaze.veil.world.object;

import com.swiftfaze.veil.entities.items.Item;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ChestTest {

    private static final Item POTION = new Item("core:health_potion", "Health Potion",
            new Item.ItemAttributes('!', "consumable", "none", new Item.BaseDamage(0, 0), List.of()));

    @Test
    void theSlotsFillFromTheFrontAndTheRestAreEmpty() {
        Chest chest = new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(3, List.of(POTION)));

        assertEquals(List.of(Optional.of(POTION), Optional.empty(), Optional.empty()), chest.getSlots());
    }

    @Test
    void aChestIsNeverWalkableAndInteractingWithItShowsItsContents() {
        Chest chest = new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(1, List.of()));

        assertFalse(chest.isWalkable());
        assertEquals(InteractionResult.SHOW_CONTENTS, chest.interact(false));
    }

    @Test
    void aChestKeepsItsDefinitionInTheCopyThatGetsPlaced() {
        Chest template = new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(2, List.of(POTION)));

        Chest placed = template.newInstance();

        assertNotSame(template, placed);
        assertEquals("test:chest", placed.getId());
        assertEquals(template.getSlots(), placed.getSlots());
    }

    @Test
    void aPlacedChestLooksLikeItsTemplate() {
        Chest placed = new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(2, List.of(POTION))).newInstance();

        assertEquals('C', placed.getSymbol());
        assertEquals(Color.YELLOW, placed.getColor());
    }
}
