package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.world.KitchenSinkScene;
import com.swiftfaze.veil.world.WorldScene;

import javax.swing.JComponent;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

/**
 * Exposes a single curated kitchen-sink scene containing every tile from
 * ModRegistry.getAllTiles(), with a live preview, movement controls, and
 * a toggleable walkability overlay for testing tile behavior.
 */
public class KitchenSinkProvider implements DevConsoleProvider {

    private static final String NAMESPACE = "sandbox";
    private static final String ENTRY_ID = "sandbox:kitchen-sink";
    private static final String CATEGORY = "Scenes";
    private static final String NAME = "Kitchen Sink";
    private final Supplier<WorldScene> sceneSupplier;

    public KitchenSinkProvider() {
        this(() -> KitchenSinkScene.holding(ModLoader.load(Paths.get("mods")).getAllTiles()));
    }

    public KitchenSinkProvider(Supplier<WorldScene> sceneSupplier) {
        this.sceneSupplier = sceneSupplier;
    }

    @Override
    public List<DevConsoleEntry> entries() {
        return List.of(new DevConsoleEntry(NAMESPACE, ENTRY_ID, CATEGORY, NAME));
    }

    @Override
    public JComponent createPanel(String id) {
        return new KitchenSinkPreviewPanel(sceneSupplier.get());
    }
}
