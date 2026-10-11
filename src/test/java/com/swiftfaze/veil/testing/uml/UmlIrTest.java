package com.swiftfaze.veil.testing.uml;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UmlIrTest {

    private static final String POPUP = "ui.popup-toggle-listener";
    private static final String DEPENDENCY = "dependency";
    private static final String MAIN = "main";
    private static final String UI_A = "ui.a";
    private static final String LISTENER = "game.game-listener";
    private static final List<List<String>> LEVELS = List.of(List.of("game", "mods"), List.of("ui"), List.of(MAIN));

    private static final Collection<JavaClass> IMPORTED = UmlExport.productionClasses();
    private static final UmlGraph GRAPH = UmlGraph.fromClasses(IMPORTED);
    private static final String RENDERED = UmlIr.render(GRAPH, LEVELS);

    @Test
    void everyTopLevelClassAppearsOnceWithItsFullyQualifiedNameAsNs() {
        long topLevel = IMPORTED.stream()
                .filter(c -> c.getName().startsWith("com.swiftfaze.veil.") && c.getEnclosingClass().isEmpty())
                .filter(c -> !c.getName().endsWith("package-info"))
                .count();
        assertEquals(topLevel, GRAPH.classes().size());
    }

    @Test
    void everyClassHasADistinctIdAndItsFullyQualifiedNameAsNs() {
        assertEquals(GRAPH.classes().size(), GRAPH.classes().stream().map(UmlGraph.Node::id).distinct().count());
        assertTrue(RENDERED.contains("{:id :" + POPUP + " :name \"PopupToggleListener\" :ns \"com.swiftfaze.veil.ui."
                + "PopupToggleListener\" :level 1}"));
    }

    @Test
    void nestedAndAnonymousClassesAreFoldedIntoTheirOuterClass() {
        assertTrue(GRAPH.classes().stream().noneMatch(n -> n.fqcn().contains("$")));
        assertFalse(RENDERED.contains("$"));
    }

    @Test
    void interfaceImplementationBecomesAnImplementsEdge() {
        assertTrue(GRAPH.edges().contains(new UmlGraph.Edge(POPUP, LISTENER, "implements")));
        assertTrue(RENDERED.contains("{:from :" + POPUP + " :to :" + LISTENER + " :kind :implements}"));
    }

    @Test
    void anInterfaceIsMarkedWithTheInterfaceStereotype() {
        assertTrue(RENDERED.contains(":ns \"com.swiftfaze.veil.game.GameListener\" :stereotype :interface :level 0}"));
    }

    @Test
    void aReferenceBecomesADependencyEdge() {
        assertTrue(GRAPH.edges().contains(new UmlGraph.Edge(MAIN, "game.game-panel", DEPENDENCY)));
    }

    @Test
    void nothingOutsideVeilPackagesIsExported() {
        assertTrue(GRAPH.classes().stream().allMatch(n -> n.fqcn().startsWith("com.swiftfaze.veil.")));
        assertFalse(RENDERED.contains("java."));
        assertFalse(RENDERED.contains("javax."));
    }

    private static String renderPlanted() {
        UmlGraph planted = new UmlGraph(List.of(), List.of(
                new UmlGraph.Edge(UI_A, MAIN, DEPENDENCY),
                new UmlGraph.Edge(MAIN, UI_A, DEPENDENCY),
                new UmlGraph.Edge("game.g", UI_A, DEPENDENCY),
                new UmlGraph.Edge(UI_A, "game.g", "implements"),
                new UmlGraph.Edge(UI_A, "ui.b", DEPENDENCY)));
        return UmlIr.render(planted, LEVELS);
    }

    @Test
    void anInnerLayerDependingOnAnOuterOneIsMarkedViolating() {
        String rendered = renderPlanted();
        assertTrue(rendered.contains("{:from :ui.a :to :main :kind :dependency :violating true}"));
        assertTrue(rendered.contains("{:from :game.g :to :ui.a :kind :dependency :violating true}"));
    }

    @Test
    void anOuterOrSameLayerDependencyIsNotViolating() {
        String rendered = renderPlanted();
        assertTrue(rendered.contains("{:from :main :to :ui.a :kind :dependency}"));
        assertTrue(rendered.contains("{:from :ui.a :to :ui.b :kind :dependency}"));
    }

    @Test
    void anImplementsEdgeIsNeverViolating() {
        assertFalse(renderPlanted().contains("implements :violating"));
    }

    @Test
    void policyWithoutAWellFormedLevelsVectorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> UmlPolicy.levels("{:title \"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> UmlPolicy.levels("{:levels [[a b]}"));
    }

    @Test
    void levelsAreReadSkippingCommasAndWhitespace() {
        assertEquals(List.of(List.of("a", "b"), List.of("c"), List.<String>of()),
                UmlPolicy.levels("{:x 1 :levels [[a, b]\n [c] []] :y [z]}"));
    }

    @Test
    void levelsAndOrderAreEmbeddedInTheIr() {
        assertTrue(RENDERED.contains(" :levels [[:game :mods] [:ui] [:main]]"));
        assertTrue(RENDERED.contains(" :order [:main :ui :game :mods]"));
        assertTrue(RENDERED.startsWith("{:hierarchical true"));
    }

    @Test
    void kebabCasingSplitsCamelCaseWords() {
        assertEquals("popup-toggle-listener", UmlGraph.kebab("PopupToggleListener"));
        assertEquals(MAIN, UmlGraph.kebab("Main"));
    }

    @Test
    void policyFileLevelsMirrorTheArchitectureLayers() throws IOException {
        String policy = Files.readString(Path.of("docs", "uml", "veil.policy.edn"));
        assertEquals(List.of(List.of("game", "world", "mods", "entities"), List.of("ui", "sandbox"),
                List.of(MAIN)), UmlPolicy.levels(policy));
    }
}
