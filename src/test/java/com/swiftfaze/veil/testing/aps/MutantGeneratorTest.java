package com.swiftfaze.veil.testing.aps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutantGeneratorTest {

    @TempDir
    Path dir;

    private MutantGenerator.Generation generate(String source) throws IOException {
        Path feature = dir.resolve("sample.feature");
        Files.writeString(feature, source);
        return MutantGenerator.generate(feature);
    }

    private static List<String> describe(List<Mutant> mutants) {
        return mutants.stream().map(m -> m.line() + ":" + m.column() + " " + m.original() + "->" + m.replacement()
                + " run@" + m.runLine()).toList();
    }

    @Test
    void integersInStepTextBecomePlusAndMinusOneRunningTheirScenario() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Scenario: Gold 12
                    Given the player gains 12 gold
                """);
        assertEquals(List.of("4:28 12->13 run@3", "4:28 12->11 run@3"), describe(generation.mutants()));
    }

    @Test
    void namesKeywordsCommentsAndTagsAreNeverMutated() throws IOException {
        MutantGenerator.Generation generation = generate("""
                @tag1
                Feature: F 1
                  Description 2
                  # comment 3
                  @tag4
                  Scenario: S 5
                    Given nothing
                """);
        assertTrue(generation.mutants().isEmpty(), describe(generation.mutants()).toString());
    }

    @Test
    void digitsInsideWordsOrDecimalsAreNotIntegers() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Scenario: S
                    Given player2 has 1.5 hp
                """);
        assertTrue(generation.mutants().isEmpty(), describe(generation.mutants()).toString());
    }

    @Test
    void quotedStringsSwapWithEveryOtherDistinctValueInTheFile() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Scenario: S
                    Given "a" and "b"
                    Then "c" and "a"
                """);
        assertEquals(List.of(
                "4:11 \"a\"->\"b\" run@3", "4:11 \"a\"->\"c\" run@3",
                "4:19 \"b\"->\"a\" run@3", "4:19 \"b\"->\"c\" run@3",
                "5:10 \"c\"->\"a\" run@3", "5:10 \"c\"->\"b\" run@3",
                "5:18 \"a\"->\"b\" run@3", "5:18 \"a\"->\"c\" run@3"), describe(generation.mutants()));
    }

    @Test
    void aQuotedStringWithNoAlternativeIsSkippedWithAReason() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Scenario: S
                    Given "only"
                """);
        assertTrue(generation.mutants().isEmpty());
        assertEquals(1, generation.skipped().size());
        assertTrue(generation.skipped().get(0).endsWith(":4  \"only\" skipped: no alternative value"));
    }

    @Test
    void examplesCellsRunOnlyTheirOwnRowAndPlaceholdersAreIgnored() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Scenario Outline: O
                    Given <x> of 3

                    Examples:
                      | x  |
                      | 7  |
                      | ab |
                """);
        assertEquals(List.of("4:18 3->4 run@3", "4:18 3->2 run@3", "8:9 7->8 run@8", "8:9 7->6 run@8"),
                describe(generation.mutants()));
    }

    @Test
    void backgroundLiteralsRunTheWholeFeature() throws IOException {
        MutantGenerator.Generation generation = generate("""
                Feature: F

                  Background:
                    Given 5 coins

                  Scenario: S
                    Then done
                """);
        assertEquals(List.of("4:11 5->6 run@0", "4:11 5->4 run@0"), describe(generation.mutants()));
    }

    @Test
    void aFeatureTaggedToNeverRunIsSkippedEntirely() throws IOException {
        MutantGenerator.Generation feature = generate("""
                @pending
                Feature: F

                  Scenario: S
                    Given 5 coins
                """);
        assertEquals("@pending", feature.featureSkipTag().orElseThrow());
        assertTrue(feature.mutants().isEmpty());
    }

    @Test
    void aScenarioTaggedToNeverRunIsSkippedWithItsTag() throws IOException {
        MutantGenerator.Generation scenario = generate("""
                Feature: F

                  @manual-verification
                  Scenario: S
                    Given 5 coins
                """);
        assertTrue(scenario.mutants().isEmpty());
        assertTrue(scenario.skipped().get(0).endsWith("skipped: @manual-verification"), scenario.skipped().toString());
    }

    @Test
    void anUnparseableFeatureIsRejected() throws IOException {
        Path feature = dir.resolve("broken.feature");
        Files.writeString(feature, "Feature: F\n  Scenario: S\n    Given a step\n    Then, broken\n");
        assertThrows(IllegalArgumentException.class, () -> MutantGenerator.generate(feature));
    }

    @Test
    void aMutantReplacesExactlyItsLiteral() {
        Mutant second = new Mutant(Path.of("x.feature"), 2, 13, "\"a\"", "\"b\"", 1, "S");
        assertEquals("line1\r\n  Given \"a\" \"b\"\r\nline3", second.applyTo("line1\r\n  Given \"a\" \"a\"\r\nline3"));
        assertThrows(IllegalStateException.class, () -> second.applyTo("line1\n  Given \"z\""));
    }
}
