package com.swiftfaze.veil.testing.qa;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses a {@code .keys} file: one key name per line ({@code ENTER}, {@code RIGHT},
 * {@code I}, ...), resolved to {@code KeyEvent.VK_<NAME>}. {@code #} comments and
 * blank lines are ignored.
 */
public final class KeyScript {
    private KeyScript() {
    }

    public static List<Integer> parse(Path file) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new QaException("Cannot read key script " + file + ": " + e.getMessage(), e);
        }
        return parse(file.toString(), lines);
    }

    static List<Integer> parse(String fileName, List<String> lines) {
        List<Integer> codes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = stripComment(lines.get(i)).trim();
            if (!line.isEmpty()) {
                codes.add(resolve(fileName, i + 1, line));
            }
        }
        return codes;
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }

    private static int resolve(String fileName, int lineNumber, String name) {
        try {
            Field field = KeyEvent.class.getField("VK_" + name.toUpperCase(Locale.ROOT));
            return field.getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new QaException(fileName + ":" + lineNumber + ": unknown key '" + name + "'", e);
        }
    }
}
