package com.swiftfaze.veil.entities.dialogue;

import org.jspecify.annotations.Nullable;

/**
 * A line the player may say. {@code next} is the node it leads to, or null when choosing it
 * ends the conversation.
 */
public record DialogueChoice(String text, @Nullable String next) {

    public boolean endsConversation() {
        return next == null;
    }
}
