package com.swiftfaze.veil.testing.aps;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.Background;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.Examples;
import io.cucumber.messages.types.Feature;
import io.cucumber.messages.types.FeatureChild;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Location;
import io.cucumber.messages.types.RuleChild;
import io.cucumber.messages.types.Scenario;
import io.cucumber.messages.types.Step;
import io.cucumber.messages.types.TableCell;
import io.cucumber.messages.types.TableRow;
import io.cucumber.messages.types.Tag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a feature file with {@code io.cucumber:gherkin} and lists its mutants.
 *
 * <p>Only step text and Examples cells are mutated, never keywords, names,
 * descriptions, comments or tags. Integers become +1 and -1. A double-quoted string
 * becomes each other distinct double-quoted value used in the same file, or is
 * skipped with a reason when there is none.
 */
public final class MutantGenerator {

    /** Tags whose scenarios never run in the real suite, so mutating them proves nothing. */
    public static final Set<String> SKIPPED_TAGS = Set.of("@manual-verification", "@pending");

    private static final Pattern LITERAL = Pattern.compile("\"[^\"]*\"|(?<![\\w.])\\d+(?![\\w.])");
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final Pattern QUOTED = Pattern.compile("\"[^\"]*\"");

    private final Path feature;
    private final List<Literal> literals = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();

    /** The result of generating one feature's mutants. */
    public record Generation(List<Mutant> mutants, List<String> skipped, Optional<String> featureSkipTag) {
        public Generation {
            mutants = List.copyOf(mutants);
            skipped = List.copyOf(skipped);
        }
    }

    private record Literal(int line, int column, String text, int runLine, String scenarioName) {
    }

    private MutantGenerator(Path feature) {
        this.feature = feature;
    }

    public static Generation generate(Path feature) throws IOException {
        return new MutantGenerator(feature).run();
    }

    private Generation run() throws IOException {
        Feature parsed = parse();
        Optional<String> featureSkipTag = skipTag(parsed.getTags());
        if (featureSkipTag.isPresent()) {
            return new Generation(List.of(), List.of(), featureSkipTag);
        }
        for (FeatureChild child : parsed.getChildren()) {
            child.getBackground().ifPresent(this::collectBackground);
            child.getScenario().ifPresent(this::collectScenario);
            child.getRule().ifPresent(rule -> rule.getChildren().forEach(this::collectRuleChild));
        }
        return new Generation(mutate(), List.copyOf(skipped), Optional.empty());
    }

    private Feature parse() throws IOException {
        GherkinParser parser = GherkinParser.builder()
                .includeSource(false)
                .includePickles(false)
                .build();
        List<Envelope> envelopes = parser.parse(feature).toList();
        envelopes.stream()
                .flatMap(e -> e.getParseError().stream())
                .findFirst()
                .ifPresent(error -> {
                    throw new IllegalArgumentException("Cannot parse " + feature + ": " + error.getMessage());
                });
        return envelopes.stream()
                .flatMap(e -> e.getGherkinDocument().stream())
                .map(GherkinDocument::getFeature)
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No Feature in " + feature));
    }

    private void collectRuleChild(RuleChild child) {
        child.getBackground().ifPresent(this::collectBackground);
        child.getScenario().ifPresent(this::collectScenario);
    }

    private void collectBackground(Background background) {
        for (Step step : background.getSteps()) {
            collectStep(step, Mutant.WHOLE_FEATURE, "Background");
        }
    }

    private void collectScenario(Scenario scenario) {
        Optional<String> tag = skipTag(scenario.getTags());
        if (tag.isPresent()) {
            skipped.add(feature + ":" + lineOf(scenario.getLocation())
                    + "  scenario \"" + scenario.getName() + "\" skipped: " + tag.get());
            return;
        }
        int scenarioLine = (int) lineOf(scenario.getLocation());
        for (Step step : scenario.getSteps()) {
            collectStep(step, scenarioLine, scenario.getName());
        }
        for (Examples examples : scenario.getExamples()) {
            collectExamples(examples, scenario.getName());
        }
    }

    private void collectStep(Step step, int runLine, String scenarioName) {
        int line = (int) lineOf(step.getLocation());
        int textColumn = (int) columnOf(step.getLocation()) + step.getKeyword().length();
        Matcher matcher = LITERAL.matcher(step.getText());
        while (matcher.find()) {
            literals.add(new Literal(line, textColumn + matcher.start(), matcher.group(), runLine, scenarioName));
        }
    }

    private void collectExamples(Examples examples, String scenarioName) {
        if (skipTag(examples.getTags()).isPresent()) {
            return;
        }
        for (TableRow row : examples.getTableBody()) {
            int rowLine = (int) lineOf(row.getLocation());
            for (TableCell cell : row.getCells()) {
                String value = cell.getValue();
                if (INTEGER.matcher(value).matches() || QUOTED.matcher(value).matches()) {
                    literals.add(new Literal(rowLine, (int) columnOf(cell.getLocation()), value, rowLine, scenarioName));
                }
            }
        }
    }

    // Gherkin's own types only ever hand back a Location; reading the line/column out of it
    // is this pair's one job, so no caller reaches past Location itself - the boundary
    // LawOfDemeter checks for.
    private static long lineOf(Location location) {
        return location.getLine();
    }

    private static long columnOf(Location location) {
        return location.getColumn().orElseThrow();
    }

    private List<Mutant> mutate() {
        Set<String> quotedValues = new LinkedHashSet<>();
        literals.stream().map(Literal::text).filter(t -> t.startsWith("\"")).forEach(quotedValues::add);

        List<Mutant> mutants = new ArrayList<>();
        for (Literal literal : literals) {
            if (literal.text().startsWith("\"")) {
                addStringMutants(literal, quotedValues, mutants);
            } else {
                long value = Long.parseLong(literal.text());
                mutants.add(mutant(literal, Long.toString(value + 1)));
                mutants.add(mutant(literal, Long.toString(value - 1)));
            }
        }
        return mutants;
    }

    private void addStringMutants(Literal literal, Set<String> quotedValues, List<Mutant> mutants) {
        List<String> alternatives = quotedValues.stream().filter(v -> !v.equals(literal.text())).toList();
        if (alternatives.isEmpty()) {
            skipped.add(feature + ":" + literal.line() + "  " + literal.text() + " skipped: no alternative value");
            return;
        }
        alternatives.forEach(alternative -> mutants.add(mutant(literal, alternative)));
    }

    private Mutant mutant(Literal literal, String replacement) {
        return new Mutant(feature, literal.line(), literal.column(), literal.text(), replacement,
                literal.runLine(), literal.scenarioName());
    }

    private static Optional<String> skipTag(List<Tag> tags) {
        return tags.stream().map(Tag::getName).filter(SKIPPED_TAGS::contains).findFirst();
    }
}
