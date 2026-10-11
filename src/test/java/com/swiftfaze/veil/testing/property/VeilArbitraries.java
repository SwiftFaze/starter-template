package com.swiftfaze.veil.testing.property;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

import java.util.Locale;

/**
 * Shared arbitrary generators for property-based testing of Veil game logic.
 * These arbitraries are used across multiple property test classes to ensure
 * consistent generation of domain values (world dimensions, coordinates, levels, etc.).
 */
public final class VeilArbitraries {

    private static final int MAX_EXPRESSION_DEPTH = 2;

    private VeilArbitraries() {
    }

    /**
     * Arbitrary for world dimensions (width or height).
     * Bounded to [1, 200] to balance test speed with coverage.
     */
    public static Arbitrary<Integer> worldDimension() {
        return Arbitraries.integers().between(1, 200);
    }

    /**
     * Arbitrary for a coordinate (x or y position) that may fall outside a world's bounds.
     * Generates both valid (within typical world bounds) and invalid (negative or very large) values.
     */
    public static Arbitrary<Integer> coordinate() {
        return Arbitraries.integers().between(-50, 250);
    }

    /**
     * Arbitrary for game levels. Bounded to [0, 200] to represent reasonable game progression.
     */
    public static Arbitrary<Integer> level() {
        return Arbitraries.integers().between(0, 200);
    }

    /**
     * Arbitrary for a well-formed CalcExpressionParser grammar expression.
     * Generates valid expressions containing 'level', small numbers (to avoid overflow),
     * binary operators (+, -, *, /), and optional parentheses.
     * Depth is capped at 2 to prevent excessively long or complex expressions and overflow.
     */
    public static Arbitrary<String> calcExpression() {
        return Arbitraries.lazy(() -> expressionAtDepth(0));
    }

    /**
     * Recursive arbitrary for building well-formed expressions up to a maximum depth.
     * At leaf nodes, generates 'level' or a small number. At non-leaf nodes, recursively
     * combines two sub-expressions with a binary operator, avoiding division of non-level values.
     * Uses small numbers (max 10) to prevent overflow when expressions are evaluated.
     */
    private static Arbitrary<String> expressionAtDepth(int depth) {
        if (depth >= MAX_EXPRESSION_DEPTH) {
            // Leaf node: 'level' or a small number (1-10 to avoid overflow)
            return Arbitraries.oneOf(
                    Arbitraries.just("level"),
                    Arbitraries.integers().between(1, 10).map(String::valueOf),
                    Arbitraries.doubles().between(0.5, 10.0)
                            .map(d -> String.format(Locale.ROOT, "%.1f", d))
            );
        }

        Arbitrary<String> leaf = expressionAtDepth(depth + 1);
        // Use only addition/subtraction at higher depths to avoid overflow/division-by-zero
        Arbitrary<String> safeOp = Arbitraries.of("+", "-");

        Arbitrary<String> binary = Combinators.combine(leaf, safeOp, leaf)
                .as((left, op, right) -> left + " " + op + " " + right);

        return Arbitraries.oneOf(leaf, binary);
    }

    /**
     * Arbitrary for a random seed (long).
     * Used for future world-generation property tests that depend on seeding.
     */
    public static Arbitrary<Long> seed() {
        return Arbitraries.longs();
    }
}
