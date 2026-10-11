package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrapGateTest {

    private static final MethodCoverage UNCOVERED_CC4 = new MethodCoverage("a/B", "risky", 4, 6, 0);
    private static final MethodCoverage COVERED_CC10 = new MethodCoverage("a/B", "atLimit", 10, 0, 6);

    @TempDir
    Path dir;

    @Test
    void aMethodExactlyAtTheLimitPasses() {
        assertTrue(new CrapGate(10, Map.of()).violations(List.of(COVERED_CC10)).isEmpty());
    }

    @Test
    void aMethodOverTheLimitFailsWithItsFullDescription() {
        List<String> failures = new CrapGate(10, Map.of()).violations(List.of(UNCOVERED_CC4));

        assertEquals(List.of("FAIL a.B#risky complexity 4, coverage 0%, CRAP 20.0 exceeds the limit 10"), failures);
    }

    @Test
    void aBaselinedMethodAtItsRecordedScorePasses() {
        assertTrue(new CrapGate(10, Map.of("a.B#risky", 20.0)).violations(List.of(UNCOVERED_CC4)).isEmpty());
    }

    @Test
    void aBaselinedMethodThatGotWorseFails() {
        List<String> failures = new CrapGate(10, Map.of("a.B#risky", 13.3)).violations(List.of(UNCOVERED_CC4));

        assertEquals(List.of("FAIL a.B#risky got worse: baselined at 13.3, now 20.0"), failures);
    }

    @Test
    void aBaselinedMethodBackWithinTheLimitMustBeRemoved() {
        List<String> failures = new CrapGate(10, Map.of("a.B#atLimit", 12.0)).violations(List.of(COVERED_CC10));

        assertEquals(List.of("FAIL a.B#atLimit CRAP 10.0 is now within the limit 10: remove its baseline entry"),
                failures);
    }

    @Test
    void aBaselineEntryForAMissingMethodMustBeRemoved() {
        List<String> failures = new CrapGate(10, Map.of("a.B#gone", 12.0)).violations(List.of());

        assertEquals(List.of("FAIL a.B#gone is in crap-baseline.txt but not in the JaCoCo report:"
                + " remove its baseline entry"), failures);
    }

    @Test
    void overloadsSharingAKeyAreJudgedByTheWorstOne() {
        MethodCoverage covered = new MethodCoverage("a/B", "risky", 4, 0, 6);

        List<String> failures = new CrapGate(10, Map.of()).violations(List.of(covered, UNCOVERED_CC4));

        assertEquals(1, failures.size());
        assertTrue(failures.get(0).contains("CRAP 20.0"));
    }

    @Test
    void baselineSkipsCommentsAndBlankLinesAndKeepsTheWorseScoreForARepeatedKey() throws Exception {
        Path file = dir.resolve("crap-baseline.txt");
        Files.writeString(file, "# comment\n\na.B#m 12.0\na.B#m 13.3\n");

        assertEquals(Map.of("a.B#m", 13.3), CrapBaseline.load(file));
    }

    @Test
    void aMalformedBaselineLineIsRejected() throws Exception {
        Path file = dir.resolve("crap-baseline.txt");
        Files.writeString(file, "a.B#m\n");

        assertThrows(IllegalArgumentException.class, () -> CrapBaseline.load(file));
    }

    @Test
    void aMissingBaselineFileIsAnEmptyBaseline() throws Exception {
        assertTrue(CrapBaseline.load(dir.resolve("absent.txt")).isEmpty());
    }
}
