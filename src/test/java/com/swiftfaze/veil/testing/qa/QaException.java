package com.swiftfaze.veil.testing.qa;

/** A QA procedure could not be loaded or run; the message is shown to the user as-is. */
public class QaException extends RuntimeException {
    public QaException(String message) {
        super(message);
    }

    public QaException(String message, Throwable cause) {
        super(message, cause);
    }
}
