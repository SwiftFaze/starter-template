package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Direct coverage of {@link CrapOutputs#write}'s STATUS column, below the level
 * crap-gate.feature exercises it at ("over-limit"/"baselined" substrings only).
 */
class CrapOutputsTest {

    private static final MethodCoverage WITHIN_LIMIT = new MethodCoverage("a/B", "fine", 1, 0, 4);
    private static final MethodCoverage OVER_LIMIT = new MethodCoverage("a/B", "risky", 4, 6, 0);
    private static final double CRAP_MAX = 10;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @TempDir
    Path root;

    @Test
    void tableHasAHeaderRowNamingEveryColumn() throws IOException {
        CrapOutputs.write(root, List.of(WITHIN_LIMIT), new CrapGate(CRAP_MAX, Map.of()), List.of());

        assertEquals("CRAP   CC  COV   STATUS            METHOD", crapTxt().get(0));
    }

    @Test
    void aMethodWithinTheLimitAndNotBaselinedIsMarkedOk() throws IOException {
        CrapOutputs.write(root, List.of(WITHIN_LIMIT), new CrapGate(CRAP_MAX, Map.of()), List.of());

        assertEquals("1.0 1 100% ok a.B#fine", normalizedRowFor("a.B#fine"));
    }

    @Test
    void anUnbaselinedMethodOverTheLimitIsMarkedOverLimit() throws IOException {
        CrapOutputs.write(root, List.of(OVER_LIMIT), new CrapGate(CRAP_MAX, Map.of()), List.of("FAIL a.B#risky ..."));

        assertEquals("20.0 4 0% over-limit a.B#risky", normalizedRowFor("a.B#risky"));
    }

    @Test
    void aBaselinedMethodOverTheLimitIsMarkedOverLimitBaselined() throws IOException {
        Map<String, Double> baseline = Map.of(OVER_LIMIT.key(), OVER_LIMIT.roundedCrap());

        CrapOutputs.write(root, List.of(OVER_LIMIT), new CrapGate(CRAP_MAX, baseline), List.of());

        assertEquals("20.0 4 0% over-limit baselined a.B#risky", normalizedRowFor("a.B#risky"));
    }

    @Test
    void theFailuresAndTheirCountAreAppendedAfterTheTable() throws IOException {
        CrapOutputs.write(root, List.of(OVER_LIMIT), new CrapGate(CRAP_MAX, Map.of()), List.of("FAIL a.B#risky ..."));

        List<String> lines = crapTxt();
        assertEquals("FAIL a.B#risky ...", lines.get(lines.size() - 2));
        assertEquals("CRAP violations: 1", lines.get(lines.size() - 1));
    }

    private String normalizedRowFor(String key) throws IOException {
        String row = crapTxt().stream().filter(l -> l.contains(key)).findFirst()
                .orElseThrow(() -> new AssertionError("No row for " + key));
        return WHITESPACE.matcher(row.trim()).replaceAll(" ");
    }

    private List<String> crapTxt() throws IOException {
        return Files.readAllLines(root.resolve(CrapReport.CRAP_TXT));
    }
}
