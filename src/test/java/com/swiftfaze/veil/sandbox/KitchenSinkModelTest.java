package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.world.Tile;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenSinkModelTest {

    private static final Tile WALL = new Tile("test:wall", '#', Color.GRAY, false);
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);

    @Test
    void markerStartsOnTheFirstWalkableTileInReadingOrder() {
        List<Tile> tiles = new ArrayList<>(Collections.nCopies(10, WALL));
        tiles.add(WALL);
        tiles.add(FLOOR);

        KitchenSinkModel model = new KitchenSinkModel(KitchenSinkScene.holding(tiles));

        assertEquals(1, model.getMarkerX());
        assertEquals(1, model.getMarkerY());
    }

    @Test
    void aSceneWithTilesReportsHavingTiles() {
        KitchenSinkModel model = new KitchenSinkModel(KitchenSinkScene.holding(List.of(FLOOR)));

        assertTrue(model.hasTiles());
    }

    @Test
    void anEmptySceneReportsNoTiles() {
        KitchenSinkModel model = new KitchenSinkModel(KitchenSinkScene.holding(List.of()));

        assertFalse(model.hasTiles());
    }
}
