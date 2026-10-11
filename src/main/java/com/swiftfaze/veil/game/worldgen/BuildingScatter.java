package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.world.WorldScene;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModRegistry;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Places up to {@link #TARGET_BUILDINGS} buildings on a scene that already holds terrain.
 * A building fits when every footprint tile is walkable and free, and every tile outside
 * each of its doors is walkable and free too. Each slot gets a bounded number of attempts,
 * so a crowded map never hangs generation.
 */
final class BuildingScatter {

    static final long SEED_SALT = 0x9E3779B97F4A7C15L;
    static final int TARGET_BUILDINGS = 5;
    static final int ATTEMPTS_PER_BUILDING = 100;

    private final WorldScene scene;
    private final ModRegistry mods;
    private final Random random;
    private final boolean[][] footprint;
    private final boolean[][] doorApproach;

    private BuildingScatter(WorldScene scene, ModRegistry mods, Random random) {
        this.scene = scene;
        this.mods = mods;
        this.random = random;
        this.footprint = new boolean[scene.getWidth()][scene.getHeight()];
        this.doorApproach = new boolean[scene.getWidth()][scene.getHeight()];
    }

    static List<Building> scatter(WorldScene scene, ModRegistry mods, Random random) {
        return new BuildingScatter(scene, mods, random).placeAll();
    }

    private List<Building> placeAll() {
        List<Building> placed = new ArrayList<>();
        List<String> ids = mods.getBuildingIds();
        if (ids.isEmpty()) {
            return placed;
        }
        for (int slot = 0; slot < TARGET_BUILDINGS; slot++) {
            Building template = mods.getBuilding(ids.get(random.nextInt(ids.size())));
            tryPlace(template).ifPresent(placed::add);
        }
        return placed;
    }

    private Optional<Building> tryPlace(Building template) {
        int width = template.getWidth();
        int height = template.getHeight();
        if (width < 1 || height < 1 || width > scene.getWidth() || height > scene.getHeight()) {
            return Optional.empty();
        }
        for (int attempt = 0; attempt < ATTEMPTS_PER_BUILDING; attempt++) {
            Building candidate = new Building(template.getBlueprint(), template.getObjects());
            candidate.setWorldX(random.nextInt(scene.getWidth() - width + 1));
            candidate.setWorldY(random.nextInt(scene.getHeight() - height + 1));
            if (fits(candidate)) {
                commit(candidate);
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private boolean fits(Building candidate) {
        for (int x = 0; x < candidate.getWidth(); x++) {
            for (int y = 0; y < candidate.getHeight(); y++) {
                if (!isFreeFootprintTile(candidate.getWorldX() + x, candidate.getWorldY() + y)) {
                    return false;
                }
            }
        }
        return DoorApproaches.outsideDoorsOf(candidate).stream().allMatch(this::isFreeApproachTile);
    }

    /** Walkable, not already footprint, and not a door approach kept clear for another building. */
    private boolean isFreeFootprintTile(int x, int y) {
        return scene.isWalkable(x, y) && !footprint[x][y] && !doorApproach[x][y];
    }

    /** Walkable and not under any building's footprint. Approaches may coincide with each other. */
    private boolean isFreeApproachTile(Point tile) {
        return scene.isWalkable(tile.x, tile.y) && !footprint[tile.x][tile.y];
    }

    private void commit(Building building) {
        for (int x = 0; x < building.getWidth(); x++) {
            for (int y = 0; y < building.getHeight(); y++) {
                footprint[building.getWorldX() + x][building.getWorldY() + y] = true;
            }
        }
        for (Point tile : DoorApproaches.outsideDoorsOf(building)) {
            doorApproach[tile.x][tile.y] = true;
        }
        scene.placeBuilding(building);
    }
}
