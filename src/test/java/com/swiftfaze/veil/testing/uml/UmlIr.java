package com.swiftfaze.veil.testing.uml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Renders a {@link UmlGraph} as uml-viewer's hierarchical IR: {@code :classes} with
 * {@code :ns} set to the fully qualified name, {@code :edges} of kind {@code :dependency}
 * or {@code :implements}, and the policy's {@code :levels}. The viewer reads levels from the
 * IR (not the policy), and only colours a dependency red when the IR's edge carries
 * {@code :violating} and each class its {@code :level}, so both are stamped here.
 */
final class UmlIr {

    private UmlIr() {
    }

    static String render(UmlGraph graph, List<List<String>> levels) {
        List<String> lines = new ArrayList<>();
        lines.add("{:hierarchical true");
        lines.add(" :title \"Veil\"");
        lines.add(" :prefix \"" + UmlGraph.ROOT_PACKAGE + "\"");
        lines.add(" :edge-kinds {}");
        lines.add(" :omit-edges []");
        lines.add(" :omit []");
        lines.add(" :levels [" + levels.stream().map(UmlIr::keywords).collect(Collectors.joining(" ")) + "]");
        lines.add(" :order " + keywords(order(levels)));
        lines.add(" :classes [");
        for (UmlGraph.Node node : graph.classes()) {
            lines.add("  " + classEntry(node, levels));
        }
        lines.add(" ]");
        lines.add(" :edges [");
        for (UmlGraph.Edge edge : graph.edges()) {
            lines.add("  " + edgeEntry(edge, levels));
        }
        lines.add(" ]}");
        return String.join("\n", lines) + "\n";
    }

    private static String classEntry(UmlGraph.Node node, List<List<String>> levels) {
        int rank = rank(node.id(), levels);
        return "{:id :" + node.id() + " :name \"" + node.name() + "\" :ns \"" + node.fqcn() + '"'
                + (node.isInterface() ? " :stereotype :interface" : "")
                + (rank >= 0 ? " :level " + rank : "") + "}";
    }

    private static String edgeEntry(UmlGraph.Edge edge, List<List<String>> levels) {
        int fromRank = rank(edge.from(), levels);
        int toRank = rank(edge.to(), levels);
        boolean violating = "dependency".equals(edge.kind()) && fromRank >= 0 && toRank >= 0 && fromRank < toRank;
        return "{:from :" + edge.from() + " :to :" + edge.to() + " :kind :" + edge.kind()
                + (violating ? " :violating true" : "") + "}";
    }

    /** Rank of the id's top-level segment (its first dotted part), or -1 when unranked. */
    static int rank(String id, List<List<String>> levels) {
        String top = id.split("\\.", -1)[0];
        for (int rank = 0; rank < levels.size(); rank++) {
            if (levels.get(rank).contains(top)) {
                return rank;
            }
        }
        return -1;
    }

    private static List<String> order(List<List<String>> levels) {
        List<String> outerFirst = new ArrayList<>();
        for (int i = levels.size() - 1; i >= 0; i--) {
            outerFirst.addAll(levels.get(i));
        }
        return outerFirst;
    }

    private static String keywords(List<String> names) {
        return names.stream().map(name -> ":" + name).collect(Collectors.joining(" ", "[", "]"));
    }
}
