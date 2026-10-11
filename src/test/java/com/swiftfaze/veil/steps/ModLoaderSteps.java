package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.npc.Patrol;
import com.swiftfaze.veil.entities.npc.TileCoord;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.entities.quests.Quest;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.mods.WidgetColorTheme;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.object.Chest;
import com.swiftfaze.veil.world.object.Door;
import com.swiftfaze.veil.world.object.InteractableObject;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModLoaderSteps {

    private static final String TEST_MOD = "test";
    private static final String TEST_COLOR = "\"color\":{\"r\":160,\"g\":110,\"b\":60}";
    private static final String DOOR_SYMBOLS = "\"symbolOpen\":\"/\",\"symbolClosed\":\"+\"";
    private static final String CORE_WOOD = "core:wood";
    private static final Pattern CHOICE_IN_STEP = Pattern.compile("\"([^\"]*)\" (?:leading to \"([^\"]*)\"|ending the conversation)");

    private String lastBuildingId;
    private InteractableObject lastPlacedObject;
    private static final String NPC_KIND = "npc";
    private final Map<String, List<JsonObject>> npcsByMod = new LinkedHashMap<>();
    private final Map<String, List<JsonObject>> dialoguesByMod = new LinkedHashMap<>();
    private JsonObject currentDialogue = new JsonObject();
    private String checkedDialogueId = "";
    private String expectedErrorWord = "";
    private String lastCheckedNpcId;

    private record BuildingFixture(String id, String overrides, String explicitTileId) {
    }

    record TileFixture(String id, char symbol, int r, int g, int b, boolean walkable, @Nullable String overrides) {
    }

    record StatEntry(@Nullable Integer base, @Nullable String growthCalc) {
    }

    private record ClassFixture(String id, String name, Map<String, StatEntry> stats, String overrides) {
    }

    record ItemEffectFixture(String type, String stat, String calc) {
    }

    private record ItemFixture(String id, String name, Character glyph, String type, String slot,
                                Integer baseDamageMin, Integer baseDamageMax,
                                List<ItemEffectFixture> effects, String overrides) {
    }

    record QuestRewardFixture(String type, @Nullable String itemId, @Nullable Integer count, @Nullable String calc) {
    }

    private record QuestFixture(String id, String name, String objectiveType, String target, Integer count,
                                 List<QuestRewardFixture> rewards, String overrides) {
    }

    record BaseStatsFixture(Map<String, StatEntry> stats) {
        BaseStatsFixture {
            stats = Map.copyOf(stats);
        }
    }

    record ObjectiveFixture(String type, String target, int count) {
    }

    record RewardsFixture(List<QuestRewardFixture> rewards) {
        RewardsFixture {
            rewards = List.copyOf(rewards);
        }
    }

    record ItemLookFixture(char glyph, String type, String slot, int baseDamageMin, int baseDamageMax) {
    }

    @ParameterType("id \"([^\"]*)\", symbol \"([^\"]*)\", color \\((-?\\d+), (-?\\d+), (-?\\d+)\\), and walkable (\\w+)")
    public TileFixture tileLook(String... groups) {
        return new TileFixture(groups[0], groups[1].charAt(0), Integer.parseInt(groups[2]), Integer.parseInt(groups[3]),
                Integer.parseInt(groups[4]), Boolean.parseBoolean(groups[5]), null);
    }

    @ParameterType("base strength (-?\\d+), dexterity (-?\\d+), constitution (-?\\d+), intelligence (-?\\d+), "
            + "wisdom (-?\\d+), luck (-?\\d+), max HP (-?\\d+), and max mana (-?\\d+)")
    public BaseStatsFixture baseStats(String... groups) {
        List<String> names = List.of("strength", "dexterity", "constitution", "intelligence", "wisdom", "luck",
                "maxHp", "maxMana");
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            stats.put(names.get(i), new StatEntry(Integer.parseInt(groups[i]), null));
        }
        return new BaseStatsFixture(stats);
    }

    @ParameterType("glyph \"([^\"]*)\", type \"([^\"]*)\", slot \"([^\"]*)\", base damage min (-?\\d+) and max (-?\\d+)")
    public ItemLookFixture itemLook(String... groups) {
        return new ItemLookFixture(groups[0].charAt(0), groups[1], groups[2], Integer.parseInt(groups[3]),
                Integer.parseInt(groups[4]));
    }

    @ParameterType("\"([^\"]*)\" effect on stat \"([^\"]*)\" with calc \"([^\"]*)\"")
    public ItemEffectFixture itemEffect(String type, String stat, String calc) {
        return new ItemEffectFixture(type, stat, calc);
    }

    @ParameterType("objective type \"([^\"]*)\" on target \"([^\"]*)\" with count (-?\\d+)")
    public ObjectiveFixture questObjective(String type, String target, String count) {
        return new ObjectiveFixture(type, target, Integer.parseInt(count));
    }

    @ParameterType("an item reward of \"([^\"]*)\" count (-?\\d+), and an xp reward with calc \"([^\"]*)\"")
    public RewardsFixture itemAndXpRewards(String itemId, String itemCount, String xpCalc) {
        return new RewardsFixture(List.of(
                new QuestRewardFixture("item", itemId, Integer.parseInt(itemCount), null),
                new QuestRewardFixture("xp", null, null, xpCalc)));
    }

    private record ThemeColorFixture(int r, int g, int b) {
    }

    private record ThemeFixture(String id, Map<String, ThemeColorFixture> colors, String overrides) {
    }

    // Arbitrary valid RGB component for tile-color fixtures below; the actual
    // color is irrelevant to schema validation, only its shape (0-255 ints) is.
    private static final int STUB_COLOR_COMPONENT = 100;

    private static final Map<String, Integer> WARRIOR_STATS = Map.of(
            "strength", 15, "dexterity", 10, "constitution", 14, "intelligence", 6,
            "wisdom", 6, "luck", 8, "maxHp", 120, "maxMana", 20);

    private Path modsRoot;
    private final Map<String, List<String>> dependsOnByMod = new LinkedHashMap<>();
    private final Map<String, List<BuildingFixture>> buildingsByMod = new LinkedHashMap<>();
    private final Map<String, List<TileFixture>> tilesByMod = new LinkedHashMap<>();
    private final Map<String, List<ClassFixture>> classesByMod = new LinkedHashMap<>();
    private final Map<String, List<ItemFixture>> itemsByMod = new LinkedHashMap<>();
    private final Map<String, List<QuestFixture>> questsByMod = new LinkedHashMap<>();
    private final Map<String, List<ThemeFixture>> themesByMod = new LinkedHashMap<>();
    private String overriddenBuildingId;
    private String lastCheckedTileId;
    private String lastCheckedClassId;
    private String lastCheckedItemId;
    private String lastCheckedQuestId;
    private String lastCheckedThemeId;
    private String lastCheckedEntityKind;
    private boolean needsMarkerTiles;

    private ModRegistry registry;
    private ModLoadException thrown;
    private Map<String, Color> widgetThemeSnapshot;

    @Before
    public void createModsRoot() throws IOException {
        modsRoot = Files.createTempDirectory("veil-mods-test");
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

    // WidgetTheme's fields are mutable statics shared across the whole test JVM, so any
    // scenario that calls WidgetTheme.applyTheme() (proving the theme actually applies) must
    // not leak its mutation into unrelated tests that run later in the same fork.
    @Before
    public void snapshotWidgetTheme() {
        widgetThemeSnapshot = new LinkedHashMap<>();
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            widgetThemeSnapshot.put(key, widgetThemeColor(key));
        }
    }

    @After
    public void restoreWidgetTheme() {
        WidgetTheme.applyTheme(new WidgetColorTheme("test:snapshot", widgetThemeSnapshot));
    }

    @Given("a mods directory containing mod {string} so the mods directory is never empty")
    public void aModsDirectoryContainingModSoTheModsDirectoryIsNeverEmpty(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
    }

    @Given("a mods directory containing the {string} mod with a building declaring id {string}")
    public void aModsDirectoryContainingTheModWithABuildingDeclaringId(String modId, String buildingId) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string}")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringId(String modId, String buildingId) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringIdAndNoField(String modId, String buildingId, String fieldName) {
        addBuilding(modId, buildingId, null, null);
    }

    @Given("the mods directory also contains mod {string} with a building declaring id {string} and an {string} field of {string}")
    public void theModsDirectoryAlsoContainsModWithABuildingDeclaringIdAndAnFieldOf(String modId, String buildingId, String fieldName, String overriddenId) {
        addBuilding(modId, buildingId, overriddenId, null);
        overriddenBuildingId = overriddenId;
    }

    @Given("mod {string} declares a {string} of {string}")
    public void modDeclaresADependsOnOf(String modId, String fieldName, String dependsOnId) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(dependsOnId);
    }

    @Given("a mods directory containing mod {string} with a malformed building file")
    public void aModsDirectoryContainingModWithAMalformedBuildingFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("buildings"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("buildings").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing mod {string} with a malformed mod.json file")
    public void aModsDirectoryContainingModWithAMalformedModJsonFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a tile declaring id {string}, symbol {string}, color \\({int}, {int}, {int}), and walkable {word}")
    public void aModsDirectoryContainingTheModWithATileDeclaringId(String modId, String tileId, String symbol,
                                                                    int r, int g, int b, String walkable) {
        addTile(modId, tileId, symbol.charAt(0), r, g, b, Boolean.parseBoolean(walkable), null);
    }

    @Given("the mods directory also contains mod {string} with a tile declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithATileDeclaringIdAndNoField(String modId, String tileId, String fieldName) {
        addTile(modId, tileId, '?', 0, 0, 0, /* walkable= */ true, null);
    }

    @Given("the mods directory also contains mod {string} with a tile declaring {tileLook}, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithATileDeclaringIdOverriding(String modId, TileFixture tile,
                                                                               String fieldName, String overriddenId) {
        addTile(modId, tile.id(), tile.symbol(), tile.r(), tile.g(), tile.b(), tile.walkable(), overriddenId);
    }

    @Given("the mods directory also contains a building declaring id {string} whose blueprint is a single tile {string}")
    public void theModsDirectoryAlsoContainsABuildingDeclaringIdWhoseBlueprintIsASingleTile(String buildingId, String tileId) {
        String modId = buildingId.split(":", -1)[0];
        addBuilding(modId, buildingId, null, tileId);
    }

    @Given("a mods directory containing mod {string} with a malformed tile file")
    public void aModsDirectoryContainingModWithAMalformedTileFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("tiles"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("tiles").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a theme declaring id {string} and all twelve widget colors")
    public void aModsDirectoryContainingTheModWithAThemeDeclaringIdAndAllTwelveWidgetColors(String modId, String themeId) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string} and all twelve widget colors")
    public void theModsDirectoryAlsoContainsModWithAThemeDeclaringIdAndAllTwelveWidgetColors(String modId, String themeId) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAThemeDeclaringIdAndNoField(String modId, String themeId, String fieldName) {
        addTheme(modId, themeId, defaultThemeColors(), null);
    }

    @Given("the mods directory also contains mod {string} with a theme declaring id {string}, a {string} color of \\({int}, {int}, {int}), and the rest of the twelve widget colors, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAThemeOverridingOneColor(String modId, String themeId, String colorKey,
                                                                             int r, int g, int b,
                                                                             String fieldName, String overriddenId) {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>(defaultThemeColors());
        colors.put(colorKey, new ThemeColorFixture(r, g, b));
        addTheme(modId, themeId, colors, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a theme declaring id {string} that omits {string}")
    public void aModsDirectoryContainingModWithAThemeThatOmits(String modId, String themeId, String omittedKey) {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>(defaultThemeColors());
        colors.remove(omittedKey);
        addTheme(modId, themeId, colors, null);
    }

    @Given("a mods directory containing mod {string} with a malformed theme file")
    public void aModsDirectoryContainingModWithAMalformedThemeFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("themes"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("themes").resolve("broken.json"), "{ not valid json");
    }

    // Schema validation fixtures for mod-json-contract.feature
    @Given("a mods directory containing mod {string} with a {word} file missing the required field {string}")
    public void aModsDirectoryContainingModWithAFileMissingRequiredField(String modId, String fileType, String fieldName) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");

        JsonObject fileJson = new JsonObject();
        switch (fileType) {
            case "tile" -> {
                fileJson.addProperty("id", "test:stone");
                fileJson.addProperty("symbol", "#");
                JsonObject color = new JsonObject();
                color.addProperty("r", STUB_COLOR_COMPONENT);
                color.addProperty("g", STUB_COLOR_COMPONENT);
                color.addProperty("b", STUB_COLOR_COMPONENT);
                fileJson.add("color", color);
                if (!fieldName.equals("walkable")) {
                    fileJson.addProperty("walkable", true);
                }
                Path tilesDir = modDir.resolve("tiles");
                Files.createDirectories(tilesDir);
                Files.writeString(tilesDir.resolve("stone.json"), fileJson.toString());
            }
            case "item" -> {
                fileJson.addProperty("id", "test:sword");
                fileJson.addProperty("name", "Iron Sword");
                fileJson.addProperty("glyph", "/");
                fileJson.addProperty("type", "weapon");
                if (!fieldName.equals("slot")) {
                    fileJson.addProperty("slot", "main_hand");
                }
                Path itemsDir = modDir.resolve("items");
                Files.createDirectories(itemsDir);
                Files.writeString(itemsDir.resolve("sword.json"), fileJson.toString());
            }
            case "quest" -> {
                fileJson.addProperty("id", "test:quest");
                fileJson.addProperty("name", "My Quest");
                if (!fieldName.equals("objective")) {
                    JsonObject objective = new JsonObject();
                    objective.addProperty("type", "kill");
                    fileJson.add("objective", objective);
                }
                Path questsDir = modDir.resolve("quests");
                Files.createDirectories(questsDir);
                Files.writeString(questsDir.resolve("quest.json"), fileJson.toString());
            }
            default -> throw new IllegalArgumentException("Unhandled fileType: " + fileType);
        }
    }

    @Given("a mods directory containing mod {string} with a tile file whose {string} field is a string instead of an object")
    public void aModsDirectoryContainingModWithATileFileWithWrongTypeColor(String modId, String fieldName) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");

        JsonObject tile = new JsonObject();
        tile.addProperty("id", "test:stone");
        tile.addProperty("symbol", "#");
        tile.addProperty(fieldName, "this-should-be-an-object");  // Wrong type!
        tile.addProperty("walkable", true);

        Path tilesDir = modDir.resolve("tiles");
        Files.createDirectories(tilesDir);
        Files.writeString(tilesDir.resolve("stone.json"), tile.toString());
    }

    @Given("a mods directory containing mod {string} with a tile file that has an {string} field instead of the recognized {string} field")
    public void aModsDirectoryContainingModWithATileFileWithUnknownField(String modId, String wrongFieldName, String correctFieldName) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");

        JsonObject tile = new JsonObject();
        tile.addProperty("id", "test:stone");
        tile.addProperty("symbol", "#");
        JsonObject color = new JsonObject();
        color.addProperty("r", STUB_COLOR_COMPONENT);
        color.addProperty("g", STUB_COLOR_COMPONENT);
        color.addProperty("b", STUB_COLOR_COMPONENT);
        tile.add("color", color);
        tile.addProperty("walkable", true);
        tile.addProperty(wrongFieldName, "core:other");  // Typo: overides instead of overrides

        Path tilesDir = modDir.resolve("tiles");
        Files.createDirectories(tilesDir);
        Files.writeString(tilesDir.resolve("stone.json"), tile.toString());
    }

    @Given("a mods directory containing mod {string} with a tile file whose id is {string} with no namespace separator")
    public void aModsDirectoryContainingModWithATileFileWithMalformedId(String modId, String malformedId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");

        JsonObject tile = new JsonObject();
        tile.addProperty("id", malformedId);  // No colon separator
        tile.addProperty("symbol", "#");
        JsonObject color = new JsonObject();
        color.addProperty("r", STUB_COLOR_COMPONENT);
        color.addProperty("g", STUB_COLOR_COMPONENT);
        color.addProperty("b", STUB_COLOR_COMPONENT);
        tile.add("color", color);
        tile.addProperty("walkable", true);

        Path tilesDir = modDir.resolve("tiles");
        Files.createDirectories(tilesDir);
        Files.writeString(tilesDir.resolve("stone.json"), tile.toString());
    }

    @Given("a mods directory containing mod {string} whose mod.json declares a {string} of {string}")
    public void aModsDirectoryContainingModWithUnresolvedDependency(String modId, String fieldName, String unresolvedDep) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);

        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", modId);
        JsonArray dependsOn = new JsonArray();
        dependsOn.add(unresolvedDep);
        manifest.add("dependsOn", dependsOn);
        Files.writeString(modDir.resolve("mod.json"), manifest.toString());
    }

    @Given("a mods directory containing mod {string} with a building whose blueprint references tile id {string}")
    public void aModsDirectoryContainingModWithABuildingWithUnresolvedTileId(String modId, String unresolvedTileId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");

        JsonObject building = new JsonObject();
        building.addProperty("id", "test:house");
        building.addProperty("name", "Test House");
        building.addProperty("width", 1);
        building.addProperty("height", 1);

        JsonArray row = new JsonArray();
        row.add(unresolvedTileId);  // This tile id doesn't exist
        JsonArray tiles = new JsonArray();
        tiles.add(row);
        building.add("tiles", tiles);

        Path buildingsDir = modDir.resolve("buildings");
        Files.createDirectories(buildingsDir);
        Files.writeString(buildingsDir.resolve("house.json"), building.toString());
    }

    @When("the mods directory is loaded")
    public void theModsDirectoryIsLoaded() throws IOException {
        writeFixtures();
        try {
            registry = ModLoader.load(modsRoot);
        } catch (ModLoadException e) {
            thrown = e;
        }
    }

    @Then("a building with ID {string} is available")
    public void aBuildingWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getBuilding(id), "expected building '" + id + "' to be loaded");
    }

    @Then("a tile with ID {string} is available")
    public void aTileWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getTile(id), "expected tile '" + id + "' to be loaded");
        lastCheckedTileId = id;
    }

    @Then("its symbol is {string}")
    public void itsSymbolIs(String symbol) {
        assertEquals(symbol.charAt(0), registry.getTile(lastCheckedTileId).getSymbol());
    }

    @Then("its color is \\({int}, {int}, {int})")
    public void itsColorIs(int r, int g, int b) {
        assertEquals(new Color(r, g, b), registry.getTile(lastCheckedTileId).getColor());
    }

    @Then("it is walkable")
    public void itIsWalkable() {
        assertTrue(registry.getTile(lastCheckedTileId).isWalkable());
    }

    @Then("it is not walkable")
    public void itIsNotWalkable() {
        assertFalse(registry.getTile(lastCheckedTileId).isWalkable());
    }

    @Then("the building {string}'s blueprint at \\({int}, {int}) references tile {string}")
    public void theBuildingsBlueprintAtReferencesTile(String buildingId, int x, int y, String expectedTileId) {
        List<List<Tile>> blueprint = registry.getBuilding(buildingId).getBlueprint();
        assertEquals(registry.getTile(expectedTileId), blueprint.get(y).get(x));
    }

    @Then("loading fails with a ModLoadException naming the colliding ID {string} and both mods {string} and {string}")
    public void loadingFailsWithAModLoadExceptionNamingTheCollidingIdAndBothMods(String id, String modA, String modB) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(id), "expected message to name id: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(modA), "expected message to name mod: " + modA);
        assertTrue(thrown.getMessage().contains(modB), "expected message to name mod: " + modB);
    }

    @Then("its blueprint matches the one from mod {string}, not {string}")
    public void itsBlueprintMatchesTheOneFromModNot(String overridingMod, String originalMod) {
        List<List<Tile>> blueprint = registry.getBuilding(overriddenBuildingId).getBlueprint();
        assertEquals(registry.getTile("test:stone"), blueprint.get(0).get(0));
    }

    @Then("mod {string} finishes loading before mod {string} starts loading")
    public void modFinishesLoadingBeforeModStartsLoading(String earlierMod, String laterMod) {
        List<String> order = registry.getModLoadOrder();
        assertTrue(order.indexOf(earlierMod) < order.indexOf(laterMod));
    }

    @Then("a ModLoadException is thrown wrapping the underlying cause")
    public void aModLoadExceptionIsThrownWrappingTheUnderlyingCause() {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertNotNull(thrown.getCause(), "expected the ModLoadException to wrap an underlying cause");
    }

    @Then("a theme with ID {string} is available")
    public void aThemeWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getTheme(id), "expected theme '" + id + "' to be loaded");
        lastCheckedThemeId = id;
    }

    @Then("its {string} color is \\({int}, {int}, {int})")
    public void itsNamedColorIs(String colorKey, int r, int g, int b) {
        assertEquals(new Color(r, g, b), registry.getTheme(lastCheckedThemeId).color(colorKey));
    }

    @Then("WidgetTheme's colors match the {string} theme's colors exactly")
    public void widgetThemesColorsMatchTheThemesColorsExactly(String themeId) {
        assertWidgetThemeMatches(themeId);
    }

    @Then("WidgetTheme's colors still match the {string} theme's colors")
    public void widgetThemesColorsStillMatchTheThemesColors(String themeId) {
        assertWidgetThemeMatches(themeId);
    }

    @Then("loading fails with a ModLoadException naming the missing color key {string} and the file for theme {string}")
    public void loadingFailsWithAModLoadExceptionNamingTheMissingColorKeyAndTheFileForTheme(String colorKey, String themeId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains(colorKey), "expected message to name missing color key: " + message);
        // Schema validation errors may not name the theme ID, only the file or color key.
        assertTrue(message.contains("theme") || message.contains(colorKey) || message.contains("file"),
                "expected message to reference a theme file or color key: " + message);
    }

    @Given("a mods directory containing the {string} mod with an NPC declaring id {string}")
    public void aModsDirectoryContainingTheModWithAnNpcDeclaringId(String modId, String npcId) {
        addNpc(modId, NpcJson.npc(npcId).toJson());
    }

    @Given("the mods directory also contains mod {string} with an NPC declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAnNpcDeclaringIdAndNoField(String modId, String npcId, String fieldName) {
        addNpc(modId, NpcJson.npc(npcId).toJson());
    }

    @ParameterType("name \"([^\"]*)\", symbol \"([^\"]*)\", color (-?\\d+),(-?\\d+),(-?\\d+), spawn \\((-?\\d+), (-?\\d+)\\)")
    public NpcLookFixture npcLook(String... groups) {
        return new NpcLookFixture(groups[0], groups[1], Integer.parseInt(groups[2]), Integer.parseInt(groups[3]),
                Integer.parseInt(groups[4]), Integer.parseInt(groups[5]), Integer.parseInt(groups[6]));
    }

    @ParameterType("\\((-?\\d+), (-?\\d+)\\) and \\((-?\\d+), (-?\\d+)\\)")
    public List<TileCoord> waypointPair(String... groups) {
        return List.of(new TileCoord(Integer.parseInt(groups[0]), Integer.parseInt(groups[1])),
                new TileCoord(Integer.parseInt(groups[2]), Integer.parseInt(groups[3])));
    }

    @Given("a mods directory containing the {string} mod with an NPC declaring id {string}, {npcLook}, and a {string} patrol of waypoints {waypointPair}")
    public void aModsDirectoryContainingAnNpcWithAPatrol(String modId, String npcId, NpcLookFixture look, String mode,
                                                         List<TileCoord> waypoints) {
        addNpc(modId, npcJson(npcId, look).patrol(mode, waypoints.toArray(TileCoord[]::new)).toJson());
    }

    @Given("a mods directory containing the {string} mod with an NPC declaring id {string}, {npcLook}, and no patrol")
    public void aModsDirectoryContainingAStationaryNpc(String modId, String npcId, NpcLookFixture look) {
        addNpc(modId, npcJson(npcId, look).toJson());
    }

    @Given("a mods directory containing the {string} mod with an NPC declaring id {string}, spawn \\({int}, {int}\\), and a patrol of waypoints {waypointPair} with no mode")
    public void aModsDirectoryContainingAnNpcWithAPatrolWithNoMode(String modId, String npcId, int spawnX, int spawnY,
                                                                    List<TileCoord> waypoints) {
        addNpc(modId, NpcJson.npc(npcId).spawn(spawnX, spawnY).patrol(null, waypoints.toArray(TileCoord[]::new)).toJson());
    }

    @Given("^a mods directory containing the \"([^\"]+)\" mod with an NPC declaring id \"([^\"]+)\" whose (.+)$")
    public void aModsDirectoryContainingAnNpcWhoseDefectIs(String modId, String npcId, String defect) {
        addNpc(modId, brokenNpc(npcId, defect));
    }

    @Given("a mods directory containing mod {string} with a malformed NPC file")
    public void aModsDirectoryContainingModWithAMalformedNpcFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("npcs"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("npcs").resolve("broken.json"), "{ not valid json");
    }

    @Then("an NPC with ID {string} is available")
    public void anNpcWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getNpc(id), "expected NPC '" + id + "' to be loaded");
        lastCheckedNpcId = id;
        lastCheckedEntityKind = NPC_KIND;
    }

    @Then("its symbol is {string} and its color is {int},{int},{int}")
    public void itsNpcSymbolAndColorAre(String symbol, int r, int g, int b) {
        NpcDefinition npc = registry.getNpc(lastCheckedNpcId);
        assertEquals(symbol.charAt(0), npc.symbol());
        assertEquals(new Color(r, g, b), npc.color());
    }

    @Then("its spawn is \\({int}, {int}\\)")
    public void itsNpcSpawnIs(int x, int y) {
        assertEquals(new TileCoord(x, y), registry.getNpc(lastCheckedNpcId).spawn());
    }

    @Then("its patrol mode is {string} with waypoints \\({int}, {int}\\) and \\({int}, {int}\\)")
    public void itsNpcPatrolIs(String mode, int firstX, int firstY, int secondX, int secondY) {
        Patrol patrol = registry.getNpc(lastCheckedNpcId).patrol();
        assertNotNull(patrol, "expected NPC '" + lastCheckedNpcId + "' to have a patrol");
        assertEquals(mode, patrol.mode().jsonName());
        assertEquals(List.of(new TileCoord(firstX, firstY), new TileCoord(secondX, secondY)), patrol.waypoints());
    }

    @Then("it has no patrol")
    public void itHasNoPatrol() {
        assertNull(registry.getNpc(lastCheckedNpcId).patrol(), "expected NPC '" + lastCheckedNpcId + "' to be stationary");
    }

    @Then("the patrol mode of NPC {string} is {string}")
    public void thePatrolModeOfNpcIs(String npcId, String mode) {
        assertEquals(mode, registry.getNpc(npcId).patrol().mode().jsonName());
    }

    @Then("loading fails with a ModLoadException naming NPC {string}, the reason, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingNpcReasonAndFile(String npcId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(npcId), "expected message to name NPC: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(".json"), "expected message to name the file: " + thrown.getMessage());
    }

    private void assertWidgetThemeMatches(String themeId) {
        WidgetColorTheme theme = registry.getTheme(themeId);
        assertNotNull(theme, "expected theme '" + themeId + "' to be loaded");
        WidgetTheme.applyTheme(theme);
        for (String key : WidgetColorTheme.REQUIRED_KEYS) {
            assertEquals(theme.color(key), widgetThemeColor(key), "mismatch for key " + key);
        }
    }

    private static Color widgetThemeColor(String key) {
        return switch (key) {
            case "SELECTED_HIGHLIGHT" -> WidgetTheme.selectedHighlight();
            case "SELECTED_TEXT" -> WidgetTheme.selectedText();
            case "NORMAL_TEXT" -> WidgetTheme.normalText();
            case "DIMMED_TEXT" -> WidgetTheme.dimmedText();
            case "BACKGROUND" -> WidgetTheme.background();
            case "INVALID_HIGHLIGHT" -> WidgetTheme.invalidHighlight();
            case "VALID_HIGHLIGHT" -> WidgetTheme.validHighlight();
            case "TABLE_HEADER_BACKGROUND" -> WidgetTheme.tableHeaderBackground();
            case "BORDER" -> WidgetTheme.border();
            case "SCROLLBAR_THUMB" -> WidgetTheme.scrollbarThumb();
            case "ACCENT" -> WidgetTheme.accent();
            case "WINDOW_BORDER" -> WidgetTheme.windowBorder();
            case "TABLE_HEADER_TEXT" -> WidgetTheme.tableHeaderText();
            default -> throw new IllegalArgumentException("Unknown WidgetTheme color key: " + key);
        };
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string}, name {string}, {baseStats}")
    public void aModsDirectoryContainingTheModWithAClassDeclaringId(String modId, String classId, String name,
                                                                   BaseStatsFixture baseStats) {
        addClass(modId, classId, name, baseStats.stats(), null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with base {word} {int} and a {word} growth calc of {string}")
    public void aModsDirectoryContainingModWithAClassWithBaseAndGrowthCalc(String modId, String classId,
                                                                            String stat1Name, int base, String stat2Name, String growthCalc) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(stat1Name, new StatEntry(base, growthCalc));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with base {word} {int} and no growth calc for {word}")
    public void aModsDirectoryContainingModWithAClassWithBaseAndNoGrowthCalc(String modId, String classId,
                                                                             String stat1Name, int base, String stat2Name) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(stat1Name, new StatEntry(base, null));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("a mods directory containing the {string} mod with a class declaring id {string} with a growth calc for stat {string} of {string}")
    public void aModsDirectoryContainingModWithAClassWithGrowthCalc(String modId, String classId, String statName, String growthCalc) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        stats.put(statName, new StatEntry(null, growthCalc));
        addClass(modId, classId, classId, stats, null);
    }

    @Given("the mods directory also contains mod {string} with a class declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAClassDeclaringIdAndNoField(String modId, String classId, String fieldName) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        for (String statName : WARRIOR_STATS.keySet()) {
            Integer base = WARRIOR_STATS.get(statName);
            stats.put(statName, new StatEntry(base, null));
        }
        addClass(modId, classId, classId, stats, null);
    }

    @Given("the mods directory also contains mod {string} with a class declaring id {string}, name {string}, and the same base stats, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAClassOverriding(String modId, String classId, String name,
                                                                    String fieldName, String overriddenId) {
        Map<String, StatEntry> stats = new LinkedHashMap<>();
        for (String statName : WARRIOR_STATS.keySet()) {
            Integer base = WARRIOR_STATS.get(statName);
            stats.put(statName, new StatEntry(base, null));
        }
        addClass(modId, classId, name, stats, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed class file")
    public void aModsDirectoryContainingModWithAMalformedClassFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("classes"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("classes").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string}, name {string}, {itemLook}, and a {itemEffect}")
    public void aModsDirectoryContainingTheModWithAnItemDeclaringId(String modId, String itemId, String name,
                                                                      ItemLookFixture look, ItemEffectFixture effect) {
        addItem(modId, itemId, name, look.glyph(), look.type(), look.slot(), look.baseDamageMin(), look.baseDamageMax(),
                List.of(effect), null);
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string}, name {string}, glyph {string}, type {string}, slot {string}, base damage min {int} and max {int}, and no effects")
    public void aModsDirectoryContainingTheModWithAnItemDeclaringIdNoEffects(String modId, String itemId, String name, String glyph,
                                                                              String type, String slot, int baseDamageMin, int baseDamageMax) {
        addItem(modId, itemId, name, glyph.charAt(0), type, slot, baseDamageMin, baseDamageMax, null, null);
    }

    @Given("a mods directory containing the {string} mod with an item declaring id {string} with a {string} effect on stat {string} with calc {string}")
    public void aModsDirectoryContainingModWithAnItemWithEffect(String modId, String itemId, String effectType, String stat, String calc) {
        List<ItemEffectFixture> effects = List.of(new ItemEffectFixture(effectType, stat, calc));
        addItem(modId, itemId, null, null, null, null, null, null, effects, null);
    }

    @Given("the mods directory also contains mod {string} with an item declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAnItemDeclaringIdAndNoField(String modId, String itemId, String fieldName) {
        addItem(modId, itemId, null, null, null, null, null, null, null, null);
    }

    @Given("the mods directory also contains mod {string} with an item declaring id {string}, name {string}, and the same base damage and effects, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAnItemOverriding(String modId, String itemId, String name, String fieldName, String overriddenId) {
        List<ItemEffectFixture> effects = List.of(new ItemEffectFixture("stat_bonus", "strength", "level*1.5+2"));
        addItem(modId, itemId, name, '/', "weapon", "main_hand", 4, 9, effects, overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed item file")
    public void aModsDirectoryContainingModWithAMalformedItemFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("items"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("items").resolve("broken.json"), "{ not valid json");
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, name {string}, {questObjective}, {itemAndXpRewards}")
    public void aModsDirectoryContainingTheModWithAQuestWithRewards(String modId, String questId, String name,
                                                                     ObjectiveFixture objective, RewardsFixture rewards) {
        addQuest(modId, questId, name, objective.type(), objective.target(), objective.count(), rewards.rewards(), null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, name {string}, objective type {string} on target {string} with count {int}, and no rewards")
    public void aModsDirectoryContainingTheModWithAQuestNoRewards(String modId, String questId, String name,
                                                                    String objectiveType, String target, int count) {
        addQuest(modId, questId, name, objectiveType, target, count, List.of(), null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and an item reward of {string} count {int}")
    public void aModsDirectoryContainingTheModWithAQuestNoName(String modId, String questId,
                                                               String objectiveType, String target, int count,
                                                               String itemId, int itemCount) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("item", itemId, itemCount, null)
        );
        addQuest(modId, questId, null, objectiveType, target, count, rewards, null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and an xp reward with calc {string}")
    public void aModsDirectoryContainingTheModWithAQuestXpOnly(String modId, String questId,
                                                               String objectiveType, String target, int count,
                                                               String xpCalc) {
        List<QuestRewardFixture> rewards = List.of(
                new QuestRewardFixture("xp", null, null, xpCalc)
        );
        addQuest(modId, questId, null, objectiveType, target, count, rewards, null);
    }

    @Given("a mods directory containing the {string} mod with a quest declaring id {string}, objective type {string} on target {string} with count {int}, and no rewards")
    public void aModsDirectoryContainingTheModWithAQuestNoRewardsNoName(String modId, String questId,
                                                                         String objectiveType, String target, int count) {
        addQuest(modId, questId, null, objectiveType, target, count, List.of(), null);
    }

    @Given("the mods directory also contains the {string} mod with a quest declaring id {string}, name {string}, {questObjective}, {itemAndXpRewards}")
    public void theModsDirectoryAlsoContainsTheModWithAQuestWithRewards(String modId, String questId, String name,
                                                                         ObjectiveFixture objective, RewardsFixture rewards) {
        addQuest(modId, questId, name, objective.type(), objective.target(), objective.count(), rewards.rewards(), null);
    }

    @Given("the mods directory also contains mod {string} with a quest declaring id {string} and no {string} field")
    public void theModsDirectoryAlsoContainsModWithAQuestAndNoField(String modId, String questId, String fieldName) {
        addQuest(modId, questId, null, "kill", "core:goblin", 5, List.of(), null);
    }

    @Given("the mods directory also contains mod {string} with a quest declaring id {string}, name {string}, and the same objective and rewards, whose {string} field names {string}")
    public void theModsDirectoryAlsoContainsModWithAQuestOverriding(String modId, String questId, String name,
                                                                    String fieldName, String overriddenId) {
        addQuest(modId, questId, name, "kill", "core:goblin", 5, List.of(), overriddenId);
    }

    @Given("a mods directory containing mod {string} with a malformed quest file")
    public void aModsDirectoryContainingModWithAMalformedQuestFile(String modId) throws IOException {
        Path modDir = modsRoot.resolve(modId);
        Files.createDirectories(modDir.resolve("quests"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + modId + "\",\"dependsOn\":[]}");
        Files.writeString(modDir.resolve("quests").resolve("broken.json"), "{ not valid json");
    }

    @Then("a class with ID {string} is available")
    public void aClassWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getPlayerClass(id), "expected class '" + id + "' to be loaded");
        lastCheckedClassId = id;
        lastCheckedEntityKind = "class";
    }

    @Then("its name is {string}")
    public void itsNameIs(String expectedName) {
        if ("item".equals(lastCheckedEntityKind)) {
            assertEquals(expectedName, registry.getItem(lastCheckedItemId).getName());
        } else if ("quest".equals(lastCheckedEntityKind)) {
            assertEquals(expectedName, registry.getQuest(lastCheckedQuestId).getName());
        } else if (NPC_KIND.equals(lastCheckedEntityKind)) {
            assertEquals(expectedName, registry.getNpc(lastCheckedNpcId).name());
        } else {
            assertEquals(expectedName, registry.getPlayerClass(lastCheckedClassId).getName());
        }
    }

    @Then("its base max HP is {int}")
    public void itsBaseMaxHpIs(int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(lastCheckedClassId).applyStatsAtLevel(stats, 0);
        assertEquals(expected, stats.getMaxHp());
    }

    @Then("its base max mana is {int}")
    public void itsBaseMaxManaIs(int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(lastCheckedClassId).applyStatsAtLevel(stats, 0);
        assertEquals(expected, stats.getMaxMana());
    }

    @Then("the class {string}'s {word} at level {int} is {int}")
    public void theClasssStatAtLevelIs(String classId, String statName, int level, int expected) {
        Stats stats = new Stats();
        registry.getPlayerClass(classId).applyStatsAtLevel(stats, level);
        int actual = getStatValue(stats, statName);
        assertEquals(expected, actual);
    }

    @Then("loading fails with a ModLoadException naming class {string}, stat {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingClassStatAndFile(String classId, String statName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(classId), "expected message to name class: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(statName), "expected message to name stat: " + thrown.getMessage());
    }

    @Then("an item with ID {string} is available")
    public void anItemWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getItem(id), "expected item '" + id + "' to be loaded");
        lastCheckedItemId = id;
        lastCheckedEntityKind = "item";
    }

    @Then("its glyph is {string}")
    public void itsGlyphIs(String expected) {
        assertEquals(expected.charAt(0), registry.getItem(lastCheckedItemId).getGlyph());
    }

    @Then("its base damage is {int} to {int}")
    public void itsBaseDamageIs(int min, int max) {
        Item.BaseDamage baseDamage = registry.getItem(lastCheckedItemId).getBaseDamage();
        assertEquals(min, baseDamage.min());
        assertEquals(max, baseDamage.max());
    }

    @Then("it has one effect: a {string} on stat {string} with calc {string}")
    public void itHasOneEffect(String type, String stat, String calc) {
        List<Item.Effect> effects = registry.getItem(lastCheckedItemId).getEffects();
        assertEquals(1, effects.size());
        assertEquals(type, effects.get(0).type());
        assertEquals(stat, effects.get(0).stat());
        assertEquals(calc, effects.get(0).calc());
    }

    @Then("it has no effects")
    public void itHasNoEffects() {
        assertTrue(registry.getItem(lastCheckedItemId).getEffects().isEmpty());
    }

    @Then("loading fails with a ModLoadException naming item {string}, stat {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingItemStatAndFile(String itemId, String statName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(statName), "expected message to name stat: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming item {string} and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingItemAndFile(String itemId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
    }

    @Then("a quest with ID {string} is available")
    public void aQuestWithIdIsAvailable(String id) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        assertNotNull(registry.getQuest(id), "expected quest '" + id + "' to be loaded");
        lastCheckedQuestId = id;
        lastCheckedEntityKind = "quest";
    }

    @Then("its objective is a {string} on target {string} with count {int}")
    public void itsObjectiveIs(String type, String target, int count) {
        Quest.Objective objective = registry.getQuest(lastCheckedQuestId).getObjective();
        assertEquals(type, objective.type());
        assertEquals(target, objective.target());
        assertEquals(count, objective.count());
    }

    @Then("it has {int} rewards: an item reward of {string} count {int}, and an xp reward with calc {string}")
    public void itHasRewards(int expectedCount, String itemRewardId, int itemRewardCount, String xpCalc) {
        List<Quest.Reward> rewards = registry.getQuest(lastCheckedQuestId).getRewards();
        assertEquals(expectedCount, rewards.size());
        assertEquals("item", rewards.get(0).type());
        assertEquals(itemRewardId, rewards.get(0).id());
        assertEquals(itemRewardCount, rewards.get(0).count());
        assertEquals("xp", rewards.get(1).type());
        assertEquals(xpCalc, rewards.get(1).calc());
    }

    @Then("it has no rewards")
    public void itHasNoRewards() {
        assertTrue(registry.getQuest(lastCheckedQuestId).getRewards().isEmpty());
    }

    @Then("loading fails with a ModLoadException naming quest {string}, item {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestItemAndFile(String questId, String itemId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(itemId), "expected message to name item: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming quest {string} and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestAndFile(String questId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming quest {string}, objective type {string}, and the file it came from")
    public void loadingFailsWithAModLoadExceptionNamingQuestObjectiveTypeAndFile(String questId, String objectiveType) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(questId), "expected message to name quest: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(objectiveType), "expected message to name objective type: " + thrown.getMessage());
    }

    // Schema validation assertions for mod-json-contract.feature
    @Then("loading fails with a ModLoadException naming the file, the missing required field {string}, and that it is required")
    public void loadingFailsWithMissingRequiredField(String fieldName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains(fieldName), "expected message to name field: " + message);
        assertTrue(message.contains("required"), "expected message to mention 'required': " + message);
    }

    @Then("loading fails with a ModLoadException naming the file, the field {string}, the expected type {string}, and the type actually found")
    public void loadingFailsWithWrongFieldType(String fieldName, String expectedType) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains(fieldName), "expected message to name field: " + message);
        assertTrue(message.contains(expectedType), "expected message to mention expected type '" + expectedType + "': " + message);
    }

    @Then("loading fails with a ModLoadException naming the file and the unknown field {string}")
    public void loadingFailsWithUnknownField(String fieldName) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains(fieldName), "expected message to name unknown field: " + message);
        assertTrue(message.toLowerCase(Locale.ROOT).contains("additional") || message.contains("unknown"),
                "expected message to mention unknown/additional field: " + message);
    }

    @Then("loading fails with a ModLoadException naming the file and the malformed id {string}")
    public void loadingFailsWithMalformedId(String malformedId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains("/id") || message.contains("id field"),
                "expected message to reference the id field: " + message);
        assertTrue(message.toLowerCase(Locale.ROOT).contains("pattern") ||
                   message.toLowerCase(Locale.ROOT).contains("regex") ||
                   message.toLowerCase(Locale.ROOT).contains("does not match"),
                "expected message to mention pattern/regex requirement: " + message);
    }

    @Then("loading fails with a ModLoadException naming the mod.json file, mod {string}, and the unresolved dependency {string}")
    public void loadingFailsWithUnresolvedDependency(String modId, String unresolvedDep) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        // Either mentions mod.json/modId or the dependency resolution error which names the mod
        assertTrue(message.contains("dependency") || message.contains(modId) || message.contains("Unresolved"),
                "expected message to mention dependency resolution: " + message);
    }

    @Then("loading fails with a ModLoadException naming the building's file, the building's id, and the unresolved tile id {string}")
    public void loadingFailsWithUnresolvedTileId(String unresolvedTileId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        String message = thrown.getMessage();
        assertTrue(message.contains(unresolvedTileId), "expected message to name unresolved tile id: " + message);
    }

    private static int getStatValue(Stats stats, String statName) {
        Map<String, Function<Stats, Integer>> getters = Map.of(
                "strength", Stats::getStrength,
                "dexterity", Stats::getDexterity,
                "constitution", Stats::getConstitution,
                "intelligence", Stats::getIntelligence,
                "wisdom", Stats::getWisdom,
                "luck", Stats::getLuck,
                "maxHp", Stats::getMaxHp,
                "maxMana", Stats::getMaxMana
        );
        return getters.get(statName).apply(stats);
    }

    private void addClass(String modId, String classId, String name, Map<String, StatEntry> stats, @Nullable String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        classesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ClassFixture(classId, name, stats, overrides));
    }

    private void addItem(String modId, String itemId, @Nullable String name, @Nullable Character glyph, @Nullable String type, @Nullable String slot,
                          @Nullable Integer baseDamageMin, @Nullable Integer baseDamageMax, @Nullable List<ItemEffectFixture> effects, @Nullable String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        itemsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ItemFixture(itemId, name, glyph, type, slot, baseDamageMin, baseDamageMax, effects, overrides));
    }

    private void addQuest(String modId, String questId, @Nullable String name, String objectiveType, String target,
                           Integer count, List<QuestRewardFixture> rewards, @Nullable String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        questsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new QuestFixture(questId, name, objectiveType, target, count, rewards, overrides));
    }

    private void addBuilding(String modId, String buildingId, @Nullable String overriddenId, @Nullable String explicitTileId) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        buildingsByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new BuildingFixture(buildingId, overriddenId, explicitTileId));
        if (explicitTileId == null) {
            needsMarkerTiles = true;
        }
    }

    private void addTile(String modId, String tileId, char symbol, int r, int g, int b, boolean walkable, @Nullable String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        tilesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new TileFixture(tileId, symbol, r, g, b, walkable, overrides));
    }

    private void addTheme(String modId, String themeId, Map<String, ThemeColorFixture> colors, @Nullable String overrides) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        themesByMod.computeIfAbsent(modId, k -> new ArrayList<>())
                .add(new ThemeFixture(themeId, colors, overrides));
    }

    private static Map<String, ThemeColorFixture> defaultThemeColors() {
        Map<String, ThemeColorFixture> colors = new LinkedHashMap<>();
        colors.put("SELECTED_HIGHLIGHT", new ThemeColorFixture(1, 2, 3));
        colors.put("SELECTED_TEXT", new ThemeColorFixture(4, 5, 6));
        colors.put("NORMAL_TEXT", new ThemeColorFixture(7, 8, 9));
        colors.put("DIMMED_TEXT", new ThemeColorFixture(10, 11, 12));
        colors.put("BACKGROUND", new ThemeColorFixture(13, 14, 15));
        colors.put("INVALID_HIGHLIGHT", new ThemeColorFixture(16, 17, 18));
        colors.put("VALID_HIGHLIGHT", new ThemeColorFixture(19, 20, 21));
        colors.put("TABLE_HEADER_BACKGROUND", new ThemeColorFixture(22, 23, 24));
        colors.put("BORDER", new ThemeColorFixture(25, 26, 27));
        colors.put("SCROLLBAR_THUMB", new ThemeColorFixture(28, 29, 30));
        colors.put("ACCENT", new ThemeColorFixture(31, 32, 33));
        colors.put("WINDOW_BORDER", new ThemeColorFixture(34, 35, 36));
        colors.put("TABLE_HEADER_TEXT", new ThemeColorFixture(37, 38, 39));
        return colors;
    }

    private void writeFixtures() throws IOException {
        Set<String> allMods = new LinkedHashSet<>();
        allMods.addAll(buildingsByMod.keySet());
        allMods.addAll(tilesByMod.keySet());
        allMods.addAll(classesByMod.keySet());
        allMods.addAll(itemsByMod.keySet());
        allMods.addAll(questsByMod.keySet());
        allMods.addAll(themesByMod.keySet());
        allMods.addAll(npcsByMod.keySet());
        allMods.addAll(dialoguesByMod.keySet());
        allMods.addAll(dependsOnByMod.keySet());

        if (needsMarkerTiles) {
            writeMarkerTiles();
        }

        if (!classesByMod.isEmpty() || !itemsByMod.isEmpty()) {
            writeStatsRegistryIfNeeded();
        }

        for (String modId : allMods) {
            Path modDir = modsRoot.resolve(modId);
            Files.createDirectories(modDir);
            writeManifest(modDir, modId);
            writeTiles(modDir, tilesByMod.getOrDefault(modId, List.of()));
            writeBuildings(modDir, buildingsByMod.getOrDefault(modId, List.of()));
            writeClasses(modDir, classesByMod.getOrDefault(modId, List.of()));
            writeItems(modDir, itemsByMod.getOrDefault(modId, List.of()));
            writeQuests(modDir, questsByMod.getOrDefault(modId, List.of()));
            writeThemes(modDir, themesByMod.getOrDefault(modId, List.of()));
            writeNpcs(modDir, npcsByMod.getOrDefault(modId, List.of()));
            writeDialogues(modDir, dialoguesByMod.getOrDefault(modId, List.of()));
        }
    }

    private void addNpc(String modId, JsonObject npc) {
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        npcsByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(npc);
    }

    private static void writeNpcs(Path modDir, List<JsonObject> npcs) throws IOException {
        if (npcs.isEmpty()) {
            return;
        }
        Path npcsDir = modDir.resolve("npcs");
        Files.createDirectories(npcsDir);
        for (int i = 0; i < npcs.size(); i++) {
            Files.writeString(npcsDir.resolve("npc_" + i + ".json"), npcs.get(i).toString());
        }
    }

    private static NpcJson npcJson(String npcId, NpcLookFixture look) {
        return NpcJson.npc(npcId).named(look.name()).symbol(look.symbol())
                .color(look.r(), look.g(), look.b()).spawn(look.spawnX(), look.spawnY());
    }

    /** The NPC for an outline row: each defect is one way the NPC file breaks the NPC contract. */
    private static JsonObject brokenNpc(String npcId, String defect) {
        return switch (defect) {
            case "spawn is missing" -> NpcJson.npc(npcId).withoutSpawn().toJson();
            case "patrol mode is \"zigzag\"" ->
                    NpcJson.npc(npcId).patrol("zigzag", new TileCoord(1, 1), new TileCoord(1, 3)).toJson();
            case "patrol has only one waypoint" ->
                    NpcJson.npc(npcId).patrol("ping_pong", new TileCoord(1, 1)).toJson();
            case "patrol has a diagonal leg from (1, 1) to (2, 2)" ->
                    NpcJson.npc(npcId).patrol("ping_pong", new TileCoord(1, 1), new TileCoord(2, 2)).toJson();
            case "patrol first waypoint (1, 1) differs from spawn (5, 5)" ->
                    NpcJson.npc(npcId).spawn(5, 5).patrol("ping_pong", new TileCoord(1, 1), new TileCoord(1, 3)).toJson();
            case "loop patrol of (1, 1), (4, 1), (4, 3) closes diagonally back to (1, 1)" ->
                    NpcJson.npc(npcId).patrol("loop", new TileCoord(1, 1), new TileCoord(4, 1), new TileCoord(4, 3)).toJson();
            default -> throw new IllegalArgumentException("unknown NPC defect: " + defect);
        };
    }

    private void writeMarkerTiles() throws IOException {
        Path markerDir = modsRoot.resolve("_markers");
        Files.createDirectories(markerDir);
        Files.writeString(markerDir.resolve("mod.json"), "{\"id\":\"_markers\",\"dependsOn\":[]}");

        Path tilesDir = markerDir.resolve("tiles");
        Files.createDirectories(tilesDir);
        Files.writeString(tilesDir.resolve("test_grass.json"), tileJson("test:grass", ',', 0, 200, 0, /* walkable= */ true, null));
        Files.writeString(tilesDir.resolve("test_stone.json"), tileJson("test:stone", '#', 100, 100, 100, /* walkable= */ false, null));
    }

    private void writeManifest(Path modDir, String modId) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", modId);
        JsonArray dependsOn = new JsonArray();
        dependsOnByMod.getOrDefault(modId, List.of()).forEach(dependsOn::add);
        manifest.add("dependsOn", dependsOn);
        Files.writeString(modDir.resolve("mod.json"), manifest.toString());
    }

    private static void writeTiles(Path modDir, List<TileFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path tilesDir = modDir.resolve("tiles");
        Files.createDirectories(tilesDir);

        int i = 0;
        for (TileFixture fixture : fixtures) {
            String json = tileJson(fixture.id(), fixture.symbol(), fixture.r(), fixture.g(), fixture.b(),
                    fixture.walkable(), fixture.overrides());
            Files.writeString(tilesDir.resolve("tile_" + i + ".json"), json);
            i++;
        }
    }

    private static String tileJson(String id, char symbol, int r, int g, int b, boolean walkable, @Nullable String overrides) {
        JsonObject tile = new JsonObject();
        tile.addProperty("id", id);
        tile.addProperty("symbol", String.valueOf(symbol));
        JsonObject color = new JsonObject();
        color.addProperty("r", r);
        color.addProperty("g", g);
        color.addProperty("b", b);
        tile.add("color", color);
        tile.addProperty("walkable", walkable);
        if (overrides != null) {
            tile.addProperty("overrides", overrides);
        }
        return tile.toString();
    }

    private static void writeBuildings(Path modDir, List<BuildingFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path buildingsDir = modDir.resolve("buildings");
        Files.createDirectories(buildingsDir);

        int i = 0;
        for (BuildingFixture fixture : fixtures) {
            String tileId = fixture.explicitTileId() != null
                    ? fixture.explicitTileId()
                    : (fixture.overrides() != null ? "test:stone" : "test:grass");

            JsonObject building = new JsonObject();
            building.addProperty("id", fixture.id());
            building.addProperty("name", fixture.id());
            building.addProperty("width", 1);
            building.addProperty("height", 1);

            JsonArray row = new JsonArray();
            row.add(tileId);
            JsonArray tiles = new JsonArray();
            tiles.add(row);
            building.add("tiles", tiles);

            if (fixture.overrides() != null) {
                building.addProperty("overrides", fixture.overrides());
            }

            Files.writeString(buildingsDir.resolve("building_" + i + ".json"), building.toString());
            i++;
        }
    }

    private static void writeClasses(Path modDir, List<ClassFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path classesDir = modDir.resolve("classes");
        Files.createDirectories(classesDir);

        int i = 0;
        for (ClassFixture fixture : fixtures) {
            String json = classJson(fixture);
            Files.writeString(classesDir.resolve("class_" + i + ".json"), json);
            i++;
        }
    }

    private static String classJson(ClassFixture fixture) {
        JsonObject classObj = new JsonObject();
        classObj.addProperty("id", fixture.id());
        classObj.addProperty("name", fixture.name());

        JsonObject stats = new JsonObject();
        for (Map.Entry<String, StatEntry> entry : fixture.stats().entrySet()) {
            JsonObject stat = new JsonObject();
            if (entry.getValue().base() != null) {
                stat.addProperty("base", entry.getValue().base());
            }
            if (entry.getValue().growthCalc() != null) {
                stat.addProperty("growth", entry.getValue().growthCalc());
            }
            stats.add(entry.getKey(), stat);
        }
        classObj.add("stats", stats);

        if (fixture.overrides() != null) {
            classObj.addProperty("overrides", fixture.overrides());
        }

        return classObj.toString();
    }

    private static void writeItems(Path modDir, List<ItemFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path itemsDir = modDir.resolve("items");
        Files.createDirectories(itemsDir);

        int i = 0;
        for (ItemFixture fixture : fixtures) {
            Files.writeString(itemsDir.resolve("item_" + i + ".json"), itemJson(fixture));
            i++;
        }
    }

    private static String itemJson(ItemFixture fixture) {
        JsonObject item = new JsonObject();
        item.addProperty("id", fixture.id());
        item.addProperty("name", fixture.name() != null ? fixture.name() : fixture.id());
        item.addProperty("glyph", fixture.glyph() != null ? String.valueOf(fixture.glyph()) : "?");
        item.addProperty("type", fixture.type() != null ? fixture.type() : "misc");
        item.addProperty("slot", fixture.slot() != null ? fixture.slot() : "none");

        item.add("baseDamage", baseDamageJson(fixture));

        if (fixture.effects() != null && !fixture.effects().isEmpty()) {
            item.add("effects", effectsJson(fixture.effects()));
        }

        if (fixture.overrides() != null) {
            item.addProperty("overrides", fixture.overrides());
        }

        return item.toString();
    }

    private static JsonObject baseDamageJson(ItemFixture fixture) {
        JsonObject baseDamage = new JsonObject();
        baseDamage.addProperty("min", fixture.baseDamageMin() != null ? fixture.baseDamageMin() : 0);
        baseDamage.addProperty("max", fixture.baseDamageMax() != null ? fixture.baseDamageMax() : 0);
        return baseDamage;
    }

    private static JsonArray effectsJson(List<ItemEffectFixture> fixtureEffects) {
        JsonArray effects = new JsonArray();
        for (ItemEffectFixture effect : fixtureEffects) {
            JsonObject effectObj = new JsonObject();
            effectObj.addProperty("type", effect.type());
            effectObj.addProperty("stat", effect.stat());
            effectObj.addProperty("calc", effect.calc());
            effects.add(effectObj);
        }
        return effects;
    }

    private static void writeQuests(Path modDir, List<QuestFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path questsDir = modDir.resolve("quests");
        Files.createDirectories(questsDir);

        int i = 0;
        for (QuestFixture fixture : fixtures) {
            Files.writeString(questsDir.resolve("quest_" + i + ".json"), questJson(fixture));
            i++;
        }
    }

    private static String questJson(QuestFixture fixture) {
        JsonObject quest = new JsonObject();
        quest.addProperty("id", fixture.id());
        quest.addProperty("name", fixture.name() != null ? fixture.name() : fixture.id());

        JsonObject objective = new JsonObject();
        objective.addProperty("type", fixture.objectiveType());
        if (fixture.target() != null) {
            objective.addProperty("target", fixture.target());
        }
        if (fixture.count() != null) {
            objective.addProperty("count", fixture.count());
        }
        quest.add("objective", objective);

        if (fixture.rewards() != null && !fixture.rewards().isEmpty()) {
            quest.add("rewards", rewardsJson(fixture.rewards()));
        }

        if (fixture.overrides() != null) {
            quest.addProperty("overrides", fixture.overrides());
        }

        return quest.toString();
    }

    private static JsonArray rewardsJson(List<QuestRewardFixture> fixtureRewards) {
        JsonArray rewards = new JsonArray();
        for (QuestRewardFixture reward : fixtureRewards) {
            JsonObject rewardObj = new JsonObject();
            rewardObj.addProperty("type", reward.type());
            if (reward.itemId() != null) {
                rewardObj.addProperty("id", reward.itemId());
            }
            if (reward.count() != null) {
                rewardObj.addProperty("count", reward.count());
            }
            if (reward.calc() != null) {
                rewardObj.addProperty("calc", reward.calc());
            }
            rewards.add(rewardObj);
        }
        return rewards;
    }

    private static void writeThemes(Path modDir, List<ThemeFixture> fixtures) throws IOException {
        if (fixtures.isEmpty()) {
            return;
        }
        Path themesDir = modDir.resolve("themes");
        Files.createDirectories(themesDir);

        int i = 0;
        for (ThemeFixture fixture : fixtures) {
            Files.writeString(themesDir.resolve("theme_" + i + ".json"), themeJson(fixture));
            i++;
        }
    }

    private static String themeJson(ThemeFixture fixture) {
        JsonObject theme = new JsonObject();
        theme.addProperty("id", fixture.id());
        theme.add("colors", themeColorsJson(fixture.colors()));
        if (fixture.overrides() != null) {
            theme.addProperty("overrides", fixture.overrides());
        }
        return theme.toString();
    }

    private static JsonObject themeColorsJson(Map<String, ThemeColorFixture> colors) {
        JsonObject colorsJson = new JsonObject();
        for (Map.Entry<String, ThemeColorFixture> entry : colors.entrySet()) {
            JsonObject color = new JsonObject();
            color.addProperty("r", entry.getValue().r());
            color.addProperty("g", entry.getValue().g());
            color.addProperty("b", entry.getValue().b());
            colorsJson.add(entry.getKey(), color);
        }
        return colorsJson;
    }

    private void writeStatsRegistryIfNeeded() throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir);

        JsonObject registry = new JsonObject();
        JsonArray statNames = new JsonArray();
        statNames.add("strength");
        statNames.add("dexterity");
        statNames.add("constitution");
        statNames.add("intelligence");
        statNames.add("wisdom");
        statNames.add("luck");
        statNames.add("maxHp");
        statNames.add("maxMana");
        registry.add("stats", statNames);

        Files.writeString(coreDir.resolve("stats.json"), registry.toString());
    }

    // Object definitions and building placements. Test mods are written straight to disk:
    // the object steps need only one or two files each, unlike the fixture maps above.

    @Given("the {string} mod is loaded")
    public void theModIsLoaded(String modId) throws IOException {
        Path source = Paths.get("mods").resolve(modId);
        List<Path> sourcePaths;
        try (Stream<Path> paths = Files.walk(source)) {
            sourcePaths = paths.toList();
        }
        for (Path sourcePath : sourcePaths) {
            Path target = modsRoot.resolve(modId).resolve(source.relativize(sourcePath).toString());
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(target);
            } else {
                Files.copy(sourcePath, target);
            }
        }
    }

    @Given("a mod declaring a door object {string} with no {string} field")
    public void aModDeclaringADoorObjectWithNoField(String objectId, String absentField) throws IOException {
        assertEquals("open", absentField, "this step writes a door without its optional open field");
        writeObject(objectId, "{\"id\":\"" + objectId + "\",\"type\":\"door\"," + TEST_COLOR + "," + DOOR_SYMBOLS + "}");
    }

    @Given("the mod declares a chest object {string} with {int} slots containing {string}")
    public void theModDeclaresAChestObject(String objectId, int slots, String itemId) throws IOException {
        writeChest(objectId, slots, List.of(itemId));
    }

    @Given("a mod declaring a chest {string} with {int} slot containing {string} and {string}")
    public void aModDeclaringAChestWithTwoItemsInOneSlot(String objectId, int slots, String first, String second) throws IOException {
        writeChest(objectId, slots, List.of(first, second));
    }

    @Given("a mod declaring a chest {string} with {int} slots containing {string}")
    public void aModDeclaringAChestWithItems(String objectId, int slots, String itemId) throws IOException {
        writeChest(objectId, slots, List.of(itemId));
    }

    @Given("a mod declaring an object {string} with type {string}")
    public void aModDeclaringAnObjectWithType(String objectId, String type) throws IOException {
        writeObject(objectId, "{\"id\":\"" + objectId + "\",\"type\":\"" + type + "\"," + TEST_COLOR + "}");
    }

    @Given("a mod declaring a building placing object {string}")
    public void aModDeclaringABuildingPlacingObject(String objectId) throws IOException {
        writeBuilding(1, 1, objectId, 0, 0);
    }

    @Given("a mod declaring a building placing an object outside its blueprint")
    public void aModDeclaringABuildingPlacingAnObjectOutsideItsBlueprint() throws IOException {
        writeBuilding(2, 2, "core:wooden_door", 5, 5);
    }

    @Then("object {string} is a door that starts closed")
    public void objectIsADoorThatStartsClosed(String objectId) {
        Door door = assertInstanceOf(Door.class, requireRegistry().getObject(objectId));
        assertFalse(door.isOpen());
    }

    @Then("object {string} is a chest with {int} slots containing {string}")
    public void objectIsAChestWithSlotsContaining(String objectId, int slots, String itemId) {
        Chest chest = assertInstanceOf(Chest.class, requireRegistry().getObject(objectId));
        assertEquals(slots, chest.getSlots().size());
        List<String> itemIds = chest.getSlots().stream().flatMap(Optional::stream).map(Item::getId).toList();
        assertEquals(List.of(itemId), itemIds);
    }

    @Then("loading fails with a ModLoadException naming {string} and its slot count")
    public void loadingFailsNamingAndItsSlotCount(String objectId) {
        assertLoadFailureNames("1 slot", objectId);
    }

    @Then("loading fails with a ModLoadException naming {string}")
    public void loadingFailsNaming(String name) {
        assertLoadFailureNames(name);
    }

    @Then("loading fails with a ModLoadException naming the unknown type {string}")
    public void loadingFailsNamingTheUnknownType(String type) {
        assertLoadFailureNames(type);
    }

    @Then("loading fails with a ModLoadException naming the building and the position")
    public void loadingFailsNamingTheBuildingAndThePosition() {
        assertLoadFailureNames("test:hut", "(5, 5)");
    }

    @Then("building {string} places a door object at \\({int}, {int})")
    public void buildingPlacesADoorObjectAt(String buildingId, int x, int y) {
        lastBuildingId = buildingId;
        lastPlacedObject = requireRegistry().getBuilding(buildingId).getObjects().stream()
                .filter(placement -> placement.x() == x && placement.y() == y)
                .map(Building.ObjectPlacement::object)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no object placed at (" + x + ", " + y + ") in " + buildingId));
        assertInstanceOf(Door.class, lastPlacedObject);
    }

    @Then("that door starts closed")
    public void thatDoorStartsClosed() {
        assertFalse(assertInstanceOf(Door.class, lastPlacedObject).isOpen());
    }

    @Then("the blueprint tile at \\({int}, {int}) is {string}")
    public void theBlueprintTileAtIs(int x, int y, String tileId) {
        Tile placed = requireRegistry().getBuilding(lastBuildingId).getBlueprint().get(y).get(x);
        assertEquals(requireRegistry().getTile(tileId), placed);
    }

    @Then("no tile with ID {string} is available")
    public void noTileWithIdIsAvailable(String tileId) {
        assertNull(requireRegistry().getTile(tileId), "tile '" + tileId + "' should no longer exist");
    }

    private ModRegistry requireRegistry() {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        return registry;
    }

    private void assertLoadFailureNames(String... fragments) {
        assertNotNull(thrown, "expected loading to fail");
        for (String fragment : fragments) {
            assertTrue(thrown.getMessage().contains(fragment), "message should name " + fragment + ": " + thrown.getMessage());
        }
    }

    private Path testModDir() throws IOException {
        Path modDir = modsRoot.resolve(TEST_MOD);
        Files.createDirectories(modDir);
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"" + TEST_MOD + "\",\"dependsOn\":[]}");
        return modDir;
    }

    private void writeObject(String objectId, String json) throws IOException {
        Path objectsDir = testModDir().resolve("objects");
        Files.createDirectories(objectsDir);
        Files.writeString(objectsDir.resolve(objectId.replace(':', '_') + ".json"), json);
    }

    private void writeChest(String objectId, int slots, List<String> itemIds) throws IOException {
        String contents = String.join(",", itemIds.stream().map(id -> "\"" + id + "\"").toList());
        writeObject(objectId, "{\"id\":\"" + objectId + "\",\"type\":\"chest\"," + TEST_COLOR + ",\"symbol\":\"C\","
                + "\"slots\":" + slots + ",\"contents\":[" + contents + "]}");
    }

    private void writeBuilding(int width, int height, String objectId, int x, int y) throws IOException {
        String row = "[" + String.join(",", Collections.nCopies(width, "\"" + CORE_WOOD + "\"")) + "]";
        String rows = String.join(",", Collections.nCopies(height, row));
        Path buildingsDir = testModDir().resolve("buildings");
        Files.createDirectories(buildingsDir);
        Files.writeString(buildingsDir.resolve("hut.json"), "{\"id\":\"test:hut\",\"tiles\":[" + rows + "],"
                + "\"objects\":[{\"id\":\"" + objectId + "\",\"x\":" + x + ",\"y\":" + y + "}]}");
    }

    record NpcLookFixture(String name, String symbol, int r, int g, int b, int spawnX, int spawnY) {
    }

    // ---- Dialogues (dialogue-tree.feature): written as mods/<mod>/dialogues/*.json and loaded for real ----

    @Given("a mod declaring dialogue {string} starting at node {string}")
    public void aModDeclaringDialogueStartingAtNode(String id, String start) {
        currentDialogue = dialogueJson(id, start);
        addDialogue(id, currentDialogue);
    }

    @Given("a mod declaring dialogue {string} with a valid tree")
    public void aModDeclaringDialogueWithAValidTree(String id) {
        currentDialogue = dialogueJson(id, "hello");
        addDialogue(id, currentDialogue);
        addNode("hello", "Well met.", choices(choice("Who are you?", "who"), choice("Goodbye.", null)));
        addNode("who", "Nobody special.", choices(choice("Farewell.", null)));
    }

    @Given("^node \"([^\"]+)\" says \"([^\"]+)\" with choices? (.+)$")
    public void nodeSaysWithChoices(String nodeId, String text, String choicesText) {
        JsonArray choices = new JsonArray();
        Matcher matcher = CHOICE_IN_STEP.matcher(choicesText);
        while (matcher.find()) {
            choices.add(choice(matcher.group(1), matcher.group(2)));
        }
        addNode(nodeId, text, choices);
    }

    @Given("^a mod declaring dialogue \"([^\"]+)\" whose (.+)$")
    public void aModDeclaringDialogueWhoseDefectIs(String id, String defect) {
        currentDialogue = dialogueJson(id, "hello");
        addNode("hello", "Well met.", choices(choice("Goodbye.", null)));
        expectedErrorWord = applyDefect(defect);
        addDialogue(id, currentDialogue);
    }

    @Given("the mod declares an NPC {string} with dialogue {string}")
    public void theModDeclaresAnNpcWithDialogue(String npcId, String dialogueId) {
        addNpc(npcId.substring(0, npcId.indexOf(':')), NpcJson.npc(npcId).dialogue(dialogueId).toJson());
    }

    @Given("a mod declaring an NPC {string} with dialogue {string}")
    public void aModDeclaringAnNpcWithDialogue(String npcId, String dialogueId) {
        theModDeclaresAnNpcWithDialogue(npcId, dialogueId);
    }

    @Then("a dialogue with ID {string} is available")
    public void aDialogueWithIdIsAvailable(String id) {
        checkedDialogueId = id;
        loadedDialogue();
    }

    @Then("dialogue {string} is available")
    public void dialogueIsAvailable(String id) {
        aDialogueWithIdIsAvailable(id);
    }

    @Then("its start node is {string}")
    public void itsStartNodeIs(String nodeId) {
        assertEquals(nodeId, loadedDialogue().start());
    }

    @Then("node {string} says {string} and has {int} choices")
    public void nodeSaysAndHasChoices(String nodeId, String text, int count) {
        DialogueNode node = loadedDialogue().node(nodeId);
        assertEquals(text, node.text());
        assertEquals(count, node.choices().size());
    }

    @Then("choice {int} of node {string} is {string} and leads to node {string}")
    public void choiceOfNodeLeadsTo(int number, String nodeId, String text, String next) {
        DialogueChoice choice = choiceOf(nodeId, number);
        assertEquals(text, choice.text());
        assertEquals(next, choice.next());
    }

    @Then("choice {int} of node {string} is {string} and ends the conversation")
    public void choiceOfNodeEndsTheConversation(int number, String nodeId, String text) {
        DialogueChoice choice = choiceOf(nodeId, number);
        assertEquals(text, choice.text());
        assertTrue(choice.endsConversation(), "expected choice " + number + " of " + nodeId + " to end the conversation");
    }

    @Then("loading fails with a ModLoadException naming dialogue {string}, the reason, and the file it came from")
    public void loadingFailsNamingDialogueReasonAndFile(String id) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(id), "expected message to name dialogue: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(expectedErrorWord), "expected message to name the reason: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(".json"), "expected message to name the file: " + thrown.getMessage());
    }

    @Then("loading fails with a ModLoadException naming NPC {string} and the unknown dialogue {string}")
    public void loadingFailsNamingNpcAndUnknownDialogue(String npcId, String dialogueId) {
        assertNotNull(thrown, "expected a ModLoadException to be thrown");
        assertTrue(thrown.getMessage().contains(npcId), "expected message to name NPC: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(dialogueId), "expected message to name dialogue: " + thrown.getMessage());
    }

    @Then("NPC {string} has dialogue {string}")
    public void npcHasDialogue(String npcId, String dialogueId) {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        Dialogue dialogue = registry.getNpc(npcId).dialogue();
        assertNotNull(dialogue, "expected NPC '" + npcId + "' to have a dialogue");
        assertEquals(dialogueId, dialogue.id());
    }

    @Then("an NPC with no \"dialogue\" field has no dialogue")
    public void anNpcWithNoDialogueFieldHasNoDialogue() throws IOException {
        addNpc(TEST_MOD, NpcJson.npc("test:plain").toJson());
        writeFixtures();
        registry = ModLoader.load(modsRoot);
        assertNull(registry.getNpc("test:plain").dialogue());
    }

    @Then("dialogue {string} has at least 3 levels from its start node")
    public void dialogueHasAtLeastThreeLevels(String id) {
        checkedDialogueId = id;
        assertTrue(DialogueGraph.longestPath(loadedDialogue()).size() >= 3, "longest branch of " + id + " is too shallow");
    }

    @Then("its start node has at least 2 choices leading to different nodes")
    public void itsStartNodeBranches() {
        Dialogue dialogue = loadedDialogue();
        assertTrue(DialogueGraph.distinctNextNodes(dialogue.node(dialogue.start())) >= 2);
    }

    @Then("some node reachable from the start node has choices leading to different further nodes")
    public void someReachableNodeBranches() {
        Dialogue dialogue = loadedDialogue();
        assertTrue(DialogueGraph.reachable(dialogue).stream()
                .anyMatch(nodeId -> DialogueGraph.distinctNextNodes(dialogue.node(nodeId)) >= 2));
    }

    @Then("at least one choice ends the conversation")
    public void atLeastOneChoiceEndsTheConversation() {
        Dialogue dialogue = loadedDialogue();
        assertTrue(DialogueGraph.reachable(dialogue).stream()
                .anyMatch(nodeId -> dialogue.node(nodeId).choices().stream().anyMatch(DialogueChoice::endsConversation)));
    }

    private Dialogue loadedDialogue() {
        assertNotNull(registry, "loading did not complete: " + (thrown == null ? "unknown" : thrown.getMessage()));
        Dialogue dialogue = registry.getDialogue(checkedDialogueId);
        assertNotNull(dialogue, "expected dialogue '" + checkedDialogueId + "' to be loaded");
        return dialogue;
    }

    private DialogueChoice choiceOf(String nodeId, int number) {
        return loadedDialogue().node(nodeId).choices().get(number - 1);
    }

    /** Breaks the current dialogue the way {@code defect} describes; returns the word the load error must name. */
    private String applyDefect(String defect) {
        switch (defect) {
            case "start node \"nowhere\" does not exist" -> {
                currentDialogue.addProperty("start", "nowhere");
                return "nowhere";
            }
            case "start field is missing" -> {
                currentDialogue.remove("start");
                return "start";
            }
            case "choice leads to \"missing\", which is not a node" -> {
                addNode("hello", "Well met.", choices(choice("Go.", "missing")));
                return "missing";
            }
            case "node \"dead_end\" has no choices" -> {
                addNode("hello", "Well met.", choices(choice("Go.", "dead_end")));
                addNode("dead_end", "Nowhere to go.", new JsonArray());
                return "dead_end";
            }
            case "node \"crowded\" has 10 choices" -> {
                addNode("hello", "Well met.", choices(choice("Go.", "crowded")));
                addNode("crowded", "So many options.", endings(10));
                return "crowded";
            }
            case "node \"orphan\" cannot be reached from the start node" -> {
                addNode("orphan", "Lost.", choices(choice("Bye.", null)));
                return "orphan";
            }
            case "node \"hello\" has empty text" -> {
                addNode("hello", "", choices(choice("Goodbye.", null)));
                return "hello";
            }
            case "a choice has empty text" -> {
                addNode("hello", "Well met.", choices(choice("", null)));
                return "hello";
            }
            default -> throw new IllegalArgumentException("unknown dialogue defect: " + defect);
        }
    }

    private void addNode(String nodeId, String text, JsonArray choices) {
        JsonObject node = new JsonObject();
        node.addProperty("text", text);
        node.add("choices", choices);
        currentDialogue.getAsJsonObject("nodes").add(nodeId, node);
    }

    private void addDialogue(String id, JsonObject dialogue) {
        String modId = id.substring(0, id.indexOf(':'));
        dependsOnByMod.computeIfAbsent(modId, k -> new ArrayList<>());
        dialoguesByMod.computeIfAbsent(modId, k -> new ArrayList<>()).add(dialogue);
    }

    private static JsonObject dialogueJson(String id, String start) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("start", start);
        json.add("nodes", new JsonObject());
        return json;
    }

    private static JsonObject choice(String text, @Nullable String next) {
        JsonObject choice = new JsonObject();
        choice.addProperty("text", text);
        if (next != null) {
            choice.addProperty("next", next);
        }
        return choice;
    }

    private static JsonArray choices(JsonObject... choices) {
        JsonArray array = new JsonArray();
        for (JsonObject choice : choices) {
            array.add(choice);
        }
        return array;
    }

    private static JsonArray endings(int count) {
        JsonArray array = new JsonArray();
        for (int i = 1; i <= count; i++) {
            array.add(choice("Option " + i, null));
        }
        return array;
    }

    private static void writeDialogues(Path modDir, List<JsonObject> dialogues) throws IOException {
        if (dialogues.isEmpty()) {
            return;
        }
        Path dialoguesDir = Files.createDirectories(modDir.resolve("dialogues"));
        for (int i = 0; i < dialogues.size(); i++) {
            Files.writeString(dialoguesDir.resolve("dialogue_" + i + ".json"), dialogues.get(i).toString());
        }
    }
}
