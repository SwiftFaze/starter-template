package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.world.WorldScene;

import com.swiftfaze.veil.entities.buildings.Building;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

/**
 * Finds where the player starts: the walkable tile nearest the map centre that no building
 * covers. Rings are searched outward by Chebyshev distance, each ring in row-major order, so
 * the answer is deterministic for a given world.
 */
final class SpawnFinder {

    private final WorldScene scene;
    private final List<Building> buildings;

    private SpawnFinder(WorldScene scene, List<Building> buildings) {
        this.scene = scene;
        this.buildings = List.copyOf(buildings);
    }

    /** Falls back to the centre when nothing in the map is walkable and free. */
    static Point nearestWalkableOutsideBuildings(WorldScene scene, List<Building> buildings) {
        SpawnFinder finder = new SpawnFinder(scene, buildings);
        int centreX = scene.getWidth() / 2;
        int centreY = scene.getHeight() / 2;
        int maxRadius = Math.max(scene.getWidth(), scene.getHeight());
        for (int radius = 0; radius <= maxRadius; radius++) {
            Optional<Point> found = finder.firstInRing(centreX, centreY, radius);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return new Point(centreX, centreY);
    }

    private Optional<Point> firstInRing(int centreX, int centreY, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) {
                    continue;
                }
                int x = centreX + dx;
                int y = centreY + dy;
                if (scene.isWalkable(x, y) && buildings.stream().noneMatch(b -> b.covers(x, y))) {
                    return Optional.of(new Point(x, y));
                }
            }
        }
        return Optional.empty();
    }
}
