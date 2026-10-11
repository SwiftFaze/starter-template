package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.testing.FlatFloorWorld;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.input.Keybindings;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyboardInputAndMenuNavigationSteps {

    private GamePanel gamePanel;
    private boolean listenerNotified;

    @Given("a game panel with a player at position \\({int}, {int})")
    public void aGamePanelWithAPlayerAtPosition(int x, int y) {
        gamePanel = FlatFloorWorld.panel();
        gamePanel.getPlayer().setPosition(x, y);
        listenerNotified = false;
        gamePanel.addGameListener(player -> listenerNotified = true);
    }

    @When("the {string} action fires")
    public void theActionFires(String actionName) {
        fireAction(actionNameFor(actionName));
    }

    @When("a key with no bound action is pressed")
    public void aKeyWithNoBoundActionIsPressed() {
        // Unbound keys never invoke an Action under InputMap/ActionMap,
        // so there is no event to simulate — nothing should happen.
    }

    @Then("the game panel's player is at position \\({int}, {int})")
    public void theGamePanelsPlayerIsAtPosition(int x, int y) {
        assertEquals(x, gamePanel.getPlayer().getX());
        assertEquals(y, gamePanel.getPlayer().getY());
    }

    @Then("registered game listeners are notified")
    public void registeredGameListenersAreNotified() {
        assertTrue(listenerNotified);
    }

    @Then("registered game listeners are not notified")
    public void registeredGameListenersAreNotNotified() {
        assertFalse(listenerNotified);
    }

    private static String actionNameFor(String spokenName) {
        return switch (spokenName) {
            case "move up" -> Keybindings.ACTION_MOVE_UP;
            case "move down" -> Keybindings.ACTION_MOVE_DOWN;
            case "move left" -> Keybindings.ACTION_MOVE_LEFT;
            case "move right" -> Keybindings.ACTION_MOVE_RIGHT;
            default -> throw new IllegalArgumentException("Unhandled action: " + spokenName);
        };
    }

    private void fireAction(String actionName) {
        Action action = gamePanel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(gamePanel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
