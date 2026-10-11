package com.swiftfaze.veil.mods;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetColorThemeTest {

    @Test
    void requiredKeysHasExactlyThirteenEntries() {
        assertEquals(13, WidgetColorTheme.REQUIRED_KEYS.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECTED_HIGHLIGHT", "SELECTED_TEXT", "NORMAL_TEXT", "DIMMED_TEXT", "BACKGROUND",
            "INVALID_HIGHLIGHT", "VALID_HIGHLIGHT", "TABLE_HEADER_BACKGROUND", "BORDER",
            "SCROLLBAR_THUMB", "ACCENT", "WINDOW_BORDER", "TABLE_HEADER_TEXT"
    })
    void requiredKeysContains(String key) {
        assertTrue(WidgetColorTheme.REQUIRED_KEYS.contains(key), "Missing required key: " + key);
    }

    @Test
    void colorReturnsTheValueRegisteredForItsKey() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("BACKGROUND", new Color(1, 2, 3));

        WidgetColorTheme theme = new WidgetColorTheme("test:theme", colors);

        assertEquals("test:theme", theme.id());
        assertEquals(new Color(1, 2, 3), theme.color("BACKGROUND"));
    }

    @Test
    void colorReturnsNullForAnUnregisteredKey() {
        WidgetColorTheme theme = new WidgetColorTheme("test:theme", Map.of());

        assertNull(theme.color("BACKGROUND"));
    }

    @Test
    void constructorCopiesTheSuppliedMapSoLaterMutationDoesNotLeakIn() {
        Map<String, Color> colors = new LinkedHashMap<>();
        colors.put("BACKGROUND", new Color(1, 2, 3));
        WidgetColorTheme theme = new WidgetColorTheme("test:theme", colors);

        colors.put("BACKGROUND", new Color(9, 9, 9));

        assertEquals(new Color(1, 2, 3), theme.color("BACKGROUND"));
    }
}
