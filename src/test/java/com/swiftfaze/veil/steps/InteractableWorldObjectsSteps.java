package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.Main;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.world.Viewport;
import com.swiftfaze.veil.world.object.Chest;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.object.InteractableObject;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Interact key, doors and chests on a flat floor, and the chest panel, driven through GamePanel. */
public class InteractableWorldObjectsSteps {

    private static final Pattern DIGIT_KEY = Pattern.compile("[1-9]");
    private static final char OPEN_SYMBOL = '/';
    private static final char CLOSED_SYMBOL = '+';
    private static final char CHEST_SYMBOL = 'C';
    private static final Color DOOR_COLOR = Color.ORANGE;
    private static final Color CHEST_COLOR = Color.YELLOW;
    private static final int FLAT_FLOOR_SIZE = 20;
    private static final int CHEST_X = 6;
    private static final int CHEST_Y = 5;

    private final GameEventLog eventLog = GameEventLog.inMemory();
    private static final Pattern KEY_SEPARATOR = Pattern.compile(", | and ");
    private GamePanelFixture game;

    @Given("a closed door at \\({int}, {int})")
    public void aClosedDoorAt(int x, int y) {
        game().placeObject(x, y, new Door("test:door", DOOR_COLOR, new Door.Glyphs(OPEN_SYMBOL, CLOSED_SYMBOL), false));
    }

    @Given("an open door at \\({int}, {int})")
    public void anOpenDoorAt(int x, int y) {
        game().placeObject(x, y, new Door("test:door", DOOR_COLOR, new Door.Glyphs(OPEN_SYMBOL, CLOSED_SYMBOL), true));
    }

    @Given("another entity stands at \\({int}, {int})")
    public void anotherEntityStandsAt(int x, int y) {
        game().addOccupant(x, y);
    }

    @Given("the player stands at \\({int}, {int})")
    public void thePlayerStandsAt(int x, int y) {
        game().movePlayerTo(x, y);
    }

    @Given("a chest at \\({int}, {int}) with {int} slot containing nothing")
    public void aChestWithNoContents(int x, int y, int slots) {
        game().placeObject(x, y, new Chest("test:chest", CHEST_COLOR, CHEST_SYMBOL, new Chest.Contents(slots, List.of())));
    }

    @Given("a chest at \\({int}, {int}) with {int} slots containing {string} and {string}")
    public void aChestWithContents(int x, int y, int slots, String firstItemId, String secondItemId) {
        List<Item> contents = List.of(coreItem(firstItemId), coreItem(secondItemId));
        game().placeObject(x, y, new Chest("test:chest", CHEST_COLOR, CHEST_SYMBOL, new Chest.Contents(slots, contents)));
    }

    @Given("the chest panel is open")
    public void theChestPanelIsOpen() {
        game().placeObject(CHEST_X, CHEST_Y, new Chest("test:chest", CHEST_COLOR, CHEST_SYMBOL, new Chest.Contents(1, List.of())));
        game().pressKey(Keybindings.INTERACT);
        assertTrue(game().chestPanel().isVisible(), "pressing E beside a chest should open its panel");
    }

    @Given("the world screen is shown")
    public void theWorldScreenIsShown() {
        // The hint bar is static on the world screen, so there is nothing to set up.
    }

    /**
     * Presses keys in order: "E", "Down, Down" or "Down and Up 5 times" (the pair repeated). Also
     * drives the dialogue popup, which shares this step.
     */
    @When("^the player presses ((?:\\w+)(?:(?:, | and )\\w+)*)(?: (\\d+) times)?$")
    public void thePlayerPresses(String keys, @Nullable String times) {
        int repeats = times == null ? 1 : Integer.parseInt(times);
        List<String> names = KEY_SEPARATOR.splitAsStream(keys).toList();
        for (int repeat = 0; repeat < repeats; repeat++) {
            for (String keyName : names) {
                game().pressKey(keyFor(keyName));
            }
        }
    }

    @Then("the door at \\({int}, {int}) is {word}")
    public void theDoorAtIs(int x, int y, String state) {
        InteractableObject object = game().scene().getObject(x, y);
        assertInstanceOf(Door.class, object, "expected a door at (" + x + ", " + y + ")");
        assertEquals("open".equals(state), ((Door) object).isOpen());
    }

    @Then("the tile \\({int}, {int}) is walkable")
    public void theTileIsWalkable(int x, int y) {
        assertTrue(game().scene().isWalkable(x, y), "expected (" + x + ", " + y + ") to be walkable");
    }

    @Then("the tile \\({int}, {int}) is not walkable")
    public void theTileIsNotWalkable(int x, int y) {
        assertFalse(game().scene().isWalkable(x, y), "expected (" + x + ", " + y + ") to be unwalkable");
    }

    @Then("the tile \\({int}, {int}) shows the door's {word} symbol")
    public void theTileShowsTheDoorsSymbol(int x, int y, String state) {
        List<String> rows = game().scene().renderToGrid(new Viewport(0, 0, FLAT_FLOOR_SIZE, FLAT_FLOOR_SIZE), List.of());
        char expected = "open".equals(state) ? OPEN_SYMBOL : CLOSED_SYMBOL;
        assertEquals(expected, rows.get(y).charAt(x));
    }

    @Then("the player is at \\({int}, {int})")
    public void thePlayerIsAt(int x, int y) {
        assertEquals(x, game().playerX());
        assertEquals(y, game().playerY());
    }

    @Then("no panel is shown")
    public void noPanelIsShown() {
        assertFalse(game().chestPanel().isVisible());
    }

    @Then("the chest panel is shown")
    public void theChestPanelIsShown() {
        assertTrue(game().chestPanel().isVisible());
    }

    @Then("the chest panel is not shown")
    public void theChestPanelIsNotShown() {
        assertFalse(game().chestPanel().isVisible());
    }

    /** Shared with dialogue-tree.feature: the other screens' keys must not open them over a popup. */
    @Then("^the (inventory|codex) is not shown$")
    public void theInventoryOrCodexIsNotShown(String screen) {
        boolean shown = "inventory".equals(screen)
                ? game().inventoryPanel().isVisible()
                : game().codexPanel().isVisible();
        assertFalse(shown, "expected the " + screen + " not to be shown");
    }

    @Then("it lists the names of {string} and {string}")
    public void itListsTheNamesOf(String firstItemId, String secondItemId) {
        List<String> labels = game().chestPanel().slotLabels();
        assertTrue(labels.containsAll(List.of(coreItem(firstItemId).getName(), coreItem(secondItemId).getName())),
                "panel lists " + labels);
    }

    @Then("it lists {int} slot as {string}")
    public void itListsSlotAs(int count, String label) {
        long matching = game().chestPanel().slotLabels().stream().filter(label::equals).count();
        assertEquals(count, matching, "panel lists " + game().chestPanel().slotLabels());
    }

    @Then("the event log shows {string}")
    public void theEventLogShows(String message) {
        boolean recorded = eventLog.getEvents().stream()
                .anyMatch(event -> event instanceof GameEvent.InteractionRefused refused && refused.reason().equals(message));
        assertTrue(recorded, "no refusal event with reason '" + message + "' in " + eventLog.getEvents());
    }

    @Then("the controls hint bar includes {string}")
    public void theControlsHintBarIncludes(String formatted) {
        int closeBracket = formatted.indexOf(']');
        ControlsHintBarWidget.Hint expected = new ControlsHintBarWidget.Hint(
                formatted.substring(1, closeBracket).toLowerCase(Locale.ROOT), formatted.substring(closeBracket + 2));
        assertTrue(Main.gameHints().contains(expected), "world screen hints are " + Main.gameHints());
    }

    /** The scenario's flat-floor game, shared with the dialogue steps; the first step to need it builds it. */
    private GamePanelFixture game() {
        if (game == null) {
            game = SharedScenarioContext.getGameFixture();
        }
        if (game == null) {
            game = GamePanelFixture.onFlatFloor(eventLog);
            SharedScenarioContext.setGameFixture(game);
        }
        if (SharedScenarioContext.getTurnGame() == null) {
            SharedScenarioContext.setTurnGame(game);
        }
        return game;
    }

    private static KeyStroke keyFor(String keyName) {
        if (DIGIT_KEY.matcher(keyName).matches()) {
            return KeyStroke.getKeyStroke(KeyEvent.VK_0 + Integer.parseInt(keyName), 0);
        }
        return switch (keyName) {
            case "E" -> Keybindings.INTERACT;
            case "I" -> Keybindings.TOGGLE_INVENTORY;
            case "X" -> Keybindings.TOGGLE_CODEX;
            case "Esc" -> Keybindings.MENU_CANCEL;
            case "Up" -> Keybindings.MENU_UP;
            case "Down" -> Keybindings.MENU_DOWN;
            case "Enter" -> Keybindings.MENU_CONFIRM;
            default -> throw new IllegalArgumentException("No key mapped for: " + keyName);
        };
    }

    private static Item coreItem(String itemId) {
        ModRegistry registry = ModLoader.load(Paths.get("mods"));
        Item item = registry.getItem(itemId);
        assertTrue(item != null, "core mod should define item " + itemId);
        return item;
    }
}
