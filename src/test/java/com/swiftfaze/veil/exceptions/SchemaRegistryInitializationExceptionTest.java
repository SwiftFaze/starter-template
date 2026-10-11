package com.swiftfaze.veil.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The only real behavior this exception has is carrying its message and
 * cause through to {@link RuntimeException}; the real failure scenario it
 * models (a malformed schema shipped under docs/schemas/) is not something
 * unit tests should reproduce via real file I/O, so this exercises the
 * exception directly.
 */
@DisplayName("SchemaRegistryInitializationException carries message and cause")
class SchemaRegistryInitializationExceptionTest {

    @Test
    void carriesMessageAndCause() {
        Throwable cause = new IllegalStateException("malformed schema");

        SchemaRegistryInitializationException thrown =
                new SchemaRegistryInitializationException("Failed to initialize JSON schema registry", cause);

        assertEquals("Failed to initialize JSON schema registry", thrown.getMessage());
        assertSame(cause, thrown.getCause());
    }
}
