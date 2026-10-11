package com.swiftfaze.veil;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fails the build if two step definitions share the same literal step text.
 *
 * <p>Cucumber matches step text regardless of the Given/When/Then keyword, so
 * the same text under two different keywords is a duplicate. A duplicate
 * poisons the <em>entire</em> glue registry for the run, not just the two
 * colliding methods, which surfaces as {@code UndefinedStepException} in
 * feature files unrelated to whatever changed — a misleading, run-to-run
 * varying failure that has cost real debugging time here before.
 *
 * <p>This replaces the manual {@code grep | sort | uniq -d} that
 * {@code .claude/workflow.md} previously required before reporting Step 5 done.
 * A check a person has to remember is a check that eventually gets skipped;
 * this one cannot be.
 */
class NoDuplicateStepDefinitionsTest {

    private static final String STEPS_PACKAGE = "com.swiftfaze.veil.steps";

    @Test
    void noTwoStepDefinitionsShareTheSameStepText() {
        Map<String, List<String>> ownersByStepText = new LinkedHashMap<>();

        JavaClasses stepClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages(STEPS_PACKAGE);

        for (JavaMethod method : stepClasses.stream()
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .toList()) {
            record(ownersByStepText, method, Given.class, Given::value);
            record(ownersByStepText, method, When.class, When::value);
            record(ownersByStepText, method, Then.class, Then::value);
        }

        List<String> duplicates = ownersByStepText.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> "  \"" + entry.getKey() + "\"\n      defined by: "
                        + String.join("\n                  ", entry.getValue()))
                .toList();

        assertTrue(duplicates.isEmpty(),
                () -> "Duplicate Cucumber step definitions found. Cucumber matches step text "
                        + "regardless of keyword, and one duplicate breaks the whole glue registry.\n\n"
                        + String.join("\n", duplicates)
                        + "\n\nIf the same text genuinely needs to act as both a setup precondition and a "
                        + "later assertion, use one method that guards on state (see "
                        + "UiComponentFrameworkSteps.theConfirmationPopupIsShown), not two annotations.");
    }

    private static <A extends Annotation> void record(Map<String, List<String>> ownersByStepText,
                                                      JavaMethod method,
                                                      Class<A> annotationType,
                                                      Function<A, String> stepTextOf) {
        method.tryGetAnnotationOfType(annotationType).ifPresent(annotation ->
                ownersByStepText
                        .computeIfAbsent(stepTextOf.apply(annotation), key -> new ArrayList<>())
                        .add(method.getOwner().getSimpleName() + "." + method.getName() + "()"));
    }
}
