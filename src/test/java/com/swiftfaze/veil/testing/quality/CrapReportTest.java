package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrapReportTest {

    private static final String POM = "<project><build><plugins><plugin><executions><execution>"
            + "<id>jacoco-check</id><configuration><excludes><exclude>**/Skip.class</exclude></excludes>"
            + "</configuration></execution></executions></plugin></plugins></build></project>";

    @TempDir
    Path root;

    @BeforeEach
    void writeProject() throws Exception {
        Files.writeString(root.resolve("pom.xml"), POM);
        Files.writeString(root.resolve("quality-gates.properties"), "# limit\ncrap.max=10\n");
    }

    @Test
    void passesAndWritesBothOutputsWhenEveryMethodIsWithinTheLimit() throws Exception {
        writeJacoco(method("ok", 2, 0, 3));

        CrapReport.Result result = CrapReport.run(root, /* reportOnly= */ false);

        assertEquals(CrapReport.PASS, result.exitCode());
        assertTrue(Files.readString(root.resolve(CrapReport.CRAP_TXT)).contains("a.B#ok"));
        assertEquals(List.of("{:entries [", "  {:name \"ok\" :namespace \"a.B\" :complexity 2 :coverage 100 :crap 2.0}",
                "]}"), Files.readAllLines(root.resolve(CrapReport.CRAP_EDN)));
    }

    @Test
    void failsInGateModeAndPassesInReportOnlyMode() throws Exception {
        writeJacoco(method("risky", 4, 5, 0));

        assertEquals(CrapReport.VIOLATIONS, CrapReport.run(root, /* reportOnly= */ false).exitCode());
        assertEquals(CrapReport.PASS, CrapReport.run(root, /* reportOnly= */ true).exitCode());
    }

    @Test
    void excludedClassesAreNeitherScoredNorWritten() throws Exception {
        Files.createDirectories(root.resolve("target/site/jacoco"));
        Files.writeString(root.resolve(CrapReport.JACOCO_XML), "<report><package><class name=\"a/Skip\">"
                + method("risky", 6, 5, 0) + "</class></package></report>");

        assertEquals(CrapReport.PASS, CrapReport.run(root, /* reportOnly= */ false).exitCode());
        assertFalse(Files.readString(root.resolve(CrapReport.CRAP_EDN)).contains("Skip"));
    }

    @Test
    void cannotRunWithoutAJacocoReportAndWritesNothing() {
        CrapReport.Result result = CrapReport.run(root, /* reportOnly= */ false);

        assertEquals(CrapReport.CANNOT_RUN, result.exitCode());
        assertTrue(result.messages().get(0).contains(CrapReport.JACOCO_XML));
        assertFalse(Files.exists(root.resolve(CrapReport.CRAP_TXT)));
    }

    @Test
    void cannotRunWithoutCrapMax() throws Exception {
        Files.writeString(root.resolve("quality-gates.properties"), "other=1\n");
        writeJacoco(method("ok", 1, 0, 1));

        CrapReport.Result result = CrapReport.run(root, /* reportOnly= */ false);

        assertEquals(CrapReport.CANNOT_RUN, result.exitCode());
        assertTrue(result.messages().get(0).contains("crap.max"));
    }

    @Test
    void cannotRunWithoutTheJacocoCheckExcludes() throws Exception {
        Files.writeString(root.resolve("pom.xml"), "<project/>");
        writeJacoco(method("ok", 1, 0, 1));

        CrapReport.Result result = CrapReport.run(root, /* reportOnly= */ false);

        assertEquals(CrapReport.CANNOT_RUN, result.exitCode());
        assertTrue(result.messages().get(0).contains("could not find the exclusion list"));
    }

    private void writeJacoco(String methods) throws IOException {
        Files.createDirectories(root.resolve("target/site/jacoco"));
        Files.writeString(root.resolve(CrapReport.JACOCO_XML),
                "<report><package><class name=\"a/B\">" + methods + "</class></package></report>");
    }

    private static String method(String name, int complexity, int missedLines, int coveredLines) {
        return "<method name=\"" + name + "\" desc=\"()V\">"
                + "<counter type=\"LINE\" missed=\"" + missedLines + "\" covered=\"" + coveredLines + "\"/>"
                + "<counter type=\"COMPLEXITY\" missed=\"" + complexity + "\" covered=\"0\"/></method>";
    }
}
