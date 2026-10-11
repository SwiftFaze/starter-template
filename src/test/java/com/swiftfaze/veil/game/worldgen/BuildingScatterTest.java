package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.mods.RegistryFixtures;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Uses the one real core blueprint: a 7x7 house with its only door at the bottom-middle (3, 6). */
class BuildingScatterTest {

    private static final int HOUSE = 7;
    private static final int DOOR_COLUMN = 3;
    private static final int DOOR_ROW = 6;
    private static final long SEED = 7L;
    private static final int ROOMY = 100;

    @Test
    void aRoomyMapReceivesTheTargetNumberOfBuildings() {
        WorldScene scene = WorldgenFixtures.sceneOf(ROOMY, ROOMY, CoreTiles.GRASS);

        List<Building> placed = scatter(scene);

        assertEquals(BuildingScatter.TARGET_BUILDINGS, placed.size());
    }

    @Test
    void aMapExactlyAsLargeAsTheHouseHasNoRoomForItsDoorApproach() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE, CoreTiles.GRASS);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void aMapWithRoomForTheHouseAndItsApproachHoldsExactlyOneAtTheOnlyValidSpot() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);

        List<Building> placed = scatter(scene);

        assertEquals(1, placed.size());
        assertEquals(0, placed.get(0).getWorldX());
        assertEquals(0, placed.get(0).getWorldY());
    }

    @Test
    void aMapNarrowerThanTheHouseHoldsNothing() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE - 1, ROOMY, CoreTiles.GRASS);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void aMapShorterThanTheHouseHoldsNothing() {
        WorldScene scene = WorldgenFixtures.sceneOf(ROOMY, HOUSE - 1, CoreTiles.GRASS);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void placedBuildingsAreStampedIntoTheScene() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);

        scatter(scene);

        assertEquals(CoreTiles.WOOD, scene.getTile(DOOR_COLUMN, DOOR_ROW).getId());
        assertEquals(CoreTiles.WOOD, scene.getTile(1, 1).getId());
        assertEquals(CoreTiles.GRASS, scene.getTile(DOOR_COLUMN, HOUSE).getId());
    }

    @Test
    void placedDoorIsAnObjectOnTheDoorTile() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);

        scatter(scene);

        assertInstanceOf(Door.class, scene.getObject(DOOR_COLUMN, DOOR_ROW));
    }

    @Test
    void aBuildingNeverCoversUnwalkableTerrain() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, 2, 2, CoreTiles.WATER);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void aBuildingNeedsItsFarCornerTileWalkable() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, HOUSE - 1, HOUSE - 1, CoreTiles.WATER);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void aBuildingNeedsTheTileOutsideItsDoorWalkable() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, HOUSE + 1, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, DOOR_COLUMN, HOUSE, CoreTiles.WATER);

        assertEquals(List.of(), scatter(scene));
    }

    @Test
    void aSecondBuildingMayNotStandOnTheFirstOnesDoorApproach() {
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, 2 * HOUSE + 1, CoreTiles.GRASS);

        assertEquals(1, scatter(scene).size());
    }

    @Test
    void twoWalkableRegionsEachTakeOneBuilding() {
        int height = 2 * (HOUSE + 1) + 1;
        WorldScene scene = WorldgenFixtures.sceneOf(HOUSE, height, CoreTiles.GRASS);
        for (int x = 0; x < HOUSE; x++) {
            WorldgenFixtures.setTile(scene, x, HOUSE + 1, CoreTiles.WATER);
        }

        List<Building> placed = scatter(scene);

        assertEquals(List.of(0, HOUSE + 2), placed.stream().map(Building::getWorldY).sorted().toList());
    }

    @Test
    void buildingsAreIndependentCopiesOfTheRegisteredBlueprint() {
        WorldScene scene = WorldgenFixtures.sceneOf(ROOMY, ROOMY, CoreTiles.GRASS);

        List<Building> placed = scatter(scene);

        assertEquals(placed.size(), placed.stream().distinct().count());
    }

    @Test
    void aFullyWalkableBuildingAsLargeAsTheMapIsPlacedOnceAndNeverOverlapped() {
        WorldScene scene = WorldgenFixtures.sceneOf(3, 3, CoreTiles.GRASS);
        Building interiorDoor = WorldgenFixtures.building(0, 0, "WWW", "WDW", "WWW");

        List<Building> placed = BuildingScatter.scatter(scene, registryOf(interiorDoor), new Random(SEED));

        assertEquals(1, placed.size());
    }

    @Test
    void aOneTileBuildingIsPlacedWhereAllFourApproachesAreFree() {
        WorldScene scene = WorldgenFixtures.sceneOf(3, 3, CoreTiles.GRASS);
        Building booth = WorldgenFixtures.building(0, 0, "D");

        List<Building> placed = BuildingScatter.scatter(scene, registryOf(booth), new Random(SEED));

        assertEquals(1, placed.size());
        assertEquals(1, placed.get(0).getWorldX());
        assertEquals(1, placed.get(0).getWorldY());
    }

    @Test
    void eachSlotMakesOnlyTheBoundedNumberOfAttempts() {
        WorldScene scene = WorldgenFixtures.sceneOf(ROOMY, ROOMY, CoreTiles.WATER);
        CountingRandom random = new CountingRandom();

        BuildingScatter.scatter(scene, WorldgenFixtures.MODS, random);

        int drawsPerSlot = 1 + 2 * BuildingScatter.ATTEMPTS_PER_BUILDING;
        assertEquals(BuildingScatter.TARGET_BUILDINGS * drawsPerSlot, random.draws);
    }

    private static ModRegistry registryOf(Building building) {
        return RegistryFixtures.withBuildings(Map.of("test:building", building));
    }

    /** Counts every bounded draw, which is how the scatter asks for a blueprint or a position. */
    private static final class CountingRandom extends Random {
        private static final long serialVersionUID = 1L;
        private int draws;

        @Override
        public int nextInt(int bound) {
            draws++;
            return super.nextInt(bound);
        }
    }

    private static List<Building> scatter(WorldScene scene) {
        return BuildingScatter.scatter(scene, WorldgenFixtures.MODS, new Random(SEED));
    }
}
