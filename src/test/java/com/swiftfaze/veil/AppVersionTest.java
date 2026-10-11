package com.swiftfaze.veil;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AppVersionTest {
    private static final String VERSION_PROPERTIES = "version=1.2.3\n";

    @Test
    void readsVersionFromProperties() {
        String properties = "version=0.5.0-beta.39\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
        assertEquals("v0.5.0-beta.39", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenResourceIsAbsent() {
        AppVersion version = new AppVersion(() -> null);
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionKeyIsMissing() {
        String properties = "other.key=value\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionIsUnfiltered() {
        String properties = "version=${project.version}\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void trimsWhitespaceFromVersion() {
        String properties = "version=  0.5.0-beta.39  \n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
        assertEquals("v0.5.0-beta.39", version.getDisplayVersion());
    }

    @Test
    void returnsEmptyStringWhenVersionIsEmpty() {
        String properties = "version=\n";
        AppVersion version = new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
        assertEquals("", version.getDisplayVersion());
    }

    @Test
    void versionsWithTheSameDisplayStringAreEqual() {
        AppVersion first = fromProperties(VERSION_PROPERTIES);
        AppVersion second = fromProperties(VERSION_PROPERTIES);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void versionsWithDifferentDisplayStringsAreNotEqual() {
        assertNotEquals(fromProperties(VERSION_PROPERTIES), fromProperties("version=1.2.4\n"));
    }

    @Test
    void aVersionIsNotEqualToNullOrAnotherType() {
        AppVersion version = fromProperties(VERSION_PROPERTIES);
        assertNotEquals(null, version);
        assertNotEquals("v1.2.3", version);
    }

    @Test
    void stringFormIsTheDisplayVersion() {
        assertEquals("v1.2.3", fromProperties(VERSION_PROPERTIES).toString());
    }

    private static AppVersion fromProperties(String properties) {
        return new AppVersion(() -> new ByteArrayInputStream(properties.getBytes(StandardCharsets.UTF_8)));
    }
}
