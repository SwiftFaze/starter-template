package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.world.object.Chest;

public interface GameListener {
    void updatePlayer(Player player);

    default void toggleInventory() {
    }

    default void toggleCodex() {
    }

    default void toggleDevConsole() {
    }

    default void togglePause() {
    }

    /** The player interacted with a chest and its contents should be shown. */
    default void openChest(Chest chest) {
        // Most listeners have no chest UI.
    }

    /**
     * The player started talking to an NPC. The conversation is built by the engine (the popup
     * cannot construct engine classes), and it always starts from its dialogue's start node.
     */
    default void openDialogue(Conversation conversation) {
        // Most listeners have no dialogue UI.
    }
}
