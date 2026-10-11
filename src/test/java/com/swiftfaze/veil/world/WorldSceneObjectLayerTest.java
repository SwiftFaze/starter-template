package com.swiftfaze.veil.world;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.object.InteractableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

class WorldSceneObjectLayerTest {

    private static final Door DOOR = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);

    private static WorldScene floorScene() {
        WorldScene scene = new WorldScene(3, 3) {
        };
        scene.fillAll(FLOOR);
        return scene;
    }

    @Test
    void aClosedDoorBlocksItsTileAndAnOpenDoorDoesNot() {
        WorldScene scene = floorScene();
        Door door = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
        scene.placeObject(1, 1, door);

        Assertions.assertFalse(scene.isWalkable(1, 1));

        door.interact(false);

        Assertions.assertTrue(scene.isWalkable(1, 1));
    }

    @Test
    void theGridShowsTheObjectGlyphOverTheTile() {
        WorldScene scene = floorScene();
        scene.placeObject(1, 1, new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false));

        List<String> rows = scene.renderToGrid(new Viewport(0, 0, 3, 3), List.of());

        Assertions.assertEquals("...", rows.get(0));
        Assertions.assertEquals(".+.", rows.get(1));
        Assertions.assertEquals("...", rows.get(2));
    }

    @Test
    void aTileWithNoObjectIsUnchanged() {
        WorldScene scene = floorScene();

        Assertions.assertEquals(FLOOR, scene.getTile(0, 0));
        Assertions.assertEquals(null, scene.getObject(0, 0));
    }

    @Test
    void objectsAtTheEdgesOfTheSceneAreFound() {
        WorldScene scene = floorScene();
        Door corner = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
        Door farCorner = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
        scene.placeObject(0, 0, corner);
        scene.placeObject(2, 2, farCorner);

        Assertions.assertSame(corner, scene.getObject(0, 0));
        Assertions.assertSame(farCorner, scene.getObject(2, 2));
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "0,-1", "3,0", "0,3"})
    void cellsBeyondTheSceneHoldNoObject(int x, int y) {
        Assertions.assertNull(floorScene().getObject(x, y));
    }

    @Test
    void placingABuildingPutsACopyOfEachObjectAtItsOffsetFromTheBuildingOrigin() {
        WorldScene scene = new WorldScene(6, 6) {
        };
        scene.fillAll(FLOOR);
        Door template = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
        Building building = new Building(List.of(List.of(FLOOR, FLOOR), List.of(FLOOR, FLOOR)),
                List.of(new Building.ObjectPlacement(1, 1, template)));
        building.setWorldX(2);
        building.setWorldY(3);

        scene.placeBuilding(building);

        InteractableObject placed = scene.getObject(3, 4);
        Assertions.assertNotNull(placed);
        Assertions.assertNotSame(template, placed);
        Assertions.assertEquals("test:door", placed.getId());
    }

    @Test
    void placingABuildingLeavesTheCellsAroundItsObjectsEmpty() {
        WorldScene scene = new WorldScene(6, 6) {
        };
        scene.fillAll(FLOOR);
        Building building = new Building(List.of(List.of(FLOOR, FLOOR), List.of(FLOOR, FLOOR)),
                List.of(new Building.ObjectPlacement(1, 1, DOOR)));
        building.setWorldX(2);
        building.setWorldY(3);

        scene.placeBuilding(building);

        Assertions.assertNull(scene.getObject(1, 0));
    }

    @Test
    void anObjectIsDrawnInItsOwnColourNotTheTilesColour() {
        WorldScene scene = new WorldScene(1, 1) {
        };
        scene.fillAll(FLOOR);
        scene.placeObject(0, 0, new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false));
        int cell = 40;
        BufferedImage image = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();

        scene.renderWorld(graphics, cell, cell, new Camera(1, 1));
        graphics.dispose();

        Assertions.assertTrue(containsColour(image, Color.ORANGE), "the door glyph should be drawn in the door's colour");
    }

    private static boolean containsColour(BufferedImage image, Color colour) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) == colour.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }
}
