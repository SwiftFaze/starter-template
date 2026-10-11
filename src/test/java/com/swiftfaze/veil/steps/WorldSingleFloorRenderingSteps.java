package com.swiftfaze.veil.steps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorldSingleFloorRenderingSteps {

    private static final Tile GRASS = new Tile("test:grass", ',', Color.GREEN, true);
    private static final Tile WATER = new Tile("test:water", '~', Color.BLUE, false);

    private WorldScene scene;
    private Player player;
    private Building building;

    @Given("a world scene {int} tiles wide and {int} tiles tall")
    public void aWorldSceneTilesWideAndTilesTall(int width, int height) {
        scene = new WorldScene(width, height) {
        };
        scene.fillAll(GRASS);
    }

    @Then("looking up a tile takes only an \\(x, y) position, not a floor")
    public void lookingUpATileTakesOnlyAnXyPositionNotAFloor() {
        assertNotNull(scene.getTile(0, 0));
    }

    @Given("a player at position \\({int}, {int})")
    public void aPlayerAtPosition(int x, int y) {
        player = new Player(x, y);
    }

    @Given("tile \\({int}, {int}) is walkable")
    public void tileIsWalkable(int x, int y) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), GRASS);
    }

    @Given("tile \\({int}, {int}) is not walkable")
    public void tileIsNotWalkable(int x, int y) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), WATER);
    }

    @When("the player moves up")
    public void thePlayerMovesUp() {
        GamePanelFixture game = SharedScenarioContext.getGameFixture();
        if (game != null) {
            game.pressMoveUp();
        } else {
            player.moveUp(scene);
        }
    }

    @When("the player moves right")
    public void thePlayerMovesRight() {
        player.moveRight(scene);
    }

    @Then("the player's position is \\({int}, {int})")
    public void thePlayersPositionIs(int x, int y) {
        assertEquals(x, player.getX());
        assertEquals(y, player.getY());
    }

    @Then("the player's position is still \\({int}, {int})")
    public void thePlayersPositionIsStill(int x, int y) {
        assertEquals(x, player.getX());
        assertEquals(y, player.getY());
    }

    @When("building {string} is loaded")
    public void buildingIsLoaded(String fileName) {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        building = registry.getBuilding("core:" + fileName.replace(".json", ""));
    }

    @Then("the building has a single 2D tile layer")
    public void theBuildingHasASingle2DTileLayer() {
        assertFalse(building.getBlueprint().isEmpty());
    }

    @Then("the building's width and height match the JSON's {string} and {string} fields")
    public void theBuildingsWidthAndHeightMatchTheJsonsFields(String widthField, String heightField) throws Exception {
        JsonObject json;
        try (Reader reader = Files.newBufferedReader(Paths.get("mods", "core", "buildings", "small_house_01.json"))) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }

        int declaredWidth = json.get(widthField).getAsInt();
        int declaredHeight = json.get(heightField).getAsInt();

        List<List<Tile>> blueprint = building.getBlueprint();
        assertEquals(declaredHeight, blueprint.size());
        assertEquals(declaredWidth, blueprint.get(0).size());
    }
}
