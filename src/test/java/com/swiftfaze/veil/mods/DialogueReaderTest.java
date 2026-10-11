package com.swiftfaze.veil.mods;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.exceptions.ModLoadException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DialogueReaderTest {

    private static final Path FILE = Path.of("talk.json");
    private static final int LIMIT = 9;

    private static JsonObject json(String start, String nodes) {
        return JsonParser.parseString("{\"id\":\"test:talk\",\"start\":\"" + start + "\",\"nodes\":" + nodes + "}")
                .getAsJsonObject();
    }

    private static String choices(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(number -> "{\"text\":\"Pick " + number + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String failureOf(JsonObject json) {
        return assertThrows(ModLoadException.class, () -> DialogueReader.read(json, "test:talk", FILE)).getMessage();
    }

    @Test
    void aNodeWithTheMaximumNumberOfChoicesIsAccepted() {
        JsonObject json = json("a", "{\"a\":{\"text\":\"Hi\",\"choices\":" + choices(LIMIT) + "}}");

        Dialogue dialogue = DialogueReader.read(json, "test:talk", FILE);

        assertEquals(LIMIT, dialogue.node("a").choices().size());
    }

    @Test
    void aNodeOverTheLimitNamesTheCountAndTheLimit() {
        JsonObject json = json("a", "{\"a\":{\"text\":\"Hi\",\"choices\":" + choices(LIMIT + 1) + "}}");

        assertEquals("Dialogue 'test:talk': node 'a' has 10 choices; the limit is 9 in file: talk.json", failureOf(json));
    }

    @Test
    void aBlankChoiceNamesItsOneBasedNumber() {
        JsonObject json = json("a", "{\"a\":{\"text\":\"Hi\",\"choices\":[{\"text\":\"Ok\"},{\"text\":\" \"}]}}");

        assertEquals("Dialogue 'test:talk': node 'a' choice 2 has empty text in file: talk.json", failureOf(json));
    }

    @Test
    void aChoiceLeadingNowhereNamesItsOneBasedNumber() {
        JsonObject json = json("a",
                "{\"a\":{\"text\":\"Hi\",\"choices\":[{\"text\":\"Ok\"},{\"text\":\"Go\",\"next\":\"nowhere\"}]}}");

        assertEquals("Dialogue 'test:talk': node 'a' choice 2 leads to 'nowhere', which is not a node in file: talk.json",
                failureOf(json));
    }
}
