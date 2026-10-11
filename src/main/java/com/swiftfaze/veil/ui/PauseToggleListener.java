package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.WorldPause;

/**
 * Wires ESC to open/dismiss the pause menu overlay. Opening holds the pause menu's id
 * in the shared {@link WorldPause}, and any dismiss of the popup releases it, mirroring
 * PopupToggleListener's role for Inventory/Codex.
 */
public class PauseToggleListener implements GameListener {
    private final WorldPause worldPause;
    private final PauseMenuPopup pauseMenuPopup;

    public PauseToggleListener(WorldPause worldPause, PauseMenuPopup pauseMenuPopup) {
        this.worldPause = worldPause;
        this.pauseMenuPopup = pauseMenuPopup;
        pauseMenuPopup.addDismissListener(() -> worldPause.release(WorldPause.PAUSE_MENU));
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as PopupToggleListener.
    }

    @Override
    public void togglePause() {
        if (pauseMenuPopup.isVisible()) {
            pauseMenuPopup.dismiss();
        } else {
            worldPause.hold(WorldPause.PAUSE_MENU);
            pauseMenuPopup.open();
        }
    }
}
