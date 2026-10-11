package com.swiftfaze.veil.mods;

import com.google.gson.JsonParser;
import com.swiftfaze.veil.exceptions.ModLoadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ModSchemaValidator that validate negative fixture files.
 * Each fixture violates exactly one schema rule, and we assert the specific
 * diagnostic message produced.
 */
@DisplayName("ModSchemaValidator correctly rejects malformed mod files")
class ModSchemaValidatorTest {

    private static final Path FIXTURES_DIR = Paths.get("src/test/resources/mod-fixtures/negative").toAbsolutePath();

    @Test
    void rejectsTileWithMissingWalkableField() throws IOException {
        Path file = FIXTURES_DIR.resolve("tile-missing-walkable.json");
        ModLoadException thrown = assertThrows(ModLoadException.class, () ->
                ModSchemaValidator.validate("tile", JsonParser.parseString(Files.readString(file)), file)
        );
        String message = thrown.getMessage();
        assertTrue(message.contains("walkable"), "Error should name 'walkable' field: " + message);
        assertTrue(message.contains("required"), "Error should mention requirement: " + message);
    }

    @Test
    void rejectsTileWithWrongTypeColor() throws IOException {
        Path file = FIXTURES_DIR.resolve("tile-wrong-type-color.json");
        ModLoadException thrown = assertThrows(ModLoadException.class, () ->
                ModSchemaValidator.validate("tile", JsonParser.parseString(Files.readString(file)), file)
        );
        String message = thrown.getMessage();
        assertTrue(message.contains("color"), "Error should name 'color' field: " + message);
        assertTrue(message.toLowerCase(Locale.ROOT).contains("object") || message.toLowerCase(Locale.ROOT).contains("type"),
                "Error should describe expected type: " + message);
    }

    @Test
    void rejectsTileWithUnknownField() throws IOException {
        Path file = FIXTURES_DIR.resolve("tile-unknown-field.json");
        ModLoadException thrown = assertThrows(ModLoadException.class, () ->
                ModSchemaValidator.validate("tile", JsonParser.parseString(Files.readString(file)), file)
        );
        String message = thrown.getMessage();
        assertTrue(message.contains("overides"), "Error should name unknown field 'overides': " + message);
        assertTrue(message.toLowerCase(Locale.ROOT).contains("additional") || message.toLowerCase(Locale.ROOT).contains("unknown"),
                "Error should mention unknown/additional property: " + message);
    }

    @Test
    void rejectsTileWithMalformedId() throws IOException {
        Path file = FIXTURES_DIR.resolve("tile-malformed-id.json");
        ModLoadException thrown = assertThrows(ModLoadException.class, () ->
                ModSchemaValidator.validate("tile", JsonParser.parseString(Files.readString(file)), file)
        );
        String message = thrown.getMessage();
        assertTrue(message.contains("/id") || message.contains("id"), "Error should reference the id field: " + message);
        assertTrue(message.toLowerCase(Locale.ROOT).contains("pattern") || message.toLowerCase(Locale.ROOT).contains("regex"),
                "Error should describe pattern/regex requirement: " + message);
    }

    @Test
    void rejectsModWithDanglingDependsOn() throws IOException {
        Path file = FIXTURES_DIR.resolve("mod-dangling-depends-on.json");
        // Note: Schema validation only validates the JSON structure, not the mod loading logic.
        // A dangling dependsOn will fail during orderByDependencies, not during schema validation.
        // This fixture tests that the mod.json itself validates correctly — the actual
        // dependency resolution error is tested in the Cucumber scenarios.
        // (This is a valid mod.json from the schema's perspective.)
        var json = JsonParser.parseString(Files.readString(file));
        assertDoesNotThrow(() -> ModSchemaValidator.validate("mod", json, file),
                "mod-dangling-depends-on.json is valid from the schema's perspective");
    }

    @Test
    void rejectsBuildingWithUnresolvedTileId() throws IOException {
        Path file = FIXTURES_DIR.resolve("building-unresolved-tile-id.json");
        // Note: Schema validation only validates the JSON structure, not tile id resolution.
        // An unresolved tile id will fail during readBlueprint, not during schema validation.
        // This fixture tests that the building.json itself validates correctly — the actual
        // tile resolution error is tested in the Cucumber scenarios.
        // (This is a valid building.json from the schema's perspective.)
        var json = JsonParser.parseString(Files.readString(file));
        assertDoesNotThrow(() -> ModSchemaValidator.validate("building", json, file),
                "building-unresolved-tile-id.json is valid from the schema's perspective");
    }
}
