package com.swiftfaze.veil.steps;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.DevConsoleEntry;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.KitchenSinkModel;
import com.swiftfaze.veil.sandbox.KitchenSinkPreviewPanel;
import com.swiftfaze.veil.sandbox.KitchenSinkProvider;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class KitchenSinkSandboxSteps {

    private static final String PREVIEW_NOT_SHOWN = "Preview panel not shown";
    private static final String KITCHEN_SINK_ENTRY_ID = "sandbox:kitchen-sink";
    private static final Function<Tile, Color> GLYPH_COLOR = Tile::getColor;

    private KitchenSinkPreviewPanel previewPanel;
    private KitchenSinkModel model;
    private WorldScene scene;
    private int markerXBeforeMove;
    private int markerYBeforeMove;

    @Given("the kitchen-sink entry is opened")
    public void theKitchenSinkEntryIsOpened() {
        Component opened = console().openEntry(KITCHEN_SINK_ENTRY_ID);
        if (opened instanceof KitchenSinkPreviewPanel panel) {
            previewPanel = panel;
            model = panel.getModel();
            scene = model.scene();
            SharedScenarioContext.setKitchenSinkPreviewPanel(panel);
        }
    }

    @Then("the results include exactly one entry from the {string} provider")
    public void theResultsIncludeExactlyOneEntryFromTheProvider(String providerName) {
        List<DevConsoleModel.SearchResult> results = consoleResults();
        Assertions.assertEquals(1, results.size(), "Expected exactly one entry from " + providerName + " provider");
    }

    @Then("that entry has namespace {string}, id {string}, category {string} and name {string}")
    public void thatEntryHasNamespaceIdCategoryAndName(String namespace, String id, String category, String name) {
        List<DevConsoleModel.SearchResult> results = consoleResults();
        Assertions.assertFalse(results.isEmpty(), "No results found");
        DevConsoleEntry entry = results.get(0).entry();
        Assertions.assertEquals(namespace, entry.namespace());
        Assertions.assertEquals(id, entry.id());
        Assertions.assertEquals(category, entry.category());
        Assertions.assertEquals(name, entry.name());
    }

    @Then("the scene contains every tile in the mod registry")
    public void theSceneContainsEveryTileInTheModRegistry() {
        assertPreviewShown();
        List<Tile> allTiles = ModLoader.load(Paths.get("mods")).getAllTiles();

        Set<String> present = new HashSet<>();
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                Tile tile = scene.getTile(x, y);
                if (tile != null) {
                    present.add(tile.getId());
                }
            }
        }
        long tilesFound = allTiles.stream().filter(tile -> present.contains(tile.getId())).count();
        Assertions.assertEquals((long) allTiles.size(), tilesFound,
                "Not all tiles from registry are in the scene");
    }

    @Then("the marker starts on a walkable tile")
    public void theMarkerStartsOnAWalkableTile() {
        assertPreviewShown();
        Assertions.assertTrue(scene.isWalkable(model.getMarkerX(), model.getMarkerY()),
                "Marker does not start on a walkable tile");
    }

    @Given("the marker stands on a walkable tile whose {word} neighbour is walkable")
    public void theMarkerStandsOnAWalkableTileWhoseNeighbourIsWalkable(String direction) {
        assertPreviewShown();
        teleportToWalkableTileWithNeighbour(direction, /* neighbourWalkable= */ true);
    }

    @Given("the marker's {word} neighbour is unwalkable")
    public void theMarkerSNeighbourIsUnwalkable(String direction) {
        assertPreviewShown();
        teleportToWalkableTileWithNeighbour(direction, /* neighbourWalkable= */ false);
    }

    @Then("the marker has moved one tile {word}")
    public void theMarkerHasMovedOneDirection(String direction) {
        assertPreviewShown();
        int[] delta = delta(direction);
        String message = "Marker did not move " + direction;
        if (delta[0] == 0) {
            Assertions.assertEquals(markerYBeforeMove + delta[1], model.getMarkerY(), message);
        } else {
            Assertions.assertEquals(markerXBeforeMove + delta[0], model.getMarkerX(), message);
        }
    }

    @Then("the marker has not moved")
    public void theMarkerHasNotMoved() {
        assertPreviewShown();
        Assertions.assertEquals(markerXBeforeMove, model.getMarkerX(), "Marker X position changed");
        Assertions.assertEquals(markerYBeforeMove, model.getMarkerY(), "Marker Y position changed");
    }

    @Given("the marker stands on the scene's {word} edge")
    public void theMarkerStandsOnTheScenesEdge(String edge) {
        assertPreviewShown();
        int edgeX = "right".equalsIgnoreCase(edge) ? scene.getWidth() - 1 : 0;
        for (int y = 0; y < scene.getHeight(); y++) {
            if (scene.isWalkable(edgeX, y)) {
                teleportTo(edgeX, y);
                return;
            }
        }
        throw new AssertionError("No walkable tile on the " + edge + " edge");
    }

    @Given("the walkability overlay is off")
    public void theWalkabilityOverlayIsOff() {
        assertPreviewShown();
        setOverlay(false);
        Assertions.assertFalse(model.isOverlayOn(), "Walkability overlay is on, expected off");
    }

    @Given("the walkability overlay is on")
    public void theWalkabilityOverlayIsOn() {
        assertPreviewShown();
        setOverlay(true);
        Assertions.assertTrue(model.isOverlayOn(), "Walkability overlay is off, expected on");
    }

    @Then("every cell is marked walkable or unwalkable as WorldScene.isWalkable reports it")
    public void everyCellIsMarkedWalkableOrUnwalkable() {
        BufferedImage overlayOff = renderWithOverlay(false);
        BufferedImage overlayOn = renderWithOverlay(true);
        Set<Integer> walkableTints = new HashSet<>();
        Set<Integer> unwalkableTints = new HashSet<>();
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                Point pixel = backgroundPixelIn(overlayOff, x, y);
                if (pixel == null) {
                    continue; // glyph fills the whole cell, so the tint behind it can't show
                }
                Assertions.assertNotEquals(overlayOff.getRGB(pixel.x, pixel.y), overlayOn.getRGB(pixel.x, pixel.y),
                        "Cell " + x + "," + y + " not tinted");
                Set<Integer> tints = scene.isWalkable(x, y) ? walkableTints : unwalkableTints;
                tints.add(overlayOn.getRGB(pixel.x, pixel.y));
            }
        }
        Assertions.assertFalse(walkableTints.isEmpty() || unwalkableTints.isEmpty(), "Scene needs both kinds of cell");
        Assertions.assertEquals(1, walkableTints.size(), "Walkable cells don't share one tint");
        Assertions.assertEquals(1, unwalkableTints.size(), "Unwalkable cells don't share one tint");
        Assertions.assertNotEquals(walkableTints, unwalkableTints, "Both kinds share a tint");
    }

    @Then("no walkability marking is drawn")
    public void noWalkabilityMarkingIsDrawn() {
        Assertions.assertFalse(model.isOverlayOn(), "Overlay is still on");
        BufferedImage now = render();
        BufferedImage overlayOff = renderWithOverlay(false);
        Assertions.assertEquals(WidgetTheme.background().getRGB(), now.getRGB(1, 1), "A cell is still tinted");
        Assertions.assertEquals(overlayOff.getRGB(1, 1), now.getRGB(1, 1));
    }

    @Given("the marker has moved away from its start position")
    public void theMarkerHasMovedAwayFromItsStartPosition() {
        assertPreviewShown();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                boolean stepsAnywhere = dx != 0 || dy != 0;
                if (stepsAnywhere && scene.isWalkable(model.getMarkerX() + dx, model.getMarkerY() + dy)) {
                    model.moveMarker(dx, dy);
                    return;
                }
            }
        }
        throw new AssertionError("Could not move marker away from start position");
    }

    @Then("the marker is at its start position")
    public void theMarkerIsAtItsStartPosition() {
        assertPreviewShown();
        // The start position is the first walkable tile with a walkable neighbour, scanning row by row
        // (KitchenSinkModel.initializeMarkerPosition); with none, the first walkable tile.
        Point start = findStart(true).or(() -> findStart(false))
                .orElseThrow(() -> new AssertionError("No walkable tile to start on"));
        Assertions.assertEquals(start.x, model.getMarkerX(), "Marker X not at start");
        Assertions.assertEquals(start.y, model.getMarkerY(), "Marker Y not at start");
    }

    private Optional<Point> findStart(boolean needsWalkableNeighbour) {
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.isWalkable(x, y) && (!needsWalkableNeighbour || hasWalkableNeighbour(x, y))) {
                    return Optional.of(new Point(x, y));
                }
            }
        }
        return Optional.empty();
    }

    private boolean hasWalkableNeighbour(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && scene.isWalkable(x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Then("the opened detail panel shows {string}")
    public void theOpenedDetailPanelShows(String text) {
        Assertions.assertTrue(console().isDetailPanelShowing(), "Provider panel not shown");
        boolean found = Arrays.stream(previewPanel.getComponents())
                .anyMatch(component -> component instanceof JLabel label && text.equals(label.getText()));
        Assertions.assertTrue(found, "No label reading \"" + text + "\" in the opened panel");
    }

    @Then("no preview is drawn")
    public void noPreviewIsDrawn() {
        Assertions.assertFalse(previewPanel.isPreviewShown(), "Preview drawn for an empty registry");
    }

    // Anchored regex, not {word}: {word} would also swallow the quoted keys of the
    // `the {string} key is pressed` step in UiComponentFrameworkSteps and make both ambiguous.
    @When("^the ([A-Za-z]+) key is pressed$")
    public void theNamedKeyIsPressed(String key) {
        SharedScenarioContext.getUiSteps().theKeyIsPressed(key);
    }

    @Given("the mod registry contains no tiles")
    public void theModRegistryContainsNoTiles() {
        KitchenSinkScene empty = KitchenSinkScene.holding(List.of());
        SharedScenarioContext.getDevConsoleSteps().runWith(new KitchenSinkProvider(() -> empty));
    }

    @Then("the marking is a background tint, so each tile's glyph is still drawn")
    public void theMarkingIsABackgroundTint() {
        BufferedImage overlayOff = renderWithOverlay(false);
        BufferedImage overlayOn = renderWithOverlay(true);
        int glyphPixels = 0;
        for (int y = 0; y < scene.getHeight() * GameConst.TILE_HEIGHT; y++) {
            for (int x = 0; x < scene.getWidth() * GameConst.TILE_WIDTH; x++) {
                Tile tile = scene.getTile(x / GameConst.TILE_WIDTH, y / GameConst.TILE_HEIGHT);
                if (tile != null && overlayOff.getRGB(x, y) == glyphRgb(tile)) {
                    glyphPixels++;
                    Assertions.assertEquals(overlayOff.getRGB(x, y), overlayOn.getRGB(x, y),
                            "Tint covers a glyph pixel at " + x + "," + y);
                }
            }
        }
        Assertions.assertTrue(glyphPixels > 0, "No glyph pixels found to compare");
    }

    private static int glyphRgb(Tile tile) {
        Color glyphColor = GLYPH_COLOR.apply(tile);
        return glyphColor.getRGB();
    }

    private void assertPreviewShown() {
        Assertions.assertNotNull(previewPanel, PREVIEW_NOT_SHOWN);
    }

    private static DevConsoleSteps console() {
        return SharedScenarioContext.getDevConsoleSteps();
    }

    private static List<DevConsoleModel.SearchResult> consoleResults() {
        return console().currentResults();
    }

    private static int[] delta(String direction) {
        return switch (direction.toLowerCase(Locale.ROOT)) {
            case "up" -> new int[]{0, -1};
            case "down" -> new int[]{0, 1};
            case "left" -> new int[]{-1, 0};
            case "right" -> new int[]{1, 0};
            default -> throw new IllegalArgumentException("Unknown direction: " + direction);
        };
    }

    private void teleportToWalkableTileWithNeighbour(String direction, boolean neighbourWalkable) {
        int[] delta = delta(direction);
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.isWalkable(x, y) && scene.isWalkable(x + delta[0], y + delta[1]) == neighbourWalkable) {
                    teleportTo(x, y);
                    return;
                }
            }
        }
        throw new AssertionError("No walkable tile whose " + direction + " neighbour is "
                + (neighbourWalkable ? "walkable" : "unwalkable"));
    }

    private void teleportTo(int x, int y) {
        model.moveMarker(x - model.getMarkerX(), y - model.getMarkerY());
        markerXBeforeMove = model.getMarkerX();
        markerYBeforeMove = model.getMarkerY();
    }

    private void setOverlay(boolean wanted) {
        if (model.isOverlayOn() != wanted) {
            model.toggleOverlay();
        }
    }

    private static @Nullable Point backgroundPixelIn(BufferedImage image, int cellX, int cellY) {
        for (int dy = 0; dy < GameConst.TILE_HEIGHT; dy++) {
            for (int dx = 0; dx < GameConst.TILE_WIDTH; dx++) {
                int pixelX = cellX * GameConst.TILE_WIDTH + dx;
                int pixelY = cellY * GameConst.TILE_HEIGHT + dy;
                if (image.getRGB(pixelX, pixelY) == WidgetTheme.background().getRGB()) {
                    return new Point(pixelX, pixelY);
                }
            }
        }
        return null;
    }

    private BufferedImage renderWithOverlay(boolean overlayOn) {
        boolean original = model.isOverlayOn();
        setOverlay(overlayOn);
        BufferedImage image = render();
        setOverlay(original);
        return image;
    }

    private BufferedImage render() {
        int width = scene.getWidth() * GameConst.TILE_WIDTH;
        int height = scene.getHeight() * GameConst.TILE_HEIGHT;
        previewPanel.setSize(width, height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        previewPanel.paint(g);
        g.dispose();
        return image;
    }
}
