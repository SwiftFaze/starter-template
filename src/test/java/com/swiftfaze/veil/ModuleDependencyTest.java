package com.swiftfaze.veil;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ModuleDependencyTest {

    private static final String UI_PACKAGE = "com.swiftfaze.veil.ui";
    private static final String WIDGET_PACKAGE = UI_PACKAGE + ".widget";
    private static final String SANDBOX_PACKAGE = "com.swiftfaze.veil.sandbox";

    private final JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.swiftfaze.veil");

    @Test
    void engineCodeMustNotDependOnUi() {
        ArchRule rule = noClasses()
                .that(resideOutsideOfPackage(UI_PACKAGE + "..")
                        .and(resideOutsideOfPackage(SANDBOX_PACKAGE + ".."))
                        .and(not(equivalentTo(Main.class))))
                .should().dependOnClassesThat().resideInAPackage(UI_PACKAGE + "..")
                .because("engine code must not depend on the UI layer; Main is the composition root "
                        + "that wires UI together, and sandbox/ is a dev-only UI tool that legitimately "
                        + "reuses ui.widget classes");
        rule.check(classes);
    }

    @Test
    void widgetsMustNotDependOnScreens() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(WIDGET_PACKAGE)
                .should().dependOnClassesThat().resideInAPackage(UI_PACKAGE)
                .because("widget classes must not depend on screen classes that sit directly in "
                        + "com.swiftfaze.veil.ui; screens may depend on widgets, not the reverse");
        rule.check(classes);
    }

    @Test
    void packagesMustBeFreeOfCycles() {
        ArchRule rule = FreezingArchRule.freeze(
                slices()
                        .matching("com.swiftfaze.(**)")
                        .should()
                        .beFreeOfCycles()
                        .because("the package graph must be acyclic; cycles create high coupling and "
                                + "make the codebase hard to test and refactor; full-depth slicing "
                                + "(every package at every level is its own slice) catches cycles "
                                + "involving the root package and intra-package cycles that a "
                                + "top-level-only slice would miss"));
        rule.check(classes);
    }

    @Test
    void noPublicMutableStaticFields() {
        // Note: the that()/should() split matters here. "That" defines SCOPE only (which fields we
        // care about); the full public+static+non-final condition lives in "should", because ArchUnit
        // reports a violation when (that) AND (should) are BOTH true for an element. Putting
        // public/static/non-final in "that" and asserting "should().notBePublic()" is a
        // self-contradiction (a field can't simultaneously satisfy "is public" and "is not public"),
        // so it always finds zero violations - confirmed by direct evaluation against WidgetTheme's
        // 13 public static non-final fields during implementation.
        ArchRule rule = FreezingArchRule.freeze(
                noFields()
                        .that()
                        .areDeclaredInClassesThat().resideInAPackage("com.swiftfaze.veil..")
                        .should()
                        .bePublic()
                        .andShould()
                        .beStatic()
                        .andShould()
                        .notBeFinal()
                        .because("public mutable static fields create shared, global state that "
                                + "is hard to reason about; constants (public static final) are fine, "
                                + "but mutable static state must be private and accessed through "
                                + "controlled interfaces"));
        rule.check(classes);
    }

    @Test
    void instantiationConfinedToCompositionRoot() {
        // callConstructorWhere + targeting the constructor call's owner class specifically (rather than
        // accessClassesThat(), which matches ANY access - method calls, field reads, generic type
        // references) keeps this rule about instantiation, matching its "should only be instantiated
        // by Main" rationale. accessClassesThat() was confirmed during implementation to catch ~116
        // violations, only 2 of which were actual constructor calls - i.e. it was really an
        // "access confined to composition roots" rule, far broader than intended.
        DescribedPredicate<JavaCall<?>> callsEngineConstructor = JavaCall.Predicates.target(
                owner(resideInAnyPackage("com.swiftfaze.veil.game..",
                                          "com.swiftfaze.veil.world..",
                                          "com.swiftfaze.veil.mods..",
                                          "com.swiftfaze.veil.entities..")));
        ArchRule rule = FreezingArchRule.freeze(
                noClasses()
                        .that()
                        .resideOutsideOfPackage("com.swiftfaze.veil.game..")
                        .and()
                        .resideOutsideOfPackage("com.swiftfaze.veil.world..")
                        .and()
                        .resideOutsideOfPackage("com.swiftfaze.veil.mods..")
                        .and()
                        .resideOutsideOfPackage("com.swiftfaze.veil.entities..")
                        .and(not(equivalentTo(Main.class)))
                        .should()
                        .callConstructorWhere(callsEngineConstructor)
                        .because("engine classes in game, world, mods, and entities packages "
                                + "should only be instantiated by Main, the composition root that "
                                + "wires the application together; arbitrary instantiation from UI "
                                + "or other packages creates hidden coupling and makes the engine "
                                + "untestable in isolation; com.swiftfaze.veil.ui.. is deliberately "
                                + "excluded so the rule targets engine construction, not general "
                                + "object creation like new JPanel()"));
        rule.check(classes);
    }
}
