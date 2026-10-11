package com.swiftfaze.veil.entities.dialogue;

import java.util.ArrayList;
import java.util.List;

/**
 * One run through a {@link Dialogue}: where the player is, and the chat log so far. Each opening
 * of a dialogue makes a new Conversation, so it always starts from the start node with an empty log.
 * Choice indices are zero-based; an invalid index changes nothing.
 */
public final class Conversation {

    private final String npcName;
    private final Dialogue dialogue;
    private final List<Line> log = new ArrayList<>();
    private DialogueNode node;
    private boolean ended;

    public Conversation(String npcName, Dialogue dialogue) {
        this.npcName = npcName;
        this.dialogue = dialogue;
        enter(dialogue.start());
    }

    public String npcName() {
        return npcName;
    }

    public DialogueNode currentNode() {
        return node;
    }

    public List<Line> log() {
        return List.copyOf(log);
    }

    public boolean isEnded() {
        return ended;
    }

    /**
     * Picks the choice at {@code index} of the current node. An ending choice is logged and ends the
     * conversation; otherwise the next node's line is logged. Returns false, and changes nothing, when
     * the index is out of range or the conversation has already ended.
     */
    public boolean choose(int index) {
        if (ended || index < 0 || index >= node.choices().size()) {
            return false;
        }
        DialogueChoice choice = node.choices().get(index);
        log.add(new Line(/* fromPlayer= */ true, "You: " + choice.text()));
        String next = choice.next();
        if (next == null) {
            ended = true;
        } else {
            enter(next);
        }
        return true;
    }

    private void enter(String nodeId) {
        node = dialogue.node(nodeId);
        log.add(new Line(/* fromPlayer= */ false, npcName + ": " + node.text()));
    }

    /** One chat-log line. {@code text} is the full display text, such as "Mira: Well met.". */
    public record Line(boolean fromPlayer, String text) {
    }
}
