package com.swiftfaze.veil.mods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.Chest;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.object.InteractableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class ObjectReaderTest {

    private static final Path FILE = Path.of("test.json");
    private static final Item POTION = new Item("core:health_potion", "Health Potion",
            new Item.ItemAttributes('!', "consumable", "none", new Item.BaseDamage(0, 0), List.of()));
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Door DOOR = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
    private static final int BLUEPRINT_SIZE = 2;

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    private static String chestJson(int slots, String contents) {
        return "{\"type\":\"chest\",\"symbol\":\"C\",\"slots\":" + slots + ",\"contents\":[" + contents
                + "],\"color\":{\"r\":1,\"g\":2,\"b\":3}}";
    }

    @Test
    void aChestMayHoldExactlyAsManyItemsAsItHasSlots() {
        JsonObject chest = json(chestJson(1, "\"core:health_potion\""));

        InteractableObject read = ObjectReader.readObject(chest, "test:chest", FILE, Map.of("core:health_potion", POTION));

        Assertions.assertEquals(List.of(Optional.of(POTION)), Assertions.assertInstanceOf(Chest.class, read).getSlots());
    }

    @Test
    void aChestHoldingMoreItemsThanSlotsIsRejected() {
        JsonObject chest = json(chestJson(1, "\"core:health_potion\",\"core:health_potion\""));
        Map<String, Item> items = Map.of("core:health_potion", POTION);

        Assertions.assertThrows(ModLoadException.class, () -> ObjectReader.readObject(chest, "test:chest", FILE, items));
    }

    @Test
    void aBuildingWithoutAnObjectsListPlacesNothing() {
        List<Building.ObjectPlacement> placements = ObjectReader.readPlacements(json("{}"), "test:house", FILE, Map.of());

        Assertions.assertTrue(placements.isEmpty());
    }

    @Test
    void placementsOnEveryEdgeCellOfTheBlueprintAreAccepted() {
        List<Building.ObjectPlacement> corners = List.of(
                new Building.ObjectPlacement(0, 0, DOOR),
                new Building.ObjectPlacement(BLUEPRINT_SIZE - 1, BLUEPRINT_SIZE - 1, DOOR));

        Assertions.assertDoesNotThrow(() -> ObjectReader.checkPlacementsInBlueprint(corners, blueprint(), "test:house", FILE));
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "2,0", "0,2"})
    void placementsJustOutsideTheBlueprintAreRejected(int x, int y) {
        List<Building.ObjectPlacement> placements = List.of(new Building.ObjectPlacement(x, y, DOOR));

        Assertions.assertThrows(ModLoadException.class,
                () -> ObjectReader.checkPlacementsInBlueprint(placements, blueprint(), "test:house", FILE));
    }

    private static List<List<Tile>> blueprint() {
        return List.of(List.of(FLOOR, FLOOR), List.of(FLOOR, FLOOR));
    }
}
