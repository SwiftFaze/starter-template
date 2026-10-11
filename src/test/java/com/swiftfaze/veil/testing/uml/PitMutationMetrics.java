package com.swiftfaze.veil.testing.uml;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Stream;

/**
 * Converts PIT's {@code target/pit-reports/mutations.xml} into the per-class snapshots
 * uml-viewer's overlay reads from {@code .metrics/mutate/}: one {@code <fqcn>.edn} of
 * {@code {:namespace fqcn :forms [{:id "defn/<method>" :killed :survived :uncovered :sites}]}}.
 * The overlay keys a class by {@code :namespace} (= the class's {@code :ns}, its FQCN) and a
 * member by the {@code defn/} name (= the method name crap.edn uses), so the match is exact;
 * overloads share one entry, as they do in crap.edn. Nested classes fold into their outer class.
 */
public final class PitMutationMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(PitMutationMetrics.class);

    static final Path MUTATIONS_XML = Path.of("target", "pit-reports", "mutations.xml");
    static final Path OUTPUT_DIR = Path.of(".metrics", "mutate");

    private PitMutationMetrics() {
    }

    record Tally(int killed, int survived, int uncovered) {
        Tally plus(Tally other) {
            return new Tally(killed + other.killed, survived + other.survived, uncovered + other.uncovered);
        }

        int sites() {
            return killed + survived + uncovered;
        }
    }

    public static void main(String[] arguments) {
        run(Path.of("").toAbsolutePath(), LOGGER::info);
    }

    static void run(Path root, Consumer<String> out) {
        out.accept(convert(root));
    }

    /** Converts if the PIT XML exists under {@code root}; returns a one-line message either way. */
    static String convert(Path root) {
        Path xml = root.resolve(MUTATIONS_XML);
        if (!Files.isRegularFile(xml)) {
            return "Mutation report missing (" + MUTATIONS_XML + "): skipping mutation colouring."
                    + " Run: mvn org.pitest:pitest-maven:mutationCoverage";
        }
        try {
            Map<String, Map<String, Tally>> byClass;
            try (InputStream stream = Files.newInputStream(xml)) {
                byClass = tally(stream);
            }
            Path outputDir = root.resolve(OUTPUT_DIR);
            clearSnapshots(outputDir);
            for (Map.Entry<String, Map<String, Tally>> entry : byClass.entrySet()) {
                Files.writeString(outputDir.resolve(entry.getKey() + ".edn"),
                        snapshot(entry.getKey(), entry.getValue()));
            }
            return "Wrote " + byClass.size() + " mutation snapshots to " + OUTPUT_DIR;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not convert " + MUTATIONS_XML, e);
        }
    }

    private static void clearSnapshots(Path dir) throws IOException {
        Files.createDirectories(dir);
        try (Stream<Path> files = Files.list(dir)) {
            for (Path old : files.filter(file -> file.toString().endsWith(".edn")).toList()) {
                Files.delete(old);
            }
        }
    }

    /** {@code fqcn -> method -> tally}, folding nested classes into the outer class. */
    static Map<String, Map<String, Tally>> tally(InputStream xml) throws IOException {
        NodeList mutations = parse(xml).getElementsByTagName("mutation");
        Map<String, Map<String, Tally>> byClass = new TreeMap<>();
        for (int i = 0; i < mutations.getLength(); i++) {
            Element mutation = (Element) mutations.item(i);
            Tally tally = outcome(mutation.getAttribute("status"));
            if (tally != null) {
                String owner = outerClass(text(mutation, "mutatedClass"));
                byClass.computeIfAbsent(owner, k -> new TreeMap<>())
                        .merge(text(mutation, "mutatedMethod"), tally, Tally::plus);
            }
        }
        return byClass;
    }

    private static Document parse(InputStream xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(xml);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("PIT mutations.xml is not readable XML", e);
        }
    }

    private static @Nullable Tally outcome(String status) {
        return switch (status) {
            case "KILLED", "TIMED_OUT", "MEMORY_ERROR" -> new Tally(1, 0, 0);
            case "SURVIVED" -> new Tally(0, 1, 0);
            case "NO_COVERAGE" -> new Tally(0, 0, 1);
            default -> null;
        };
    }

    private static String outerClass(String className) {
        int nested = className.indexOf('$');
        return nested < 0 ? className : className.substring(0, nested);
    }

    private static String text(Element parent, String tag) {
        return parent.getElementsByTagName(tag).item(0).getTextContent();
    }

    static String snapshot(String fqcn, Map<String, Tally> methods) {
        List<String> forms = new ArrayList<>();
        methods.forEach((method, t) -> forms.add("{:id \"defn/" + method + "\" :killed " + t.killed()
                + " :survived " + t.survived() + " :uncovered " + t.uncovered() + " :sites " + t.sites() + "}"));
        return "{:namespace \"" + fqcn + "\"\n :forms [" + String.join("\n         ", forms) + "]}\n";
    }
}
