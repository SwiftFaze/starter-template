package com.swiftfaze.veil.entities.dialogue;

import java.util.List;

/** One NPC line and the choices the player can pick after it. */
public record DialogueNode(String text, List<DialogueChoice> choices) {

    public DialogueNode {
        choices = List.copyOf(choices);
    }
}
