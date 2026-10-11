package com.swiftfaze.veil.testing.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Direct coverage of {@link JacocoReportParser}, below the level {@link CrapReportTest} and
 * crap-gate.feature exercise it at: every {@code <class>} across every {@code <package>}, and
 * the counter-missing default that a report only reaches when a method has no LINE data at all.
 */
class JacocoReportParserTest {

    @TempDir
    Path dir;

    @Test
    void readsMethodsFromEveryClassInEveryPackage() throws Exception {
        Path jacocoXml = writeReport("<report><package name=\"p1\"><class name=\"p1/A\">"
                + "<method name=\"a\" desc=\"()V\"><counter type=\"COMPLEXITY\" missed=\"1\" covered=\"0\"/>"
                + "<counter type=\"LINE\" missed=\"0\" covered=\"2\"/></method></class></package>"
                + "<package name=\"p2\"><class name=\"p2/B\">"
                + "<method name=\"b\" desc=\"()V\"><counter type=\"COMPLEXITY\" missed=\"2\" covered=\"0\"/>"
                + "<counter type=\"LINE\" missed=\"1\" covered=\"1\"/></method></class></package></report>");

        List<MethodCoverage> methods = JacocoReportParser.parse(jacocoXml);

        assertEquals(List.of("p1.A#a", "p2.B#b"), methods.stream().map(MethodCoverage::key).toList());
    }

    @Test
    void aMethodWithNoLineCounterDefaultsToZeroMissedAndZeroCovered() throws Exception {
        Path jacocoXml = writeReport("<report><package name=\"p\"><class name=\"p/C\">"
                + "<method name=\"c\" desc=\"()V\"><counter type=\"COMPLEXITY\" missed=\"3\" covered=\"0\"/>"
                + "</method></class></package></report>");

        MethodCoverage method = JacocoReportParser.parse(jacocoXml).get(0);

        assertEquals(0, method.linesMissed());
        assertEquals(0, method.linesCovered());
        assertEquals(3, method.complexity());
    }

    @Test
    void aMethodWithNoComplexityCounterDefaultsToZeroComplexity() throws Exception {
        Path jacocoXml = writeReport("<report><package name=\"p\"><class name=\"p/D\">"
                + "<method name=\"d\" desc=\"()V\"><counter type=\"LINE\" missed=\"0\" covered=\"5\"/>"
                + "</method></class></package></report>");

        MethodCoverage method = JacocoReportParser.parse(jacocoXml).get(0);

        assertEquals(0, method.complexity());
        assertEquals(5, method.linesCovered());
    }

    private Path writeReport(String xml) throws IOException {
        Path jacocoXml = dir.resolve("jacoco.xml");
        Files.writeString(jacocoXml, xml);
        return jacocoXml;
    }
}
