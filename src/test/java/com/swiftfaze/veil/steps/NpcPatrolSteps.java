package com.swiftfaze.veil.steps;

import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.npc.NonPlayerCharacter;
import com.swiftfaze.veil.entities.npc.TileCoord;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps for npc-patrol-framework.feature. NPCs reach the world the way Main spawns them: written
 * as mod files, loaded by ModLoader, then spawned from the registry via GamePanel.spawnNpcs. The
 * player and screens come from the turn-tick fixture shared through SharedScenarioContext, so
 * "When the player steps right" is the one defined in TurnTickEngineSteps.
 */
public class NpcPatrolSteps {

    private static final Pattern COORD = Pattern.compile("\\((\\d+), (\\d+)\\)");

    /** Every NPC definition in this scenario, in the order they were defined or spawned. */
    private final List<JsonObject> definitions = new ArrayList<>();
    private Path scenarioRoot;

    @Before
    public void createTempRoot() throws IOException {
        scenarioRoot = Files.createTempDirectory("veil-npc-world");
    }

    @After
    public void deleteTempRoot() throws IOException {
        try (Stream<Path> paths = Files.walk(scenarioRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    @Given("a turn-ticking game on an all-walkable map with the player at \\({int}, {int}\\)")
    public void aTurnTickingGameOnAnAllWalkableMap(int x, int y) {
        GamePanelFixture game = GamePanelFixture.withAllWalkableMap();
        game.movePlayerTo(x, y);
        SharedScenarioContext.setTurnGame(game);
    }

    @Given("a turn-ticking game on a map where tile \\({int}, {int}\\) is not walkable, with the player at \\({int}, {int}\\)")
    public void aTurnTickingGameWithABlockedTile(int blockedX, int blockedY, int playerX, int playerY) {
        GamePanelFixture game = GamePanelFixture.withAllWalkableMap();
        game.blockAt(blockedX, blockedY);
        game.movePlayerTo(playerX, playerY);
        SharedScenarioContext.setTurnGame(game);
    }

    @Given("^an NPC \"([^\"]+)\" with symbol \"([^\"]+)\" and color (\\d+),(\\d+),(\\d+) is defined with spawn \\((\\d+), (\\d+)\\) and no patrol$")
    public void anNpcIsDefinedWithSymbolAndColorAndSpawn(String id, String symbol, int red, int green, int blue, int x, int y) {
        definitions.add(NpcJson.npc(id).symbol(symbol).color(red, green, blue).spawn(x, y).toJson());
    }

    @When("the NPCs are spawned into the game world")
    public void theNpcsAreSpawnedIntoTheGameWorld() throws IOException {
        spawnAllDefinitions();
    }

    @Then("the world draws an NPC at \\({int}, {int}\\) with symbol {string} and color {int},{int},{int}")
    public void theWorldDrawsAnNpcAt(int x, int y, String symbol, int red, int green, int blue) {
        Color expectedColor = new Color(red, green, blue);
        boolean drawn = game().panel().getEntitiesToDraw().stream()
                .anyMatch(entity -> entity instanceof DrawableAsciiEntity drawable
                        && drawable.getX() == x && drawable.getY() == y
                        && drawable.getSymbol() == symbol.charAt(0)
                        && expectedColor.equals(drawable.getColor()));
        assertTrue(drawn, "expected an NPC drawn at (" + x + ", " + y + ") with symbol " + symbol);
    }

    @Then("the NPC is registered with the turn scheduler")
    public void theNpcIsRegisteredWithTheTurnScheduler() {
        assertEquals(1, game().registeredTurnListeners());
    }

    @Given("^an NPC \"([^\"]+)\" with spawn \\((\\d+), (\\d+)\\) and no patrol is spawned$")
    public void anNpcWithSpawnAndNoPatrolIsSpawned(String id, int x, int y) throws IOException {
        spawnOne(NpcJson.npc(id).spawn(x, y).toJson());
    }

    @Given("^an NPC \"([^\"]+)\" with a \"(\\w+)\" patrol of waypoints (.+) is spawned$")
    public void anNpcWithAPatrolIsSpawned(String id, String mode, String waypointText) throws IOException {
        List<TileCoord> waypoints = waypointsIn(waypointText);
        NpcJson npc = NpcJson.npc(id).spawn(waypoints.get(0).x(), waypoints.get(0).y())
                .patrol(mode, waypoints.toArray(TileCoord[]::new));
        spawnOne(npc.toJson());
    }

    @Given("{int} turns have passed")
    public void turnsHavePassed(int turns) {
        advanceTurns(turns);
    }

    @When("{int} turn(s) pass(es)")
    public void turnsPass(int turns) {
        advanceTurns(turns);
    }

    @When("map tile \\({int}, {int}\\) becomes walkable")
    public void mapTileBecomesWalkable(int x, int y) {
        game().openFloorAt(x, y);
    }

    @When("the game state is reset by exiting to the main menu and the NPCs are spawned again")
    public void theGameStateIsResetAndTheNpcsAreSpawnedAgain() throws IOException {
        game().panel().resetState();
        spawnAllDefinitions();
    }

    @Then("the NPC is at \\({int}, {int}\\)")
    public void theNpcIsAt(int x, int y) {
        List<NonPlayerCharacter> npcs = game().panel().getNpcs();
        assertEquals(1, npcs.size(), "expected exactly one NPC in the world");
        assertPosition(npcs.get(0), x, y);
    }

    @Then("the NPC {string} is at \\({int}, {int}\\)")
    public void theNpcNamedIsAt(String id, int x, int y) {
        NonPlayerCharacter npc = game().panel().getNpcs().stream()
                .filter(candidate -> candidate.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected an NPC with ID " + id));
        assertPosition(npc, x, y);
    }

    @Then("exactly one NPC is registered with the turn scheduler")
    public void exactlyOneNpcIsRegisteredWithTheTurnScheduler() {
        assertEquals(1, game().registeredTurnListeners());
    }

    private static GamePanelFixture game() {
        return SharedScenarioContext.getTurnGame();
    }

    private static void advanceTurns(int turns) {
        for (int i = 0; i < turns; i++) {
            game().advanceTurn();
        }
    }

    /** Records an NPC and spawns just that one, so each "is spawned" step adds exactly one. */
    private void spawnOne(JsonObject npc) throws IOException {
        definitions.add(npc);
        game().panel().spawnNpcs(registryOf(List.of(npc)));
    }

    /** Spawns every NPC defined so far; used where the world is set up from scratch. */
    private void spawnAllDefinitions() throws IOException {
        game().panel().spawnNpcs(registryOf(definitions));
    }

    /** Writes the NPCs as a "core" mod in a fresh directory and loads it through the real registry. */
    private ModRegistry registryOf(List<JsonObject> npcs) throws IOException {
        Path modsRoot = Files.createTempDirectory(scenarioRoot, "mods");
        Path coreDir = modsRoot.resolve("core");
        Path npcsDir = Files.createDirectories(coreDir.resolve("npcs"));
        Files.writeString(coreDir.resolve("mod.json"), "{\"id\":\"core\",\"dependsOn\":[]}");
        for (int i = 0; i < npcs.size(); i++) {
            Files.writeString(npcsDir.resolve("npc_" + i + ".json"), npcs.get(i).toString());
        }
        return ModLoader.load(modsRoot);
    }

    private static List<TileCoord> waypointsIn(String text) {
        List<TileCoord> waypoints = new ArrayList<>();
        Matcher matcher = COORD.matcher(text);
        while (matcher.find()) {
            waypoints.add(new TileCoord(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        }
        return waypoints;
    }

    private static void assertPosition(NonPlayerCharacter npc, int x, int y) {
        assertEquals(new TileCoord(x, y), new TileCoord(npc.getX(), npc.getY()), "NPC " + npc.getId() + " position");
    }
}
