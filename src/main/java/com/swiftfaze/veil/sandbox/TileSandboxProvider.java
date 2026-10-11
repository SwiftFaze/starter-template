package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.mods.ModLoader;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

/**
 * Exposes every mod-loaded tile as its own searchable dev-console entry -
 * held as {@code Inspectable}s rather than directly importing
 * {@code com.swiftfaze.veil.world.Tile} to avoid a sandbox → world import
 * edge that would add a new frozen ArchUnit cycle path.
 */
public class TileSandboxProvider implements DevConsoleProvider {

    private static final String CATEGORY = "Tiles";
    private final List<? extends Inspectable> tiles;

    public TileSandboxProvider() {
        this(ModLoader.load(Paths.get("mods")).getAllTiles());
    }

    public TileSandboxProvider(Collection<? extends Inspectable> tiles) {
        this.tiles = List.copyOf(tiles);
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return tiles.stream()
                .map(tile -> {
                    String id = tile.getName();
                    return new DevConsoleEntry(namespaceOf(id), id, CATEGORY, localNameOf(id));
                })
                .toList();
    }

    @Override
    public JComponent createPanel(String id) {
        Inspectable tile = tiles.stream()
                .filter(t -> t.getName().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown tile id: " + id));

        return new InspectableDetailPanel(tile.getName(), tile);
    }

    private static String namespaceOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(0, colon) : id;
    }

    private static String localNameOf(String id) {
        int colon = id.indexOf(':');
        return colon >= 0 ? id.substring(colon + 1) : id;
    }
}
