package com.swiftfaze.veil.testing.quality;

/**
 * One {@code <method>} row of a JaCoCo XML report: its complexity and line
 * counters, and the CRAP score derived from them.
 *
 * <pre>CRAP(m) = CC(m)^2 * (1 - cov(m))^3 + CC(m)</pre>
 *
 * @param className JaCoCo's slash-separated class name, e.g. {@code com/swiftfaze/veil/Main$1}
 */
record MethodCoverage(String className, String method, int complexity, int linesMissed, int linesCovered) {

    private static final double PERCENT = 100.0;
    private static final double ONE_DECIMAL_PLACE = 10.0;

    String fqcn() {
        return className.replace('/', '.');
    }

    String key() {
        return fqcn() + "#" + method;
    }

    /** Line coverage in [0, 1]; a method with no lines counts as fully covered. */
    double coverage() {
        int lines = linesMissed + linesCovered;
        return lines == 0 ? 1.0 : (double) linesCovered / lines;
    }

    int coveragePercent() {
        return (int) Math.round(coverage() * PERCENT);
    }

    double crap() {
        double uncovered = 1.0 - coverage();
        return (double) complexity * complexity * uncovered * uncovered * uncovered + complexity;
    }

    /** The score as reported and as recorded in the baseline: one decimal place. */
    double roundedCrap() {
        return Math.round(crap() * ONE_DECIMAL_PLACE) / ONE_DECIMAL_PLACE;
    }
}
