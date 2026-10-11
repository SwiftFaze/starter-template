package com.swiftfaze.veil.testing.quality;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Judges scored methods against {@code crap.max} and the baseline. Returns one
 * {@code FAIL ...} message per violation; an empty list passes.
 *
 * <p>A baselined method passes while its rounded score stays at or under its recorded
 * score. A baseline entry whose method is back within the limit, or no longer exists,
 * fails until it is removed, so the baseline can only shrink.
 */
final class CrapGate {

    private static final double TOLERANCE = 1e-9;
    private static final String FAIL_PREFIX = "FAIL ";

    private final double max;
    private final Map<String, Double> baseline;

    CrapGate(double max, Map<String, Double> baseline) {
        this.max = max;
        this.baseline = baseline;
    }

    List<String> violations(List<MethodCoverage> methods) {
        Map<String, MethodCoverage> worstByKey = worstByKey(methods);
        List<String> failures = new ArrayList<>();
        for (MethodCoverage method : worstByKey.values()) {
            checkMethod(method, failures);
        }
        for (String key : baseline.keySet()) {
            if (!worstByKey.containsKey(key)) {
                failures.add(FAIL_PREFIX + key + " is in crap-baseline.txt but not in the JaCoCo report:"
                        + " remove its baseline entry");
            }
        }
        return failures;
    }

    boolean isBaselined(MethodCoverage method) {
        return baseline.containsKey(method.key());
    }

    boolean isOverLimit(MethodCoverage method) {
        return method.crap() > max + TOLERANCE;
    }

    /** Split so the negation lives only in the "back within the limit" branch, not as an else-of-negation. */
    private void checkMethod(MethodCoverage method, List<String> failures) {
        Double recorded = baseline.get(method.key());
        if (isOverLimit(method)) {
            checkOverLimitMethod(method, recorded, failures);
        } else if (recorded != null) {
            failures.add(FAIL_PREFIX + method.key() + " CRAP " + CrapFormat.score(method.roundedCrap())
                    + " is now within the limit " + CrapFormat.limit(max) + ": remove its baseline entry");
        }
    }

    private void checkOverLimitMethod(MethodCoverage method, Double recorded, List<String> failures) {
        if (recorded == null) {
            failures.add(FAIL_PREFIX + method.key() + " complexity " + method.complexity()
                    + ", coverage " + method.coveragePercent() + "%, CRAP " + CrapFormat.score(method.roundedCrap())
                    + " exceeds the limit " + CrapFormat.limit(max));
        } else if (method.roundedCrap() > recorded + TOLERANCE) {
            failures.add(FAIL_PREFIX + method.key() + " got worse: baselined at " + CrapFormat.score(recorded)
                    + ", now " + CrapFormat.score(method.roundedCrap()));
        }
    }

    private static Map<String, MethodCoverage> worstByKey(List<MethodCoverage> methods) {
        Map<String, MethodCoverage> worst = new LinkedHashMap<>();
        for (MethodCoverage method : methods) {
            worst.merge(method.key(), method, (a, b) -> a.crap() >= b.crap() ? a : b);
        }
        return worst;
    }
}
