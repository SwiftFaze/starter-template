package com.swiftfaze.veil.steps;

import io.cucumber.java.ParameterType;

import java.util.List;

/**
 * Glue for {@code {quotedList}}: a comma-separated run of double-quoted values such as
 * {@code "Class", "Strength", "Dexterity"}. Lets a step take a long list of names as one
 * parameter while the .feature text keeps listing them inline.
 */
public class QuotedListParameterType {

    @ParameterType("\"[^\"]*\"(?:, \"[^\"]*\")*")
    public List<String> quotedList(String match) {
        return List.of(match.substring(1, match.length() - 1).split("\", \"", -1));
    }
}
