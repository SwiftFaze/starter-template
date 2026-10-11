@manual-verification
Feature: forbidden-apis — signature gate on default-charset and default-locale JDK calls
  `String.getBytes()`, `new String(byte[])`, `FileReader`, no-arg
  `toLowerCase()`/`toUpperCase()`, `String.format(...)` without a `Locale`
  and similar JDK calls quietly use the JVM's default charset or locale.
  Windows does not default to UTF-8, so on this project that is a live bug
  class: mod JSON is read from disk, and case-folded lookups run on game
  data.

  This feature adds the `de.thetaphi:forbiddenapis` Maven plugin, bound to
  `verify`, with the `jdk-unsafe` and `jdk-deprecated` signature bundles for
  the Java 17 target. It runs the `check` goal over `src/main` and the
  `testCheck` goal over `src/test`. It matches compiled bytecode against
  signatures, so unlike PMD it has no AST shapes to miss, and
  `@SuppressWarnings` can't silence it.

  Unlike the diff-scoped `check-clean.sh` gates (`spotbugs-fb-contrib.feature`,
  `pmd-full-rule-catalogue.feature`), this gate is repo-wide. Every existing
  call site is fixed in this change, and none is excluded. Anything that
  genuinely can't be fixed is recorded in `impacts.md` with its reason.

  Supersedes: PMD's `RelianceOnDefaultCharset` and
  `UseLocaleWithCaseConversions` rules in `.pmd-clean-code.xml`. This change
  removes both, so one defect isn't reported several times. Error Prone's
  `StringCaseLocaleUsage` stays at ERROR because it fails at compile time and
  in the IDE, so case-conversion bugs are still caught early, and this gate
  backstops it at `verify`.

  Out of scope: other forbidden-apis bundles (`jdk-system-out`,
  `jdk-non-portable`, `jdk-internal`, third-party bundles) and
  project-specific signature files.

  QA: none - build-time gate with no runtime or keyboard input; verified by `mvn verify`

  Background:
    Given the `forbiddenapis` plugin is bound to the `verify` phase
    And its `check` and `testCheck` goals both use the `jdk-unsafe` and `jdk-deprecated` bundles targeting Java 17
    And no signature exclusions or `@SuppressForbidden` escape hatch are configured

  Scenario: A clean checkout of develop is green with the gate added
    Given a clean checkout of `develop` with this change applied
    When `mvn verify` is run
    Then the build succeeds
    And every default-charset and default-locale call site that existed before this change has been rewritten with an explicit charset or locale

  Scenario Outline: A forbidden signature in main or test code fails verify
    Given a change adds <call> to a class under <source root>
    When `mvn verify` is run
    Then the build fails
    And the failure names the class, the line, and the forbidden signature

    Examples:
      | call                                    | source root |
      | `"x".getBytes()`                        | `src/main`  |
      | `new String(bytes)`                     | `src/main`  |
      | `new FileReader(file)`                  | `src/main`  |
      | `name.toLowerCase()`                    | `src/main`  |
      | `String.format("%d", n)`                | `src/main`  |
      | `"x".getBytes()`                        | `src/test`  |
      | `name.toUpperCase()`                    | `src/test`  |

  Scenario Outline: The explicit-charset or explicit-locale form passes
    Given a change adds <call> to a class under `src/main`
    When `mvn verify` is run
    Then the forbidden-apis check reports no violation for it

    Examples:
      | call                                                  |
      | `"x".getBytes(StandardCharsets.UTF_8)`                |
      | `new String(bytes, StandardCharsets.UTF_8)`           |
      | `Files.newBufferedReader(path, StandardCharsets.UTF_8)` |
      | `name.toLowerCase(Locale.ROOT)`                       |
      | `String.format(Locale.ROOT, "%d", n)`                 |

  Scenario: A deprecated JDK API fails verify
    Given a change adds a call to a JDK API listed in the `jdk-deprecated` bundle for Java 17
    When `mvn verify` is run
    Then the build fails, naming the deprecated signature

  Scenario: @SuppressWarnings does not silence the gate
    Given a change adds `"x".getBytes()` inside a method annotated `@SuppressWarnings("all")`
    When `mvn verify` is run
    Then the build still fails on that call

  Scenario: The gate does not run under mvn test
    Given a change adds `"x".getBytes()` to a class under `src/main`
    When `mvn test` is run
    Then the forbidden-apis check does not execute

  Scenario: PMD no longer double-reports default-charset or default-locale use
    Given this change is complete
    Then `.pmd-clean-code.xml` no longer references `RelianceOnDefaultCharset`
    And `.pmd-clean-code.xml` no longer references `UseLocaleWithCaseConversions`
    And Error Prone's `StringCaseLocaleUsage` is still configured at ERROR

  Scenario: A no-arg case conversion still fails at compile time
    Given a change adds `name.toLowerCase()` to a class under `src/main`
    When `mvn compile` is run
    Then the build fails on Error Prone's `StringCaseLocaleUsage`

  Scenario: The documentation lands with the gate
    Given this change is complete
    Then `docs/clean-code-gate.md`'s coverage table gains a row for default-charset/default-locale and deprecated JDK API use, enforced by forbidden-apis repo-wide at `mvn verify`
    And `impacts.md` lists every call site that could not be fixed, with its reason, or states that there are none

  # Non-goals:
  #   - Other bundles (jdk-system-out, jdk-non-portable, jdk-internal) and
  #     third-party signature bundles. Each would be its own issue with its
  #     own call-site measurement.
  #   - Project-specific custom signature files.
  #   - Diff-scoping through check-clean.sh. Every call site is fixed here,
  #     so the gate can be repo-wide from day one, the same way
  #     .pmd-minimal.xml is.
  #   - Removing Error Prone's StringCaseLocaleUsage. It gives earlier
  #     feedback than this gate.
  #
  # Risks:
  #   - Third-party types are not in jdk-unsafe, so default-charset use
  #     hidden inside a library (e.g. a JSON parser handed a Reader we
  #     didn't build) isn't caught. Only our own call sites are checked.
  #   - jdk-unsafe flags String.format without a Locale, which is common in
  #     rendering/HUD code. The fix is mechanical (Locale.ROOT), but the
  #     call-site count may be larger than the PMD rules suggested.
  #   - testCheck needs the test classpath (JUnit, Cucumber, AssertJ) to
  #     resolve signatures. A missing-class failure there is configuration,
  #     not a violation. Fix the classpath rather than setting
  #     failOnMissingClasses=false, which would hide real gaps.
  #
  # Open questions:
  #   - None outstanding. Whether to drop the overlapping locale checks was
  #     settled in specs/intent/forbidden-apis.md's Clarifications: drop the
  #     PMD rule and keep Error Prone.
