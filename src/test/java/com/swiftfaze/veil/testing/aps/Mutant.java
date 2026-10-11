package com.swiftfaze.veil.testing.aps;

import java.nio.file.Path;

/**
 * One change to one literal in a feature file.
 *
 * @param feature      the feature file the literal is in
 * @param line         1-based line of the literal
 * @param column       1-based column where the literal starts
 * @param original     the literal as written, quotes included for a string
 * @param replacement  what the mutant puts in its place
 * @param runLine      the line Cucumber is pointed at to run only the affected
 *                     scenario or Examples row; {@link #WHOLE_FEATURE} for a
 *                     Background step, which affects every scenario
 * @param scenarioName the scenario the literal belongs to, for the report
 */
public record Mutant(Path feature, int line, int column, String original, String replacement,
                     int runLine, String scenarioName) {

    public static final int WHOLE_FEATURE = 0;

    /** Applies this mutant to the feature's source text. */
    public String applyTo(String source) {
        String[] lines = source.split("\n", -1);
        String target = lines[line - 1];
        int start = column - 1;
        if (!target.startsWith(original, start)) {
            throw new IllegalStateException("Expected " + original + " at " + this + " but found: " + target);
        }
        lines[line - 1] = target.substring(0, start) + replacement + target.substring(start + original.length());
        return String.join("\n", lines);
    }

    @Override
    public String toString() {
        return feature + ":" + line + "  " + original + " → " + replacement;
    }
}
