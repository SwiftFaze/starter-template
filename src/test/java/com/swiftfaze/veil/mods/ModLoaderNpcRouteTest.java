package com.swiftfaze.veil.mods;

import com.swiftfaze.veil.exceptions.ModLoadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModLoaderNpcRouteTest {

    @TempDir
    Path modsRoot;

    @Test
    void routeThatDoesNotStartAtSpawnNamesBothTilesInTheError() throws IOException {
        writeNpc("{\"x\":3,\"y\":4}", "{\"x\":7,\"y\":4},{\"x\":9,\"y\":4}");

        ModLoadException thrown = assertThrows(ModLoadException.class, () -> ModLoader.load(modsRoot));

        assertTrue(thrown.getMessage().contains("spawn (3, 4)"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("first waypoint is (7, 4)"), thrown.getMessage());
    }

    @Test
    void diagonalLegNamesBothEndsOfTheLegInTheError() throws IOException {
        writeNpc("{\"x\":3,\"y\":4}", "{\"x\":3,\"y\":4},{\"x\":5,\"y\":6}");

        ModLoadException thrown = assertThrows(ModLoadException.class, () -> ModLoader.load(modsRoot));

        assertTrue(thrown.getMessage().contains("from (3, 4) to (5, 6)"), thrown.getMessage());
    }

    private void writeNpc(String spawn, String waypoints) throws IOException {
        Path coreDir = modsRoot.resolve("core");
        Path npcsDir = Files.createDirectories(coreDir.resolve("npcs"));
        Files.writeString(coreDir.resolve("mod.json"), "{\"id\":\"core\",\"dependsOn\":[]}");
        Files.writeString(npcsDir.resolve("walker.json"),
                "{\"id\":\"core:walker\",\"name\":\"Walker\",\"symbol\":\"W\","
                        + "\"color\":{\"r\":1,\"g\":2,\"b\":3},\"spawn\":" + spawn
                        + ",\"patrol\":{\"waypoints\":[" + waypoints + "]}}");
    }
}
