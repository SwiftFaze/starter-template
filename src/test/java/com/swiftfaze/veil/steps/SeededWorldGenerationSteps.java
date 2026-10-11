package com.swiftfaze.veil.steps;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.CoreTiles;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.world.PerlinNoise;
import com.swiftfaze.veil.world.TerrainBuckets;
import com.swiftfaze.veil.world.WorldSeeds;
import com.swiftfaze.veil.world.object.Door;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;

public class SeededWorldGenerationSteps {

    private static final int MAP_WIDTH = GameConst.DEFAULT_MAP_WIDTH;
    private static final int MAP_HEIGHT = GameConst.DEFAULT_MAP_HEIGHT;
    private static final int SMALL_MAP = 40;
    private static final int DEFAULT_SEED = 12_345;
    private static final long FRESH_SEED_SOURCE = 42;
    private static final String SEED_LOG_PREFIX = "World generated from seed ";
    private static final List<String> CORE_TERRAIN = List.of(CoreTiles.WATER, CoreTiles.SAND, CoreTiles.MUD,
            CoreTiles.GRASS, CoreTiles.TREE, CoreTiles.ROCK, CoreTiles.STONE, CoreTiles.SNOW, CoreTiles.ICE);

    private enum SpawnSetup { CENTRE_CLEAR, CENTRE_WATER, CENTRE_UNDER_BUILDING }

    private ModRegistry realRegistry;
    private ModRegistry registry;
    private DoubleBinaryOperator noise;
    private final TempModsRoot tempModsRoot = new TempModsRoot();
    private long worldSeed;
    private GeneratedWorldScene world;
    private GeneratedWorldScene otherWorld;
    private String bucketedTile;
    private SpawnSetup spawnSetup;
    private int walkableOffset;
    private Set<String> blueprintsRegistered = Set.of();
    private final Set<String> blueprintsSeen = new HashSet<>();
    private GamePanel panel;
    private final LogCapture logCapture = new LogCapture();

    @Before
    public void clearSeedOverride() {
        System.clearProperty(WorldSeeds.PROPERTY);
    }

    @Before
    public void attachLogCapture() {
        logCapture.attach();
    }

    @After
    public void detachLogCapture() {
        logCapture.detach();
    }

    /** The world a fresh game generates for {@code seed} with the real mods. */
    static GeneratedWorldScene worldFromSeed(long seed) {
        return new GeneratedWorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT, seed);
    }

    @After
    public void restoreSeedOverrideAndTempMods() throws IOException {
        System.clearProperty(WorldSeeds.PROPERTY);
        tempModsRoot.delete();
    }

    // --- Determinism -------------------------------------------------------------------------

    @Given("a world generated from seed {int}")
    public void aWorldGeneratedFromSeed(int seed) {
        world = generate(seed);
    }

    @When("another world is generated from seed {int}")
    public void anotherWorldIsGeneratedFromSeed(int seed) {
        otherWorld = generate(seed);
    }

    @Then("both worlds have the same tile at every position")
    public void bothWorldsHaveTheSameTileAtEveryPosition() {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                Assertions.assertEquals(WorldComparison.tileId(world.getTile(x, y)), WorldComparison.tileId(otherWorld.getTile(x, y)), "tile at " + x + "," + y);
            }
        }
    }

    @Then("both worlds have the same buildings at the same positions")
    public void bothWorldsHaveTheSameBuildingsAtTheSamePositions() {
        Assertions.assertEquals(WorldComparison.positions(world), WorldComparison.positions(otherWorld));
    }

    @Then("both worlds spawn the player at the same position")
    public void bothWorldsSpawnThePlayerAtTheSamePosition() {
        Assertions.assertEquals(world.getSpawnX(), otherWorld.getSpawnX());
        Assertions.assertEquals(world.getSpawnY(), otherWorld.getSpawnY());
    }

    @Then("the two worlds differ in at least one tile")
    public void theTwoWorldsDifferInAtLeastOneTile() {
        boolean differ = false;
        for (int x = 0; x < MAP_WIDTH && !differ; x++) {
            for (int y = 0; y < MAP_HEIGHT && !differ; y++) {
                differ = !WorldComparison.tileId(world.getTile(x, y)).equals(WorldComparison.tileId(otherWorld.getTile(x, y)));
            }
        }
        Assertions.assertTrue(differ, "expected seeds 12345 and 12601 to produce different terrain");
    }

    // --- Terrain -----------------------------------------------------------------------------

    @Then("^every tile outside a building is one of (.+)$")
    public void everyTileOutsideABuildingIsOneOf(String expectedList) {
        Assertions.assertEquals(CORE_TERRAIN, List.of(expectedList.split(", ")));
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                if (!coveredByAnyBuilding(world.getBuildings(), x, y)) {
                    Assertions.assertTrue(CORE_TERRAIN.contains(WorldComparison.tileId(world.getTile(x, y))), "tile at " + x + "," + y);
                }
            }
        }
    }

    @When("^the noise value (-?\\d+\\.\\d+) is bucketed$")
    public void theNoiseValueIsBucketed(String value) {
        bucketedTile = TerrainBuckets.tileIdFor(Double.parseDouble(value));
    }

    @Then("the terrain tile is {string}")
    public void theTerrainTileIs(String expected) {
        Assertions.assertEquals(expected, bucketedTile);
    }

    @Then("every tile is one of the core terrain tiles")
    public void everyTileIsOneOfTheCoreTerrainTiles() {
        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                Assertions.assertTrue(CORE_TERRAIN.contains(WorldComparison.tileId(world.getTile(x, y))), "tile at " + x + "," + y);
            }
        }
    }

    // --- Buildings ---------------------------------------------------------------------------

    @Then("^the world contains (\\d+) buildings?$")
    public void theWorldContainsBuildings(int expected) {
        Assertions.assertEquals(expected, world.getBuildings().size());
    }

    @Then("no two placed buildings share a tile")
    public void noTwoPlacedBuildingsShareATile() {
        List<Building> buildings = world.getBuildings();
        for (int i = 0; i < buildings.size(); i++) {
            for (int j = i + 1; j < buildings.size(); j++) {
                for (Point tile : footprint(buildings.get(i))) {
                    Assertions.assertFalse(buildings.get(j).covers(tile.x, tile.y), "overlap at " + tile);
                }
            }
        }
    }

    @Then("every tile under each building's footprint was walkable terrain before placement")
    public void everyFootprintTileWasWalkableTerrainBeforePlacement() {
        PerlinNoise terrain = new PerlinNoise(worldSeed);
        for (Building building : world.getBuildings()) {
            for (Point tile : footprint(building)) {
                String id = TerrainBuckets.tileIdFor(terrain.noise(tile.x, tile.y));
                Assertions.assertTrue(realRegistry().getTile(id).isWalkable(), "unwalkable " + id + " under " + tile);
            }
        }
    }

    @Then("the tile directly outside each building's door is walkable and not under any building")
    public void theTileOutsideEachDoorIsWalkableAndFree() {
        for (Building building : world.getBuildings()) {
            List<Point> doorways = outsideDoorTiles(building);
            Assertions.assertFalse(doorways.isEmpty(), "building has no door");
            for (Point tile : doorways) {
                Assertions.assertTrue(world.isWalkable(tile.x, tile.y), "door opens onto unwalkable " + tile);
                Assertions.assertFalse(coveredByAnyBuilding(world.getBuildings(), tile.x, tile.y), "door opens into a building at " + tile);
            }
        }
    }

    @Then("every building's footprint lies within the map bounds")
    public void everyFootprintLiesWithinTheMapBounds() {
        for (Building building : world.getBuildings()) {
            Assertions.assertTrue(building.getWorldX() >= 0 && building.getWorldY() >= 0);
            Assertions.assertTrue(building.getWorldX() + building.getWidth() <= MAP_WIDTH);
            Assertions.assertTrue(building.getWorldY() + building.getHeight() <= MAP_HEIGHT);
        }
    }

    @Given("^the registered blueprints are \"([^\"]*)\" and \"([^\"]*)\"$")
    public void theRegisteredBlueprintsAre(String first, String second) throws IOException {
        blueprintsRegistered = Set.of(first, second);
        registry = tempModsRoot.load(TempModsRoot.Setup.WITH_HUT);
        Assertions.assertEquals(blueprintsRegistered, new HashSet<>(registry.getBuildingIds()));
    }

    @When("^worlds are generated from seeds (\\d+) to (\\d+)$")
    public void worldsAreGeneratedFromSeeds(int first, int last) {
        for (int seed = first; seed <= last; seed++) {
            for (Building building : generate(seed).getBuildings()) {
                blueprintsSeen.addAll(blueprintIdsMatching(building));
            }
        }
    }

    @Then("both blueprints appear among the placed buildings")
    public void bothBlueprintsAppearAmongThePlacedBuildings() {
        Assertions.assertTrue(blueprintsSeen.containsAll(blueprintsRegistered), "seen " + blueprintsSeen);
    }

    @Given("terrain where every tile is core:water")
    public void terrainWhereEveryTileIsWater() {
        noise = (x, y) -> -1.0;
    }

    @When("the world is generated")
    public void theWorldIsGenerated() {
        world = generate(DEFAULT_SEED);
    }

    @Then("generation completes without error")
    public void generationCompletesWithoutError() {
        Assertions.assertNotNull(world);
    }

    @Given("no building blueprints are registered")
    public void noBuildingBlueprintsAreRegistered() throws IOException {
        registry = tempModsRoot.load(TempModsRoot.Setup.NO_BUILDINGS);
        Assertions.assertEquals(List.of(), registry.getBuildingIds());
    }

    @When("a world is generated from seed {int}")
    public void aWorldIsGeneratedFromSeed(int seed) {
        world = generate(seed);
    }

    // --- Spawn -------------------------------------------------------------------------------

    @Given("a generated world whose centre tile is walkable and outside any building")
    public void aGeneratedWorldWhoseCentreIsWalkableAndClear() {
        spawnSetup = SpawnSetup.CENTRE_CLEAR;
    }

    @Given("a generated world whose centre tile is core:water")
    public void aGeneratedWorldWhoseCentreIsWater() {
        spawnSetup = SpawnSetup.CENTRE_WATER;
    }

    @Given("the nearest walkable tile outside any building is {int} tiles from the centre")
    public void theNearestWalkableTileIsTilesFromTheCentre(int offset) {
        walkableOffset = offset;
    }

    @Given("a generated world whose centre tile is under a building")
    public void aGeneratedWorldWhoseCentreIsUnderABuilding() {
        spawnSetup = SpawnSetup.CENTRE_UNDER_BUILDING;
    }

    @When("the player spawns")
    public void thePlayerSpawns() {
        world = switch (spawnSetup) {
            case CENTRE_CLEAR -> WorldComparison.firstWorld(this::generate, SeededWorldGenerationSteps::centreIsWalkableAndClear);
            case CENTRE_WATER -> generateWaterWithOneWalkableTile();
            case CENTRE_UNDER_BUILDING -> WorldComparison.firstWorld(this::smallGrassWorld, SeededWorldGenerationSteps::blockedAtCentre);
        };
    }

    @Then("the player is at the map centre")
    public void thePlayerIsAtTheMapCentre() {
        Assertions.assertEquals(MAP_WIDTH / 2, world.getSpawnX());
        Assertions.assertEquals(MAP_HEIGHT / 2, world.getSpawnY());
    }

    @Then("the player is on that tile")
    public void thePlayerIsOnThatTile() {
        Assertions.assertEquals(MAP_WIDTH / 2 + walkableOffset, world.getSpawnX());
        Assertions.assertEquals(MAP_HEIGHT / 2, world.getSpawnY());
    }

    @Then("the player is on a walkable tile outside every building footprint")
    public void thePlayerIsOnAWalkableTileOutsideEveryFootprint() {
        int x = world.getSpawnX();
        int y = world.getSpawnY();
        Assertions.assertTrue(world.isWalkable(x, y), "spawn " + x + "," + y + " is not walkable");
        Assertions.assertFalse(coveredByAnyBuilding(world.getBuildings(), x, y), "spawn lies inside a building");
    }

    // --- Seed source -------------------------------------------------------------------------

    @Given("no world-seed override is set")
    public void noWorldSeedOverrideIsSet() {
        System.clearProperty(WorldSeeds.PROPERTY);
    }

    @Given("the world-seed override is {string}")
    public void theWorldSeedOverrideIs(String value) {
        System.setProperty(WorldSeeds.PROPERTY, value);
    }

    @When("two new games are started")
    public void twoNewGamesAreStarted() {
        Random fresh = new Random(FRESH_SEED_SOURCE);
        newGame(fresh);
        newGame(fresh);
    }

    @When("a new game is started")
    public void aNewGameIsStarted() {
        panel = newGame(new Random(FRESH_SEED_SOURCE));
    }

    @Then("^each game logs \"World generated from seed <n>\" at INFO$")
    public void eachGameLogsAtInfo() {
        List<ILoggingEvent> seedLines = worldSeedLines();
        Assertions.assertEquals(2, seedLines.size(), "one log line per new game");
        for (ILoggingEvent line : seedLines) {
            Assertions.assertEquals(Level.INFO, line.getLevel());
            Assertions.assertTrue(line.getFormattedMessage().matches(SEED_LOG_PREFIX + "-?\\d+"), line.getFormattedMessage());
        }
    }

    @Then("the two logged seeds differ")
    public void theTwoLoggedSeedsDiffer() {
        List<String> lines = worldSeedLines().stream().map(ILoggingEvent::getFormattedMessage).toList();
        Assertions.assertNotEquals(lines.get(0), lines.get(1));
    }

    @Then("^the log reads \"World generated from seed (-?\\d+)\"$")
    public void theLogReads(String seed) {
        Assertions.assertTrue(worldSeedLines().stream().anyMatch(e -> e.getFormattedMessage().equals(SEED_LOG_PREFIX + seed)),
                "logged: " + worldSeedLines());
    }

    @Then("^the world matches a world generated from seed (-?\\d+)$")
    public void theWorldMatchesAWorldGeneratedFromSeed(String seed) {
        WorldComparison.assertSameWorld(generate(Long.parseLong(seed)), panel::getScene);
    }

    @Then("^a warning naming \"([^\"]*)\" is logged$")
    public void aWarningNamingIsLogged(String value) {
        Assertions.assertTrue(logCapture.events().stream().anyMatch(e -> e.getLevel() == Level.WARN
                && e.getFormattedMessage().contains(value)), "warnings: " + logCapture.events());
    }

    @Then("the game starts with a world generated from a random seed")
    public void theGameStartsWithARandomSeedWorld() {
        Assertions.assertTrue(panel.getScene() instanceof GeneratedWorldScene);
        Assertions.assertEquals(1, worldSeedLines().size());
    }

    // --- Helpers -----------------------------------------------------------------------------

    private GeneratedWorldScene generate(long seed) {
        worldSeed = seed;
        ModRegistry mods = registry != null ? registry : realRegistry();
        if (noise == null) {
            return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, seed, mods);
        }
        return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, seed, new GeneratedWorldScene.Sources(mods, noise));
    }

    private ModRegistry realRegistry() {
        if (realRegistry == null) {
            realRegistry = ModLoader.load(Paths.get("mods"));
        }
        return realRegistry;
    }

    private GeneratedWorldScene smallGrassWorld(long seed) {
        return new GeneratedWorldScene(SMALL_MAP, SMALL_MAP, seed, new GeneratedWorldScene.Sources(realRegistry(), (x, y) -> 0.0));
    }

    private GeneratedWorldScene generateWaterWithOneWalkableTile() {
        int centre = MAP_WIDTH / 2;
        DoubleBinaryOperator island = (x, y) -> x == centre + walkableOffset && y == centre ? 0.0 : -1.0;
        return new GeneratedWorldScene(MAP_WIDTH, MAP_HEIGHT, 1, new GeneratedWorldScene.Sources(realRegistry(), island));
    }

    private static boolean centreIsWalkableAndClear(GeneratedWorldScene scene) {
        return scene.isWalkable(scene.getWidth() / 2, scene.getHeight() / 2) && !blockedAtCentre(scene);
    }

    private static boolean blockedAtCentre(GeneratedWorldScene scene) {
        return coveredByAnyBuilding(scene.getBuildings(), scene.getWidth() / 2, scene.getHeight() / 2);
    }

    private static boolean coveredByAnyBuilding(List<Building> buildings, int x, int y) {
        return buildings.stream().anyMatch(b -> b.covers(x, y));
    }

    private static List<Point> footprint(Building building) {
        List<Point> tiles = new ArrayList<>();
        for (int x = 0; x < building.getWidth(); x++) {
            for (int y = 0; y < building.getHeight(); y++) {
                tiles.add(new Point(building.getWorldX() + x, building.getWorldY() + y));
            }
        }
        return tiles;
    }

    /** Tiles directly outside each door on an edge row or column (corner doors give two). */
    private static List<Point> outsideDoorTiles(Building building) {
        List<Point> tiles = new ArrayList<>();
        for (Building.ObjectPlacement placement : building.getObjects()) {
            if (placement.object() instanceof Door) {
                tiles.addAll(edgeNeighbours(building, placement.x(), placement.y()));
            }
        }
        return tiles;
    }

    /** The world tiles just beyond each building edge that the blueprint cell (column, row) sits on. */
    private static List<Point> edgeNeighbours(Building building, int column, int row) {
        int worldX = building.getWorldX() + column;
        int worldY = building.getWorldY() + row;
        List<Point> neighbours = new ArrayList<>();
        if (row == 0) {
            neighbours.add(new Point(worldX, worldY - 1));
        }
        if (row == building.getHeight() - 1) {
            neighbours.add(new Point(worldX, worldY + 1));
        }
        if (column == 0) {
            neighbours.add(new Point(worldX - 1, worldY));
        }
        if (column == building.getWidth() - 1) {
            neighbours.add(new Point(worldX + 1, worldY));
        }
        return neighbours;
    }

    /** Ids of every registered blueprint whose tiles match this placed building's. */
    private List<String> blueprintIdsMatching(Building building) {
        List<String> matches = new ArrayList<>();
        for (String id : realOrTestRegistry().getBuildingIds()) {
            if (WorldComparison.tileIds(realOrTestRegistry().getBuilding(id)).equals(WorldComparison.tileIds(building))) {
                matches.add(id);
            }
        }
        return matches;
    }

    private ModRegistry realOrTestRegistry() {
        return registry != null ? registry : realRegistry();
    }

    private static GamePanel newGame(Random fresh) {
        return new GamePanel(GameEventLog.noOp(), WorldSeeds.fromSystemProperty(fresh::nextLong));
    }

    private List<ILoggingEvent> worldSeedLines() {
        return logCapture.events().stream()
                .filter(e -> e.getFormattedMessage().startsWith(SEED_LOG_PREFIX))
                .toList();
    }
}
