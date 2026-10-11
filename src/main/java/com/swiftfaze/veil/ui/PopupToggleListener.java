package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.game.event.GameEvent;
import com.swiftfaze.veil.game.event.GameEventLog;

/**
 * Minimal stopgap wiring so the I/X toggles keep working with EastPanel gone —
 * no sidebar, no player-info display, just open/close/mutual-exclusion between
 * the inventory and codex popups, same behavior EastPanel used to provide.
 *
 * <p>Each popup holds its id in the shared {@link WorldPause} when it opens and
 * releases it on any dismiss, so the world stays paused while either is open.
 */
public class PopupToggleListener implements GameListener {
    static final String INVENTORY = "inventory";

    private final InventoryPanel inventoryPanel;
    private final CodexPanel codexPanel;
    private final WorldPause worldPause;
    private final GameEventLog eventLog;

    public PopupToggleListener(InventoryPanel inventoryPanel, CodexPanel codexPanel,
                               WorldPause worldPause, GameEventLog eventLog) {
        this.inventoryPanel = inventoryPanel;
        this.codexPanel = codexPanel;
        this.worldPause = worldPause;
        this.eventLog = eventLog;
        inventoryPanel.addDismissListener(() -> worldPause.release(WorldPause.INVENTORY));
        codexPanel.addDismissListener(() -> worldPause.release(WorldPause.CODEX));
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now — removed with the rest of the early UI shell.
    }

    @Override
    public void toggleInventory() {
        if (modalPopupOpen()) {
            return;
        }
        if (inventoryPanel.isVisible()) {
            inventoryPanel.dismiss();
            eventLog.recordEvent(GameEvent.popupToggled(INVENTORY, /* open= */ false));
        } else {
            if (codexPanel.isVisible()) {
                codexPanel.dismiss();
            }
            worldPause.hold(WorldPause.INVENTORY);
            inventoryPanel.open();
            eventLog.recordEvent(GameEvent.popupToggled(INVENTORY, /* open= */ true));
        }
    }

    @Override
    public void toggleCodex() {
        if (modalPopupOpen()) {
            return;
        }
        if (codexPanel.isVisible()) {
            codexPanel.dismiss();
        } else {
            if (inventoryPanel.isVisible()) {
                inventoryPanel.dismiss();
            }
            worldPause.hold(WorldPause.CODEX);
            codexPanel.open();
        }
    }

    /** The dialogue and chest popups take their keys from the focused panel, so I and X must not open over them. */
    private boolean modalPopupOpen() {
        return worldPause.isHeldBy(WorldPause.DIALOGUE) || worldPause.isHeldBy(WorldPause.CHEST);
    }
}
