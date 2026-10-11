package com.swiftfaze.veil.mods;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.swiftfaze.veil.exceptions.ModLoadException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration test that validates all mod content files under mods/core/** against
 * their corresponding JSON schemas. This ensures that the schema definitions and the
 * actual shipped content remain in sync — if the schema is updated, the mods/core
 * files must also be updated to match, or this test will fail.
 */
@DisplayName("Shipped mods/core/** must validate against schema definitions")
class ModSchemaValidationIt {

    @Test
    void allCoreModFilesValidateAgainstSchemas() throws IOException {
        Path modsRoot = Paths.get("mods").toAbsolutePath();
        assertTrue(Files.isDirectory(modsRoot), "mods/ directory must exist");

        Path coreModDir = modsRoot.resolve("core");
        assertTrue(Files.isDirectory(coreModDir), "mods/core/ directory must exist");

        List<String> errors = new ArrayList<>();

        // Validate mod.json
        validateFile(coreModDir.resolve("mod.json"), "mod", errors);

        // Validate stats.json if it exists
        Path statsFile = coreModDir.resolve("stats.json");
        if (Files.exists(statsFile)) {
            validateFile(statsFile, "stats", errors);
        }

        // Validate all tiles
        validateDirectory(coreModDir.resolve("tiles"), "tile", errors);

        // Validate all buildings
        validateDirectory(coreModDir.resolve("buildings"), "building", errors);

        // Validate all classes
        validateDirectory(coreModDir.resolve("classes"), "class", errors);

        // Validate all items
        validateDirectory(coreModDir.resolve("items"), "item", errors);

        // Validate all quests
        validateDirectory(coreModDir.resolve("quests"), "quest", errors);

        // Validate all NPCs
        validateDirectory(coreModDir.resolve("npcs"), "npc", errors);

        // Validate all dialogues
        validateDirectory(coreModDir.resolve("dialogues"), "dialogue", errors);

        // Validate all themes
        validateDirectory(coreModDir.resolve("themes"), "theme", errors);

        if (!errors.isEmpty()) {
            fail("Schema validation failed for mods/core/** files:\n" + String.join("\n", errors));
        }
    }

    private static void validateFile(Path file, String schemaType, List<String> errors) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            var json = JsonParser.parseString(Files.readString(file));
            ModSchemaValidator.validate(schemaType, json, file);
        } catch (ModLoadException | IOException | JsonSyntaxException e) {
            errors.add("  " + file + ": " + e.getMessage());
        }
    }

    private static void validateDirectory(Path dir, String schemaType, List<String> errors) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".json"))
                    .forEach(f -> validateFile(f, schemaType, errors));
        }
    }
}
