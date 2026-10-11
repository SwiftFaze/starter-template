package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.ui.widget.PatternFieldWidget;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiWidgetPatternFieldSteps {
    private PatternFieldWidget patternField;

    @Given("a pattern field with pattern {string} and empty input")
    public void aPatternFieldWithPattern(String pattern) {
        patternField = new PatternFieldWidget(pattern);
    }

    @Given("the pattern field has keyboard focus")
    public void thePatternFieldHasKeyboardFocus() {
        // Keyboard focus is modeled at the widget level
    }

    @When("the characters {string} are typed")
    public void theCharactersAreTyped(String chars) {
        patternField.typeCharacters(chars);
    }

    @Then("the pattern field's input is {string}")
    public void thePatternFieldsInputIs(String expected) {
        assertEquals(expected, patternField.getInput());
    }

    @Then("the pattern field is in the valid state")
    public void thePatternFieldIsInValidState() {
        assertTrue(patternField.patternIsValid());
    }

    @Then("the pattern field is in the invalid state")
    public void thePatternFieldIsInInvalidState() {
        assertFalse(patternField.patternIsValid());
    }

    @Given("a pattern field with pattern {string} and input {string}")
    public void aPatternFieldWithInput(String pattern, String input) {
        patternField = new PatternFieldWidget(pattern);
        patternField.typeCharacters(input);
    }

    @When("the last character is deleted")
    public void theLastCharacterIsDeleted() {
        patternField.deleteLastCharacter();
    }
}
