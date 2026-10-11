package com.swiftfaze.veil.testing.uml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the {@code :levels} vector of {@code docs/uml/veil.policy.edn}: groups of
 * top-level package segments, inner (index 0) to outer. Only that one key is read,
 * so no EDN library is needed.
 */
final class UmlPolicy {

    private static final Pattern LEVELS = Pattern.compile(":levels\\s*\\[((?:\\s*\\[[^\\[\\]]*\\]\\s*)*)\\]");
    private static final Pattern GROUP = Pattern.compile("\\[([^\\[\\]]*)\\]");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s,]+");

    private UmlPolicy() {
    }

    static List<List<String>> levels(String policyEdn) {
        Matcher vector = LEVELS.matcher(policyEdn);
        if (!vector.find()) {
            throw new IllegalArgumentException("policy has no well-formed :levels vector of vectors");
        }
        List<List<String>> groups = new ArrayList<>();
        Matcher group = GROUP.matcher(vector.group(1));
        while (group.find()) {
            groups.add(names(group.group(1)));
        }
        return groups;
    }

    private static List<String> names(String groupBody) {
        return Arrays.stream(SEPARATORS.split(groupBody)).filter(name -> !name.isEmpty()).toList();
    }
}
