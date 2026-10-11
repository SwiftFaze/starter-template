package com.swiftfaze.veil.world;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KitchenSinkSceneTest {

    private static List<Tile> tiles(int count) {
        List<Tile> tiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tiles.add(new Tile("test:t" + i, '.', Color.GRAY, true));
        }
        return tiles;
    }

    @Test
    void anExactMultipleOfTheRowWidthFillsWholeRowsOnly() {
        KitchenSinkScene scene = KitchenSinkScene.holding(tiles(20));

        assertEquals(10, scene.getWidth());
        assertEquals(2, scene.getHeight());
    }

    @Test
    void aPartialLastRowStillGetsItsOwnRow() {
        KitchenSinkScene scene = KitchenSinkScene.holding(tiles(21));

        assertEquals(3, scene.getHeight());
    }

    @Test
    void tilesFillRowsLeftToRightThenWrap() {
        List<Tile> tiles = tiles(12);

        KitchenSinkScene scene = KitchenSinkScene.holding(tiles);

        assertEquals(tiles.get(9), scene.getTile(9, 0));
        assertEquals(tiles.get(11), scene.getTile(1, 1));
    }

    @Test
    void anEmptyTileListYieldsOneEmptyCell() {
        KitchenSinkScene scene = KitchenSinkScene.holding(List.of());

        assertEquals(1, scene.getWidth());
        assertEquals(1, scene.getHeight());
        assertNull(scene.getTile(0, 0));
    }
}
