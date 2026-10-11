package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import com.swiftfaze.veil.world.object.Door;

import java.awt.Color;
import java.awt.Rectangle;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Shared builders for the world-generation unit tests: the real core mods and small hand-made scenes. */
final class WorldgenFixtures {

    private static final char DOOR_CELL = 'D';

    static final ModRegistry MODS = ModLoader.load(Paths.get("mods"));

    private WorldgenFixtures() {
    }

    static Tile tile(String id) {
        return MODS.getTile(id);
    }

    /** A scene whose every tile is {@code id}. */
    static WorldScene sceneOf(int width, int height, String id) {
        WorldScene scene = new WorldScene(width, height) {
        };
        scene.fillRegion(new Rectangle(0, 0, width, height), tile(id));
        return scene;
    }

    static void setTile(WorldScene scene, int x, int y, String id) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), tile(id));
    }

    /**
     * A building whose blueprint rows are given as strings: {@code D} is wood with a door object
     * placed on it, {@code S} stone, anything else wood.
     */
    static Building building(int worldX, int worldY, String... rows) {
        List<List<Tile>> blueprint = new ArrayList<>();
        List<Building.ObjectPlacement> objects = new ArrayList<>();
        for (int row = 0; row < rows.length; row++) {
            List<Tile> tiles = new ArrayList<>();
            for (int column = 0; column < rows[row].length(); column++) {
                char cell = rows[row].charAt(column);
                tiles.add(tile(cell == 'S' ? CoreTiles.STONE : CoreTiles.WOOD));
                if (cell == DOOR_CELL) {
                    objects.add(new Building.ObjectPlacement(column, row, new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false)));
                }
            }
            blueprint.add(tiles);
        }
        Building building = new Building(blueprint, objects);
        building.setWorldX(worldX);
        building.setWorldY(worldY);
        return building;
    }
}
