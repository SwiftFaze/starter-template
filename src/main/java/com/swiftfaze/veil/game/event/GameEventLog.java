package com.swiftfaze.veil.game.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Append-only record of {@link GameEvent}s for QA replays. A no-op unless the game
 * starts with {@code -Dveil.qaLog=<path>}; then every event is written to that file
 * as one JSON line and flushed the moment it is appended, so a killed run still
 * leaves everything recorded up to that point.
 */
public final class GameEventLog {
    public static final String QA_LOG_PROPERTY = "veil.qaLog";

    private static final Logger logger = LoggerFactory.getLogger(GameEventLog.class);
    private static final Gson GSON = new Gson();

    private final boolean recording;
    private final @Nullable Path path;
    private final List<GameEvent> events = new ArrayList<>();
    private @Nullable Writer writer;
    private @Nullable String writeFailure;

    private GameEventLog(boolean recording, @Nullable Path path) {
        this.recording = recording;
        this.path = path;
    }

    /** A log that records nothing and writes nothing: the default in normal play. */
    public static GameEventLog noOp() {
        return new GameEventLog(/* recording= */ false, null);
    }

    /** A log that keeps events in memory only. */
    public static GameEventLog inMemory() {
        return new GameEventLog(/* recording= */ true, null);
    }

    /** A log that keeps events in memory and appends each one to {@code path}. */
    public static GameEventLog toFile(Path path) {
        return new GameEventLog(/* recording= */ true, path);
    }

    /** {@link #toFile} when {@code -Dveil.qaLog} is set, otherwise {@link #noOp}. */
    public static GameEventLog fromSystemProperties() {
        String qaLog = System.getProperty(QA_LOG_PROPERTY);
        if (qaLog == null || qaLog.isBlank()) {
            return noOp();
        }
        return toFile(Path.of(qaLog));
    }

    /**
     * Records {@code event} and, for a file-backed log, writes and flushes it as one
     * JSON line. A write failure is logged once, naming the path, and never thrown:
     * the game keeps running and the event stays in memory.
     */
    public void recordEvent(GameEvent event) {
        if (!recording) {
            return;
        }
        events.add(event);
        if (path != null && writeFailure == null) {
            persistLine(path, toJsonLine(event));
        }
    }

    /** An unmodifiable view of the events recorded so far, oldest first. */
    public List<GameEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    /** The error from the first failed write, naming the path; empty if none failed. */
    public Optional<String> getWriteFailure() {
        return Optional.ofNullable(writeFailure);
    }

    private void persistLine(Path target, String line) {
        try {
            currentWriter(target).write(line);
            currentWriter(target).write(System.lineSeparator());
            currentWriter(target).flush();
        } catch (IOException e) {
            writeFailure = "Could not write QA event log to " + target + ": " + e.getMessage();
            logger.error(writeFailure, e);
        }
    }

    // The writer is deliberately held open for the life of the process and never closed:
    // each line is flushed as it is written, so a killed run loses nothing.
    private Writer currentWriter(Path target) throws IOException {
        if (writer == null) {
            writer = Files.newBufferedWriter(target, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }
        return writer;
    }

    static String toJsonLine(GameEvent event) {
        JsonObject json = new JsonObject();
        json.addProperty("type", event.getClass().getSimpleName());
        GSON.toJsonTree(event).getAsJsonObject().entrySet()
                .forEach(field -> json.add(field.getKey(), field.getValue()));
        return GSON.toJson(json);
    }
}
