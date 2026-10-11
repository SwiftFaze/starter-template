package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.world.object.Chest;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestPanelListenerTest {

    private static final Chest EMPTY_CHEST = new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(1, List.of()));

    @Test
    void openingAChestPausesTheWorld() {
        WorldPause worldPause = new WorldPause();
        ChestPanel chestPanel = new ChestPanel(new ControlsHintBarWidget()::setHints);
        ChestPanelListener listener = new ChestPanelListener(chestPanel, worldPause);

        listener.openChest(EMPTY_CHEST);

        assertTrue(chestPanel.isVisible());
        assertTrue(worldPause.isPaused());
    }

    @Test
    void dismissingTheChestPanelResumesTheWorld() {
        WorldPause worldPause = new WorldPause();
        ChestPanel chestPanel = new ChestPanel(new ControlsHintBarWidget()::setHints);
        ChestPanelListener listener = new ChestPanelListener(chestPanel, worldPause);
        listener.openChest(EMPTY_CHEST);

        chestPanel.dismiss();

        assertFalse(worldPause.isPaused());
    }
}
