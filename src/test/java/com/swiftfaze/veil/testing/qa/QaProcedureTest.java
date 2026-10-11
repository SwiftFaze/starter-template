package com.swiftfaze.veil.testing.qa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaProcedureTest {
    private static final String RIGHT_KEY = "RIGHT\n";
    private static final String FOO = "foo";
    private static final String FOO_KEYS = "foo.keys";
    private static final String FOO_JSON = "foo.json";
    private static final int MOVED_X = 6;

    @TempDir
    Path dir;

    @Test
    void loadsKeysAndExpectedEvents() throws IOException {
        Files.writeString(dir.resolve("p.keys"), "# go\nRIGHT\n");
        Files.writeString(dir.resolve("p.json"),
                "{\"script\":\"p.keys\",\"expect\":[{\"type\":\"PlayerMoved\",\"x\":6,\"y\":5}]}");

        QaProcedure procedure = QaProcedure.load(dir, "p");

        assertEquals(List.of(KeyEvent.VK_RIGHT), procedure.keys());
        assertEquals(1, procedure.expected().size());
        assertEquals(MOVED_X, procedure.expected().get(0).get("x").getAsInt());
    }

    @Test
    void missingJsonNamesTheJsonFile() throws IOException {
        Files.writeString(dir.resolve(FOO_KEYS), RIGHT_KEY);

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, FOO));

        assertTrue(e.getMessage().contains(FOO_JSON), e.getMessage());
    }

    @Test
    void missingKeysNamesTheKeysFile() throws IOException {
        Files.writeString(dir.resolve(FOO_JSON), "{\"expect\":[]}");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, FOO));

        assertTrue(e.getMessage().contains(FOO_KEYS), e.getMessage());
    }

    @Test
    void anUnknownKeyFailsAtLoadTimeNamingFileAndLine() throws IOException {
        Files.writeString(dir.resolve(FOO_KEYS), "RIGHT\nNOT_A_KEY\n");
        Files.writeString(dir.resolve(FOO_JSON), "{\"expect\":[]}");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, FOO));

        assertTrue(e.getMessage().contains(FOO_KEYS + ":2"), e.getMessage());
        assertTrue(e.getMessage().contains("NOT_A_KEY"), e.getMessage());
    }

    @Test
    void aJsonFileWithoutExpectFails() throws IOException {
        Files.writeString(dir.resolve(FOO_KEYS), RIGHT_KEY);
        Files.writeString(dir.resolve(FOO_JSON), "{}");

        assertThrows(QaException.class, () -> QaProcedure.load(dir, FOO));
    }

    @Test
    void aScriptPropertyPointsAtAnotherKeysFile() throws IOException {
        Files.writeString(dir.resolve("shared.keys"), "DOWN\n");
        Files.writeString(dir.resolve(FOO_KEYS), RIGHT_KEY);
        Files.writeString(dir.resolve(FOO_JSON), "{\"script\":\"shared.keys\",\"expect\":[]}");

        assertEquals(List.of(KeyEvent.VK_DOWN), QaProcedure.load(dir, FOO).keys());
    }

    @Test
    void anExpectEntryThatIsNotAnObjectFails() throws IOException {
        Files.writeString(dir.resolve(FOO_KEYS), RIGHT_KEY);
        Files.writeString(dir.resolve(FOO_JSON), "{\"expect\":[1]}");

        QaException e = assertThrows(QaException.class, () -> QaProcedure.load(dir, FOO));

        assertTrue(e.getMessage().contains("must be an object"), e.getMessage());
    }
}
