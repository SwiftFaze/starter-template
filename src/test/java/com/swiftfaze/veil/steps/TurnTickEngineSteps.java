package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.game.Tickable;
import com.swiftfaze.veil.game.TurnScheduler;
import com.swiftfaze.veil.game.event.GameEventLog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps for turn-tick-engine.feature. Screens are opened and closed through the real
 * toggle actions on the game panel, never by poking the world pause directly.
 *
 * <p>The screen steps use an anchored regex, not {@code {word}}, so they cannot capture
 * other features' steps. "closed" is one method for the When in the outline and the Then
 * in "Switching directly between screens": it closes the screen if still open, then
 * asserts it is closed.
 */
public class TurnTickEngineSteps {

    private final List<CountingTickConsumer> consumers = new ArrayList<>();
    private final List<String> steppedOrder = new ArrayList<>();

    @Given("a turn-ticking game with the player at \\({int}, {int}\\)")
    public void aTurnTickingGameWithThePlayerAt(int x, int y) {
        SharedScenarioContext.setTurnGame(GamePanelFixture.withAllScreens(GameEventLog.noOp()));
        game().movePlayerTo(x, y);
    }

    @Given("a counting tick consumer is registered with the turn scheduler")
    public void aCountingTickConsumerIsRegistered() {
        registerConsumer("first");
    }

    @Given("a second counting tick consumer is registered with the turn scheduler")
    public void aSecondCountingTickConsumerIsRegistered() {
        registerConsumer("second");
    }

    @Given("map tile \\({int}, {int}\\) is walkable")
    public void mapTileIsWalkable(int x, int y) {
        game().openFloorAt(x, y);
    }

    @Given("map tile \\({int}, {int}\\) is not walkable")
    public void mapTileIsNotWalkable(int x, int y) {
        game().blockAt(x, y);
    }

    @Given("map tiles \\({int}, {int}\\), \\({int}, {int}\\) and \\({int}, {int}\\) are walkable")
    public void mapTilesAreWalkable(int firstX, int firstY, int secondX, int secondY, int thirdX, int thirdY) {
        game().openFloorAt(firstX, firstY);
        game().openFloorAt(secondX, secondY);
        game().openFloorAt(thirdX, thirdY);
    }

    @Given("the player has stepped right")
    public void thePlayerHasSteppedRight() {
        game().pressMoveRight();
    }

    @When("the player steps right")
    public void thePlayerStepsRight() {
        game().pressMoveRight();
    }

    @When("the player steps right {int} times")
    public void thePlayerStepsRightTimes(int times) {
        for (int i = 0; i < times; i++) {
            game().pressMoveRight();
        }
    }

    @When("the player is repositioned directly to \\({int}, {int}\\)")
    public void thePlayerIsRepositionedDirectlyTo(int x, int y) {
        game().movePlayerTo(x, y);
    }

    @When("the game state is reset by exiting to the main menu")
    public void theGameStateIsResetByExitingToTheMainMenu() {
        game().panel().resetState();
    }

    @Given("^the (inventory|codex|pause menu) screen is open$")
    public void theScreenIsOpen(String screen) {
        ensureOpen(screen);
    }

    @When("^the (inventory|codex|pause menu) screen is opened$")
    public void theScreenIsOpened(String screen) {
        ensureOpen(screen);
    }

    @When("^the (inventory|codex|pause menu) screen is closed$")
    public void theScreenIsClosed(String screen) {
        if (isOpen(screen)) {
            toggle(screen);
        }
        assertFalse(isOpen(screen), "expected the " + screen + " screen to be closed");
    }

    @Then("^the (inventory|codex|pause menu) screen is no longer open$")
    public void theScreenIsNoLongerOpen(String screen) {
        assertFalse(isOpen(screen), "expected the " + screen + " screen to be closed");
    }

    @Then("the world is paused")
    public void theWorldIsPaused() {
        assertTrue(game().worldPaused(), "expected the world to be paused");
    }

    @Then("the world is not paused")
    public void theWorldIsNotPaused() {
        assertFalse(game().worldPaused(), "expected the world not to be paused");
    }

    @Then("the turn counter is {int}")
    public void theTurnCounterIs(int expected) {
        assertEquals(expected, scheduler().getTurn());
    }

    @Then("the player is at position \\({int}, {int}\\)")
    public void thePlayerIsAtPosition(int x, int y) {
        assertEquals(x, game().playerX());
        assertEquals(y, game().playerY());
    }

    @Then("the counting tick consumer has been stepped {int} time(s)")
    public void theCountingTickConsumerHasBeenStepped(int expected) {
        assertEquals(expected, consumers.get(0).steps);
    }

    @Then("each counting tick consumer has been stepped {int} time(s)")
    public void eachCountingTickConsumerHasBeenStepped(int expected) {
        for (CountingTickConsumer consumer : consumers) {
            assertEquals(expected, consumer.steps, consumer.name + " step count");
        }
    }

    @Then("the first consumer was stepped before the second")
    public void theFirstConsumerWasSteppedBeforeTheSecond() {
        int first = steppedOrder.indexOf("first");
        int second = steppedOrder.indexOf("second");
        assertTrue(first >= 0 && first < second, "stepping order was " + steppedOrder);
    }

    @Then("no tick consumers are registered")
    public void noTickConsumersAreRegistered() {
        assertEquals(0, scheduler().registeredCount());
    }

    private void registerConsumer(String name) {
        CountingTickConsumer consumer = new CountingTickConsumer(name);
        consumers.add(consumer);
        scheduler().register(consumer);
    }

    private void recordStepped(String name) {
        steppedOrder.add(name);
    }

    private static GamePanelFixture game() {
        return SharedScenarioContext.getTurnGame();
    }

    private static TurnScheduler scheduler() {
        return game().panel().getTurnScheduler();
    }

    private static void ensureOpen(String screen) {
        if (!isOpen(screen)) {
            toggle(screen);
        }
        assertTrue(isOpen(screen), "expected the " + screen + " screen to be open");
    }

    private static boolean isOpen(String screen) {
        return switch (screen) {
            case "inventory" -> game().inventoryPanel().isVisible();
            case "codex" -> game().codexPanel().isVisible();
            case "pause menu" -> game().pauseMenuPopup().isVisible();
            default -> throw new IllegalArgumentException("unknown screen: " + screen);
        };
    }

    private static void toggle(String screen) {
        switch (screen) {
            case "inventory" -> game().pressInventoryKey();
            case "codex" -> game().pressCodexKey();
            case "pause menu" -> game().pressPauseKey();
            default -> throw new IllegalArgumentException("unknown screen: " + screen);
        }
    }

    /** A no-op tick consumer that counts how often it is stepped, and logs its name into a shared order. */
    private final class CountingTickConsumer implements Tickable {
        private final String name;
        private int steps;

        CountingTickConsumer(String name) {
            this.name = name;
        }

        @Override
        public void onTurn(int turn) {
            steps++;
            recordStepped(name);
        }
    }
}
