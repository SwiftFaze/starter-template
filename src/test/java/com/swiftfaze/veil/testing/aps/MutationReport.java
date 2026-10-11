package com.swiftfaze.veil.testing.aps;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** What one acceptance-mutation run found. */
public final class MutationReport {

    public static final int EXIT_CLEAN = 0;
    public static final int EXIT_ERROR = 2;
    public static final int EXIT_SURVIVORS = 3;

    private final List<Mutant> mutantsRun = new ArrayList<>();
    private final List<Mutant> survivors = new ArrayList<>();
    private final List<String> skipped = new ArrayList<>();
    private final List<String> failingOriginals = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    void ran(Mutant mutant, boolean survived) {
        mutantsRun.add(mutant);
        if (survived) {
            survivors.add(mutant);
        }
    }

    void skipped(String reason) {
        skipped.add(reason);
    }

    public List<String> skipped() {
        return List.copyOf(skipped);
    }

    void failingOriginal(String scenario) {
        failingOriginals.add(scenario);
    }

    void error(String message) {
        errors.add(message);
    }

    public List<Mutant> mutantsRun() {
        return List.copyOf(mutantsRun);
    }

    public List<Mutant> survivors() {
        return List.copyOf(survivors);
    }

    public List<String> failingOriginals() {
        return List.copyOf(failingOriginals);
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    /** 2 when anything could not be checked, else 3 when a mutant survived, else 0. */
    public int exitCode() {
        if (!errors.isEmpty() || !failingOriginals.isEmpty()) {
            return EXIT_ERROR;
        }
        return survivors.isEmpty() ? EXIT_CLEAN : EXIT_SURVIVORS;
    }

    public void print(Consumer<String> out) {
        skipped.forEach(s -> out.accept("SKIPPED   " + s));
        failingOriginals.forEach(s -> out.accept("FAILS ON THE ORIGINAL   " + s));
        errors.forEach(s -> out.accept("ERROR   " + s));
        survivors.forEach(m -> out.accept("SURVIVED  " + m));
        out.accept("Acceptance mutation: " + mutantsRun.size() + " mutants run, "
                + survivors.size() + " survived, exit " + exitCode());
    }
}
