package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.PauseMenuPopup;
import com.swiftfaze.veil.ui.PauseToggleListener;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PauseScreenSteps {

    private GamePanel gamePanel;
    private PauseMenuPopup pauseMenuPopup;
    private List<String> menuSelectNotifications;

    @Before
    public void setup() {
        gamePanel = new GamePanel();
        pauseMenuPopup = new PauseMenuPopup();
        menuSelectNotifications = new ArrayList<>();

        pauseMenuPopup.setOnMenuSelect(item -> menuSelectNotifications.add(item));

        PauseToggleListener listener = new PauseToggleListener(gamePanel.getWorldPause(), pauseMenuPopup);
        gamePanel.addGameListener(listener);
    }

    @Given("a game panel is running")
    public void aGamePanelIsRunning() {
        assertNotNull(gamePanel);
        assertFalse(gamePanel.isPaused());
        assertFalse(pauseMenuPopup.isVisible());
    }

    @Given("a game panel is running with a player at position \\({int}, {int})")
    public void aGamePanelIsRunningWithPlayerAtPosition(int x, int y) {
        aGamePanelIsRunning();
        gamePanel.getPlayer().setPosition(x, y);
    }

    @Given("a game panel is running with pause menu open")
    public void aGamePanelIsRunningWithPauseMenuOpen() {
        aGamePanelIsRunning();
        fireAction(gamePanel, Keybindings.ACTION_TOGGLE_PAUSE);
        assertTrue(pauseMenuPopup.isVisible());
        assertTrue(gamePanel.isPaused());
    }

    @When("escape is pressed")
    public void escapeIsPressed() {
        fireAction(gamePanel, Keybindings.ACTION_TOGGLE_PAUSE);
    }

    @When("escape is pressed to open pause menu")
    public void escapeIsPressedToOpenPauseMenu() {
        escapeIsPressed();
    }

    @When("resume is selected")
    public void resumeIsSelected() {
        // Resume is already selected by default, so just confirm it
        fireAction(pauseMenuPopup, "pause-confirm");
    }

    @When("settings is selected from pause menu")
    public void settingsIsSelectedFromPauseMenu() {
        fireMenuDown(pauseMenuPopup);
        assertEquals(PauseMenuPopup.SETTINGS, pauseMenuPopup.getSelectedItem());
        fireAction(pauseMenuPopup, "pause-confirm");
    }

    @When("exit to main menu is selected from pause menu")
    public void exitToMainMenuIsSelectedFromPauseMenu() {
        fireMenuDown(pauseMenuPopup);
        fireMenuDown(pauseMenuPopup);
        assertEquals(PauseMenuPopup.EXIT_TO_MAIN_MENU, pauseMenuPopup.getSelectedItem());
        fireAction(pauseMenuPopup, "pause-confirm");
    }

    @Then("the pause menu is open")
    public void thePauseMenuIsOpen() {
        assertTrue(pauseMenuPopup.isVisible());
        assertTrue(gamePanel.isPaused());
    }

    @Then("the pause menu is closed")
    public void thePauseMenuIsClosed() {
        assertFalse(pauseMenuPopup.isVisible());
    }

    @Then("the game is no longer paused")
    public void theGameIsNoLongerPaused() {
        assertFalse(gamePanel.isPaused());
    }

    @Then("the selected menu item is {string}")
    public void theSelectedMenuItemIs(String expectedItem) {
        assertEquals(expectedItem, pauseMenuPopup.getSelectedItem());
    }

    @And("moving down selects {string}")
    public void movingDownSelects(String expectedItem) {
        fireMenuDown(pauseMenuPopup);
        assertEquals(expectedItem, pauseMenuPopup.getSelectedItem());
    }

    @And("moving down again selects {string}")
    public void movingDownAgainSelects(String expectedItem) {
        movingDownSelects(expectedItem);
    }

    @Then("the host was notified of {string} selection")
    public void theHostWasNotifiedOfSelection(String menuItem) {
        assertTrue(menuSelectNotifications.contains(menuItem),
            "Expected notification for: " + menuItem + ", but got: " + menuSelectNotifications);
    }

    private static void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }

    private static void fireAction(PauseMenuPopup popup, String actionName) {
        Action action = popup.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(popup, ActionEvent.ACTION_PERFORMED, actionName));
    }

    private static void fireMenuDown(PauseMenuPopup popup) {
        fireAction(popup, "popup-down");
    }
}
