package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JacocoExcludesTest {

    private static final String THREE_EXCLUDE_GLOBS = "<execution><id>jacoco-check</id><configuration><excludes>"
            + "<exclude>**/Main.class</exclude><exclude>**/Main$*.class</exclude>"
            + "<exclude>**/ui/widget/Widget.class</exclude></excludes></configuration></execution>";

    @TempDir
    Path dir;

    @Test
    void matchesEveryConfiguredGlobAgainstItsSlashSeparatedClassName() throws Exception {
        JacocoExcludes excludes = excludesFrom(THREE_EXCLUDE_GLOBS);

        assertTrue(excludes.matches("com/swiftfaze/veil/Main"));
        assertTrue(excludes.matches("com/swiftfaze/veil/Main$1"));
        assertTrue(excludes.matches("com/swiftfaze/veil/ui/widget/Widget"));
    }

    @Test
    void doesNotMatchAClassNameThatOnlySharesAPrefixWithAGlob() throws Exception {
        JacocoExcludes excludes = excludesFrom(THREE_EXCLUDE_GLOBS);

        assertFalse(excludes.matches("com/swiftfaze/veil/MainMenu"));
        assertFalse(excludes.matches("com/swiftfaze/veil/ui/widget/WidgetTheme"));
    }

    @Test
    void singleStarDoesNotCrossDirectories() {
        String regex = JacocoExcludes.globToRegex("a/*.class");

        assertTrue("a/B.class".matches(regex));
        assertFalse("a/b/C.class".matches(regex));
    }

    @Test
    void ignoresExcludesOfOtherExecutions() throws Exception {
        String pom = "<execution><id>report</id><configuration><excludes><exclude>**/X.class</exclude>"
                + "</excludes></configuration></execution>";

        assertTrue(JacocoExcludes.fromPom(writePom(pom)).isEmpty());
    }

    @Test
    void isEmptyWhenTheCheckExecutionHasNoExcludesList() throws Exception {
        String pom = "<execution><id>jacoco-check</id><configuration/></execution>";

        assertTrue(JacocoExcludes.fromPom(writePom(pom)).isEmpty());
    }

    private JacocoExcludes excludesFrom(String executions) throws IOException, ParserConfigurationException, SAXException {
        return JacocoExcludes.fromPom(writePom(executions)).orElseThrow();
    }

    private Path writePom(String executions) throws IOException {
        Path pom = dir.resolve("pom.xml");
        Files.writeString(pom, "<project><build><plugins><plugin><executions>" + executions
                + "</executions></plugin></plugins></build></project>");
        return pom;
    }
}
