package com.swiftfaze.veil.world;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.testing.property.VeilArbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldSceneTest {

    private static final Tile WALL = new Tile("test:wall", '#', Color.GRAY, false);
    private static final Tile GRASS = new Tile("test:grass", ',', Color.GREEN, true);
    private static final Tile STONE = new Tile("test:stone", '%', Color.DARK_GRAY, false);
    private static final Tile WOOD = new Tile("test:wood", '+', Color.ORANGE, true);
    private static final Tile DOOR = new Tile("test:door", '/', Color.YELLOW, true);
    private static final Tile WATER = new Tile("test:water", '~', Color.BLUE, false);

    private static WorldScene sceneOf(int width, int height) {
        return new WorldScene(width, height) {
        };
    }

    @Test
    void createBorderFillsAllFourEdgesButNotTheInterior() {
        WorldScene scene = sceneOf(5, 5);

        scene.createBorder(5, 5, WALL);

        assertEquals(WALL, scene.getTile(0, 0));
        assertEquals(WALL, scene.getTile(4, 0));
        assertEquals(WALL, scene.getTile(0, 4));
        assertEquals(WALL, scene.getTile(4, 4));
        assertEquals(WALL, scene.getTile(2, 0));
        assertEquals(WALL, scene.getTile(0, 2));
        assertNull(scene.getTile(2, 2));
    }

    @Test
    void isWalkableReturnsFalseOutOfBounds() {
        WorldScene scene = sceneOf(5, 5);

        assertFalse(scene.isWalkable(-1, 0));
        assertFalse(scene.isWalkable(0, -1));
        assertFalse(scene.isWalkable(5, 0));
        assertFalse(scene.isWalkable(0, 5));
    }

    @Test
    void getTileReturnsNullOutOfBounds() {
        WorldScene scene = sceneOf(5, 5);

        assertNull(scene.getTile(-1, 0));
        assertNull(scene.getTile(0, 5));
    }

    @Test
    void getTileReturnsActualTileInBounds() {
        WorldScene scene = sceneOf(5, 5);
        scene.fillRegion(new Rectangle(2, 2, 1, 1), STONE);

        assertEquals(STONE, scene.getTile(2, 2));
    }

    @Test
    void getWidthAndHeightReturnConstructorValues() {
        WorldScene scene = sceneOf(7, 9);

        assertEquals(7, scene.getWidth());
        assertEquals(9, scene.getHeight());
    }

    @Test
    void positionSymbolAndColorAreFixedDefaults() {
        WorldScene scene = sceneOf(5, 5);

        assertEquals(0, scene.getX());
        assertEquals(0, scene.getY());
        assertEquals(' ', scene.getSymbol());
        assertEquals(Color.WHITE, scene.getColor());
    }

    @Test
    void renderDrawsNonEmptyTilesWithoutThrowing() {
        WorldScene scene = sceneOf(5, 5);
        scene.fillRegion(new Rectangle(1, 1, 1, 1), GRASS);
        Graphics2D g2d = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();

        assertDoesNotThrow(() -> scene.render(g2d, 15, 15, new Camera(0, 0)));
    }

    @Test
    void renderWorldSkipsEmptyTilesWithoutThrowing() {
        WorldScene scene = sceneOf(5, 5);
        Graphics2D g2d = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();

        assertDoesNotThrow(() -> scene.renderWorld(g2d, 15, 15, new Camera(0, 0)));
    }

    @Test
    void fillAllSetsEveryTileToTheGivenType() {
        WorldScene scene = sceneOf(4, 3);

        scene.fillAll(GRASS);

        assertEquals(GRASS, scene.getTile(0, 0));
        assertEquals(GRASS, scene.getTile(3, 0));
        assertEquals(GRASS, scene.getTile(0, 2));
        assertEquals(GRASS, scene.getTile(3, 2));
        assertEquals(GRASS, scene.getTile(1, 1));
        assertEquals(GRASS, scene.getTile(2, 1));
    }

    @Test
    void placeBuildingStampsBlueprintAtWorldOffsetWithoutTransposingAxes() {
        WorldScene scene = sceneOf(10, 10);
        List<List<Tile>> blueprint = List.of(
                List.of(WALL, WOOD),
                List.of(STONE, DOOR)
        );
        Building building = new Building(blueprint);
        building.setWorldX(3);
        building.setWorldY(4);

        scene.placeBuilding(building);

        assertEquals(WALL, scene.getTile(3, 4));
        assertEquals(WOOD, scene.getTile(4, 4));
        assertEquals(STONE, scene.getTile(3, 5));
        assertEquals(DOOR, scene.getTile(4, 5));
    }

    @Test
    void placeBuildingOverwritesTilesAlreadyInTheScene() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillRegion(new Rectangle(6, 6, 1, 1), WATER);
        Building building = new Building(List.of(List.of(WALL)));
        building.setWorldX(6);
        building.setWorldY(6);

        scene.placeBuilding(building);

        assertEquals(WALL, scene.getTile(6, 6));
    }

    @Test
    void placeBuildingWithEmptyBlueprintLeavesTheSceneUnchanged() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Building building = new Building(List.of());
        building.setWorldX(2);
        building.setWorldY(2);

        assertDoesNotThrow(() -> scene.placeBuilding(building));

        assertEquals(GRASS, scene.getTile(2, 2));
    }

    // ========== Property-based tests ==========

    @Provide
    Arbitrary<Integer> worldDimensions() {
        return VeilArbitraries.worldDimension();
    }

    @Provide
    Arbitrary<Integer> coordinates() {
        return VeilArbitraries.coordinate();
    }

    @Property
    void tileReturnsNullAndIsWalkableReturnsFalseOutOfBounds(
            @ForAll("worldDimensions") int width,
            @ForAll("worldDimensions") int height,
            @ForAll("coordinates") int x,
            @ForAll("coordinates") int y) {
        WorldScene scene = sceneOf(width, height);

        // Call both methods for every generated coordinate to verify:
        // 1. Neither throws an exception for any coordinate (in- or out-of-bounds)
        // 2. Out-of-bounds coordinates return null and false respectively
        Tile tile = scene.getTile(x, y);
        boolean walkable = scene.isWalkable(x, y);

        boolean isInBounds = x >= 0 && x < width && y >= 0 && y < height;
        if (!isInBounds) {
            assertNull(tile);
            assertFalse(walkable);
        }
    }
}
