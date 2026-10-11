package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.world.object.Chest;

import java.util.function.Consumer;

/**
 * Shows the chest panel when the player interacts with a chest. Like the inventory and
 * codex, the panel holds its id in the shared {@link WorldPause} while open, and any
 * dismiss releases it, so the world is paused while a chest is showing.
 */
public class ChestPanelListener implements GameListener {
    private final Consumer<Chest> show;
    private final Runnable holdWorld;

    public ChestPanelListener(ChestPanel chestPanel, WorldPause worldPause) {
        this.show = chestPanel::show;
        this.holdWorld = () -> worldPause.hold(WorldPause.CHEST);
        chestPanel.addDismissListener(() -> worldPause.release(WorldPause.CHEST));
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as PopupToggleListener.
    }

    @Override
    public void openChest(Chest chest) {
        holdWorld.run();
        show.accept(chest);
    }
}
