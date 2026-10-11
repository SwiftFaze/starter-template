package com.swiftfaze.veil.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.Chest;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.object.InteractableObject;

import java.awt.Color;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads object definitions (mods/&lt;mod&gt;/objects/*.json) and a building's object placements. */
final class ObjectReader {

    private ObjectReader() {
    }

    static InteractableObject readObject(JsonObject json, String id, Path file, Map<String, Item> itemsById) {
        String type = json.get("type").getAsString();
        return switch (type) {
            case "door" -> readDoor(json, id);
            case "chest" -> readChest(json, id, file, itemsById);
            default -> throw new ModLoadException("Object '" + id + "' has unknown type '" + type + "' in file: " + file);
        };
    }

    private static Door readDoor(JsonObject json, String id) {
        Color color = ModLoader.readColor(json.getAsJsonObject("color"));
        Door.Glyphs glyphs = new Door.Glyphs(
                json.get("symbolOpen").getAsString().charAt(0),
                json.get("symbolClosed").getAsString().charAt(0));
        boolean open = json.has("open") && json.get("open").getAsBoolean();
        return new Door(id, color, glyphs, open);
    }

    private static Chest readChest(JsonObject json, String id, Path file, Map<String, Item> itemsById) {
        Color color = ModLoader.readColor(json.getAsJsonObject("color"));
        char symbol = json.get("symbol").getAsString().charAt(0);
        int slots = json.get("slots").getAsInt();
        JsonArray contentIds = json.has("contents") ? json.getAsJsonArray("contents") : new JsonArray();
        if (contentIds.size() > slots) {
            throw new ModLoadException("Chest '" + id + "' has " + slots + " slot(s) but " + contentIds.size()
                    + " contents entries in file: " + file);
        }
        List<Item> contents = new ArrayList<>();
        for (JsonElement element : contentIds) {
            String itemId = element.getAsString();
            Item item = itemsById.get(itemId);
            if (item == null) {
                throw new ModLoadException("Chest '" + id + "' contains unknown item '" + itemId + "' in file: " + file);
            }
            contents.add(item);
        }
        return new Chest(id, color, symbol, new Chest.Contents(slots, contents));
    }

    /** Reads a building's optional "objects" list; each entry must name a known object. */
    static List<Building.ObjectPlacement> readPlacements(JsonObject json, String buildingId, Path file,
                                                         Map<String, InteractableObject> objectsById) {
        List<Building.ObjectPlacement> placements = new ArrayList<>();
        if (!json.has("objects")) {
            return placements;
        }
        for (JsonElement element : json.getAsJsonArray("objects")) {
            JsonObject entry = element.getAsJsonObject();
            String objectId = entry.get("id").getAsString();
            InteractableObject template = objectsById.get(objectId);
            if (template == null) {
                throw new ModLoadException("Building '" + buildingId + "' places unknown object ID: "
                        + objectId + " in file: " + file);
            }
            placements.add(new Building.ObjectPlacement(entry.get("x").getAsInt(), entry.get("y").getAsInt(), template));
        }
        return placements;
    }

    /** Rejects any placement that falls outside the building's blueprint. */
    static void checkPlacementsInBlueprint(List<Building.ObjectPlacement> placements, List<List<Tile>> blueprint,
                                           String buildingId, Path file) {
        int height = blueprint.size();
        int width = blueprint.get(0).size();
        for (Building.ObjectPlacement placement : placements) {
            boolean inside = placement.x() >= 0 && placement.x() < width && placement.y() >= 0 && placement.y() < height;
            if (!inside) {
                throw new ModLoadException("Building '" + buildingId + "' places object '" + placement.object().getId()
                        + "' at (" + placement.x() + ", " + placement.y() + "), outside its " + width + "x" + height
                        + " blueprint in file: " + file);
            }
        }
    }
}
