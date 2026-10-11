package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A throwaway mods root holding a copy of core and, optionally, the {@code test:hut} blueprint.
 * Created by {@link #load(Setup)}, removed by {@link #delete()}.
 */
final class TempModsRoot {

    /** Which blueprints the temporary root offers. */
    enum Setup { WITH_HUT, NO_BUILDINGS }

    private static final String HUT_BUILDING = """
            {
              "id": "test:hut",
              "name": "hut",
              "type": "house",
              "width": 3,
              "height": 3,
              "tiles": [
                ["core:stone", "core:stone", "core:stone"],
                ["core:stone", "core:wood", "core:stone"],
                ["core:stone", "core:wood", "core:stone"]
              ],
              "objects": [{"id": "core:wooden_door", "x": 1, "y": 2}]
            }
            """;

    private Path root;

    /** Copies core (without its buildings for {@link Setup#NO_BUILDINGS}) and loads the result. */
    ModRegistry load(Setup setup) throws IOException {
        root = Files.createTempDirectory("veil-seeded-mods");
        Path core = root.resolve("core");
        Path source = Paths.get("mods", "core");
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                if (setup == Setup.NO_BUILDINGS && relative.startsWith("buildings")) {
                    continue;
                }
                copy(path, core.resolve(relative.toString()));
            }
        }
        if (setup == Setup.WITH_HUT) {
            Path mod = root.resolve("test");
            Files.createDirectories(mod.resolve("buildings"));
            Files.writeString(mod.resolve("mod.json"), "{\"id\": \"test\", \"dependsOn\": [\"core\"]}");
            Files.writeString(mod.resolve("buildings").resolve("hut.json"), HUT_BUILDING);
        }
        return ModLoader.load(root);
    }

    /** Removes the root, if one was created. */
    void delete() throws IOException {
        if (root == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(TempModsRoot::deleteQuietly);
        }
    }

    private static void copy(Path from, Path target) throws IOException {
        if (Files.isDirectory(from)) {
            Files.createDirectories(target);
        } else {
            Files.createDirectories(target.getParent());
            Files.copy(from, target);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
