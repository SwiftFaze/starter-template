package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MethodCoverageTest {

    @ParameterizedTest
    @CsvSource({
        "1, 4, 0, 2.0",
        "4, 10, 0, 20.0",
        "4, 0, 10, 4.0",
        "10, 0, 10, 10.0",
        "6, 5, 5, 10.5",
        "4, 5, 1, 13.3",
    })
    void crapCombinesComplexityAndLineCoverage(int complexity, int missed, int covered, double expected) {
        MethodCoverage method = new MethodCoverage("a/B", "m", complexity, missed, covered);

        assertEquals(expected, method.roundedCrap(), 1e-9);
    }

    @Test
    void aMethodWithNoLinesCountsAsFullyCovered() {
        MethodCoverage method = new MethodCoverage("a/B", "m", 3, 0, 0);

        assertEquals(100, method.coveragePercent());
        assertEquals(3.0, method.crap(), 1e-9);
    }

    @Test
    void keyUsesTheDottedClassNameAndKeepsInnerClassMarkers() {
        MethodCoverage method = new MethodCoverage("com/swiftfaze/veil/Main$1", "run", 1, 0, 1);

        assertEquals("com.swiftfaze.veil.Main$1", method.fqcn());
        assertEquals("com.swiftfaze.veil.Main$1#run", method.key());
    }

    @Test
    void coveragePercentRoundsToTheNearestInteger() {
        assertEquals(67, new MethodCoverage("a/B", "m", 1, 1, 2).coveragePercent());
    }
}
