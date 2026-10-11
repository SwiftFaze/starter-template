package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.ClassDetailPanel;
import com.swiftfaze.veil.sandbox.ClassSandboxModel;
import com.swiftfaze.veil.ui.widget.TableWidget;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ClassStatsLevelRangeSteps {

    private ClassSandboxModel model;
    private ClassDetailPanel detailPanel;
    private List<PlayerClass> classesForModel;
    private Optional<Exception> openFailure = Optional.empty();

    @Given("a class {string} with base strength {int} growing by {string} and base max HP {int}")
    public void aClassWithBaseStrengthGrowingByAndBaseMaxHp(String className, int baseStrength, String growthCalc, int baseMaxHp) {
        // Create a fixture PlayerClass with the given properties
        Map<String, PlayerClass.StatCurve> statCurves = Map.of(
                "strength", new PlayerClass.StatCurve(baseStrength, growthCalc),
                "maxHp", new PlayerClass.StatCurve(baseMaxHp, null)
        );
        PlayerClass growerClass = new PlayerClass("test:grower", className, statCurves);
        classesForModel = List.of(growerClass);
        model = new ClassSandboxModel(classesForModel);
    }

    @Given("the core classes are loaded")
    public void theCoreClassesAreLoaded() {
        if (model == null || classesForModel == null || classesForModel.isEmpty()) {
            // Load the core classes from mods
            classesForModel = ModLoader.load(Paths.get("mods")).getAllPlayerClasses();
            model = new ClassSandboxModel(classesForModel);
        } else {
            // Append core classes to the existing list
            List<PlayerClass> allClasses = new ArrayList<>(classesForModel);
            allClasses.addAll(ModLoader.load(Paths.get("mods")).getAllPlayerClasses());
            model = new ClassSandboxModel(allClasses);
        }
    }

    @When("the detail view for {string} is opened")
    public void theDetailViewForIsOpened(String className) {
        try {
            openFailure = Optional.empty();
            detailPanel = new ClassDetailPanel(model, className);
        } catch (IllegalArgumentException e) {
            openFailure = Optional.of(e);
        }
    }

    @Then("the detail table's columns are {string}")
    public void theDetailTableSColumnsAre(String expectedColumnsStr) {
        List<String> expectedColumns = List.of(expectedColumnsStr.split(", "));
        List<String> actualColumns = openedPanel().getStatsTable().getColumnHeaders();
        assertEquals(expectedColumns, actualColumns,
                "Expected columns " + expectedColumns + " but got " + actualColumns);
    }

    @Then("the detail table's rows are the stats {string}")
    public void theDetailTableSRowsAreTheStats(String expectedRowsStr) {
        List<String> expectedRows = List.of(expectedRowsStr.split(", "));
        List<String> actualRows = readRows().stream().map(row -> row.get(0)).toList();
        assertEquals(expectedRows, actualRows,
                "Expected rows " + expectedRows + " but got " + actualRows);
    }

    @Then("the {string} row reads {string}")
    public void theRowReads(String statName, String expectedValuesStr) {
        List<String> expectedValues = List.of(expectedValuesStr.split(", "));
        for (List<String> row : readRows()) {
            if (!row.isEmpty() && row.get(0).equals(statName)) {
                List<String> actualValues = row.subList(1, row.size());
                assertEquals(expectedValues, actualValues,
                        "Expected " + statName + " row to have values " + expectedValues + " but got " + actualValues);
                return;
            }
        }
        fail("Row not found for stat: " + statName);
    }

    private ClassDetailPanel openedPanel() {
        if (detailPanel == null) {
            fail("Detail panel is null; detail view may have failed to open");
        }
        return detailPanel;
    }

    private List<List<String>> readRows() {
        TableWidget<List<String>> table = openedPanel().getStatsTable();
        List<List<String>> rows = new ArrayList<>();
        table.moveToStart();
        for (int i = 0; i < table.getRowCount(); i++) {
            rows.add(new ArrayList<>(table.getSelectedRow()));
            table.moveDown();
        }
        return rows;
    }

    @Then("opening the detail view fails with {string}")
    public void openingTheDetailViewFailsWith(String expectedMessage) {
        if (openFailure.isEmpty()) {
            fail("Expected an exception with message '" + expectedMessage + "' but no exception was thrown");
        }
        String actualMessage = openFailure.get().getMessage();
        assertEquals(expectedMessage, actualMessage,
                "Expected exception message '" + expectedMessage + "' but got '" + actualMessage + "'");
    }
}
