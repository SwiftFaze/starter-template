package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.sandbox.ClassSandboxModel;
import com.swiftfaze.veil.sandbox.ClassSandboxPanel;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import javax.swing.Action;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Step definitions for the class sandbox panel (class labels and computed-stats display). */
public class ClassSandboxSteps {

    private ClassSandboxPanel classPanel;
    private ClassSandboxModel classModel;
    private List<String> classNames;

    @Given("a class sandbox panel is showing")
    public void aClassSandboxPanelIsShowing() {
        classModel = new ClassSandboxModel();
        classPanel = new ClassSandboxPanel(classModel);
        classNames = classModel.classNames();
    }

    @Then("the first class's label is colored {string}")
    public void theFirstClassSLabelIsColored(String hex) {
        assertEquals(Color.decode(hex), classPanel.getClassLabel(0).getForeground());
    }

    @Then("the stats label shows the first class's computed stats")
    public void theStatsLabelShowsTheFirstClassSComputedStats() {
        assertStatsLabelShows(0);
    }

    @When("the down-bound action fires")
    public void theDownBoundActionFires() {
        fireAction(Keybindings.ACTION_MENU_DOWN);
    }

    @When("the up-bound action fires")
    public void theUpBoundActionFires() {
        fireAction(Keybindings.ACTION_MENU_UP);
    }

    @Then("the previously selected class's label is white")
    public void thePreviouslySelectedClassSLabelIsWhite() {
        assertEquals(Color.WHITE, classPanel.getClassLabel(0).getForeground());
    }

    @Then("the newly selected class's label is colored {string}")
    public void theNewlySelectedClassSLabelIsColored(String hex) {
        assertEquals(Color.decode(hex), classPanel.getClassLabel(1).getForeground());
    }

    @Then("the stats label shows the newly selected class's computed stats")
    public void theStatsLabelShowsTheNewlySelectedClassSComputedStats() {
        assertStatsLabelShows(1);
    }

    @Then("the last class's label is colored {string}")
    public void theLastClassSLabelIsColored(String hex) {
        assertEquals(Color.decode(hex), classPanel.getClassLabel(classNames.size() - 1).getForeground());
    }

    @Then("the stats label shows the last class's computed stats")
    public void theStatsLabelShowsTheLastClassSComputedStats() {
        assertStatsLabelShows(classNames.size() - 1);
    }

    private void assertStatsLabelShows(int index) {
        Stats stats = classModel.computedStats(classNames.get(index));
        String expected = String.format(
                Locale.ROOT,
                "ATK %d  DEF %d  HP %d  MP %d",
                stats.getAttackPower(), stats.getDefense(), stats.getMaxHp(), stats.getMaxMana()
        );
        assertEquals(expected, classPanel.getStatsLabel().getText());
    }

    private void fireAction(String actionName) {
        Action action = classPanel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(classPanel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
