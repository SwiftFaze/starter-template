package com.swiftfaze.veil.world.object;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoorTest {

    private static Door door(boolean open) {
        return new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), open);
    }

    @Test
    void aClosedDoorOpensWhenInteractedWith() {
        Door door = door(false);

        assertEquals(InteractionResult.TOGGLED, door.interact(false));

        assertTrue(door.isOpen());
        assertTrue(door.isWalkable());
    }

    @Test
    void anOpenDoorShowsItsOpenGlyph() {
        assertEquals('/', door(true).getSymbol());
    }

    @Test
    void anOpenDoorClosesWhenNothingStandsInIt() {
        Door door = door(true);

        assertEquals(InteractionResult.TOGGLED, door.interact(false));

        assertFalse(door.isOpen());
        assertFalse(door.isWalkable());
    }

    @Test
    void aClosedDoorShowsItsClosedGlyph() {
        assertEquals('+', door(false).getSymbol());
    }

    @Test
    void anOpenDoorRefusesToCloseOnAnOccupant() {
        Door door = door(true);

        assertEquals(InteractionResult.REFUSED, door.interact(true));

        assertTrue(door.isOpen());
    }

    @Test
    void newInstancesStartInTheDefinitionsStateAndDoNotShareState() {
        Door template = door(false);
        Door placed = template.newInstance();
        placed.interact(false);

        assertTrue(placed.isOpen());
        assertFalse(template.newInstance().isOpen());
    }

    @Test
    void aDoorReportsItsIdAndColour() {
        Door door = door(false);

        assertEquals("test:door", door.getId());
        assertEquals(Color.ORANGE, door.getColor());
    }
}
