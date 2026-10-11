package com.swiftfaze.veil.testing.quality;

import java.math.BigDecimal;
import java.util.Locale;

/** Locale-independent number formatting shared by the gate messages and output files. */
final class CrapFormat {

    private CrapFormat() {
    }

    /** One decimal place, e.g. {@code 13.3}. */
    static String score(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /** The limit as written in quality-gates.properties, e.g. {@code 10} rather than {@code 10.0}. */
    static String limit(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
