package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.entities.player.PlayerInfo;
import com.swiftfaze.veil.entities.player.QuestLog;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.sandbox.ClassSandboxProvider;
import com.swiftfaze.veil.sandbox.DevConsoleEntry;
import com.swiftfaze.veil.sandbox.DevConsoleModel;
import com.swiftfaze.veil.sandbox.DevConsolePanel;
import com.swiftfaze.veil.sandbox.DevConsoleProvider;
import com.swiftfaze.veil.sandbox.InspectableDetailPanel;
import com.swiftfaze.veil.sandbox.ItemDetailPanel;
import com.swiftfaze.veil.sandbox.ItemSandboxProvider;
import com.swiftfaze.veil.sandbox.KitchenSinkProvider;
import com.swiftfaze.veil.sandbox.PlayerDetailPanel;
import com.swiftfaze.veil.sandbox.PlayerSandboxProvider;
import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.sandbox.QuestSandboxProvider;
import com.swiftfaze.veil.sandbox.TileSandboxProvider;
import com.swiftfaze.veil.sandbox.WorldRegenerator;
import com.swiftfaze.veil.sandbox.ReloadableFakeProvider;
import com.swiftfaze.veil.testing.FlatFloorWorld;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.TranscriptWidget;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DevConsoleSteps {

    // Column order matches DevConsoleCommandRunner.RESULT_HEADERS: "#", "ID", "Name", "Category", "Mod".
    private static final int ID_COLUMN = 1;
    private static final int NAME_COLUMN = 2;
    private static final int CATEGORY_COLUMN = 3;
    private static final int MOD_COLUMN = 4;
    private static final String TRANSCRIPT_SHOULD_HAVE_ENTRIES = "Transcript should have entries";
    private static final String CLASSES_PROVIDER_NAME = "Classes";
    private static final String PLAYER_PROVIDER_NAME = "Player";
    private static final String ITEMS_PROVIDER_NAME = "Items";
    private static final String KITCHEN_SINK_PROVIDER_NAME = "Kitchen Sink";
    private static final String QUESTS_PROVIDER_NAME = "Quests";
    private static final String TILES_PROVIDER_NAME = "Tiles";

    private DevConsoleModel model;
    private DevConsolePanel panel;
    private Player livePlayer;
    private Player playerBeforeReload;
    private @Nullable PlayerDetailPanel playerDetailPanel;
    private Player identityBeforeEdit;
    private int maxHpBeforeEdit;
    private String lastEditedFieldName;
    private int lastEditedFieldValue;
    private Stats statsSnapshot;
    private String classSnapshot;
    private ReloadableFakeProvider fakeProvider;
    private ReloadableFakeProvider secondProvider;
    private int transcriptSizeWhenItemRowArmed;
    private GamePanel gameForRegen;
    private Player runningPlayer;
    private long nextFreshWorldSeed = 1L;

    @Given("the dev console is running with the {string} provider registered")
    public void theDevConsoleIsRunningWithTheProviderRegistered(String providerName) {
        List<DevConsoleProvider> providers = List.of(providerFor(providerName));
        startConsole(new DevConsoleModel(providers));

        // If it's the Quests provider, create a live player and snapshot the quest log
        if (QUESTS_PROVIDER_NAME.equals(providerName)) {
            if (livePlayer == null) {
                livePlayer = new Player(0, 0);
            }
            // Snapshot all quest states for later verification
            QuestLog questLog = liveQuestLog();
            Map<String, String> snapshot = new HashMap<>();
            for (DevConsoleModel.SearchResult result : model.allResults()) {
                String questId = result.entry().id();
                String state = String.valueOf(questLog.getState(questId));
                snapshot.put(questId, state);
            }
            SharedScenarioContext.setQuestLogSnapshot(snapshot);
        }
    }

    @Given("the dev console is running with the {string} and {string} providers registered")
    public void theDevConsoleIsRunningWithTheProvidersRegistered(String provider1, String provider2) {
        livePlayer = new Player(0, 0);
        List<DevConsoleProvider> providers = List.of(providerFor(provider1), providerFor(provider2));
        startConsole(new DevConsoleModel(providers));
    }

    @Given("the dev console is running with the {string} provider attached to the running player")
    public void theDevConsoleIsRunningWithThePlayerProviderAttached(String providerName) {
        livePlayer = new Player(0, 0);
        List<DevConsoleProvider> providers = List.of(new PlayerSandboxProvider(() -> livePlayer));
        startConsole(new DevConsoleModel(providers));
    }

    @Given("the running player is at position \\({int}, {int}\\)")
    public void theRunningPlayerIsAtPosition(int x, int y) {
        // Set position and verify it was set correctly.
        // This method works for both Given and Then contexts - set the position
        // and assert it to ensure the scenario is correct.
        livePlayer.setPosition(x, y);
        assertEquals(x, livePlayer.getX(), "Expected player X to be " + x);
        assertEquals(y, livePlayer.getY(), "Expected player Y to be " + y);
    }

    @When("the running player is moved to position \\({int}, {int}\\)")
    public void theRunningPlayerIsMovedToPosition(int x, int y) {
        livePlayer.setPosition(x, y);
    }

    @When("the search text is set to {string}")
    public void theSearchTextIsSetTo(String text) {
        getCurrentModel().setSearchText(text);
    }

    @When("the command {string} is entered")
    public void theCommandIsEntered(String command) {
        panel.getSearchField().setText(command);
        panel.runCommand();
    }

    @When("the command {string} has been entered")
    public void theCommandHasBeenEntered(String command) {
        theCommandIsEntered(command);
    }

    @Given("the command field contains {string}")
    public void theCommandFieldContains(String text) {
        panel.getSearchField().setText(text);
    }

    @Then("the command field text is {string}")
    public void theCommandFieldTextIs(String text) {
        assertEquals(text, panel.getSearchField().getText(),
                "Expected command field to contain: " + text);
    }

    @When("Tab is pressed")
    public void tabIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_COMPLETE);
    }

    @When("Up is pressed")
    public void upIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_HISTORY_UP);
    }

    @When("Down is pressed")
    public void downIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_HISTORY_DOWN);
    }

    @When("Enter is pressed")
    public void enterIsPressed() {
        fireCommandAction(Keybindings.ACTION_MENU_CONFIRM);
    }

    @When("Escape is pressed")
    public void escapeIsPressed() {
        fireCommandAction(Keybindings.ACTION_DEV_CONSOLE_DISMISS_OVERLAY);
    }

    @Then("the suggestion overlay is showing with candidates {string}")
    public void theSuggestionOverlayIsShowingWithCandidates(String candidatesStr) {
        assertTrue(panel.getSuggestionOverlay().isShowing(), "Expected the suggestion overlay to be showing");
        Set<String> expected = Set.of(candidatesStr.split(", "));
        Set<String> actual = Set.copyOf(panel.getSuggestionOverlay().candidates());
        assertEquals(expected, actual, "Expected candidates " + expected + " but got " + actual);
    }

    @Then("the suggestion overlay is not showing")
    public void theSuggestionOverlayIsNotShowing() {
        assertFalse(panel.getSuggestionOverlay().isShowing(), "Expected the suggestion overlay to not be showing");
    }

    @Then("the transcript has no entries")
    public void assertTranscriptHasNoEntries() {
        assertTrue(panel.getTranscript().entries().isEmpty(),
                "Expected transcript to have no entries");
    }

    @Then("the command history size is {int}")
    public void assertCommandHistorySize(int expectedSize) {
        assertEquals(expectedSize, panel.getHistory().size(),
                "Expected command history size to be " + expectedSize);
    }

    @Then("the transcript's last entry is an info line reporting {int} results for {string}")
    public void theTranscriptsLastEntryIsAnInfoLineReporting(int count, String term) {
        assertFalse(panel.getTranscript().entries().isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        TranscriptWidget.TranscriptEntry lastEntry = panel.getTranscript().entries().get(panel.getTranscript().entries().size() - 1);
        assertEquals(TranscriptWidget.Level.INFO, lastEntry.level());
        assertTrue(lastEntry.text().contains(String.valueOf(count)));
        assertTrue(lastEntry.text().contains(term));
    }

    @Then("the transcript's first entry is a command line for {string}")
    public void theTranscriptsFirstEntryIsACommandLineFor(String command) {
        assertFalse(panel.getTranscript().entries().isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        var firstEntry = panel.getTranscript().entries().get(0);
        assertEquals(TranscriptWidget.Level.COMMAND, firstEntry.level());
        assertEquals(command, firstEntry.text());
    }

    @Then("the transcript's last entry is an error line for {string}")
    public void theTranscriptsLastEntryIsAnErrorLineFor(String input) {
        assertFalse(panel.getTranscript().entries().isEmpty(), "Transcript should have entries after: " + input);
        TranscriptWidget.TranscriptEntry lastEntry = panel.getTranscript().entries().get(panel.getTranscript().entries().size() - 1);
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level(), "Expected an error line after: " + input);
    }

    @Then("the transcript's most recent result table includes a row with id {string}, name {string}, category {string}, and mod {string}")
    public void theTranscriptsMostRecentResultTableIncludesARow(String id, String name, String category, String mod) {
        assertTrue(panel.getTranscript().lastResultTable().isPresent());
        var rows = panel.getTranscript().lastResultTable().get();
        boolean found = rows.stream().anyMatch(row ->
            row.size() > MOD_COLUMN &&
            id.equals(row.get(ID_COLUMN)) &&
            name.equals(row.get(NAME_COLUMN)) &&
            category.equals(row.get(CATEGORY_COLUMN)) &&
            mod.equals(row.get(MOD_COLUMN))
        );
        assertTrue(found, "No result table row found matching: " + id + ", " + name + ", " + category + ", " + mod);
    }

    @Then("the transcript has no result table")
    public void theTranscriptHasNoResultTable() {
        assertTrue(panel.getTranscript().lastResultTable().isEmpty());
    }

    @Then("the console view is shown")
    public void theConsoleViewIsShown() {
        // The console view (transcript) is shown when the provider panel is not visible
        assertFalse(panel.isProviderPanelShowing(), "Provider panel should not be showing");
    }

    @Then("the transcript still contains an info line reporting {int} results for {string}")
    public void theTranscriptStillContainsAnInfoLineReporting(int count, String term) {
        boolean found = panel.getTranscript().entries().stream().anyMatch(entry ->
            entry.level() == TranscriptWidget.Level.INFO &&
            entry.text().contains(String.valueOf(count)) &&
            entry.text().contains(term)
        );
        assertTrue(found);
    }

    @Then("the transcript contains an info line reporting {int} results for {string}")
    public void theTranscriptContainsAnInfoLineReporting(int count, String term) {
        theTranscriptStillContainsAnInfoLineReporting(count, term);
    }

    @Then("the transcript contains an error line for {string}")
    public void theTranscriptContainsAnErrorLineFor(String input) {
        boolean found = panel.getTranscript().entries().stream()
                .anyMatch(entry -> entry.level() == TranscriptWidget.Level.ERROR);
        assertTrue(found, "Expected an error line in the transcript after: " + input);
    }

    @Then("the results include an entry named {string}")
    public void theResultsIncludeAnEntryNamed(String name) {
        assertTrue(resultNames().contains(name));
    }

    @Then("the results do not include an entry named {string}")
    public void theResultsDoNotIncludeAnEntryNamed(String name) {
        assertFalse(resultNames().contains(name));
    }

    @Then("the results do not include any entry in category {string}")
    public void theResultsDoNotIncludeAnyEntryInCategory(String category) {
        boolean found = getCurrentModel().filteredResults().stream()
                .anyMatch(result -> category.equals(result.entry().category()));
        assertFalse(found, "Expected no entries in category '" + category + "'");
    }

    @Then("the results are empty")
    public void theResultsAreEmpty() {
        assertTrue(getCurrentModel().filteredResults().isEmpty());
    }

    @Then("the {string} result has namespace {string} and category {string}")
    public void theResultHasNamespaceAndCategory(String name, String namespace, String category) {
        DevConsoleModel.SearchResult result = findResult(name);
        assertEquals(namespace, result.entry().namespace());
        assertEquals(category, result.entry().category());
    }

    @When("{string} is opened")
    public void isOpened(String entryName) {
        DevConsoleModel.SearchResult result = findResult(entryName);
        String id = result.entry().id();
        getCurrentPanel().getSearchField().setText("edit " + id);
        getCurrentPanel().runCommand();
    }

    @Then("when {string} is opened the item detail shows {string} as {string}")
    public void whenIsOpenedTheItemDetailShows(String itemName, String fieldName, String expectedValue) {
        isOpened(itemName);
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();

        for (int i = 0; i < itemDetailPanel.tableCount(); i++) {
            TableWidget<List<String>> table = itemDetailPanel.table(i);
            table.moveToStart();
            for (int row = 0; row < table.getRowCount(); row++) {
                List<String> rowData = table.getSelectedRow();
                if (!rowData.isEmpty() && fieldName.equals(rowData.get(0))) {
                    assertEquals(expectedValue, rowData.get(1),
                            "Expected " + fieldName + " to show " + expectedValue + " but got " + rowData.get(1));
                    return;
                }
                if (row < table.getRowCount() - 1) {
                    table.moveDown();
                }
            }
        }
        fail("Field " + fieldName + " not found in item detail");
    }

    @Then("when {string} is opened the item detail has no {string} row")
    public void whenIsOpenedTheItemDetailHasNoRow(String itemName, String fieldName) {
        isOpened(itemName);
        theItemDetailHasNoRow(fieldName);
    }

    @Then("the opened detail panel is shown")
    public void theOpenedDetailPanelIsShown() {
        assertTrue(panel.isProviderPanelShowing());
    }

    @When("the back action is triggered")
    public void theBackActionIsTriggered() {
        getCurrentPanel().showSearchView();
        playerDetailPanel = null;
    }

    @Then("there is one result per mod-loaded item")
    public void thereIsOneResultPerModLoadedItem() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        int expectedCount = mods.getAllItems().size();
        assertEquals(expectedCount, model.filteredResults().size(),
                "Expected one result per mod-loaded item");
    }

    @Then("the item detail shows field rows:")
    public void theItemDetailShowsFieldRows(DataTable dataTable) {
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();
        List<List<String>> expectedRows = dataTable.asLists();

        assertTrue(itemDetailPanel.tableCount() >= 1, "Expected at least one table for field rows");
        TableWidget<List<String>> table = itemDetailPanel.table(0);
        assertNotNull(table, "First table should not be null");

        List<List<String>> expectedData = expectedRows.subList(1, expectedRows.size());

        int rowIndex = 0;
        table.moveToStart();
        for (List<String> expectedRow : expectedData) {
            List<String> actualRow = table.getSelectedRow();
            assertEquals(expectedRow, actualRow,
                    "Row " + rowIndex + " should match");
            if (rowIndex < expectedData.size() - 1) {
                table.moveDown();
            }
            rowIndex++;
        }
    }

    @Then("the item detail shows an {string} table with row {string}, {string}, {string}")
    public void theItemDetailShowsTableWithRow(String tableLabel, String col1, String col2, String col3) {
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();

        List<String> expectedRow = List.of(col1, col2, col3);
        boolean found = false;
        for (int i = 0; i < itemDetailPanel.tableCount(); i++) {
            TableWidget<List<String>> table = itemDetailPanel.table(i);
            table.moveToStart();
            for (int row = 0; row < table.getRowCount(); row++) {
                List<String> rowData = table.getSelectedRow();
                if (rowData.equals(expectedRow)) {
                    found = true;
                    break;
                }
                if (row < table.getRowCount() - 1) {
                    table.moveDown();
                }
            }
            if (found) {
                break;
            }
        }
        assertTrue(found, "Expected to find row with " + col1 + ", " + col2 + ", " + col3
                + " in table " + tableLabel);
    }

    @Then("the item detail has no {string} row")
    public void theItemDetailHasNoRow(String fieldName) {
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();

        for (int i = 0; i < itemDetailPanel.tableCount(); i++) {
            TableWidget<List<String>> table = itemDetailPanel.table(i);
            table.moveToStart();
            for (int row = 0; row < table.getRowCount(); row++) {
                List<String> rowData = table.getSelectedRow();
                if (!rowData.isEmpty() && fieldName.equals(rowData.get(0))) {
                    fail("Field row should not be present: " + fieldName);
                }
                if (row < table.getRowCount() - 1) {
                    table.moveDown();
                }
            }
        }
    }

    @Then("the item detail has no {string} table")
    public void theItemDetailHasNoTable(String tableLabel) {
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();

        for (int i = 0; i < itemDetailPanel.tableCount(); i++) {
            TableWidget<List<String>> table = itemDetailPanel.table(i);
            table.moveToStart();
            List<String> firstRow = table.getSelectedRow();
            boolean matchesLabel = !firstRow.isEmpty()
                    && (tableLabel.equals(firstRow.get(0) + " table")
                    || ("Effects: table".equals(tableLabel) && firstRow.get(0).contains("Type")));
            if (matchesLabel) {
                fail("Table should not be present: " + tableLabel);
            }
        }
    }

    @When("the item row {string} is armed")
    public void theItemRowIsArmed(String fieldName) {
        TableWidget<List<String>> table = openedItemDetailPanel().table(0);
        table.moveToStart();
        while (!fieldName.equals(table.getSelectedRow().get(0)) && !table.isAtLastRow()) {
            table.moveDown();
        }
        assertEquals(fieldName, table.getSelectedRow().get(0), "No item row named " + fieldName);
        transcriptSizeWhenItemRowArmed = panel.getTranscript().entries().size();
        fireItemPanelAction(Keybindings.ACTION_MENU_CONFIRM);
    }

    @Then("no transcript line was written since the item row was armed")
    public void noTranscriptLineWasWrittenSinceTheItemRowWasArmed() {
        assertEquals(transcriptSizeWhenItemRowArmed, panel.getTranscript().entries().size(),
                "Keyboard edits in the item panel should not write transcript lines");
    }

    @When("{word} is pressed in the item panel")
    public void keyIsPressedInTheItemPanel(String key) {
        keyIsPressedInTheItemPanelTimes(key, 1);
    }

    @When("{word} is pressed in the item panel {int} times")
    public void keyIsPressedInTheItemPanelTimes(String key, int times) {
        String actionName = switch (key) {
            case "left" -> Keybindings.ACTION_MENU_LEFT;
            case "right" -> Keybindings.ACTION_MENU_RIGHT;
            case "down" -> Keybindings.ACTION_MENU_DOWN;
            case "escape" -> Keybindings.ACTION_MENU_CANCEL;
            default -> throw new IllegalArgumentException("Unknown item panel key: " + key);
        };
        for (int i = 0; i < times; i++) {
            fireItemPanelAction(actionName);
        }
    }

    @Then("the open item detail shows {string} as {string}")
    public void theOpenItemDetailShows(String fieldName, String expectedValue) {
        List<String> row = openedItemDetailPanel().table(0).getRows().stream()
                .filter(candidate -> fieldName.equals(candidate.get(0)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field " + fieldName + " not found in item detail"));
        assertEquals(expectedValue, row.get(1), "Unexpected value for " + fieldName);
    }

    private void fireItemPanelAction(String actionName) {
        ItemDetailPanel itemDetailPanel = openedItemDetailPanel();
        // Escape is only bound while a row is armed, so look it up through the
        // input map's current binding rather than firing an always-present action.
        if (Keybindings.ACTION_MENU_CANCEL.equals(actionName)
                && itemDetailPanel.getInputMap(JComponent.WHEN_FOCUSED).get(Keybindings.MENU_CANCEL) == null) {
            return;
        }
        Action action = itemDetailPanel.getActionMap().get(actionName);
        if (action != null) {
            action.actionPerformed(new ActionEvent(itemDetailPanel, ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    @When("the Items provider is asked for the panel of {string}")
    public void theItemsProviderIsAskedForThePanelOf(String itemId) {
        // The failure itself is asserted in the next step; here we only confirm the id is not registered.
        boolean registered = new ItemSandboxProvider().entries().stream()
                .anyMatch(entry -> entry.id().equals(itemId));
        assertFalse(registered, "Expected no item registered under id: " + itemId);
    }

    @Then("it fails with an unknown item id error for {string}")
    public void itFailsWithUnknownItemIdError(String itemId) {
        ItemSandboxProvider provider = new ItemSandboxProvider();
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> provider.createPanel(itemId));
        assertTrue(failure.getMessage().contains("Unknown item id"),
                "Error message should mention 'Unknown item id'");
        assertTrue(failure.getMessage().contains(itemId),
                "Error message should include the item id: " + itemId);
    }

    @Then("no player's quest log has changed")
    public void noPlayerQuestLogHasChanged() {
        assertNotNull(livePlayer, "Expected a live player for quest log verification");
        Map<String, String> snapshot = SharedScenarioContext.getQuestLogSnapshot();
        assertNotNull(snapshot, "Expected a quest log snapshot to be taken");

        // Verify all quest states match the snapshot
        QuestLog questLog = liveQuestLog();
        for (Map.Entry<String, String> entry : snapshot.entrySet()) {
            String questId = entry.getKey();
            String expectedState = entry.getValue();
            String actualState = String.valueOf(questLog.getState(questId));
            assertEquals(expectedState, actualState,
                    "Quest log state changed for quest " + questId + " after opening in console");
        }
    }

    @Then("the table includes editable rows {quotedList}")
    public void theTableIncludesEditableRows(List<String> rowNames) {
        capturePlayerDetailPanel();
        for (int i = 0; i < rowNames.size(); i++) {
            assertEquals(rowNames.get(i), playerDetailPanel.getStatsTable().getRowCount() > i ?
                getRowFieldName(i) : "missing row " + i);
        }
    }

    @Then("the table includes read-only rows {string}, {string}")
    public void theTableIncludesReadOnlyRows(String r1, String r2) {
        capturePlayerDetailPanel();
        assertEquals(r1, getRowFieldName(13));
        assertEquals(r2, getRowFieldName(14));
    }

    @Given("the running player's {string} is {int}")
    public void theRunningPlayerSFieldIsValue(String fieldName, int value) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(value);
            case "Dexterity" -> stats.setDexterity(value);
            case "Constitution" -> stats.setConstitution(value);
            case "Intelligence" -> stats.setIntelligence(value);
            case "Wisdom" -> stats.setWisdom(value);
            case "Luck" -> stats.setLuck(value);
            case "Max HP" -> stats.setMaxHp(value);
            case "Max Mana" -> stats.setMaxMana(value);
            case "Current HP" -> stats.setCurrentHp(value);
            case "Current Mana" -> stats.setCurrentMana(value);
            default -> throw new IllegalArgumentException("Unhandled fieldName: " + fieldName);
        }
        refreshDisplayedRows();
    }

    @Then("the running player's {string} value is {int}")
    public void theRunningPlayerSFieldValueIsAsserted(String fieldName, int expectedValue) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        assertEquals(expectedValue, getStatValue(stats, fieldName),
            "Expected " + fieldName + " to be " + expectedValue);
    }

    @Then("the displayed {string} value is {int}")
    public void theDisplayedFieldValueIsAsserted(String fieldName, int expectedValue) {
        capturePlayerDetailPanel();
        for (int i = 0; i < playerDetailPanel.getStatsTable().getRowCount(); i++) {
            if (getRowFieldName(i).equals(fieldName)) {
                List<String> row = getRow(i);
                if (row.size() > 1) {
                    assertEquals(String.valueOf(expectedValue), row.get(1));
                }
                return;
            }
        }
        fail("Field not found: " + fieldName);
    }

    @Given("the running player's class is {string}")
    public void theRunningPlayerClassIs(String className) {
        PlayerClass cls = findPlayerClass(className);
        if (cls != null) {
            livePlayer.getPlayerInfo().setPlayerClass(cls);
        }
        refreshDisplayedRows();
    }

    @Then("the running player's class value is {string}")
    public void theRunningPlayerClassValueIsAsserted(String expectedClassName) {
        assertEquals(expectedClassName, livePlayer.getPlayerInfo().getPlayerClass().getName());
    }

    @Then("the running player's {string} is Mage's level-0 base strength")
    public void fieldIsMageLevelZeroBaseStrength(String fieldName) {
        PlayerClass mage = findPlayerClass("Mage");
        assertNotNull(mage, "Class not found: Mage");

        Stats baseStats = new Stats();
        mage.applyStatsAtLevel(baseStats, 0);

        int expectedValue = getStatValue(baseStats, fieldName);
        Stats actualStats = livePlayer.getPlayerInfo().getStats();
        int actualValue = getStatValue(actualStats, fieldName);

        assertEquals(expectedValue, actualValue,
            fieldName + " should match Mage's level-0 base value");
    }

    @Given("the running player's {string} has been changed from its class default")
    public void fieldHasBeenChangedFromDefault(String fieldName) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(stats.getStrength() + 1);
            case "Max HP" -> stats.setMaxHp(stats.getMaxHp() + 1);
            default -> throw new IllegalArgumentException("Unhandled fieldName: " + fieldName);
        }
        refreshDisplayedRows();
    }

    @Given("the running player's {string} has been edited away from its class default")
    public void fieldEditedAwayFromDefault(String fieldName) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        switch (fieldName) {
            case "Strength" -> stats.setStrength(Math.max(0, stats.getStrength() - 1));
            default -> throw new IllegalArgumentException("Unhandled fieldName: " + fieldName);
        }
        lastEditedFieldName = fieldName;
        lastEditedFieldValue = getStatValue(stats, fieldName);
        refreshDisplayedRows();
    }

    @Then("the running player's {string} still reflects the edit")
    public void fieldStillReflectsTheEdit(String fieldName) {
        assertEquals(fieldName, lastEditedFieldName);
        Stats stats = livePlayer.getPlayerInfo().getStats();
        assertEquals(lastEditedFieldValue, getStatValue(stats, fieldName));
    }

    @Given("the dev console is attached to a running game")
    public void theDevConsoleIsAttachedToARunningGame() {
        runningPlayer = new Player(0, 0);
        gameForRegen = new GamePanel(GameEventLog.noOp(), runningPlayer, FlatFloorWorld.create());
        startConsole(new DevConsoleModel(List.of()), gameRegenerator());
    }

    /** Regenerates the attached game panel; the fresh seed comes from the scenario, not the wall clock. */
    private WorldRegenerator gameRegenerator() {
        return new WorldRegenerator() {
            @Override
            public long freshSeed() {
                return nextFreshWorldSeed;
            }

            @Override
            public Optional<String> regenerate(long seed) {
                gameForRegen.regenerateWorld(seed);
                return Optional.empty();
            }
        };
    }

    @Given("the next fresh world seed is {int}")
    public void theNextFreshWorldSeedIs(int seed) {
        nextFreshWorldSeed = seed;
    }

    @Given("the running game's world was generated from seed {int}")
    public void theRunningGameSWorldWasGeneratedFromSeed(int seed) {
        gameForRegen.regenerateWorld(seed);
    }

    @Given("the running player has {int} Strength and is at a position other than the new world's spawn")
    public void theRunningPlayerHasStrengthAndIsAwayFromSpawn(int strength) {
        Optional.of(runningPlayer).map(Player::getStats).ifPresent(stats -> stats.setStrength(strength));
        runningPlayer.setPosition(0, 0);
    }

    @Then("the running game's world matches a world generated from seed {int}")
    public void theRunningGameSWorldMatchesAWorldGeneratedFromSeed(int seed) {
        WorldComparison.assertSameWorld(SeededWorldGenerationSteps.worldFromSeed(seed),
                gameForRegen::getScene);
    }

    @Then("the running player is the same player with {int} Strength")
    public void theRunningPlayerIsTheSamePlayerWithStrength(int strength) {
        assertSame(runningPlayer, gameForRegen.getPlayer());
        assertEquals(Optional.of(strength), Optional.of(runningPlayer).map(Player::getStats).map(Stats::getStrength));
    }

    @Then("the running player is at the spawn point of a world generated from seed {int}")
    public void theRunningPlayerIsAtTheSpawnPointOfAWorldGeneratedFromSeed(int seed) {
        GeneratedWorldScene expected = SeededWorldGenerationSteps.worldFromSeed(seed);
        assertEquals(expected.getSpawnX(), runningPlayer.getX());
        assertEquals(expected.getSpawnY(), runningPlayer.getY());
    }

    @When("the command bar is set to {string}")
    public void theCommandBarIsSetTo(String command) {
        Optional<PlayerInfo> playerInfo = Optional.ofNullable(livePlayer).map(Player::getPlayerInfo);
        playerInfo.map(PlayerInfo::getStats).ifPresent(stats -> statsSnapshot = snapshot(stats));
        playerInfo.map(PlayerInfo::getPlayerClass).map(PlayerClass::getName).ifPresent(name -> classSnapshot = name);
        playerBeforeReload = livePlayer;
        panel.getSearchField().setText(command);
        panel.runCommand();
    }

    @Then("the transcript's last line is a SUCCESS line for {string} set to {int}")
    public void transcriptsLastLineIsSuccessForFieldSetTo(String fieldName, int value) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains(fieldName));
        assertTrue(lastEntry.text().contains(String.valueOf(value)));
    }

    @Then("the transcript's last line is an ERROR line for {word} {string}")
    public void transcriptsLastLineIsErrorFor(String category, String token) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertTrue(lastEntry.text().contains(token), "Expected error text to mention: " + token);
    }

    @Then("the transcript's last line is a SUCCESS line for snapshot {string} saved")
    public void transcriptsLastLineIsSuccessForSnapshotSaved(String snapshotName) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains(snapshotName), "Expected success text to mention snapshot name: " + snapshotName);
        assertTrue(lastEntry.text().contains("saved"), "Expected success text to contain 'saved'");
    }

    @Then("the transcript's last line is a SUCCESS line for snapshot {string} restored")
    public void transcriptsLastLineIsSuccessForSnapshotRestored(String snapshotName) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level());
        assertTrue(lastEntry.text().contains(snapshotName), "Expected success text to mention snapshot name: " + snapshotName);
        assertTrue(lastEntry.text().contains("restored"), "Expected success text to contain 'restored'");
    }

    @Then("the transcript's last line is an ERROR line reading {string}")
    public void transcriptsLastLineIsErrorLineReading(String expectedText) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level());
        assertEquals(expectedText, lastEntry.text(), "Expected exact error text");
    }

    @Given("the dev console also has the {string} provider attached")
    public void theDevConsoleAlsoHasTheProviderAttached(String providerName) {
        List<DevConsoleProvider> providers = List.of(
            new PlayerSandboxProvider(() -> livePlayer),
            providerFor(providerName)
        );
        model = new DevConsoleModel(providers);
        panel = new DevConsolePanel(model);
    }

    @Then("the running player's {string} value is unchanged")
    public void theRunningPlayerSFieldValueIsUnchanged(String fieldName) {
        int before = getStatValue(statsSnapshot, fieldName);
        int after = getStatValue(livePlayer.getPlayerInfo().getStats(), fieldName);
        assertEquals(before, after, fieldName + " should be unchanged");
    }

    @Then("the running player's class value is unchanged")
    public void theRunningPlayerSClassValueIsUnchanged() {
        assertEquals(classSnapshot, livePlayer.getPlayerInfo().getPlayerClass().getName());
    }

    @Given("the dev console is running with a Tiles provider that has no tiles")
    public void theDevConsoleIsRunningWithATilesProviderThatHasNoTiles() {
        List<DevConsoleProvider> providers = List.of(new TileSandboxProvider(List.of()));
        startConsole(new DevConsoleModel(providers));
    }

    @Then("the Tiles provider contributes one result per loaded tile")
    public void theTilesProviderContributesOneResultPerLoadedTile() {
        int loadedTiles = ModLoader.load(Paths.get("mods")).getAllTiles().size();
        assertEquals(loadedTiles, model.filteredResults().size());
    }

    @Then("the tile detail shows these fields:")
    public void theTileDetailShowsTheseFields(DataTable dataTable) {
        assertTrue(panel.isProviderPanelShowing(), "Provider panel should be showing");

        InspectableDetailPanel detailsPane = (InspectableDetailPanel) panel.getOpenedProviderPanel();
        assertNotNull(detailsPane, "Details pane should be opened");

        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        TableWidget<List<String>> table = detailsPane.table(0);
        assertNotNull(table, "Table should exist");
        assertEquals(rows.size(), table.getRowCount(), "Detail row count mismatch");

        table.moveToStart();
        for (int i = 0; i < rows.size(); i++) {
            if (i > 0) {
                table.moveDown();
            }
            Map<String, String> expectedRow = rows.get(i);
            List<String> actualRow = table.getSelectedRow();

            assertEquals(expectedRow.get("Field"), actualRow.get(0),
                "Row " + i + " field name mismatch");
            assertEquals(expectedRow.get("Value"), actualRow.get(1),
                "Row " + i + " value mismatch");
        }
    }

    @Then("the Tiles provider rejects opening {string}")
    public void theTilesProviderRejectsOpening(String id) {
        TileSandboxProvider provider = new TileSandboxProvider();
        assertThrows(IllegalArgumentException.class, () -> provider.createPanel(id),
            "Expected IllegalArgumentException for unknown tile id: " + id);
    }

    @Then("the running player's {string} is Warrior's level-0 base Strength")
    public void fieldIsWarriorLevelZeroBaseStrength(String fieldName) {
        assertFieldIsWarriorBase(fieldName);
    }

    @Then("the running player's {string} is Warrior's level-0 base Max HP")
    public void fieldIsWarriorLevelZeroBaseMaxHp(String fieldName) {
        assertFieldIsWarriorBase(fieldName);
    }

    private void assertFieldIsWarriorBase(String fieldName) {
        PlayerClass warrior = findPlayerClass("Warrior");
        assertNotNull(warrior, "Class not found: Warrior");
        Stats baseStats = new Stats();
        warrior.applyStatsAtLevel(baseStats, 0);
        int expectedValue = getStatValue(baseStats, fieldName);
        int actualValue = getStatValue(livePlayer.getPlayerInfo().getStats(), fieldName);
        assertEquals(expectedValue, actualValue, fieldName + " should match Warrior's level-0 base value");
    }

    @Given("the game's live player is the same object identity before and after opening the provider")
    public void theGameSLivePlayerIsSameIdentityBeforeAndAfter() {
        identityBeforeEdit = livePlayer;
        maxHpBeforeEdit = livePlayer.getPlayerInfo().getStats().getMaxHp();
    }

    @Then("the game's live player is still the same object identity")
    public void theGameSLivePlayerIsStillSameIdentity() {
        assertSame(identityBeforeEdit, livePlayer);
    }

    @Then("the game's live player's {string} reflects the edit")
    public void theGameSLivePlayerSFieldReflectsTheEdit(String fieldName) {
        if ("Max HP".equals(fieldName)) {
            assertEquals(maxHpBeforeEdit + 1, livePlayer.getPlayerInfo().getStats().getMaxHp());
        }
    }

    @When("{string} is armed")
    public void fieldIsArmed(String fieldName) {
        capturePlayerDetailPanel();
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
    }

    @When("left is pressed")
    public void leftIsPressed() {
        fireAction(Keybindings.ACTION_MENU_LEFT);
    }

    @When("right is pressed")
    public void rightIsPressed() {
        fireAction(Keybindings.ACTION_MENU_RIGHT);
    }

    @When("{string} is decreased to {int}")
    public void fieldIsDecreasedTo(String fieldName, int target) {
        Stats stats = livePlayer.getPlayerInfo().getStats();
        int guard = 0;
        while (getStatValue(stats, fieldName) > target && guard < 1000) {
            fireAction(Keybindings.ACTION_MENU_LEFT);
            guard++;
        }
    }

    @When("{string} is opened again")
    public void isOpenedAgain(String entryName) {
        isOpened(entryName);
    }

    @Then("{string} cannot be armed")
    public void fieldCannotBeArmed(String fieldName) {
        // A real user can't navigate to a different row while one is still armed (Up/Down
        // are blocked in that state) - disarm first, same as Escape would, so this check
        // reflects reachable UI state instead of a test-only shortcut around that rule.
        fireAction(Keybindings.ACTION_MENU_CANCEL);
        Stats stats = livePlayer.getPlayerInfo().getStats();
        int before = getStatValue(stats, fieldName);
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
        fireAction(Keybindings.ACTION_MENU_RIGHT);
        int after = getStatValue(stats, fieldName);
        assertEquals(before, after, fieldName + " should not be editable");
    }

    @Then("{string} can be armed")
    public void fieldCanBeArmed(String fieldName) {
        // Mirror fieldCannotBeArmed: disarm any prior armed row, select the row,
        // arm it with Enter, step it with Right, and verify the coordinate changed.
        fireAction(Keybindings.ACTION_MENU_CANCEL);
        int before = getPositionValue(fieldName);
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
        fireAction(Keybindings.ACTION_MENU_RIGHT);
        int after = getPositionValue(fieldName);
        assertNotEquals(before, after, fieldName + " should be editable");
        // Restore to original value for subsequent scenarios
        fireAction(Keybindings.ACTION_MENU_CANCEL);
        selectRow(fieldName);
        fireAction(Keybindings.ACTION_MENU_CONFIRM);
        fireAction(Keybindings.ACTION_MENU_LEFT);
        fireAction(Keybindings.ACTION_MENU_CANCEL);
    }

    @Given("the running player is at position {int}, {int}")
    public void theRunningPlayerIsPlacedAt(int x, int y) {
        livePlayer.setPosition(x, y);
        refreshDisplayedRows();
    }

    @Then("the running player's position is {int}, {int}")
    public void theRunningPlayerPositionIs(int expectedX, int expectedY) {
        assertEquals(expectedX, livePlayer.getX(), "Expected X coordinate to be " + expectedX);
        assertEquals(expectedY, livePlayer.getY(), "Expected Y coordinate to be " + expectedY);
    }

    private void refreshDisplayedRows() {
        capturePlayerDetailPanel();
        if (playerDetailPanel != null) {
            playerDetailPanel.refreshAllRows();
        }
    }

    private void selectRow(String fieldName) {
        TableWidget<List<String>> table = playerDetailPanel.getStatsTable();
        int targetIndex = rowIndexOf(fieldName);
        table.moveToStart();
        for (int i = 0; i < targetIndex; i++) {
            table.moveDown();
        }
    }

    private void fireAction(String actionName) {
        capturePlayerDetailPanel();
        if (playerDetailPanel != null) {
            Action action = playerDetailPanel.getActionMap().get(actionName);
            if (action != null) {
                action.actionPerformed(new ActionEvent(playerDetailPanel, ActionEvent.ACTION_PERFORMED, ""));
            }
        }
    }

    private void fireCommandAction(String actionName) {
        Action action = panel.getSearchField().getActionMap().get(actionName);
        if (action != null) {
            action.actionPerformed(new ActionEvent(panel.getSearchField(), ActionEvent.ACTION_PERFORMED, ""));
        }
    }

    private static int rowIndexOf(String fieldName) {
        return switch (fieldName) {
            case "Class" -> 0;
            case "Strength" -> 1;
            case "Dexterity" -> 2;
            case "Constitution" -> 3;
            case "Intelligence" -> 4;
            case "Wisdom" -> 5;
            case "Luck" -> 6;
            case "Max HP" -> 7;
            case "Max Mana" -> 8;
            case "Current HP" -> 9;
            case "Current Mana" -> 10;
            case "X" -> 11;
            case "Y" -> 12;
            case "Attack Power" -> 13;
            case "Defense" -> 14;
            default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
        };
    }

    private void capturePlayerDetailPanel() {
        if (playerDetailPanel == null && panel.isProviderPanelShowing()
                && panel.getOpenedProviderPanel() instanceof PlayerDetailPanel opened) {
            playerDetailPanel = opened;
        }
    }

    private ItemDetailPanel openedItemDetailPanel() {
        assertTrue(panel.isProviderPanelShowing(), "An item detail panel should be open");
        assertTrue(panel.getOpenedProviderPanel() instanceof ItemDetailPanel,
                "The opened provider panel should be an item detail panel");
        return (ItemDetailPanel) panel.getOpenedProviderPanel();
    }

    private String getRowFieldName(int rowIndex) {
        List<String> row = getRow(rowIndex);
        return row.isEmpty() ? "" : row.get(0);
    }

    private List<String> getRow(int rowIndex) {
        TableWidget<List<String>> table = playerDetailPanel.getStatsTable();
        if (rowIndex >= table.getRowCount()) {
            return List.of();
        }
        table.moveToStart();
        for (int i = 0; i < rowIndex; i++) {
            table.moveDown();
        }
        return table.getSelectedRow();
    }

    private static int getStatValue(Stats stats, String fieldName) {
        return switch (fieldName) {
            case "Strength" -> stats.getStrength();
            case "Dexterity" -> stats.getDexterity();
            case "Constitution" -> stats.getConstitution();
            case "Intelligence" -> stats.getIntelligence();
            case "Wisdom" -> stats.getWisdom();
            case "Luck" -> stats.getLuck();
            case "Max HP" -> stats.getMaxHp();
            case "Max Mana" -> stats.getMaxMana();
            case "Current HP" -> stats.getCurrentHp();
            case "Current Mana" -> stats.getCurrentMana();
            case "Attack Power" -> stats.getAttackPower();
            case "Defense" -> stats.getDefense();
            default -> 0;
        };
    }

    private int getPositionValue(String fieldName) {
        return switch (fieldName) {
            case "X" -> livePlayer.getX();
            case "Y" -> livePlayer.getY();
            default -> 0;
        };
    }

    private TranscriptWidget.TranscriptEntry lastTranscriptEntry() {
        var entries = panel.getTranscript().entries();
        assertFalse(entries.isEmpty(), TRANSCRIPT_SHOULD_HAVE_ENTRIES);
        return entries.get(entries.size() - 1);
    }

    private static Stats snapshot(Stats stats) {
        Stats copy = new Stats();
        copy.setStrength(stats.getStrength());
        copy.setDexterity(stats.getDexterity());
        copy.setConstitution(stats.getConstitution());
        copy.setIntelligence(stats.getIntelligence());
        copy.setWisdom(stats.getWisdom());
        copy.setLuck(stats.getLuck());
        copy.setMaxHp(stats.getMaxHp());
        copy.setMaxMana(stats.getMaxMana());
        copy.setCurrentHp(stats.getCurrentHp());
        copy.setCurrentMana(stats.getCurrentMana());
        return copy;
    }

    private static @Nullable PlayerClass findPlayerClass(String name) {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        return mods.getAllPlayerClasses().stream()
            .filter(cls -> name.equals(cls.getName()))
            .findFirst()
            .orElse(null);
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }

    /**
     * Starts the console on {@code newModel} and publishes it to {@link SharedScenarioContext},
     * so a later step in the same scenario can't be shadowed by a model an earlier step shared.
     */
    private void startConsole(DevConsoleModel newModel) {
        startConsole(newModel, WorldRegenerator.noGame(new Random()::nextLong));
    }

    private void startConsole(DevConsoleModel newModel, WorldRegenerator regenerator) {
        model = newModel;
        panel = new DevConsolePanel(newModel, regenerator);
        SharedScenarioContext.setDevConsoleModel(model);
        SharedScenarioContext.setDevConsolePanel(panel);
        SharedScenarioContext.setDevConsoleSteps(this);
    }

    private DevConsoleModel getCurrentModel() {
        // Check if there's an updated model in the shared context (e.g., from loading fixture quests)
        DevConsoleModel sharedModel = SharedScenarioContext.getDevConsoleModel();
        if (sharedModel != null) {
            model = sharedModel;
        }
        return model;
    }

    private DevConsolePanel getCurrentPanel() {
        // Check if there's an updated panel in the shared context (e.g., from loading fixture quests)
        DevConsolePanel sharedPanel = SharedScenarioContext.getDevConsolePanel();
        if (sharedPanel != null) {
            panel = sharedPanel;
        }
        return panel;
    }

    private List<String> resultNames() {
        return getCurrentModel().filteredResults().stream().map(result -> result.entry().name()).toList();
    }

    private DevConsoleModel.SearchResult findResult(String name) {
        return getCurrentModel().filteredResults().stream()
                .filter(result -> result.entry().name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No result named: " + name));
    }

    private QuestLog liveQuestLog() {
        return Optional.of(livePlayer)
                .map(Player::getPlayerInfo)
                .map(PlayerInfo::getQuestLog)
                .orElseThrow();
    }

    @Given("the dev console is running with a provider whose entries can change")
    public void theDevConsoleIsRunningWithAProviderWhoseEntriesCanChange() {
        fakeProvider = new ReloadableFakeProvider(CLASSES_PROVIDER_NAME);
        List<DevConsoleProvider> providers = List.of(fakeProvider);
        startConsole(new DevConsoleModel(providers));
    }

    @Given("the provider starts with the entry {string}")
    public void theProviderStartsWithTheEntry(String entryId) {
        // Entries must be set before model construction, so recreate
        fakeProvider.addEntry(createEntry(entryId));
        startConsole(new DevConsoleModel(List.of(fakeProvider)));
    }

    @Given("the provider starts with the entries {string} and {string}")
    public void theProviderStartsWithTheEntries(String entry1, String entry2) {
        fakeProvider.addEntry(createEntry(entry1));
        fakeProvider.addEntry(createEntry(entry2));
        startConsole(new DevConsoleModel(List.of(fakeProvider)));
    }

    @Given("the provider's data now also contains the entry {string}")
    public void theProviderSDataNowAlsoContainsTheEntry(String entryId) {
        fakeProvider.addEntry(createEntry(entryId));
    }

    @Given("the provider's data now only contains the entry {string}")
    public void theProviderSDataNowOnlyContainsTheEntry(String entryId) {
        fakeProvider.clearEntries();
        fakeProvider.addEntry(createEntry(entryId));
    }

    @Given("the provider's next refresh fails with {string}")
    public void theProviderSNextRefreshFailsWith(String errorMessage) {
        fakeProvider.setNextReloadFails(errorMessage);
    }

    @Then("searching for {string} finds the entry {string}")
    public void searchingForFindsTheEntry(String searchTerm, String expectedEntryId) {
        model.setSearchText(searchTerm);
        List<DevConsoleModel.SearchResult> results = model.filteredResults();
        boolean found = results.stream()
                .anyMatch(r -> r.entry().id().equals(expectedEntryId));
        assertTrue(found, "Entry " + expectedEntryId + " not found in search results for: " + searchTerm);
    }

    @Then("searching for {string} finds no entries")
    public void searchingForFindsNoEntries(String searchTerm) {
        model.setSearchText(searchTerm);
        List<DevConsoleModel.SearchResult> results = model.filteredResults();
        assertTrue(results.isEmpty(), "Expected no results for search: " + searchTerm);
    }

    @Then("the transcript's last line is a SUCCESS line {string}")
    public void theTranscriptSLastLineIsASuccessLine(String expectedText) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.SUCCESS, lastEntry.level(),
            "Expected SUCCESS line but got: " + lastEntry.level());
        assertTrue(lastEntry.text().contains(expectedText),
            "Expected SUCCESS line to contain: " + expectedText + ", but got: " + lastEntry.text());
    }

    @Then("the transcript's last line is an ERROR line {string}")
    public void theTranscriptSLastLineIsAnErrorLine(String expectedText) {
        TranscriptWidget.TranscriptEntry lastEntry = lastTranscriptEntry();
        assertEquals(TranscriptWidget.Level.ERROR, lastEntry.level(),
            "Expected ERROR line but got: " + lastEntry.level());
        assertTrue(lastEntry.text().contains(expectedText),
            "Expected ERROR line to contain: " + expectedText + ", but got: " + lastEntry.text());
    }

    @Then("every registered provider was asked to refresh exactly once")
    public void everyRegisteredProviderWasAskedToRefreshExactlyOnce() {
        assertEquals(1, fakeProvider.reloadCallCount(), "First provider should be reloaded once");
        if (secondProvider != null) {
            assertEquals(1, secondProvider.reloadCallCount(), "Second provider should be reloaded once");
        }
    }

    @Then("no provider was asked to refresh")
    public void noProviderWasAskedToRefresh() {
        assertEquals(0, fakeProvider.reloadCallCount(), "Provider should not be reloaded");
        if (secondProvider != null) {
            assertEquals(0, secondProvider.reloadCallCount(), "Second provider should not be reloaded");
        }
    }

    @Given("a second provider is also registered")
    public void aSecondProviderIsAlsoRegistered() {
        secondProvider = new ReloadableFakeProvider("Quests");
        List<DevConsoleProvider> providers = List.of(fakeProvider, secondProvider);
        startConsole(new DevConsoleModel(providers));
    }

    @Given("the dev console also has the {string} provider attached to the running player")
    public void theDevConsoleAlsoHasTheProviderAttachedToTheRunningPlayer(String providerName) {
        if (livePlayer == null) {
            livePlayer = new Player(0, 0);
        }
        List<DevConsoleProvider> providers = List.of(fakeProvider, new PlayerSandboxProvider(() -> livePlayer));
        startConsole(new DevConsoleModel(providers));
    }

    @Then("the transcript reports {int} results for {string}")
    public void theTranscriptReportsResultsFor(int expectedCount, String searchTerm) {
        List<TranscriptWidget.TranscriptEntry> entries = panel.getTranscript().entries();
        boolean found = entries.stream().anyMatch(entry ->
            entry.level() == TranscriptWidget.Level.INFO &&
            entry.text().contains(String.valueOf(expectedCount)) &&
            entry.text().contains(searchTerm)
        );
        assertTrue(found, "Expected transcript to report " + expectedCount + " results for: " + searchTerm);
    }

    @Then("the running player is the same player instance as before the reload")
    public void theRunningPlayerIsSamePlayerInstanceAsBeforeTheReload() {
        assertSame(playerBeforeReload, livePlayer, "Player should be the same instance");
    }

    private DevConsoleProvider providerFor(String name) {
        if (CLASSES_PROVIDER_NAME.equals(name)) {
            return new ClassSandboxProvider();
        }
        if (PLAYER_PROVIDER_NAME.equals(name)) {
            return new PlayerSandboxProvider(() -> livePlayer);
        }
        if (ITEMS_PROVIDER_NAME.equals(name)) {
            return new ItemSandboxProvider();
        }
        if (KITCHEN_SINK_PROVIDER_NAME.equals(name)) {
            ModRegistry mods = ModLoader.load(Paths.get("mods"));
            KitchenSinkScene scene = KitchenSinkScene.holding(mods.getAllTiles());
            return new KitchenSinkProvider(() -> scene);
        }
        if (QUESTS_PROVIDER_NAME.equals(name)) {
            return new QuestSandboxProvider();
        }
        if (TILES_PROVIDER_NAME.equals(name)) {
            return new TileSandboxProvider();
        }
        throw new IllegalArgumentException("Unknown provider: " + name);
    }

    /** Restarts the console with just the given provider, for scenarios that need a custom one. */
    public void runWith(DevConsoleProvider provider) {
        startConsole(new DevConsoleModel(List.of(provider)));
    }

    public List<DevConsoleModel.SearchResult> currentResults() {
        return model.filteredResults();
    }

    public boolean isDetailPanelShowing() {
        return panel.isProviderPanelShowing();
    }

    /** Runs {@code edit <entryId>}; returns the detail panel it opened, or null. */
    public @Nullable Component openEntry(String entryId) {
        JTextField searchField = panel.getSearchField();
        searchField.setText("edit " + entryId);
        panel.runCommand();
        return panel.isProviderPanelShowing() ? panel.getOpenedProviderPanel() : null;
    }

    private static DevConsoleEntry createEntry(String id) {
        String namespace = "core";
        String category = CLASSES_PROVIDER_NAME;
        String name = id.replace("core:", "");
        return new DevConsoleEntry(namespace, id, category, name);
    }
}
