package com.swiftfaze.veil.sandbox;

import java.util.List;

/**
 * Lets a {@link DevConsoleProvider} wire live field mutation into the command bar's set/add/
 * subtract verbs. {@link PlayerSandboxProvider} and {@link ItemSandboxProvider} implement this - a
 * provider with no mutable state (e.g. {@code ClassSandboxProvider}) simply doesn't return one from
 * {@link DevConsoleProvider#fieldMutator(String)}.
 */
public interface DevConsoleFieldMutator {

    /**
     * Applies a SET/ADD/SUBTRACT mutation to one field.
     *
     * @param fieldToken the field's abbreviation token (e.g. "str", "maxhp")
     * @param rawValue   the typed value token - a base-10 integer, or "default" (SET only, and
     *                   only for fields with a class-default to reset to)
     */
    DevConsoleMutationResult apply(DevConsoleMutationVerb verb, String fieldToken, String rawValue);

    /** Tab-completion candidates for the field-name argument position. */
    List<String> fieldTokens();

    /** Whether {@code fieldToken} has a class-default the SET verb's "default" keyword can restore. */
    boolean hasClassDefault(String fieldToken);
}
