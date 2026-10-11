package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GameListener;
import com.swiftfaze.veil.game.WorldPause;

import java.util.function.Consumer;

/**
 * Shows the dialogue popup when the player starts talking to an NPC. Like the chest panel, the
 * popup holds its id in the shared {@link WorldPause} while open, and any dismiss releases it, so
 * the NPC stops patrolling while the conversation is on screen.
 */
public class DialoguePanelListener implements GameListener {
    private final Consumer<Conversation> show;
    private final Runnable holdWorld;

    public DialoguePanelListener(DialoguePanel dialoguePanel, WorldPause worldPause) {
        this.show = dialoguePanel::show;
        this.holdWorld = () -> worldPause.hold(WorldPause.DIALOGUE);
        dialoguePanel.addDismissListener(() -> worldPause.release(WorldPause.DIALOGUE));
    }

    @Override
    public void updatePlayer(Player player) {
        // No player-info display right now, same as ChestPanelListener.
    }

    @Override
    public void openDialogue(Conversation conversation) {
        holdWorld.run();
        show.accept(conversation);
    }
}
