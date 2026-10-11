package com.swiftfaze.veil.world;

/**
 * Maps a noise value to a core terrain tile id. Ranges are contiguous and each lower bound
 * is inclusive, running from water (lowest) up to ice (highest).
 */
public final class TerrainBuckets {

    /** Exclusive upper bound of each bucket except the last, in ascending order. */
    private static final double[] UPPER_BOUNDS = {-0.65, -0.6, -0.5, 0.35, 0.4, 0.5, 0.8, 0.9};
    private static final String[] TILE_IDS = {
        CoreTiles.WATER, CoreTiles.SAND, CoreTiles.MUD, CoreTiles.GRASS,
        CoreTiles.TREE, CoreTiles.ROCK, CoreTiles.STONE, CoreTiles.SNOW, CoreTiles.ICE,
    };

    private TerrainBuckets() {
    }

    public static String tileIdFor(double noise) {
        for (int bucket = 0; bucket < UPPER_BOUNDS.length; bucket++) {
            if (noise < UPPER_BOUNDS[bucket]) {
                return TILE_IDS[bucket];
            }
        }
        return TILE_IDS[UPPER_BOUNDS.length];
    }
}
