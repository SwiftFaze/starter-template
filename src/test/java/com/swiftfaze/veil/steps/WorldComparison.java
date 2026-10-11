package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Step-side helpers for comparing generated worlds tile by tile and building by building. */
final class WorldComparison {

    private static final int SEED_SEARCH_LIMIT = 5000;

    private WorldComparison() {
    }

    /** Asserts two worlds match in terrain, building positions and spawn point. */
    static void assertSameWorld(GeneratedWorldScene expected, GeneratedWorldScene actual) {
        Assertions.assertEquals(positions(expected), positions(actual));
        Assertions.assertEquals(expected.getSpawnX(), actual.getSpawnX());
        Assertions.assertEquals(expected.getSpawnY(), actual.getSpawnY());
        Assertions.assertTrue(tilesMatch(expected, actual), "terrain differs");
    }

    /** As above, for the world a running game currently holds. */
    static void assertSameWorld(GeneratedWorldScene expected, Supplier<? extends WorldScene> actualSource) {
        assertSameWorld(expected, (GeneratedWorldScene) actualSource.get());
    }

    /** The first world, seeds counted up from 1, that satisfies {@code wanted}: a scenario's precondition. */
    static GeneratedWorldScene firstWorld(LongFunction<GeneratedWorldScene> build, Predicate<GeneratedWorldScene> wanted) {
        for (long seed = 1; seed <= SEED_SEARCH_LIMIT; seed++) {
            GeneratedWorldScene candidate = build.apply(seed);
            if (wanted.test(candidate)) {
                return candidate;
            }
        }
        throw new AssertionError("no seed up to " + SEED_SEARCH_LIMIT + " met the precondition");
    }

    static List<String> positions(GeneratedWorldScene scene) {
        return scene.getBuildings().stream().map(building -> building.getWorldX() + "," + building.getWorldY()).toList();
    }

    static String tileId(Tile tile) {
        return tile == null ? "" : tile.getId();
    }

    static List<List<String>> tileIds(Building building) {
        return building.getBlueprint().stream().map(row -> row.stream().map(Tile::getId).toList()).toList();
    }

    private static boolean tilesMatch(WorldScene first, WorldScene second) {
        for (int x = 0; x < first.getWidth(); x++) {
            for (int y = 0; y < first.getHeight(); y++) {
                if (!tileId(first.getTile(x, y)).equals(tileId(second.getTile(x, y)))) {
                    return false;
                }
            }
        }
        return true;
    }
}
