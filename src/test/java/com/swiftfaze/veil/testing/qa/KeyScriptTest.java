package com.swiftfaze.veil.testing.qa;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KeyScriptTest {

    @Test
    void resolvesKeyNamesToVirtualKeyCodes() {
        assertEquals(List.of(KeyEvent.VK_ENTER, KeyEvent.VK_RIGHT, KeyEvent.VK_I, KeyEvent.VK_ESCAPE),
                KeyScript.parse("a.keys", List.of("ENTER", "RIGHT", "I", "ESCAPE")));
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        assertEquals(List.of(KeyEvent.VK_DOWN, KeyEvent.VK_ENTER),
                KeyScript.parse("a.keys", List.of("# a comment", "", "DOWN  # trailing", "   ", "ENTER")));
    }

    @Test
    void anUnknownKeyNamesTheFileTheLineAndTheKey() {
        QaException e = assertThrows(QaException.class,
                () -> KeyScript.parse("specs/qa/x.keys", List.of("ENTER", "", "NOT_A_KEY")));

        assertEquals("specs/qa/x.keys:3: unknown key 'NOT_A_KEY'", e.getMessage());
    }
}
