package com.swiftfaze.veil.steps;

import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.entities.dialogue.DialogueChoice;
import com.swiftfaze.veil.entities.dialogue.DialogueNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph queries over a loaded {@link Dialogue}: how far it branches, its longest branch, and the
 * choice numbers that lead from one node to another. Used by the dialogue scenarios to check the
 * placeholder tree and to play a conversation through the keyboard.
 */
final class DialogueGraph {

    private DialogueGraph() {
    }

    /** Node ids on the longest simple route from the start node, start included. A loop back ends a route. */
    static List<String> longestPath(Dialogue dialogue) {
        return longestPathFrom(dialogue, dialogue.start(), List.of());
    }

    /** The choice numbers (from 1) of the longest route from the start that ends with an ending choice. */
    static List<Integer> longestBranchToEnding(Dialogue dialogue) {
        return longestEndingFrom(dialogue, dialogue.start(), List.of());
    }

    /** The choice numbers (from 1) that lead from node {@code from} to node {@code to} by the shortest route. */
    static List<Integer> choicesBetween(Dialogue dialogue, String from, String to) {
        Map<String, String> parent = new HashMap<>();
        Map<String, Integer> pickInto = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>(List.of(from));
        Set<String> seen = new LinkedHashSet<>(List.of(from));
        while (!queue.isEmpty()) {
            String nodeId = queue.removeFirst();
            List<DialogueChoice> choices = dialogue.node(nodeId).choices();
            for (int i = 0; i < choices.size(); i++) {
                String next = choices.get(i).next();
                if (next != null && seen.add(next)) {
                    parent.put(next, nodeId);
                    pickInto.put(next, i + 1);
                    queue.addLast(next);
                }
            }
        }
        List<Integer> picks = new ArrayList<>();
        for (String at = to; !at.equals(from); at = parent.get(at)) {
            picks.add(0, pickInto.get(at));
        }
        return picks;
    }

    /** How many different nodes the node's choices lead to (ending choices do not count). */
    static long distinctNextNodes(DialogueNode node) {
        return node.choices().stream().map(DialogueChoice::next).filter(next -> next != null).distinct().count();
    }

    /** Every node id reachable from the start node, start included. */
    static Set<String> reachable(Dialogue dialogue) {
        Set<String> reached = new LinkedHashSet<>(List.of(dialogue.start()));
        Deque<String> pending = new ArrayDeque<>(List.of(dialogue.start()));
        while (!pending.isEmpty()) {
            for (DialogueChoice choice : dialogue.node(pending.removeFirst()).choices()) {
                if (choice.next() != null && reached.add(choice.next())) {
                    pending.addLast(choice.next());
                }
            }
        }
        return reached;
    }

    private static List<String> longestPathFrom(Dialogue dialogue, String nodeId, List<String> before) {
        List<String> route = new ArrayList<>(before);
        route.add(nodeId);
        List<String> best = route;
        for (DialogueChoice choice : dialogue.node(nodeId).choices()) {
            if (choice.next() != null && !route.contains(choice.next())) {
                List<String> candidate = longestPathFrom(dialogue, choice.next(), route);
                if (candidate.size() > best.size()) {
                    best = candidate;
                }
            }
        }
        return best;
    }

    /** The longest route from {@code nodeId} that ends in an ending choice, as choice numbers; empty if none. */
    private static List<Integer> longestEndingFrom(Dialogue dialogue, String nodeId, List<String> before) {
        List<String> route = new ArrayList<>(before);
        route.add(nodeId);
        List<Integer> best = List.of();
        List<DialogueChoice> choices = dialogue.node(nodeId).choices();
        for (int i = 0; i < choices.size(); i++) {
            DialogueChoice choice = choices.get(i);
            List<Integer> tail = choice.next() == null ? List.of(i + 1) : continuation(dialogue, choice.next(), route, i + 1);
            if (!tail.isEmpty() && (best.isEmpty() || tail.size() > best.size())) {
                best = tail;
            }
        }
        return best;
    }

    private static List<Integer> continuation(Dialogue dialogue, String next, List<String> route, int pick) {
        if (route.contains(next)) {
            return List.of();
        }
        List<Integer> rest = longestEndingFrom(dialogue, next, route);
        if (rest.isEmpty()) {
            return List.of();
        }
        List<Integer> picks = new ArrayList<>();
        picks.add(pick);
        picks.addAll(rest);
        return picks;
    }
}
