package com.swiftfaze.veil.steps;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.player.Stats;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.sandbox.ClassSandboxModel;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClassStatsSandboxSteps {

    private Path modsRoot;
    private ClassSandboxModel model;
    private Stats selectedStats;

    @Before
    public void createModsRoot() throws IOException {
        modsRoot = Files.createTempDirectory("veil-sandbox-test");
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

    @Given("the class sandbox is running")
    public void theClassSandboxIsRunning() {
        model = new ClassSandboxModel();
    }

    @When("{string} is selected")
    public void isSelected(String className) {
        selectedStats = model.computedStats(className);
    }

    @Then("the class list includes {string} and {string}")
    public void theClassListIncludesAnd(String first, String second) {
        List<String> names = model.classNames();
        assertTrue(names.contains(first));
        assertTrue(names.contains(second));
    }

    @Then("the displayed attack power is {int}")
    public void theDisplayedAttackPowerIs(int expected) {
        assertEquals(expected, selectedStats.getAttackPower());
    }

    @Then("the displayed defense is {int}")
    public void theDisplayedDefenseIs(int expected) {
        assertEquals(expected, selectedStats.getDefense());
    }

    @Then("the displayed max HP is {int}")
    public void theDisplayedMaxHpIs(int expected) {
        assertEquals(expected, selectedStats.getMaxHp());
    }

    @Then("the displayed max mana is {int}")
    public void theDisplayedMaxManaIs(int expected) {
        assertEquals(expected, selectedStats.getMaxMana());
    }

    @Given("the {string} class JSON has been edited to set max HP to {int}")
    public void theClassJsonHasBeenEditedToSetMaxHpTo(String className, int newMaxHp) throws IOException {
        writeFixtures(newMaxHp);
    }

    @When("the class sandbox is started fresh and {string} is selected")
    public void theClassSandboxIsStartedFreshAndIsSelected(String className) {
        ModRegistry mods = ModLoader.load(modsRoot);
        model = new ClassSandboxModel(mods.getAllPlayerClasses());
        selectedStats = model.computedStats(className);
    }

    private void writeFixtures(int newMaxHp) throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Files.createDirectories(coreDir.resolve("classes"));

        writeManifest(coreDir);
        writeStatsRegistry(coreDir);

        JsonObject classJson = new JsonObject();
        classJson.addProperty("id", "core:mage");
        classJson.addProperty("name", "Mage");

        JsonObject stats = new JsonObject();
        addStat(stats, "strength", 6);
        addStat(stats, "dexterity", 9);
        addStat(stats, "constitution", 8);
        addStat(stats, "intelligence", 16);
        addStat(stats, "wisdom", 14);
        addStat(stats, "luck", 7);
        addStat(stats, "maxHp", newMaxHp);
        addStat(stats, "maxMana", 100);

        classJson.add("stats", stats);
        Files.writeString(coreDir.resolve("classes").resolve("mage.json"), classJson.toString());
    }

    private static void writeManifest(Path coreDir) throws IOException {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("id", "core");
        manifest.add("dependsOn", new JsonArray());
        Files.writeString(coreDir.resolve("mod.json"), manifest.toString());
    }

    private static void writeStatsRegistry(Path coreDir) throws IOException {
        JsonObject stats = new JsonObject();
        JsonArray statNames = new JsonArray();
        statNames.add("strength");
        statNames.add("dexterity");
        statNames.add("constitution");
        statNames.add("intelligence");
        statNames.add("wisdom");
        statNames.add("luck");
        statNames.add("maxHp");
        statNames.add("maxMana");
        stats.add("stats", statNames);
        Files.writeString(coreDir.resolve("stats.json"), stats.toString());
    }

    private static void addStat(JsonObject stats, String name, int base) {
        JsonObject stat = new JsonObject();
        stat.addProperty("base", base);
        stats.add(name, stat);
    }
}
