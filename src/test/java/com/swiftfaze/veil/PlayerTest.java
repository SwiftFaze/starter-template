package com.swiftfaze.veil;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PlayerTest {

    private static final Tile GRASS = new Tile("test:grass", ',', Color.GREEN, true);
    private static final Tile WATER = new Tile("test:water", '~', Color.BLUE, false);

    private static WorldScene sceneOf(int width, int height) {
        return new WorldScene(width, height) {
        };
    }

    @Test
    void movingRightIncreasesXWhenTileIsWalkable() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Player player = new Player(5, 5);

        player.moveRight(scene);

        assertEquals(6, player.getX());
        assertEquals(5, player.getY());
    }

    @Test
    void movingUpIncreasesYWhenTileIsWalkable() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Player player = new Player(5, 5);

        player.moveUp(scene);

        assertEquals(5, player.getX());
        assertEquals(4, player.getY());
    }

    @Test
    void movingIntoANonWalkableTileDoesNotMovePlayer() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        scene.fillRegion(new Rectangle(6, 5, 1, 1), WATER);
        Player player = new Player(5, 5);

        player.moveRight(scene);

        assertEquals(5, player.getX());
        assertEquals(5, player.getY());
    }

    @Test
    void movingDownIncreasesYWhenTileIsWalkable() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Player player = new Player(5, 5);

        player.moveDown(scene);

        assertEquals(5, player.getX());
        assertEquals(6, player.getY());
    }

    @Test
    void movingLeftDecreasesXWhenTileIsWalkable() {
        WorldScene scene = sceneOf(10, 10);
        scene.fillAll(GRASS);
        Player player = new Player(5, 5);

        player.moveLeft(scene);

        assertEquals(4, player.getX());
        assertEquals(5, player.getY());
    }

    @Test
    void getPlayerInfoIsNeverNull() {
        Player player = new Player(0, 0);

        assertNotNull(player.getPlayerInfo());
    }

    @Test
    void symbolAndColorAreFixedDefaults() {
        Player player = new Player(0, 0);

        assertEquals('◼', player.getSymbol());
        assertEquals(Color.decode("#ef481f"), player.getColor());
    }

    @Test
    void renderDrawsWithoutThrowing() {
        Player player = new Player(3, 3);
        Graphics2D g2d = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();

        assertDoesNotThrow(() -> player.render(g2d, 15, 15, new Camera(0, 0)));
    }
}
