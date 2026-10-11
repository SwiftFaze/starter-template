package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.world.PerlinNoise;
import com.swiftfaze.veil.world.TerrainBuckets;
import com.swiftfaze.veil.world.WorldScene;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;

import java.awt.Point;
import java.awt.Rectangle;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleBinaryOperator;

/**
 * A world whose terrain and building layout both derive from one seed. Terrain is Perlin
 * noise bucketed into core tiles; buildings are scattered onto walkable ground; the player
 * spawns at the nearest walkable tile outside every building, searched outward from the centre.
 */
public class GeneratedWorldScene extends WorldScene {

    private final List<Building> buildings;
    private final Point spawn;

    /** What the scene is generated from: the blueprints and tiles registered, and the noise field. */
    public record Sources(ModRegistry mods, DoubleBinaryOperator noise) {
    }

    public GeneratedWorldScene(int width, int height, long seed) {
        this(width, height, seed, ModLoader.load(Paths.get("mods")));
    }

    public GeneratedWorldScene(int width, int height, long seed, ModRegistry mods) {
        this(width, height, seed, new Sources(mods, new PerlinNoise(seed)::noise));
    }

    /** As above, with the noise field supplied, so tests can force specific terrain. */
    public GeneratedWorldScene(int width, int height, long seed, Sources sources) {
        super(width, height);
        fillTerrain(this, sources);
        this.buildings = BuildingScatter.scatter(this, sources.mods(), new Random(seed ^ BuildingScatter.SEED_SALT));
        this.spawn = SpawnFinder.nearestWalkableOutsideBuildings(this, buildings);
    }

    private static void fillTerrain(WorldScene scene, Sources sources) {
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                String tileId = TerrainBuckets.tileIdFor(sources.noise().applyAsDouble(x, y));
                scene.fillRegion(new Rectangle(x, y, 1, 1), sources.mods().getTile(tileId));
            }
        }
    }

    public List<Building> getBuildings() {
        return List.copyOf(buildings);
    }

    public int getSpawnX() {
        return spawn.x;
    }

    public int getSpawnY() {
        return spawn.y;
    }
}
