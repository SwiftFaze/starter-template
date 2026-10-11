package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.PlayerInfo;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultPlayerClassSteps {

    private PlayerInfo playerInfo;

    @Given("a new player is created")
    public void aNewPlayerIsCreated() {
        playerInfo = new PlayerInfo();
    }

    @Then("the player's class should be {string}")
    public void thePlayersClassShouldBe(String expectedClassName) {
        assertEquals(expectedClassName, playerInfo.getPlayerClass().getName());
    }

    @Then("the player's max HP should be {int}")
    public void thePlayersMaxHpShouldBe(int expectedMaxHp) {
        assertEquals(expectedMaxHp, playerInfo.getStats().getMaxHp());
    }
}
