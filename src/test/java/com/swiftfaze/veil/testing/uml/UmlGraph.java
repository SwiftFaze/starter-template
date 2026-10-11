package com.swiftfaze.veil.testing.uml;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The Veil-only class graph the viewer draws. Nested and anonymous classes fold into their
 * top-level class (as crap.edn keys by class), and nothing outside {@link #ROOT_PACKAGE}
 * appears.
 */
record UmlGraph(List<Node> classes, List<Edge> edges) {

    static final String ROOT_PACKAGE = "com.swiftfaze.veil";

    private static final Pattern CAMEL_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");
    private static final Comparator<Node> NODE_ORDER = Comparator.comparing(Node::id);
    private static final Comparator<Edge> EDGE_ORDER = Comparator.comparing(Edge::from).thenComparing(Edge::to);

    UmlGraph {
        classes = List.copyOf(classes);
        edges = List.copyOf(edges);
    }

    /** One top-level class: {@code id} is the viewer id (kebab-case path below the root package). */
    record Node(String id, String name, String fqcn, boolean isInterface) {
    }

    record Edge(String from, String to, String kind) {
    }

    static UmlGraph fromClasses(Collection<JavaClass> imported) {
        Map<String, Node> nodes = new LinkedHashMap<>();
        for (JavaClass javaClass : imported) {
            if (isExported(javaClass) && javaClass.getEnclosingClass().isEmpty()) {
                nodes.put(javaClass.getName(), node(javaClass));
            }
        }
        Map<String, Edge> edges = new LinkedHashMap<>();
        for (JavaClass javaClass : imported) {
            if (nodes.containsKey(topLevel(javaClass).getName())) {
                collectEdges(javaClass, nodes, edges);
            }
        }
        List<Node> sortedNodes = new ArrayList<>(nodes.values());
        sortedNodes.sort(NODE_ORDER);
        List<Edge> sortedEdges = new ArrayList<>(edges.values());
        sortedEdges.sort(EDGE_ORDER);
        return new UmlGraph(sortedNodes, sortedEdges);
    }

    private static JavaClass topLevel(JavaClass javaClass) {
        JavaClass current = javaClass;
        while (current.getEnclosingClass().isPresent()) {
            current = current.getEnclosingClass().get();
        }
        return current;
    }

    private static boolean isExported(JavaClass javaClass) {
        String name = javaClass.getName();
        return name.startsWith(ROOT_PACKAGE + ".") && !name.endsWith(".package-info");
    }

    private static Node node(JavaClass javaClass) {
        String relative = javaClass.getName().substring(ROOT_PACKAGE.length() + 1);
        int lastDot = relative.lastIndexOf('.');
        String packagePrefix = lastDot < 0 ? "" : relative.substring(0, lastDot + 1);
        return new Node(packagePrefix + kebab(javaClass.getSimpleName()), javaClass.getSimpleName(),
                javaClass.getName(), javaClass.isInterface());
    }

    static String kebab(String simpleName) {
        return CAMEL_BOUNDARY.matcher(simpleName).replaceAll("$1-$2").toLowerCase(Locale.ROOT);
    }

    private static void collectEdges(JavaClass source, Map<String, Node> nodes, Map<String, Edge> edges) {
        Node from = nodes.get(topLevel(source).getName());
        Set<String> implemented = new TreeSet<>();
        for (JavaClass anInterface : source.getRawInterfaces()) {
            implemented.add(topLevel(anInterface).getName());
        }
        for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
            String target = topLevel(dependency.getTargetClass().getBaseComponentType()).getName();
            addEdge(nodes, edges, from, target, implemented.contains(target) ? "implements" : "dependency");
        }
        for (String target : implemented) {
            addEdge(nodes, edges, from, target, "implements");
        }
    }

    private static void addEdge(Map<String, Node> nodes, Map<String, Edge> edges, Node from, String target,
                                String kind) {
        Node destination = nodes.get(target);
        if (destination == null || destination.equals(from)) {
            return;
        }
        String key = from.id() + ">" + destination.id();
        Edge existing = edges.get(key);
        if (existing == null || "dependency".equals(existing.kind())) {
            edges.put(key, new Edge(from.id(), destination.id(), kind));
        }
    }
}
