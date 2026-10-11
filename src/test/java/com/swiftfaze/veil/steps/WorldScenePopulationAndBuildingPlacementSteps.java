package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldScenePopulationAndBuildingPlacementSteps {

    private static final Tile GRASS = new Tile("test:grass", ',', Color.GREEN, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.GRAY, false);
    private static final Tile WOOD = new Tile("test:wood", '+', Color.ORANGE, true);
    private static final Tile STONE = new Tile("test:stone", '%', Color.DARK_GRAY, false);
    private static final Tile DOOR = new Tile("test:door", '/', Color.YELLOW, true);
    private static final Tile WATER = new Tile("test:water", '~', Color.BLUE, false);

    private static final Map<String, Tile> NAMED_TILES = Map.of(
            "GRASS", GRASS,
            "WALL", WALL,
            "WOOD", WOOD,
            "STONE", STONE,
            "DOOR", DOOR
    );

    private WorldScene scene;
    private List<List<Tile>> pendingBlueprint;
    private Building building;

    @Given("an empty world scene {int} tiles wide and {int} tiles tall")
    public void anEmptyWorldSceneTilesWideAndTilesTall(int width, int height) {
        scene = new WorldScene(width, height) {
        };
    }

    @Given("the scene is filled with grass")
    public void theSceneIsFilledWithGrass() {
        scene.fillAll(GRASS);
    }

    @Then("^every tile in the scene is (GRASS|WALL|WOOD|STONE|DOOR)$")
    public void everyTileInTheSceneIs(String tileName) {
        Tile expected = NAMED_TILES.get(tileName);
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                assertEquals(expected, scene.getTile(x, y));
            }
        }
    }

    @Given("tile \\({int}, {int}) is water")
    public void tileIsWater(int x, int y) {
        scene.fillRegion(new Rectangle(x, y, 1, 1), WATER);
    }

    @Given("a building with the following blueprint:")
    public void aBuildingWithTheFollowingBlueprint(DataTable table) {
        List<List<String>> rows = table.asLists();
        pendingBlueprint = rows.stream()
                .map(row -> row.stream().map(cell -> NAMED_TILES.get(cell.trim())).toList())
                .toList();
    }

    @Given("a building with an empty blueprint")
    public void aBuildingWithAnEmptyBlueprint() {
        pendingBlueprint = List.of();
    }

    @Given("the building's world position is set to \\({int}, {int})")
    public void theBuildingsWorldPositionIsSetTo(int worldX, int worldY) {
        building = new Building(pendingBlueprint);
        building.setWorldX(worldX);
        building.setWorldY(worldY);
    }

    @When("the building is placed in the scene")
    public void theBuildingIsPlacedInTheScene() {
        scene.placeBuilding(building);
    }

    @Then("^tile \\((\\d+), (\\d+)\\) is (GRASS|WALL|WOOD|STONE|DOOR)$")
    public void tileIs(int x, int y, String tileName) {
        assertEquals(NAMED_TILES.get(tileName), scene.getTile(x, y));
    }
}
