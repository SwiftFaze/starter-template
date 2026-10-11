@manual-verification
Feature: Error Prone + NullAway — compile-time bug detection and incremental null-safety
  Every existing static-analysis gate (`pmd-jacoco-quality-gates.feature`,
  `spotbugs-fb-contrib.feature`, `module-dependency-gate.feature`) runs as a
  separate analysis phase, after `javac` has already produced class files.
  Error Prone is different: it is a compiler plugin, so it reports at
  `javac` time and cannot be skipped the way a separate `verify`-bound
  phase can — failing the compile is failing the build the agent already
  has to run. Its default check set catches defects neither PMD nor
  SpotBugs reliably will: `MissingCasesInEnumSwitch` (every time a
  tile/entity/class enum gains a constant), `@Override` on a method that
  overrides nothing after a rename, format string mismatches, mutable
  static constants, `ReferenceEquality` on boxed types.

  NullAway rides on the same compiler-plugin hook and enforces non-null-by-
  default over an explicit, ratcheting list of annotated packages —
  `@Nullable` is the only way to opt a reference out. This file starts that
  ratchet on `com.swiftfaze.veil.world` and `com.swiftfaze.veil.entities`
  (including its `player`, `items`, `buildings`, and `quests`
  subpackages) — chosen because a null tile or a null entity/player field
  is where a null reference actually crashes the game, not because they
  were the easiest packages to satisfy.

  This feature does not annotate the whole codebase (see Non-goals below);
  the annotated-packages list is designed to grow one package at a time in
  later, separate changes.

  Background:
    Given `maven-compiler-plugin` loads Error Prone with `error_prone_core` on its annotation processor path
    And Error Prone runs with the default check set, with any demoted or disabled check carrying a one-line reason in `pom.xml`
    And the JDK 17 compiler-plugin flags (`-XDcompilePolicy=simple`, `--should-stop=ifError=FLOW`, and the required `-J--add-exports` entries) are configured on `maven-compiler-plugin`
    And NullAway loads as an Error Prone plugin, scoped to an explicit annotated-packages list containing `com.swiftfaze.veil.world` and `com.swiftfaze.veil.entities`

  Scenario: A clean checkout of develop is green
    Given a clean checkout of `develop` with this change applied
    When `mvn compile` is run
    Then the build succeeds
    And Error Prone and NullAway both ran (not silently skipped)

  Scenario: A default-check Error Prone finding fails the compile
    Given a change introduces a method annotated `@Override` that overrides nothing (e.g. after a rename)
    When `mvn compile` is run
    Then the build fails with an Error Prone finding naming the file and line

  Scenario Outline: Assigning null to a non-@Nullable field in an annotated package fails the compile
    Given a change in <package> assigns `null` to a field with no `@Nullable` annotation
    When `mvn compile` is run
    Then the build fails with a NullAway finding naming the file and line

    Examples:
      | package                                       |
      | com.swiftfaze.veil.world                      |
      | com.swiftfaze.veil.entities                   |
      | com.swiftfaze.veil.entities.player             |
      | com.swiftfaze.veil.entities.items              |
      | com.swiftfaze.veil.entities.buildings          |
      | com.swiftfaze.veil.entities.quests             |

  Scenario: A package outside the annotated-packages list is not null-checked
    Given a change in a package not on the NullAway annotated-packages list (e.g. `com.swiftfaze.veil.ui`) assigns `null` to a field with no `@Nullable` annotation
    When `mvn compile` is run
    Then the build succeeds
    And no NullAway finding is reported for that assignment

  Scenario: Compile time impact is measured and stated
    Given this change is complete
    Then the PR description states the `mvn compile` wall-clock time added by Error Prone + NullAway

  Scenario: The documentation lands with the gate
    Given this change is complete
    Then `docs/testing.md` documents how to add the next package to the NullAway annotated-packages list
    And `docs/testing.md` states that the annotated-packages list only grows, never shrinks
    And every Error Prone check demoted or disabled from the default set has a one-line reason next to it in `pom.xml`

  # Non-goals:
  #   - Annotating the whole codebase with NullAway in one change. The
  #     annotated-packages list is a ratchet: `world` and `entities` are the
  #     starting packages, not the ceiling. Widening it further is a later,
  #     separate change.
  #   - Adopting JSpecify or migrating any existing nullability annotation
  #     style already used elsewhere in the codebase.
  #   - Error Prone's optional/experimental check sets — only the default
  #     set, per issue #199.
  #   - Binding Error Prone/NullAway to a separate profile the way
  #     SpotBugs/fb-contrib's clean-code profile does. The entire point of
  #     a compiler-plugin gate is that `mvn compile` itself cannot succeed
  #     while skipping it — see spotbugs-fb-contrib.feature's Risks section
  #     for why that gate needed the opposite (profile) escape valve.
  #
  # Risks:
  #   - Error Prone's JDK 17 compiler-plugin flags are fiddly; a
  #     misconfiguration can leave the plugin silently checking nothing
  #     while `mvn compile` still reports green. The "clean checkout is
  #     green" scenario above is not sufficient proof by itself — the
  #     default-check and NullAway scenarios (a deliberately introduced
  #     violation that must fail) are what actually demonstrate the gate is
  #     live, per issue #199's own warning.
  #   - Error Prone runs on every `mvn compile`, which is the agent's inner
  #     loop, not just `verify`. If its added wall-clock time turns out to
  #     be large, that is a real cost to the existing spec-first pipeline's
  #     iteration speed — worth surfacing in the PR description even though
  #     issue #199 does not gate merge on a specific time budget.
  #
  # Open questions:
  #   - None outstanding. Both open questions from specs/intent/
  #     error-prone-nullaway.md (new-dependency approval; which package(s)
  #     start the NullAway ratchet) were resolved by the user before this
  #     spec was drafted — see that file's Clarifications section.
