package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.Main;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.sandbox.ClassSandbox;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.QuestSandboxProvider;
import com.swiftfaze.veil.sandbox.InspectableDetailPanel;
import com.swiftfaze.veil.ui.widget.TableWidget;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;


public class QuestSandboxSteps {
    private static final int DEFAULT_KILL_COUNT = 5;
    private static final String CORE_PREFIX = "core:";
    private static final int ID_COLUMN = 1;
    private static final int COUNT_COLUMN = 2;
    private static final int CALC_COLUMN = 3;

    private Path modsRoot;
    private DevConsoleModel model;

    @Before
    public void createModsRoot() throws IOException {
        modsRoot = Files.createTempDirectory("veil-quest-sandbox-test");
    }

    @After
    public void deleteModsRoot() throws IOException {
        try (Stream<Path> paths = Files.walk(modsRoot)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    @Given("a loaded quest {string} whose objective has no target and no count")
    public void aLoadedQuestWhoseObjectiveHasNoTargetAndNoCount(String questName) throws IOException {
        writeFixtureQuest(questName, "kill", null, null, List.of());
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Given("a loaded quest {string} with no rewards")
    public void aLoadedQuestWithNoRewards(String questName) throws IOException {
        writeFixtureQuest(questName, "kill", "core:goblin", DEFAULT_KILL_COUNT, List.of());
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Given("no quests are loaded")
    public void noQuestsAreLoaded() throws IOException {
        // Create an empty mod structure with no quests
        writeManifest();
        // Don't write any quest files
        reloadConsoleWithTempQuestsAndUpdatePanel();
    }

    @Then("the quest detail shows these fields:")
    public void theQuestDetailShowsTheseFields(DataTable dataTable) {
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        Assertions.assertNotNull(panel, "Expected a dev console panel");

        InspectableDetailPanel detailsPane = Assertions.assertInstanceOf(InspectableDetailPanel.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be an InspectableDetailPanel");

        Assertions.assertTrue(detailsPane.tableCount() > 0, "Expected quest details to be shown, not a placeholder");

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);

        // Get the first table (the main fields table)
        TableWidget<List<String>> table = detailsPane.table(0);
        Assertions.assertNotNull(table, "Expected at least one table in the quest detail");

        List<List<String>> rows = table.getRows();

        // Find each expected row and verify its value
        for (Map<String, String> expectedRow : expectedRows) {
            String expectedField = expectedRow.get("Field");
            String expectedValue = expectedRow.get("Value");

            boolean found = false;
            for (List<String> row : rows) {
                if (expectedField.equals(row.get(0))) {
                    Assertions.assertEquals(expectedValue, row.get(1), "Value mismatch for field '" + expectedField + "'");
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found, "Expected to find field '" + expectedField + "' in quest detail");
        }
    }

    @Then("the quest detail has a {string} table with these rows:")
    public void theQuestDetailHasATableWithTheseRows(String tableLabel, DataTable dataTable) {
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        Assertions.assertNotNull(panel, "Expected a dev console panel");

        InspectableDetailPanel detailsPane = Assertions.assertInstanceOf(InspectableDetailPanel.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be an InspectableDetailPanel");

        // The "Rewards:" table is the second table (index 1)
        Assertions.assertEquals("Rewards:", tableLabel, "Currently only supporting Rewards table verification");
        Assertions.assertTrue(detailsPane.tableCount() > 1, "Expected at least 2 tables for rewards");

        TableWidget<List<String>> table = detailsPane.table(1);
        Assertions.assertNotNull(table, "Expected a Rewards table at index 1");

        List<Map<String, String>> expectedRows = dataTable.asMaps(String.class, String.class);
        List<List<String>> rows = table.getRows();

        Assertions.assertEquals(expectedRows.size(), rows.size(), "Row count mismatch in Rewards table");

        for (int i = 0; i < expectedRows.size(); i++) {
            Map<String, String> expectedRow = expectedRows.get(i);
            List<String> row = rows.get(i);

            Assertions.assertEquals(expectedRow.get("Type"), row.get(0), "Type mismatch at row " + i);
            Assertions.assertEquals(expectedRow.get("ID"), row.get(ID_COLUMN), "ID mismatch at row " + i);
            Assertions.assertEquals(expectedRow.get("Count"), row.get(COUNT_COLUMN), "Count mismatch at row " + i);
            Assertions.assertEquals(expectedRow.get("Calc"), row.get(CALC_COLUMN), "Calc mismatch at row " + i);
        }
    }

    @Then("the quest detail has no {string} table")
    public void theQuestDetailHasNoTable(String tableLabel) {
        DevConsolePanel panel = SharedScenarioContext.getDevConsolePanel();
        Assertions.assertNotNull(panel, "Expected a dev console panel");

        InspectableDetailPanel detailsPane = Assertions.assertInstanceOf(InspectableDetailPanel.class,
                panel.getOpenedProviderPanel(),
                "Expected the opened panel to be an InspectableDetailPanel");

        // For a quest with no rewards, there should only be 1 table (the main fields table)
        Assertions.assertEquals(1, detailsPane.tableCount(), "Expected only the main fields table when there are no rewards");
    }

    @Given("the F1 in-game dev console is built")
    public void theF1InGameDevConsoleIsBuilt() {
        // Use Main.buildDevConsoleProviders to get the correct provider list
        List<DevConsoleProvider> providers = Main.buildDevConsoleProviders(
                () -> new Player(0, 0)
        );
        model = new DevConsoleModel(providers);
    }

    @Given("the standalone sandbox dev console is built")
    public void theStandaloneSandboxDevConsoleIsBuilt() {
        // Use the static method from ClassSandbox
        List<DevConsoleProvider> providers = ClassSandbox.providers();
        model = new DevConsoleModel(providers);
    }

    @Then("its providers include the {string} provider")
    public void itsProvidersIncludeTheProvider(String providerName) {
        Assertions.assertNotNull(model, "Expected a dev console model to be built");

        // Check if any entry belongs to the specified provider by category
        boolean found = model.allResults().stream()
                .anyMatch(result -> providerName.equals(result.entry().category()));
        Assertions.assertTrue(found, "Expected to find the " + providerName + " provider (no entries in " + providerName + " category)");
    }

    private void writeFixtureQuest(String questName, String objectiveType, @Nullable String objectiveTarget,
                                    @Nullable Integer objectiveCount, List<JsonObject> rewards) throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir.resolve("quests"));

        writeManifest();

        JsonObject questJson = new JsonObject();
        String questId = "core:" + questName.toLowerCase(Locale.ROOT).replace(" ", "_");
        questJson.addProperty("id", questId);
        questJson.addProperty("name", questName);

        JsonObject objective = new JsonObject();
        objective.addProperty("type", objectiveType);
        if (objectiveTarget != null) {
            objective.addProperty("target", objectiveTarget);
        }
        if (objectiveCount != null) {
            objective.addProperty("count", objectiveCount);
        }
        questJson.add("objective", objective);

        if (!rewards.isEmpty()) {
            JsonArray rewardsArray = new JsonArray();
            for (JsonObject reward : rewards) {
                rewardsArray.add(reward);
            }
            questJson.add("rewards", rewardsArray);
        }

        Files.writeString(coreDir.resolve("quests").resolve(questId.substring(CORE_PREFIX.length()) + ".json"), questJson.toString());
    }

    private void writeManifest() throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", "core");
        manifest.add("dependsOn", new JsonArray());
        Files.writeString(coreDir.resolve("mod.json"), manifest.toString());
    }

    private void reloadConsoleWithTempQuestsAndUpdatePanel() {
        List<Quest> quests = ModLoader.load(modsRoot).getAllQuests();
        List<DevConsoleProvider> providers = List.of(new QuestSandboxProvider(quests));
        model = new DevConsoleModel(providers);
        DevConsolePanel panel = new DevConsolePanel(model);
        // Update the shared context panel so that DevConsoleSteps and verification steps can access it
        SharedScenarioContext.setDevConsolePanel(panel);
        SharedScenarioContext.setDevConsoleModel(model);
    }
}
