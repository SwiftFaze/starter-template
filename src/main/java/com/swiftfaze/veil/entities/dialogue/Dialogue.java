package com.swiftfaze.veil.entities.dialogue;

import java.util.Map;

/**
 * A branching conversation: the node a conversation opens on, and every node it can reach by
 * id. Immutable registry content. {@code ModLoader} validates the shape (see {@code DialogueReader}).
 */
public record Dialogue(String id, String start, Map<String, DialogueNode> nodes) {

    public Dialogue {
        nodes = Map.copyOf(nodes);
    }

    public DialogueNode node(String nodeId) {
        return nodes.get(nodeId);
    }
}
