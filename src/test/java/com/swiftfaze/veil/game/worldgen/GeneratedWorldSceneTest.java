package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeneratedWorldSceneTest {

    private static final int SIZE = 100;
    private static final long SEED = 42L;
    private static final long NEXT_SEED = 43L;

    /** Pinned: buildings are scattered from a Random seeded with the world seed mixed with a fixed salt. */
    @Test
    void buildingsOnFlatGrassFollowTheSaltedSeed() {
        assertEquals("[69,26, 73,84, 91,44, 11,14, 37,11] spawn 50,50", positions(flatWorld(SEED)));
    }

    @Test
    void anotherSeedScattersTheBuildingsElsewhere() {
        assertEquals("[85,73, 40,47, 30,46, 29,38, 63,74] spawn 50,50", positions(flatWorld(NEXT_SEED)));
    }

    private static GeneratedWorldScene flatWorld(long seed) {
        GeneratedWorldScene.Sources flatGrass = new GeneratedWorldScene.Sources(WorldgenFixtures.MODS, (x, y) -> 0.0);
        return new GeneratedWorldScene(SIZE, SIZE, seed, flatGrass);
    }

    private static String positions(GeneratedWorldScene world) {
        List<Building> buildings = world.getBuildings();
        return buildings.stream().map(b -> b.getWorldX() + "," + b.getWorldY()).toList() + " spawn "
                + world.getSpawnX() + "," + world.getSpawnY();
    }
}
