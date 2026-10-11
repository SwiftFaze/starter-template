package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnFinderTest {

    private static final int SIZE = 11;
    private static final int CENTRE = 5;

    @Test
    void spawnsOnTheCentreWhenItIsWalkableAndFree() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.GRASS);

        assertEquals(new Point(CENTRE, CENTRE), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void blockedCentreFallsToTheFirstTileOfTheNextRingInRowMajorOrder() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, CENTRE, CENTRE, CoreTiles.WATER);

        assertEquals(new Point(CENTRE - 1, CENTRE - 1), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void ringTilesAreScannedRowByRowWithinTheRing() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.WATER);
        WorldgenFixtures.setTile(scene, CENTRE + 1, CENTRE, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, CENTRE - 1, CENTRE + 1, CoreTiles.GRASS);

        assertEquals(new Point(CENTRE + 1, CENTRE), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void aWalkableTileOnlyOnTheRingCornerIsFoundAtThatDistance() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.WATER);
        WorldgenFixtures.setTile(scene, CENTRE + 2, CENTRE + 2, CoreTiles.GRASS);
        WorldgenFixtures.setTile(scene, CENTRE - 3, CENTRE, CoreTiles.GRASS);

        assertEquals(new Point(CENTRE + 2, CENTRE + 2), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void skipsEveryTileCoveredByABuilding() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.GRASS);
        Building covering = WorldgenFixtures.building(CENTRE - 2, CENTRE - 2, "SSSSS", "SWWWS", "SWWWS", "SWWWS", "SSDSS");

        Point spawn = SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of(covering));

        assertEquals(new Point(CENTRE - 3, CENTRE - 3), spawn);
    }

    @Test
    void findsAWalkableTileFarAcrossAWideMap() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE * 4, 3, CoreTiles.WATER);
        WorldgenFixtures.setTile(scene, SIZE * 4 - 1, 1, CoreTiles.GRASS);

        assertEquals(new Point(SIZE * 4 - 1, 1), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void findsAWalkableTileFarAcrossATallMap() {
        WorldScene scene = WorldgenFixtures.sceneOf(3, SIZE * 4, CoreTiles.WATER);
        WorldgenFixtures.setTile(scene, 1, 0, CoreTiles.GRASS);

        assertEquals(new Point(1, 0), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }

    @Test
    void fallsBackToTheCentreWhenNothingIsWalkable() {
        WorldScene scene = WorldgenFixtures.sceneOf(SIZE, SIZE, CoreTiles.WATER);

        assertEquals(new Point(CENTRE, CENTRE), SpawnFinder.nearestWalkableOutsideBuildings(scene, List.of()));
    }
}
