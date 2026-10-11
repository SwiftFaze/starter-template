package com.swiftfaze.veil.testing.qa;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventMatcherTest {
    private static final String MOVED_6 = "{\"type\":\"PlayerMoved\",\"x\":6,\"y\":5}";
    private static final String MOVED_7 = "{\"type\":\"PlayerMoved\",\"x\":7,\"y\":5}";
    private static final String MENU = "{\"type\":\"MenuSelectionChanged\",\"to\":\"New\"}";

    private static List<JsonObject> events(String... json) {
        return Arrays.stream(json).map(s -> JsonParser.parseString(s).getAsJsonObject()).toList();
    }

    @Test
    void exactSequencePasses() {
        assertTrue(EventMatcher.match(events(MOVED_6, MOVED_7), events(MOVED_6, MOVED_7)).passed());
    }

    @Test
    void extraEventsBetweenExpectedOnesAreFine() {
        assertTrue(EventMatcher.match(events(MOVED_6, MOVED_7), events(MENU, MOVED_6, MENU, MOVED_7, MENU)).passed());
    }

    @Test
    void outOfOrderFails() {
        assertFalse(EventMatcher.match(events(MOVED_7, MOVED_6), events(MOVED_6, MOVED_7)).passed());
    }

    @Test
    void aMissingEventFails() {
        assertFalse(EventMatcher.match(events(MOVED_6, MOVED_7), events(MOVED_6)).passed());
    }

    @Test
    void anEmptyExpectationPasses() {
        assertTrue(EventMatcher.match(events(), events(MOVED_6)).passed());
    }

    @Test
    void theDiffNamesTheFirstUnmatchedEventAndItsPosition() {
        EventMatcher.Result result = EventMatcher.match(events(MOVED_6, MOVED_7), events(MENU, MOVED_6, MENU));

        String diff = result.describeFailure();
        assertEquals(1, result.matchedCount());
        assertTrue(diff.contains("#2 of 2"), diff);
        assertTrue(diff.contains("\"x\":7"), diff);
    }

    @Test
    void theDiffListsOnlyWhatWasRecordedAfterTheLastMatch() {
        String diff = EventMatcher.match(events(MOVED_6, MOVED_7), events(MENU, MOVED_6, MENU)).describeFailure();

        assertTrue(diff.contains("MenuSelectionChanged"), diff);
        assertFalse(diff.contains("\"x\":6"), diff);
    }

    @Test
    void aPassingResultHasNoFailureText() {
        assertEquals("", EventMatcher.match(events(MOVED_6), events(MOVED_6)).describeFailure());
    }

    @Test
    void whenNothingWasRecordedAfterTheLastMatchTheDiffSaysNone() {
        String diff = EventMatcher.match(events(MOVED_6, MOVED_7), events(MOVED_6)).describeFailure();

        assertTrue(diff.contains("(none)"), diff);
    }

    @Test
    void whenNothingMatchedTheDiffListsEverythingRecorded() {
        String diff = EventMatcher.match(events(MOVED_7), events(MENU, MOVED_6)).describeFailure();

        assertTrue(diff.contains("none matched"), diff);
        assertTrue(diff.contains("MenuSelectionChanged") && diff.contains("\"x\":6"), diff);
    }
}
