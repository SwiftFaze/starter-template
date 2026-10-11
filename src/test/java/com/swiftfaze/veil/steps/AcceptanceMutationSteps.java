package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.testing.aps.AcceptanceMutator;
import com.swiftfaze.veil.testing.aps.Mutant;
import com.swiftfaze.veil.testing.aps.MutantGenerator;
import com.swiftfaze.veil.testing.aps.MutationReport;
import com.swiftfaze.veil.testing.aps.ScenarioRunner;
import com.swiftfaze.veil.testing.aps.ScenarioRunner.RunResult;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the real {@link AcceptanceMutator} against the fixture features in
 * {@code src/test/resources/aps-fixtures/}, with the fixture glue in
 * {@code com.swiftfaze.veil.testing.aps.fixtures}. Neither is visible to the real
 * suite. State lives in this instance, which Cucumber creates per scenario.
 */
public class AcceptanceMutationSteps {

    private static final String FIXTURE_GLUE = "com.swiftfaze.veil.testing.aps.fixtures";

    private Path feature;
    private MutantGenerator.Generation generation;
    private MutationReport report;
    private int exitCode;
    private String output = "";
    private RunResult mutantRun;

    private static Path fixture(String relativePath) {
        URL url = Objects.requireNonNull(
                AcceptanceMutationSteps.class.getResource("/aps-fixtures/" + relativePath),
                "missing fixture " + relativePath);
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private void runMutator(List<Path> features) {
        report = new AcceptanceMutator(FIXTURE_GLUE).mutate(features);
        exitCode = report.exitCode();
    }

    private void runCommandLine(String... args) {
        List<String> lines = new ArrayList<>();
        AcceptanceMutator.Outcome outcome = AcceptanceMutator.execute(List.of(args), lines::add);
        output = String.join("\n", lines);
        report = outcome.report();
        exitCode = outcome.exitCode();
    }

    private List<Mutant> mutantsOf(String original) {
        return generation.mutants().stream().filter(m -> m.original().equals(original)).toList();
    }

    @Given("a fixture feature with a scenario {string} using the integer {int}")
    public void aFixtureFeatureWithAScenarioUsingTheInteger(String scenarioName, int value) throws IOException {
        feature = fixture("with-integer-ignore.feature");
        String source = Files.readString(feature);
        assertTrue(source.contains("Scenario: " + scenarioName.replace("<n>", Integer.toString(value))), source);
        assertTrue(source.contains("the player gains " + value + " gold"), source);
    }

    @Given("its step definition ignores the integer")
    public void itsStepDefinitionIgnoresTheInteger() {
        // The fixture glue's "the player gains {int} gold" ignores its argument; prove it
        // by running the unmutated scenario, which must pass for survivors to mean anything.
        RunResult original = new ScenarioRunner(FIXTURE_GLUE).run(feature, Mutant.WHOLE_FEATURE);
        assertTrue(original.passed(), "unmutated fixture must pass: " + original);
    }

    @Given("a fixture feature whose step definitions check every literal")
    public void aFixtureFeatureWhoseStepDefinitionsCheckEveryLiteral() {
        feature = fixture("with-integer-check.feature");
    }

    @Given("a fixture feature using the quoted strings {string} and {string}")
    public void aFixtureFeatureUsingTheQuotedStrings(String first, String second) throws IOException {
        feature = fixture("with-strings.feature");
        String source = Files.readString(feature);
        assertTrue(source.contains("\"" + first + "\"") && source.contains("\"" + second + "\""), source);
    }

    @Given("a fixture feature whose only quoted string is {string}")
    public void aFixtureFeatureWhoseOnlyQuotedStringIs(String only) throws IOException {
        feature = fixture("with-string-unique.feature");
        assertEquals(1, Files.readString(feature).split("\"", -1).length / 2, "exactly one quoted string");
        assertTrue(Files.readString(feature).contains("\"" + only + "\""));
    }

    @Given("a fixture Scenario Outline with an Examples row {string}")
    public void aFixtureScenarioOutlineWithAnExamplesRow(String row) throws IOException {
        feature = fixture("with-examples.feature");
        assertTrue(Files.readString(feature).contains(row), "fixture has row " + row);
    }

    @Given("a fixture feature with two scenarios")
    public void aFixtureFeatureWithTwoScenarios() {
        feature = fixture("with-two-scenarios.feature");
    }

    @Given("fixture features `alpha` and `beta`")
    public void fixtureFeaturesAlphaAndBeta() {
        feature = fixture("pair/alpha.feature");
        assertTrue(Files.isRegularFile(fixture("pair/beta.feature")));
    }

    @Given("a fixture feature tagged `@manual-verification` and one tagged `@pending`")
    public void aFixtureFeatureTaggedManualVerificationAndOneTaggedPending() {
        feature = fixture("tagged/manual-verification.feature");
    }

    @Given("a fixture feature whose scenario fails before any mutation")
    public void aFixtureFeatureWhoseScenarioFailsBeforeAnyMutation() {
        feature = fixture("with-failure.feature");
    }

    @When("the acceptance mutator runs on that feature")
    public void theAcceptanceMutatorRunsOnThatFeature() {
        runMutator(List.of(feature));
    }

    @When("the acceptance mutator generates mutants")
    public void theAcceptanceMutatorGeneratesMutants() throws IOException {
        generation = MutantGenerator.generate(feature);
    }

    @When("the acceptance mutator runs a mutant of the first scenario")
    public void theAcceptanceMutatorRunsAMutantOfTheFirstScenario() throws IOException {
        generation = MutantGenerator.generate(feature);
        Mutant first = generation.mutants().stream()
                .filter(m -> "First scenario".equals(m.scenarioName()))
                .findFirst()
                .orElseThrow();
        mutantRun = new AcceptanceMutator(FIXTURE_GLUE).runMutant(first);
    }

    @When("the acceptance mutator runs with `--feature {word}`")
    public void theAcceptanceMutatorRunsWithFeature(String slug) {
        runCommandLine("--feature", slug, "--spec-dir", feature == null
                ? fixture("pair").toString() : feature.getParent().toString(), "--glue", FIXTURE_GLUE);
    }

    @When("the acceptance mutator runs on all features")
    public void theAcceptanceMutatorRunsOnAllFeatures() {
        runCommandLine("--spec-dir", feature.getParent().toString(), "--glue", FIXTURE_GLUE);
    }

    @Then("it reports survivors for {int} → {int} and {int} → {int} at the literal's file and line")
    public void itReportsSurvivorsAtTheLiteralsFileAndLine(int from1, int to1, int from2, int to2) {
        List<String> survivors = report.survivors().stream().map(Mutant::toString).toList();
        assertEquals(List.of(feature + ":4  " + from1 + " → " + to1, feature + ":4  " + from2 + " → " + to2),
                survivors);
    }

    @Then("it exits {int}")
    public void itExits(int expected) {
        assertEquals(expected, exitCode);
    }

    @Then("it reports no survivors")
    public void itReportsNoSurvivors() {
        assertFalse(report.mutantsRun().isEmpty(), "mutants must actually have run");
        assertTrue(report.survivors().isEmpty(), report.survivors().toString());
    }

    @Then("{string} is mutated to {string} and {string} to {string}")
    public void isMutatedToAndTo(String firstOriginal, String firstReplacement,
                                  String secondOriginal, String secondReplacement) {
        assertEquals(List.of("\"" + firstReplacement + "\""),
                mutantsOf("\"" + firstOriginal + "\"").stream().map(Mutant::replacement).toList());
        assertEquals(List.of("\"" + secondReplacement + "\""),
                mutantsOf("\"" + secondOriginal + "\"").stream().map(Mutant::replacement).toList());
    }

    @Then("no string mutant is generated for {string}")
    public void noStringMutantIsGeneratedFor(String value) throws IOException {
        assertTrue(Files.readString(feature).contains("\"" + value + "\""), "the fixture holds " + value);
        assertTrue(mutantsOf("\"" + value + "\"").isEmpty(), generation.mutants().toString());
    }

    @Then("the report says it was skipped for having no alternative value")
    public void theReportSaysItWasSkippedForHavingNoAlternativeValue() {
        assertTrue(generation.skipped().stream().anyMatch(s -> s.endsWith("skipped: no alternative value")),
                generation.skipped().toString());
    }

    @Then("it generates mutants for {int} and for {int} separately, each running only that Examples row")
    public void itGeneratesMutantsForEachCellRunningOnlyThatRow(int first, int second) throws IOException {
        List<String> lines = Files.readString(feature).lines().toList();
        String row = "| " + first + " | " + second + " |";
        int rowLine = lines.indexOf(lines.stream().filter(l -> l.contains(row)).findFirst().orElseThrow()) + 1;
        for (int value : new int[]{first, second}) {
            List<Mutant> mutants = mutantsOf(Integer.toString(value));
            assertEquals(List.of(Integer.toString(value + 1), Integer.toString(value - 1)),
                    mutants.stream().map(Mutant::replacement).toList());
            mutants.forEach(m -> assertEquals(rowLine, m.runLine(), m.toString()));
        }
        RunResult run = new AcceptanceMutator(FIXTURE_GLUE).runMutant(mutantsOf(Integer.toString(first)).get(0));
        assertEquals(1, run.testsRun(), "only the one Examples row runs: " + run.executedScenarios());
    }

    @Then("only the first scenario is executed for that mutant")
    public void onlyTheFirstScenarioIsExecutedForThatMutant() {
        assertEquals(List.of("First scenario"), mutantRun.executedScenarios());
    }

    @Then("only mutants from `alpha.feature` are run")
    public void onlyMutantsFromAlphaFeatureAreRun() {
        assertFalse(report.mutantsRun().isEmpty(), output);
        report.mutantsRun().forEach(m -> assertEquals("alpha.feature", m.feature().getFileName().toString()));
    }

    @Then("it fails, naming the slug and the features directory searched")
    public void itFailsNamingTheSlugAndTheFeaturesDirectorySearched() {
        assertEquals(MutationReport.EXIT_ERROR, exitCode);
        assertTrue(output.contains("does-not-exist"), output);
        assertTrue(output.contains(fixture("pair").toAbsolutePath().toString()), output);
    }

    @Then("neither feature is mutated")
    public void neitherFeatureIsMutated() {
        assertTrue(report.mutantsRun().isEmpty(), report.mutantsRun().toString());
    }

    @Then("the report lists both as skipped with their tag")
    public void theReportListsBothAsSkippedWithTheirTag() {
        assertTrue(report.skipped().stream().anyMatch(s -> s.contains("manual-verification.feature")
                && s.endsWith("@manual-verification")), report.skipped().toString());
        assertTrue(report.skipped().stream().anyMatch(s -> s.contains("pending.feature")
                && s.endsWith("@pending")), report.skipped().toString());
    }

    @Then("it reports the scenario as failing on the original")
    public void itReportsTheScenarioAsFailingOnTheOriginal() {
        assertEquals(1, report.failingOriginals().size(), report.failingOriginals().toString());
        assertTrue(report.failingOriginals().get(0).contains("Failing scenario"));
    }

    @Then("it generates no mutants for it")
    public void itGeneratesNoMutantsForIt() {
        assertTrue(report.mutantsRun().isEmpty(), report.mutantsRun().toString());
    }

    @Then("it exits with a code other than {int} and {int}")
    public void itExitsWithACodeOtherThanAnd(int first, int second) {
        assertNotEquals(first, exitCode);
        assertNotEquals(second, exitCode);
    }
}
