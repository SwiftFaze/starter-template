package com.swiftfaze.veil.testing.qa;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * In-order subsequence match: every expected event must appear in the recorded
 * events, in order, with any number of other events allowed in between.
 */
public final class EventMatcher {
    private EventMatcher() {
    }

    /**
     * Outcome of a match.
     *
     * @param passed         whether every expected event was matched
     * @param expected       the expected events
     * @param matchedCount   how many expected events were matched, in order
     * @param recordedAfter  the recorded events after the last match (all of them if none matched)
     */
    public record Result(boolean passed, int matchedCount, List<JsonObject> expected, List<JsonObject> recordedAfter) {

        public Result {
            expected = List.copyOf(expected);
            recordedAfter = List.copyOf(recordedAfter);
        }

        public String describeFailure() {
            if (passed) {
                return "";
            }
            List<String> lines = new ArrayList<>();
            lines.add("first unmatched expected event (#" + (matchedCount + 1) + " of " + expected.size() + "): "
                    + expected.get(matchedCount));
            lines.add("events recorded after the last match ("
                    + (matchedCount == 0 ? "none matched" : "expected #" + matchedCount) + "):");
            if (recordedAfter.isEmpty()) {
                lines.add("  (none)");
            }
            recordedAfter.forEach(event -> lines.add("  " + event));
            return String.join("\n", lines);
        }
    }

    public static Result match(List<JsonObject> expected, List<JsonObject> recorded) {
        int matched = 0;
        int lastMatchIndex = -1;
        for (int i = 0; i < recorded.size() && matched < expected.size(); i++) {
            if (expected.get(matched).equals(recorded.get(i))) {
                matched++;
                lastMatchIndex = i;
            }
        }
        List<JsonObject> after = recorded.subList(lastMatchIndex + 1, recorded.size());
        return new Result(matched == expected.size(), matched, expected, after);
    }
}
