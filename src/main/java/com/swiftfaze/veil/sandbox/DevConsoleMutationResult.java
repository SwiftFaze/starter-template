package com.swiftfaze.veil.sandbox;

/**
 * Outcome of one {@link DevConsoleFieldMutator#apply} call: the field's new value on success, or
 * the offending token (an unknown field name, or a value that failed to parse/validate) on
 * failure - the caller writes either into the transcript as a leveled line.
 */
public sealed interface DevConsoleMutationResult {

    record Success(String fieldName, int newValue) implements DevConsoleMutationResult {
    }

    record Failure(String token) implements DevConsoleMutationResult {
    }
}
