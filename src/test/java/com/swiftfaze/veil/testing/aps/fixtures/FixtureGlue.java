package com.swiftfaze.veil.testing.aps.fixtures;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Glue for the fixture features under {@code src/test/resources/aps-fixtures/}, which
 * the acceptance mutator runs in a nested Cucumber launch. It lives outside
 * {@code com.swiftfaze.veil.steps} so the real suite never loads it, and it keeps no
 * state, so nothing leaks between the nested run and the outer one.
 *
 * <p>Most steps deliberately ignore their arguments: that is what makes a mutant of
 * them survive, which is the behaviour the acceptance-mutation feature pins.
 */
public class FixtureGlue {

    @Given("the player gains {int} gold")
    public void playerGainsGoldIgnoringAmount(int amount) {
        // Ignores the amount on purpose, so its mutants survive.
    }

    @Given("the player gains {int} gold and it equals {int}")
    public void playerGainsGoldAndChecks(int amount, int expected) {
        assertEquals(expected, amount);
    }

    @Given("the player chooses {string}")
    public void playerChooses(String className) {
        // Ignores the class on purpose.
    }

    @When("they change their mind to {string}")
    public void changeChoice(String newClass) {
        // Ignores the class on purpose.
    }

    @Given("a damage value of {int}")
    public void damageValue(int x) {
        // Ignores the value on purpose.
    }

    @When("combined with {int}")
    public void combineWith(int y) {
        // Ignores the value on purpose.
    }

    @Then("the total is checked")
    public void totalIsChecked() {
        // Checks nothing on purpose.
    }

    @Given("something happens with {int}")
    public void somethingHappens(int value) {
        // Ignores the value on purpose.
    }

    @Given("something different happens with {int}")
    public void somethingDifferentHappens(int value) {
        // Ignores the value on purpose.
    }

    @Given("a condition that fails with {int}")
    public void conditionThatFails(int value) {
        fail("This fixture step always fails, got " + value);
    }
}
