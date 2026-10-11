package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.testing.quality.CrapReport;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for crap-gate.feature. Each scenario builds a throwaway project root
 * (jacoco.xml, pom.xml with a jacoco-check excludes block, quality-gates.properties,
 * crap-baseline.txt) and runs the real {@link CrapReport} against it.
 */
public class CrapGateSteps {

    private static final String SAMPLE_CLASS = "com/example/Sample";
    private static final String DEFAULT_EXCLUDES = "<exclude>**/Excluded.class</exclude>";
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    /** Generous headroom for the fixture XML's header plus every method/counter tag appended after it. */
    private static final int FIXTURE_XML_CAPACITY = 1024;

    /** Line counters that give a complexity-4 method the scores the baseline scenarios name. */
    private static final Map<String, int[]> COMPLEXITY_4_SCORES = Map.of(
            "13.3", new int[] {5, 1},
            "20.0", new int[] {6, 0},
            "4.0", new int[] {0, 6});

    private Path root;
    private final List<Fixture> methods = new ArrayList<>();
    private String excludes = DEFAULT_EXCLUDES;
    private boolean pomHasExcludes = true;
    private boolean writeJacocoReport = true;
    private String baseline = "";
    private CrapReport.Result result;

    private record Fixture(String className, String method, int complexity, int missed, int covered) {
    }

    @After
    public void deleteFixtureRoot() throws IOException {
        if (root == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    @Given("`crap.max` is {int} in `quality-gates.properties`")
    public void crapMaxIsInQualityGatesProperties(int max) throws IOException {
        root = Files.createTempDirectory("crap-gate");
        Files.writeString(root.resolve("quality-gates.properties"), "crap.max=" + max + "\n");
    }

    @Given("a JaCoCo report with a method of complexity {int} and {int} of {int} lines covered")
    public void aJacocoReportWithAMethodOfComplexity(int complexity, int covered, int lines) {
        methods.add(new Fixture(SAMPLE_CLASS, "sample", complexity, lines - covered, covered));
    }

    @Given("a JaCoCo report where every method scores at most {int}")
    public void aJacocoReportWhereEveryMethodScoresAtMost(int max) {
        methods.add(new Fixture(SAMPLE_CLASS, "atLimit", max, 0, 4));
        methods.add(new Fixture(SAMPLE_CLASS, "halfCovered", 2, 1, 1));
        methods.add(new Fixture(SAMPLE_CLASS, "uncovered", 1, 3, 0));
    }

    @Given("a JaCoCo report containing an uncovered method of complexity {int}")
    public void aJacocoReportContainingAnUncoveredMethod(int complexity) {
        methods.add(new Fixture(SAMPLE_CLASS, "risky", complexity, 5, 0));
    }

    @Given("a JaCoCo report containing a fully covered method of complexity {int}")
    public void aJacocoReportContainingAFullyCoveredMethod(int complexity) {
        methods.add(new Fixture(SAMPLE_CLASS, "tested", complexity, 0, 5));
    }

    @Given("^the `jacoco-check` `<excludes>` contains `(.+)` and `(.+)`$")
    public void theJacocoCheckExcludesContain(String first, String second) {
        excludes = "<exclude>" + first + "</exclude><exclude>" + second + "</exclude>";
    }

    @Given("^a JaCoCo report containing an uncovered complexity-(\\d+) method in `(\\w+)` and one in `([\\w$]+)`$")
    public void aJacocoReportWithUncoveredMethodsIn(int complexity, String outer, String inner) {
        methods.add(new Fixture("com/swiftfaze/veil/" + outer, "run", complexity, 5, 0));
        methods.add(new Fixture("com/swiftfaze/veil/" + inner, "run", complexity, 5, 0));
    }

    @Given("^a JaCoCo report with method `(\\w+)` of complexity (\\d+), fully covered, in `([\\w.]+)`$")
    public void aJacocoReportWithMethodFullyCoveredIn(String method, int complexity, String fqcn) {
        methods.add(new Fixture(fqcn.replace('.', '/'), method, complexity, 0, 3));
    }

    @Given("no `target\\/site\\/jacoco\\/jacoco.xml` exists")
    public void noJacocoReportExists() {
        writeJacocoReport = false;
    }

    @Given("`pom.xml` has no `jacoco-check` execution with an `<excludes>` list")
    public void pomHasNoJacocoCheckExcludes() {
        pomHasExcludes = false;
    }

    @Given("^`crap-baseline.txt` records `(\\S+) ([\\d.]+)`$")
    public void crapBaselineRecords(String key, String score) {
        baseline += key + " " + score + "\n";
    }

    @Given("^a JaCoCo report where `(\\w+)#(\\w+)` (?:now )?scores ([\\d.]+)$")
    public void aJacocoReportWhereUiMethodScores(String simpleClass, String method, String score) {
        int[] lines = COMPLEXITY_4_SCORES.get(score);
        assertTrue(lines != null, "No fixture counters for a complexity-4 score of " + score);
        methods.add(new Fixture("com/swiftfaze/veil/ui/" + simpleClass, method, 4, lines[0], lines[1]));
    }

    @Given("`crap-baseline.txt` records a method that is not in the JaCoCo report")
    public void crapBaselineRecordsAMissingMethod() {
        baseline += "com.swiftfaze.veil.Gone#vanished 15.0\n";
        methods.add(new Fixture(SAMPLE_CLASS, "present", 1, 0, 2));
    }

    @When("the CRAP report runs")
    public void theCrapReportRuns() throws IOException {
        runReport(false);
    }

    @When("the CRAP report runs in gate mode")
    public void theCrapReportRunsInGateMode() throws IOException {
        runReport(false);
    }

    @When("the CRAP report runs with `--report-only`")
    public void theCrapReportRunsReportOnly() throws IOException {
        runReport(true);
    }

    @Then("that method's CRAP score is {double}")
    public void thatMethodsCrapScoreIs(double expected) throws IOException {
        String entry = ednEntryFor("sample");
        assertTrue(entry.endsWith(":crap " + String.format(Locale.ROOT, "%.1f", expected) + "}"),
                "Expected CRAP " + expected + " in " + entry);
    }

    @Then("it passes")
    public void itPasses() {
        assertEquals(0, result.exitCode(), () -> "Expected pass, got: " + result.messages());
    }

    @Then("it fails")
    public void itFails() {
        assertEquals(1, result.exitCode(), () -> "Expected gate failure, got: " + result.messages());
    }

    @Then("`target\\/crap\\/crap.txt` lists every method, worst first")
    public void crapTxtListsEveryMethodWorstFirst() throws IOException {
        List<String> rows = crapTxt().stream().filter(l -> l.contains("com.example.Sample#")).toList();
        assertEquals(methods.size(), rows.size(), () -> "Rows: " + rows);
        List<Double> scores = rows.stream().map(r -> Double.parseDouble(WHITESPACE.split(r.trim(), -1)[0])).toList();
        List<Double> sorted = new ArrayList<>(scores);
        sorted.sort(Comparator.reverseOrder());
        assertEquals(sorted, scores, "crap.txt is not worst first");
    }

    @Then("the failure names the method's class and name, its complexity, coverage and CRAP score, and the limit")
    public void theFailureNamesTheMethodInFull() {
        assertFailureMentions("com.example.Sample#risky", "complexity 4", "coverage 0%", "CRAP 20.0", "limit 10");
    }

    @Then("the method still appears in `target\\/crap\\/crap.txt` above the limit")
    public void theMethodStillAppearsAboveTheLimit() throws IOException {
        assertTrue(crapTxt().stream().anyMatch(l -> l.contains("com.example.Sample#risky") && l.contains("over-limit")));
    }

    @Then("neither method appears in either output file")
    public void neitherMethodAppearsInEitherOutputFile() throws IOException {
        String txt = String.join("\n", crapTxt());
        String edn = Files.readString(root.resolve(".metrics/crap.edn"));
        assertFalse(txt.contains("com.swiftfaze.veil.Main"), txt);
        assertFalse(edn.contains("com.swiftfaze.veil.Main"), edn);
    }

    @Then("^`.metrics/crap.edn` contains an entry with `:name` \"(\\w+)\", `:namespace` \"([\\w.]+)\", "
            + "`:complexity` (\\d+), `:coverage` (\\d+) and `:crap` ([\\d.]+)$")
    public void crapEdnContainsEntry(String name, String namespace, String complexity, String coverage, String crap)
            throws IOException {
        String expected = "{:name \"" + name + "\" :namespace \"" + namespace + "\" :complexity " + complexity
                + " :coverage " + coverage + " :crap " + crap + "}";
        assertEquals(expected, ednEntryFor(name));
    }

    @Then("it fails, naming the missing file")
    public void itFailsNamingTheMissingFile() {
        assertEquals(2, result.exitCode());
        assertFailureMentions("target/site/jacoco/jacoco.xml");
    }

    @Then("it does not write an empty report that passes")
    public void itDoesNotWriteAnEmptyReport() {
        assertFalse(Files.exists(root.resolve("target/crap/crap.txt")));
    }

    @Then("it fails, saying it could not find the exclusion list")
    public void itFailsSayingItCouldNotFindTheExclusionList() {
        assertEquals(2, result.exitCode());
        assertFailureMentions("could not find the exclusion list");
    }

    @Then("`target\\/crap\\/crap.txt` marks the method as baselined")
    public void crapTxtMarksTheMethodAsBaselined() throws IOException {
        assertTrue(crapTxt().stream().anyMatch(l -> l.contains("TableWidget#moveLeft") && l.contains("baselined")));
    }

    @Then("it fails, naming the method, its baselined score and its new score")
    public void itFailsNamingBaselinedAndNewScore() {
        itFails();
        assertFailureMentions("com.swiftfaze.veil.ui.TableWidget#moveLeft", "baselined at 13.3", "now 20.0");
    }

    @Then("it fails, saying the method is now within the limit and its baseline entry must be removed")
    public void itFailsSayingTheEntryIsStale() {
        itFails();
        assertFailureMentions("com.swiftfaze.veil.ui.TableWidget#moveLeft", "now within the limit",
                "remove its baseline entry");
    }

    @Then("it fails, naming the entry and saying it must be removed")
    public void itFailsNamingTheMissingEntry() {
        itFails();
        assertFailureMentions("com.swiftfaze.veil.Gone#vanished", "remove its baseline entry");
    }

    private void runReport(boolean reportOnly) throws IOException {
        Files.writeString(root.resolve("pom.xml"), pom());
        Files.writeString(root.resolve("crap-baseline.txt"), baseline);
        if (writeJacocoReport) {
            Path jacoco = root.resolve("target/site/jacoco/jacoco.xml");
            Files.createDirectories(jacoco.getParent());
            Files.writeString(jacoco, jacocoXml());
        }
        result = CrapReport.run(root, reportOnly);
    }

    private String pom() {
        String configuration = pomHasExcludes ? "<configuration><excludes>" + excludes + "</excludes></configuration>" : "";
        return "<project><build><plugins><plugin><artifactId>jacoco-maven-plugin</artifactId><executions>"
                + "<execution><id>jacoco-check</id>" + configuration + "</execution>"
                + "</executions></plugin></plugins></build></project>";
    }

    private String jacocoXml() {
        Map<String, StringBuilder> classes = new LinkedHashMap<>();
        for (Fixture m : methods) {
            classes.computeIfAbsent(m.className(), k -> new StringBuilder())
                    .append("<method name=\"").append(m.method()).append("\" desc=\"()V\" line=\"1\">")
                    .append(counter("LINE", m.missed(), m.covered()))
                    .append(counter("COMPLEXITY", m.complexity(), 0))
                    .append("</method>");
        }
        StringBuilder xml = new StringBuilder(FIXTURE_XML_CAPACITY).append("<report name=\"fixture\"><package name=\"fixture\">");
        classes.forEach((name, body) -> xml.append("<class name=\"").append(name).append("\">")
                .append(body).append("</class>"));
        return xml.append("</package></report>").toString();
    }

    private static String counter(String type, int missed, int covered) {
        return "<counter type=\"" + type + "\" missed=\"" + missed + "\" covered=\"" + covered + "\"/>";
    }

    private List<String> crapTxt() throws IOException {
        return Files.readAllLines(root.resolve("target/crap/crap.txt"));
    }

    private String ednEntryFor(String name) throws IOException {
        return Files.readAllLines(root.resolve(".metrics/crap.edn")).stream()
                .map(String::trim)
                .filter(l -> l.startsWith("{:name \"" + name + "\""))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No crap.edn entry named " + name));
    }

    private void assertFailureMentions(String... fragments) {
        String messages = String.join("\n", result.messages());
        for (String fragment : fragments) {
            assertTrue(messages.contains(fragment), () -> "Expected '" + fragment + "' in:\n" + messages);
        }
    }
}
