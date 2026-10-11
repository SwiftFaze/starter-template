package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.npc.TileCoord;
import org.jspecify.annotations.Nullable;

/**
 * Builds the JSON of an NPC definition file for scenarios. Shared by ModLoaderSteps (which writes
 * mod directories) and NpcPatrolSteps (which loads them to spawn into the world), so both write
 * the same shape. Defaults are a valid NPC with no patrol; each setter changes one field.
 */
final class NpcJson {

    private final JsonObject json = new JsonObject();

    private NpcJson(String id) {
        json.addProperty("id", id);
        json.addProperty("name", id);
        json.addProperty("symbol", "N");
        json.add("color", colorJson(0, 0, 0));
        json.add("spawn", coordJson(new TileCoord(1, 1)));
    }

    static NpcJson npc(String id) {
        return new NpcJson(id);
    }

    NpcJson named(String name) {
        json.addProperty("name", name);
        return this;
    }

    NpcJson dialogue(String dialogueId) {
        json.addProperty("dialogue", dialogueId);
        return this;
    }

    NpcJson symbol(String symbol) {
        json.addProperty("symbol", symbol);
        return this;
    }

    NpcJson color(int red, int green, int blue) {
        json.add("color", colorJson(red, green, blue));
        return this;
    }

    NpcJson spawn(int x, int y) {
        json.add("spawn", coordJson(new TileCoord(x, y)));
        return this;
    }

    NpcJson withoutSpawn() {
        json.remove("spawn");
        return this;
    }

    /** A patrol over {@code waypoints}; a null {@code mode} is omitted so the loader's default applies. */
    NpcJson patrol(@Nullable String mode, TileCoord... waypoints) {
        JsonObject patrol = new JsonObject();
        if (mode != null) {
            patrol.addProperty("mode", mode);
        }
        JsonArray points = new JsonArray();
        for (TileCoord waypoint : waypoints) {
            points.add(coordJson(waypoint));
        }
        patrol.add("waypoints", points);
        json.add("patrol", patrol);
        return this;
    }

    JsonObject toJson() {
        return json.deepCopy();
    }

    private static JsonObject colorJson(int red, int green, int blue) {
        JsonObject color = new JsonObject();
        color.addProperty("r", red);
        color.addProperty("g", green);
        color.addProperty("b", blue);
        return color;
    }

    private static JsonObject coordJson(TileCoord tile) {
        JsonObject tileJson = new JsonObject();
        tileJson.addProperty("x", tile.x());
        tileJson.addProperty("y", tile.y());
        return tileJson;
    }
}
