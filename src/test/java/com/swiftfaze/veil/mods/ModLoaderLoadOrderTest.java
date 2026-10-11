package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.world.Tile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModLoaderLoadOrderTest {

    @TempDir
    Path modsRoot;

    @Test
    void tilesLoadInFileNameOrderWhateverOrderTheFilesWereWrittenIn() throws IOException {
        Path modDir = Files.createDirectories(modsRoot.resolve("test"));
        Files.writeString(modDir.resolve("mod.json"), "{\"id\":\"test\",\"dependsOn\":[]}");
        Path tilesDir = Files.createDirectories(modDir.resolve("tiles"));
        // Reverse alphabetical on purpose: directory listings are unordered, so the loader must sort.
        for (String name : List.of("c", "b", "a")) {
            Files.writeString(tilesDir.resolve(name + ".json"),
                    "{\"id\":\"test:" + name + "\",\"symbol\":\"" + name + "\","
                            + "\"color\":{\"r\":1,\"g\":2,\"b\":3},\"walkable\":true}");
        }

        List<String> loaded = ModLoader.load(modsRoot).getAllTiles().stream().map(Tile::getId).toList();

        assertEquals(List.of("test:a", "test:b", "test:c"), loaded);
    }
}
