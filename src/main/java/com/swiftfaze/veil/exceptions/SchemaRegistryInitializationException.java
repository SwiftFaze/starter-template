package com.swiftfaze.veil.exceptions;

/**
 * Thrown when the JSON schema registry used to validate mod content fails to
 * initialize (e.g. a schema file committed under {@code docs/schemas/} is
 * itself malformed). This is a defect in Veil's own shipped schemas, not "bad
 * mod data", so it is deliberately distinct from {@link ModLoadException}.
 */
public class SchemaRegistryInitializationException extends RuntimeException {

    public SchemaRegistryInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
