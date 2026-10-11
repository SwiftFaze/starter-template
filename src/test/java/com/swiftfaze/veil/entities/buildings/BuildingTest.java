package com.swiftfaze.veil.entities.buildings;

import com.swiftfaze.veil.world.Tile;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingTest {

    private static final int ORIGIN_X = 10;
    private static final int ORIGIN_Y = 20;
    private static final int WIDTH = 3;
    private static final int HEIGHT = 2;

    @Test
    void coversEveryTileOfItsFootprint() {
        Building building = footprintAt(ORIGIN_X, ORIGIN_Y);

        assertTrue(building.covers(ORIGIN_X, ORIGIN_Y));
        assertTrue(building.covers(ORIGIN_X + WIDTH - 1, ORIGIN_Y + HEIGHT - 1));
    }

    @Test
    void doesNotCoverTheTileJustLeftOfTheFootprint() {
        assertFalse(footprintAt(ORIGIN_X, ORIGIN_Y).covers(ORIGIN_X - 1, ORIGIN_Y));
    }

    @Test
    void doesNotCoverTheTileJustRightOfTheFootprint() {
        assertFalse(footprintAt(ORIGIN_X, ORIGIN_Y).covers(ORIGIN_X + WIDTH, ORIGIN_Y));
    }

    @Test
    void doesNotCoverTheTileJustAboveTheFootprint() {
        assertFalse(footprintAt(ORIGIN_X, ORIGIN_Y).covers(ORIGIN_X, ORIGIN_Y - 1));
    }

    @Test
    void doesNotCoverTheTileJustBelowTheFootprint() {
        assertFalse(footprintAt(ORIGIN_X, ORIGIN_Y).covers(ORIGIN_X, ORIGIN_Y + HEIGHT));
    }

    private static Building footprintAt(int worldX, int worldY) {
        Tile tile = new Tile("test:floor", '.', Color.GRAY, true);
        Building building = new Building(List.of(List.of(tile, tile, tile), List.of(tile, tile, tile)));
        building.setWorldX(worldX);
        building.setWorldY(worldY);
        return building;
    }
}
