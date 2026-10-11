package com.swiftfaze.veil.world;

import java.awt.Rectangle;
import java.util.Collection;

/**
 * A dev-only scene containing every given tile,
 * arranged in a grid for preview/testing purposes. Used only by the
 * KitchenSinkProvider sandbox, not in the live game.
 */
public final class KitchenSinkScene extends WorldScene {

    private static final int TILES_PER_ROW = 10;

    /**
     * Static factory (not a constructor call) so sandbox code can build the scene without
     * instantiating an engine class, which the composition-root ArchUnit rule forbids.
     */
    public static KitchenSinkScene holding(Collection<Tile> tiles) {
        return new KitchenSinkScene(tiles);
    }

    private KitchenSinkScene(Collection<Tile> tiles) {
        super(calculateWidth(tiles), calculateHeight(tiles));
        fillWithTiles(tiles);
    }

    private void fillWithTiles(Collection<Tile> allTiles) {
        int index = 0;
        for (Tile tile : allTiles) {
            setTile(index % TILES_PER_ROW, index / TILES_PER_ROW, tile);
            index++;
        }
    }

    // WorldScene has no single-tile setter; a 1x1 fillRegion is its public way to place one.
    private void setTile(int x, int y, Tile tile) {
        fillRegion(new Rectangle(x, y, 1, 1), tile);
    }

    private static int calculateWidth(Collection<Tile> allTiles) {
        return Math.min(TILES_PER_ROW, Math.max(1, allTiles.size()));
    }

    private static int calculateHeight(Collection<Tile> allTiles) {
        if (allTiles.isEmpty()) {
            return 1;
        }
        return (allTiles.size() + TILES_PER_ROW - 1) / TILES_PER_ROW;
    }
}
