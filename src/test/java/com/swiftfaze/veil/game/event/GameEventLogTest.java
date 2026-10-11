package com.swiftfaze.veil.game.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameEventLogTest {
    @TempDir
    Path logDirectory;

    @AfterEach
    void clearProperty() {
        System.clearProperty(GameEventLog.QA_LOG_PROPERTY);
    }

    @Test
    void noOpLogKeepsNothing() {
        GameEventLog log = GameEventLog.noOp();
        log.recordEvent(GameEvent.playerMoved(1, 2));
        assertTrue(log.getEvents().isEmpty());
    }

    @Test
    void inMemoryLogKeepsEventsInOrder() {
        GameEventLog log = GameEventLog.inMemory();
        log.recordEvent(GameEvent.menuSelectionChanged("New"));
        log.recordEvent(GameEvent.playerMoved(1, 2));
        assertEquals(List.of(new GameEvent.MenuSelectionChanged("New"), new GameEvent.PlayerMoved(1, 2)),
                log.getEvents());
    }

    @Test
    void fileLogWritesEachEventAsATypedJsonLineImmediately() throws IOException {
        Path file = logDirectory.resolve("qa.jsonl");
        GameEventLog log = GameEventLog.toFile(file);
        log.recordEvent(GameEvent.screenChanged("title", "game"));
        assertEquals(List.of("{\"type\":\"ScreenChanged\",\"from\":\"title\",\"to\":\"game\"}"),
                Files.readAllLines(file));
        log.recordEvent(GameEvent.popupToggled("inventory", true));
        assertEquals("{\"type\":\"PopupToggled\",\"name\":\"inventory\",\"open\":true}",
                Files.readAllLines(file).get(1));
    }

    @Test
    void unwritablePathRecordsAFailureNamingThePathAndKeepsTheEvent() {
        Path file = logDirectory.resolve("missing").resolve("qa.jsonl");
        GameEventLog log = GameEventLog.toFile(file);
        log.recordEvent(GameEvent.playerMoved(1, 2));
        log.recordEvent(GameEvent.playerMoved(2, 2));
        assertTrue(log.getWriteFailure().orElseThrow().contains(file.toString()));
        assertEquals(2, log.getEvents().size());
    }

    @Test
    void systemPropertySelectsAFileLog() throws IOException {
        Path file = logDirectory.resolve("qa.jsonl");
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, file.toString());
        GameEventLog.fromSystemProperties().recordEvent(GameEvent.playerMoved(1, 2));
        assertEquals(1, Files.readAllLines(file).size());
    }

    @Test
    void blankSystemPropertySelectsANoOpLog() {
        System.setProperty(GameEventLog.QA_LOG_PROPERTY, " ");
        GameEventLog log = GameEventLog.fromSystemProperties();
        log.recordEvent(GameEvent.playerMoved(1, 2));
        assertTrue(log.getEvents().isEmpty());
    }
}
