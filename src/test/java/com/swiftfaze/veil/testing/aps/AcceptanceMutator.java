package com.swiftfaze.veil.testing.aps;

import com.swiftfaze.veil.testing.aps.ScenarioRunner.RunResult;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the acceptance tests: changes each literal in a scenario and expects the
 * scenario to fail. A mutant whose scenario still passes has survived, which means a
 * step definition ignores that literal. A manual Step 6 command, not a CI gate:
 *
 * <pre>
 * mvn -q exec:java -Dexec.classpathScope=test \
 *   -Dexec.mainClass=com.swiftfaze.veil.testing.aps.AcceptanceMutator \
 *   -Dexec.args="[--feature &lt;slug&gt;] [--spec-dir &lt;dir&gt;] [--glue &lt;package&gt;]"
 * </pre>
 *
 * Exit 0: no survivors. 3: a mutant survived. 2: something could not be checked (a
 * scenario fails unmutated, an unknown slug, a run that found no tests); 2 wins over 3.
 */
public final class AcceptanceMutator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptanceMutator.class);

    static final String DEFAULT_SPEC_DIR = "specs/features";
    static final String DEFAULT_GLUE = "com.swiftfaze.veil.steps";

    private final ScenarioRunner runner;

    /** The outcome of a command-line run. */
    public record Outcome(int exitCode, MutationReport report) {
    }

    public AcceptanceMutator(String glue) {
        this.runner = new ScenarioRunner(glue);
    }

    public static void main(String[] args) {
        System.exit(execute(List.of(args), LOGGER::info).exitCode());
    }

    /** Parses the arguments, runs, prints the report, and returns the exit code. */
    public static Outcome execute(List<String> args, Consumer<String> out) {
        Map<String, String> options = parse(args);
        Path specDir = Path.of(options.getOrDefault("--spec-dir", DEFAULT_SPEC_DIR));
        AcceptanceMutator mutator = new AcceptanceMutator(options.getOrDefault("--glue", DEFAULT_GLUE));
        MutationReport report = new MutationReport();
        String slug = options.get("--feature");
        if (slug != null && !Files.isRegularFile(specDir.resolve(slug + ".feature"))) {
            report.error("unknown feature '" + slug + "': no " + slug + ".feature in "
                    + specDir.toAbsolutePath());
        } else {
            List<Path> features = slug != null ? List.of(specDir.resolve(slug + ".feature")) : listFeatures(specDir);
            mutator.mutate(features, report);
        }
        report.print(out);
        return new Outcome(report.exitCode(), report);
    }

    private static Map<String, String> parse(List<String> args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.size(); i += 2) {
            if (!args.get(i).startsWith("--") || i + 1 >= args.size()) {
                throw new IllegalArgumentException("Expected --option value pairs, got: " + String.join(" ", args));
            }
            options.put(args.get(i), args.get(i + 1));
        }
        return options;
    }

    private static List<Path> listFeatures(Path specDir) {
        try (Stream<Path> files = Files.list(specDir)) {
            return files.filter(p -> p.toString().endsWith(".feature")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list " + specDir, e);
        }
    }

    public MutationReport mutate(List<Path> features) {
        MutationReport report = new MutationReport();
        mutate(features, report);
        return report;
    }

    private void mutate(List<Path> features, MutationReport report) {
        for (Path feature : features) {
            try {
                mutateFeature(feature, report);
            } catch (IOException | IllegalArgumentException | IllegalStateException e) {
                report.error(feature + ": " + e.getMessage());
            }
        }
    }

    private void mutateFeature(Path feature, MutationReport report) throws IOException {
        MutantGenerator.Generation generation = MutantGenerator.generate(feature);
        if (generation.featureSkipTag().isPresent()) {
            report.skipped(feature + "  skipped: " + generation.featureSkipTag().get());
            return;
        }
        generation.skipped().forEach(report::skipped);
        Map<Integer, Boolean> originalPasses = new HashMap<>();
        for (Mutant mutant : generation.mutants()) {
            boolean runnable = originalPasses.computeIfAbsent(mutant.runLine(),
                    line -> checkOriginal(feature, line, mutant.scenarioName(), report));
            if (runnable) {
                RunResult result = runMutant(mutant);
                if (result.testsRun() == 0) {
                    report.error(mutant + ": the mutant run found no scenario to execute");
                } else {
                    report.ran(mutant, result.passed());
                }
            }
        }
    }

    private boolean checkOriginal(Path feature, int line, String scenarioName, MutationReport report) {
        RunResult original = runner.run(feature, line);
        if (original.testsRun() == 0) {
            report.error(feature + ":" + line + "  no scenario ran for \"" + scenarioName + "\"");
            return false;
        }
        if (!original.passed()) {
            report.failingOriginal(feature + ":" + line + "  scenario \"" + scenarioName + "\"");
            return false;
        }
        return true;
    }

    /** Writes the mutated feature to a temp directory and runs only what it affects. */
    public RunResult runMutant(Mutant mutant) throws IOException {
        Path dir = Files.createTempDirectory("acceptance-mutant-");
        Path copy = dir.resolve(mutant.feature().getFileName());
        try {
            Files.writeString(copy, mutant.applyTo(Files.readString(mutant.feature())));
            return runner.run(copy, mutant.runLine());
        } finally {
            Files.deleteIfExists(copy);
            Files.deleteIfExists(dir);
        }
    }
}
