package com.swiftfaze.veil.testing.qa;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** A loaded {@code specs/qa/<slug>} procedure: the keys to press and the events to expect. */
public record QaProcedure(String slug, List<Integer> keys, List<JsonObject> expected) {

    public QaProcedure {
        keys = List.copyOf(keys);
        expected = List.copyOf(expected);
    }

    public static QaProcedure load(Path dir, String slug) {
        Path keysFile = dir.resolve(slug + ".keys");
        Path jsonFile = dir.resolve(slug + ".json");
        requireExists(keysFile);
        requireExists(jsonFile);
        JsonObject json = parseJson(jsonFile);
        Path script = json.has("script") ? dir.resolve(json.get("script").getAsString()) : keysFile;
        requireExists(script);
        return new QaProcedure(slug, KeyScript.parse(script), expectedEvents(json, jsonFile));
    }

    private static void requireExists(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new QaException("Missing QA procedure file: " + file);
        }
    }

    private static JsonObject parseJson(Path file) {
        try {
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new QaException("Cannot parse " + file + ": " + e.getMessage(), e);
        }
    }

    private static List<JsonObject> expectedEvents(JsonObject json, Path file) {
        if (!json.has("expect") || !json.get("expect").isJsonArray()) {
            throw new QaException(file + ": missing \"expect\" array");
        }
        List<JsonObject> events = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("expect")) {
            if (!element.isJsonObject()) {
                throw new QaException(file + ": every \"expect\" entry must be an object");
            }
            events.add(element.getAsJsonObject());
        }
        return events;
    }
}
