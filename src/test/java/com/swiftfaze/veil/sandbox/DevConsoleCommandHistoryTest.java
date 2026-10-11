package com.swiftfaze.veil.sandbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DevConsoleCommandHistoryTest {

    @Test
    void upOnEmptyHistoryReturnsTheDraftUnchanged() {
        DevConsoleCommandHistory history = new DevConsoleCommandHistory();

        assertEquals("draft", history.navigateUp("draft"));
    }

    @Test
    void downOnEmptyHistoryIsANoOp() {
        DevConsoleCommandHistory history = new DevConsoleCommandHistory();

        assertNull(history.navigateDown());
    }

    @Test
    void upStopsAtTheOldestEntryInsteadOfGoingOutOfBounds() {
        DevConsoleCommandHistory history = new DevConsoleCommandHistory();
        history.record("search classes");
        history.record("edit core:mage");

        history.navigateUp("draft");
        history.navigateUp("draft");
        String thirdUp = history.navigateUp("draft");

        assertEquals("search classes", thirdUp);
    }
}
