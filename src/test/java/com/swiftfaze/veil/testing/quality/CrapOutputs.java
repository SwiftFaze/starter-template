package com.swiftfaze.veil.testing.quality;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Writes the CRAP gate's two outputs: {@code target/crap/crap.txt}, a worst-first table
 * whose {@code FAIL} lines check-clean.sh reads, and {@code .metrics/crap.edn}, the
 * {@code {:entries [{:name :namespace :complexity :coverage :crap}]}} shape uml-viewer reads.
 */
final class CrapOutputs {

    private static final Comparator<MethodCoverage> WORST_FIRST = Comparator.comparingDouble(MethodCoverage::crap).reversed();

    private CrapOutputs() {
    }

    static void write(Path root, List<MethodCoverage> methods, CrapGate gate, List<String> failures)
            throws IOException {
        List<MethodCoverage> worstFirst = new ArrayList<>(methods);
        worstFirst.sort(WORST_FIRST);
        writeFile(root.resolve(CrapReport.CRAP_TXT), table(worstFirst, gate, failures));
        writeFile(root.resolve(CrapReport.CRAP_EDN), edn(worstFirst));
    }

    private static List<String> table(List<MethodCoverage> worstFirst, CrapGate gate, List<String> failures) {
        List<String> lines = new ArrayList<>();
        lines.add("CRAP   CC  COV   STATUS            METHOD");
        for (MethodCoverage method : worstFirst) {
            lines.add(String.format(Locale.ROOT, "%5s %4d %4d%%  %-16s  %s",
                    CrapFormat.score(method.roundedCrap()), method.complexity(), method.coveragePercent(),
                    status(method, gate), method.key()));
        }
        lines.add("");
        lines.addAll(failures);
        lines.add("CRAP violations: " + failures.size());
        return lines;
    }

    private static String status(MethodCoverage method, CrapGate gate) {
        String limit = gate.isOverLimit(method) ? "over-limit" : "ok";
        return gate.isBaselined(method) ? limit + " baselined" : limit;
    }

    private static List<String> edn(List<MethodCoverage> worstFirst) {
        List<String> lines = new ArrayList<>();
        lines.add("{:entries [");
        for (MethodCoverage method : worstFirst) {
            lines.add("  {:name " + quote(method.method())
                    + " :namespace " + quote(method.fqcn())
                    + " :complexity " + method.complexity()
                    + " :coverage " + method.coveragePercent()
                    + " :crap " + CrapFormat.score(method.roundedCrap()) + "}");
        }
        lines.add("]}");
        return lines;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static void writeFile(Path file, List<String> lines) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, lines);
    }
}
