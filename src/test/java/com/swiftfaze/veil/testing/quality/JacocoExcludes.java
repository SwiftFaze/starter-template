package com.swiftfaze.veil.testing.quality;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The {@code <excludes>} globs of pom.xml's {@code jacoco-check} execution: the single
 * exclusion list the CRAP gate shares with JaCoCo's coverage check.
 *
 * <p>Globs are matched against JaCoCo's slash-separated class file name
 * ({@code com/swiftfaze/veil/Main$1.class}) with Ant semantics, independent of the
 * host file system's separator.
 */
final class JacocoExcludes {

    private static final String EXECUTION_ID = "jacoco-check";
    private static final char SINGLE_STAR = '*';
    private static final char SINGLE_QUESTION = '?';

    private final List<Pattern> patterns;

    private JacocoExcludes(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    /** Empty when the pom has no {@code jacoco-check} execution with an {@code <excludes>} list. */
    static Optional<JacocoExcludes> fromPom(Path pom) throws IOException, ParserConfigurationException, SAXException {
        Document doc = XmlDocuments.parse(pom);
        NodeList executions = doc.getElementsByTagName("execution");
        for (int i = 0; i < executions.getLength(); i++) {
            Element execution = (Element) executions.item(i);
            if (EXECUTION_ID.equals(firstText(execution, "id"))) {
                return excludesOf(execution);
            }
        }
        return Optional.empty();
    }

    boolean matches(String className) {
        String classFile = className + ".class";
        return patterns.stream().anyMatch(p -> p.matcher(classFile).matches());
    }

    private static Optional<JacocoExcludes> excludesOf(Element execution) {
        NodeList lists = execution.getElementsByTagName("excludes");
        if (lists.getLength() == 0) {
            return Optional.empty();
        }
        NodeList entries = ((Element) lists.item(0)).getElementsByTagName("exclude");
        List<Pattern> patterns = new ArrayList<>();
        for (int e = 0; e < entries.getLength(); e++) {
            String glob = entries.item(e).getTextContent().trim();
            if (!glob.isEmpty()) {
                patterns.add(compileGlob(glob));
            }
        }
        return Optional.of(new JacocoExcludes(patterns));
    }

    /** Each call compiles a different exclude entry, not one fixed pattern reused. */
    private static Pattern compileGlob(String glob) {
        return Pattern.compile(globToRegex(glob));
    }

    private static String firstText(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    /** Ant-style glob: {@code **}/ spans directories, {@code *} and {@code ?} stay within one. */
    static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder();
        int index = 0;
        while (index < glob.length()) {
            index = appendToken(glob, index, regex);
        }
        return regex.toString();
    }

    private static int appendToken(String glob, int index, StringBuilder regex) {
        if (glob.startsWith("**/", index)) {
            regex.append("(?:.*/)?");
            return index + "**/".length();
        }
        if (glob.startsWith("**", index)) {
            regex.append(".*");
            return index + "**".length();
        }
        char character = glob.charAt(index);
        if (character == SINGLE_STAR) {
            regex.append("[^/]*");
        } else if (character == SINGLE_QUESTION) {
            regex.append("[^/]");
        } else {
            regex.append(Pattern.quote(String.valueOf(character)));
        }
        return index + 1;
    }
}
