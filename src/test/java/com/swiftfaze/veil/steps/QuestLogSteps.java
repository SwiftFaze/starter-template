package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.player.QuestLog;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuestLogSteps {

    private Player player;

    @Given("a new player")
    public void aNewPlayer() {
        player = new Player(0, 0);
    }

    @When("the player's quest state for {string} is set to {string}")
    public void thePlayersQuestStateForIsSetTo(String questId, String stateName) {
        player.getPlayerInfo().getQuestLog().setState(questId, parseState(stateName));
    }

    @Then("the player's quest state for {string} is {string}")
    public void thePlayersQuestStateForIs(String questId, String stateName) {
        assertEquals(parseState(stateName), player.getPlayerInfo().getQuestLog().getState(questId));
    }

    private static QuestLog.State parseState(String stateName) {
        return QuestLog.State.valueOf(stateName.toUpperCase(Locale.ROOT).replace(' ', '_'));
    }
}
