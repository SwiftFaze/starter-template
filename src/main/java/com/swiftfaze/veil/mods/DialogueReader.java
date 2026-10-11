package com.swiftfaze.veil.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;
import com.swiftfaze.veil.exceptions.ModLoadException;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a {@link Dialogue} from its JSON and checks what the schema cannot: the start node exists,
 * every {@code next} names a node, each node has 1 to 9 choices, text is not blank, and every node is
 * reachable from the start. Each failure names the dialogue id, the node or choice, and the file.
 */
final class DialogueReader {

    private static final int MAX_CHOICES = 9;

    private DialogueReader() {
    }

    /** The dialogue being read, for the error messages. */
    private record Source(String id, Path file) {
        ModLoadException fail(String reason) {
            return new ModLoadException("Dialogue '" + id + "': " + reason + " in file: " + file);
        }
    }

    static Dialogue read(JsonObject json, String id, Path file) {
        Source source = new Source(id, file);
        String start = json.get("start").getAsString();
        Map<String, DialogueNode> nodes = readNodes(json.getAsJsonObject("nodes"), source);
        if (!nodes.containsKey(start)) {
            throw source.fail("start node '" + start + "' does not exist");
        }
        checkNextTargets(nodes, source);
        checkReachable(start, nodes, source);
        return new Dialogue(id, start, nodes);
    }

    private static Map<String, DialogueNode> readNodes(JsonObject nodesJson, Source source) {
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        for (String nodeId : nodesJson.keySet()) {
            nodes.put(nodeId, readNode(nodeId, nodesJson.getAsJsonObject(nodeId), source));
        }
        return nodes;
    }

    private static DialogueNode readNode(String nodeId, JsonObject nodeJson, Source source) {
        String text = nodeJson.get("text").getAsString();
        if (text.isBlank()) {
            throw source.fail("node '" + nodeId + "' has empty text");
        }
        return new DialogueNode(text, readChoices(nodeId, nodeJson.getAsJsonArray("choices"), source));
    }

    private static List<DialogueChoice> readChoices(String nodeId, JsonArray choicesJson, Source source) {
        if (choicesJson.isEmpty()) {
            throw source.fail("node '" + nodeId + "' has no choices");
        }
        if (choicesJson.size() > MAX_CHOICES) {
            throw source.fail("node '" + nodeId + "' has " + choicesJson.size() + " choices; the limit is " + MAX_CHOICES);
        }
        List<DialogueChoice> choices = new ArrayList<>();
        for (int i = 0; i < choicesJson.size(); i++) {
            choices.add(readChoice(nodeId, i + 1, choicesJson.get(i).getAsJsonObject(), source));
        }
        return choices;
    }

    private static DialogueChoice readChoice(String nodeId, int number, JsonObject choiceJson, Source source) {
        String text = choiceJson.get("text").getAsString();
        if (text.isBlank()) {
            throw source.fail("node '" + nodeId + "' choice " + number + " has empty text");
        }
        String next = choiceJson.has("next") ? choiceJson.get("next").getAsString() : null;
        return new DialogueChoice(text, next);
    }

    private static void checkNextTargets(Map<String, DialogueNode> nodes, Source source) {
        for (Map.Entry<String, DialogueNode> entry : nodes.entrySet()) {
            List<DialogueChoice> choices = entry.getValue().choices();
            for (int i = 0; i < choices.size(); i++) {
                String next = choices.get(i).next();
                if (next != null && !nodes.containsKey(next)) {
                    throw source.fail("node '" + entry.getKey() + "' choice " + (i + 1)
                            + " leads to '" + next + "', which is not a node");
                }
            }
        }
    }

    private static void checkReachable(String start, Map<String, DialogueNode> nodes, Source source) {
        Set<String> reached = reachableFrom(start, nodes);
        for (String nodeId : nodes.keySet()) {
            if (!reached.contains(nodeId)) {
                throw source.fail("node '" + nodeId + "' cannot be reached from the start node");
            }
        }
    }

    /** Every node id a walk from {@code start} reaches, start included. Targets were checked to exist. */
    private static Set<String> reachableFrom(String start, Map<String, DialogueNode> nodes) {
        Set<String> reached = new HashSet<>(List.of(start));
        Deque<String> pending = new ArrayDeque<>(List.of(start));
        while (!pending.isEmpty()) {
            for (DialogueChoice choice : nodes.get(pending.removeFirst()).choices()) {
                String next = choice.next();
                if (next != null && reached.add(next)) {
                    pending.addLast(next);
                }
            }
        }
        return reached;
    }
}
