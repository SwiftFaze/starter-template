package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.game.WorldPause;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialoguePanelListenerTest {

    private static final Dialogue TALK = new Dialogue("test:talk", "hello", Map.of(
            "hello", new DialogueNode("Hi.", List.of(new DialogueChoice("Bye.", null)))));

    @Test
    void openingAConversationPausesTheWorld() {
        WorldPause worldPause = new WorldPause();
        DialoguePanel panel = new DialoguePanel(new ControlsHintBarWidget()::setHints);
        DialoguePanelListener listener = new DialoguePanelListener(panel, worldPause);

        listener.openDialogue(new Conversation("Mira", TALK));

        assertTrue(panel.isVisible());
        assertTrue(worldPause.isPaused());
    }

    @Test
    void endingTheConversationResumesTheWorld() {
        WorldPause worldPause = new WorldPause();
        DialoguePanel panel = new DialoguePanel(new ControlsHintBarWidget()::setHints);
        DialoguePanelListener listener = new DialoguePanelListener(panel, worldPause);
        listener.openDialogue(new Conversation("Mira", TALK));

        panel.chooseNumber(1);

        assertFalse(panel.isVisible());
        assertFalse(worldPause.isPaused());
    }

    @Test
    void closingTheConversationKeepsAnotherScreensPauseInPlace() {
        WorldPause worldPause = new WorldPause();
        DialoguePanel panel = new DialoguePanel(new ControlsHintBarWidget()::setHints);
        DialoguePanelListener listener = new DialoguePanelListener(panel, worldPause);
        listener.openDialogue(new Conversation("Mira", TALK));
        worldPause.hold(WorldPause.INVENTORY);

        panel.dismiss();

        assertTrue(worldPause.isPaused());
    }
}
