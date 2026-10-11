package com.swiftfaze.veil.exceptions;

public class ModLoadException extends RuntimeException {

    public ModLoadException(String message) {
        super(message);
    }

    public ModLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
