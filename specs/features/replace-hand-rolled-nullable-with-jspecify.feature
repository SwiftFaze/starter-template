@manual-verification
Feature: Replace the hand-rolled @Nullable with JSpecify
  `com.swiftfaze.veil.annotations.Nullable` was a deliberate, scoped-down
  stand-in added when NullAway landed (error-prone-nullaway.feature, issue
  #199), chosen specifically to avoid a new transitive dependency at the
  time. NullAway matches it today only because NullAway resolves `@Nullable`
  by simple name regardless of package — nothing else does. This feature
  swaps it for `org.jspecify.annotations.Nullable`, the joint Google/
  JetBrains/NullAway-team standard, at the same two call sites, and removes
  the hand-rolled annotation and its now-empty package entirely.

  It also adopts `@NullMarked`, JSpecify's package-level non-null-by-default
  idiom, on every package NullAway's `AnnotatedPackages` build arg currently
  covers (`com.swiftfaze.veil.world`, `com.swiftfaze.veil.entities`, and its
  `buildings`/`items`/`player`/`player.classes`/`quests` subpackages —
  `@NullMarked` does not cascade to subpackages, so each needs its own
  `package-info.java`), and switches NullAway to
  `-XepOpt:NullAway:OnlyNullMarked=true -XepOpt:NullAway:JSpecifyMode=true`
  (both required together), removing `AnnotatedPackages` entirely. This was
  measured, not assumed: a dry run with `@NullMarked` + JSpecify mode across
  the full package set produced zero new findings against production code,
  confirmed live (not silently disabled) by a deliberate violation that
  NullAway caught. (The full `mvn verify` later found one real instance in
  test code, outside that dry run's scope — see Risks below.) See
  specs/intent/replace-hand-rolled-nullable-with-jspecify.md's
  Clarifications for both the measurement and its correction.

  It supersedes error-prone-nullaway.feature's Non-goals line "Adopting
  JSpecify or migrating any existing nullability annotation style already
  used elsewhere in the codebase" (that line is now stale; this file is
  that migration) and its Background's "NullAway loads as an Error Prone
  plugin, scoped to an explicit annotated-packages list" (the scoping
  mechanism is now `@NullMarked` package-info files plus JSpecify mode, not
  the `AnnotatedPackages` build arg). Everything else in
  error-prone-nullaway.feature is unchanged: which packages are covered,
  Error Prone's default check set, and the growth-only-never-shrinks policy
  for which packages opt in (a package still only ever gains `@NullMarked`,
  never loses it, mirroring the old list's one-directional growth).

  This is a behavior-neutral swap for the two existing `@Nullable` sites,
  and a measured-zero-impact change for the annotated-packages-to-
  `@NullMarked` migration: every existing nullability finding must still
  fire, and no new one may appear, after either change.

  It is tagged `@manual-verification` for the same reason
  `error-prone-nullaway.feature` is: the behavior under test is
  `mvn compile`'s own pass/fail, which a Cucumber step running inside that
  same build cannot assert on itself.

  Background:
    Given `org.jspecify:jspecify` is a declared Maven dependency
    And `WorldScene.getTile(int, int)` returns `org.jspecify.annotations.@Nullable Tile`
    And `PlayerClass.StatCurve.growthCalc` is typed `org.jspecify.annotations.@Nullable String`
    And `com.swiftfaze.veil.annotations.Nullable` no longer exists in the source tree
    And the `com.swiftfaze.veil.annotations` package no longer exists
    And `com.swiftfaze.veil.world` and `com.swiftfaze.veil.entities` (including its `buildings`, `items`, `player`, `player.classes`, and `quests` subpackages) each have a `package-info.java` annotated `@NullMarked`
    And NullAway runs with `-XepOpt:NullAway:OnlyNullMarked=true` and `-XepOpt:NullAway:JSpecifyMode=true` (both required together — NullAway 0.13.8 rejects `JSpecifyMode` alone with neither `AnnotatedPackages` nor `OnlyNullMarked` set)
    And NullAway's `AnnotatedPackages` argument no longer appears in `pom.xml`

  Scenario: A clean checkout of develop is green
    Given a clean checkout of `develop` with this change applied
    When `mvn compile` is run
    Then the build succeeds
    And NullAway ran (not silently skipped)

  Scenario Outline: NullAway still enforces non-null-by-default identically after the migration
    Given a change in <package> assigns `null` to a field with no `@Nullable` annotation
    When `mvn compile` is run
    Then the build fails with a NullAway finding naming the file and line

    Examples:
      | package                                        |
      | com.swiftfaze.veil.world                       |
      | com.swiftfaze.veil.entities                    |
      | com.swiftfaze.veil.entities.buildings          |
      | com.swiftfaze.veil.entities.items              |
      | com.swiftfaze.veil.entities.player             |
      | com.swiftfaze.veil.entities.player.classes     |
      | com.swiftfaze.veil.entities.quests             |

  Scenario: The two existing @Nullable sites still permit null after the swap
    Given `WorldScene.getTile` returns `null` for out-of-bounds coordinates
    And a `PlayerClass.StatCurve` is constructed with a `null` `growthCalc`
    When `mvn compile` is run
    Then the build succeeds
    And NullAway reports no finding against either site

  Scenario: A package outside the @NullMarked set is not null-checked
    Given a change in a package with no `@NullMarked` package-info (e.g. `com.swiftfaze.veil.ui`) assigns `null` to a field with no `@Nullable` annotation
    When `mvn compile` is run
    Then the build succeeds
    And no NullAway finding is reported for that assignment

  Scenario: The hand-rolled annotation is gone
    Given the source tree after this change
    Then no file references `com.swiftfaze.veil.annotations.Nullable`
    And `src/main/java/com/swiftfaze/veil/annotations/` does not exist

  Scenario: The documentation reflects the new scoping mechanism
    Given this change is complete
    Then `docs/testing-quality-gates.md` describes adding a new package to nullability checking as adding a `@NullMarked` package-info file, not editing an `AnnotatedPackages` list
    And the "only grows, never shrinks" policy is restated in terms of `@NullMarked` packages

  # Non-goals:
  #   - Broader adoption of JSpecify annotations beyond the two existing
  #     @Nullable sites (e.g. annotating currently-unannotated
  #     fields/parameters that NullAway doesn't already flag).
  #   - Extending @NullMarked coverage to any package beyond the ones
  #     NullAway's AnnotatedPackages arg already covered before this change.
  #     Widening coverage further is a later, separate change, same as
  #     error-prone-nullaway.feature's own ratchet policy.
  #   - Kotlin interop work of any kind.
  #   - Removing the `com.swiftfaze.veil.annotations` package's other
  #     purpose as a plausible home for suppression bypasses (issue #216's
  #     own Note) — that's a side effect worth mentioning in the PR
  #     description, not a scenario to assert on here.
  #
  # Risks:
  #   - `@NullMarked` does not cascade to subpackages the way the old
  #     AnnotatedPackages string-prefix match did. Missing a
  #     package-info.java in any of the four entities subpackages would
  #     silently narrow nullability coverage — the Scenario Outline above
  #     exists specifically to catch that.
  #   - JSpecify mode's stricter generic-type-argument nullness checking
  #     was measured at zero new findings against production code (see
  #     intent doc's Clarifications), but that measurement only covered
  #     `mvn compile`, not `mvn verify`. The full build found one real
  #     instance in test code: PlayerClassTest.growthCalcs() (same package
  #     as PlayerClass, now @NullMarked) returned Arbitrary<String> from a
  #     generator that injects null 20% of the time - a pre-existing
  #     type-accuracy gap AnnotatedPackages mode never caught. Fixed by
  #     annotating the actual nullable generic type argument, not by
  #     suppressing. This is a strengthening, not a regression, and is the
  #     kind of thing worth re-checking (`mvn clean verify`, not just
  #     `mvn compile`) if this scoping mechanism is extended to a new
  #     package later.
  #   - NullAway 0.13.8 requires exactly one of `AnnotatedPackages` or
  #     `OnlyNullMarked` - `JSpecifyMode=true` with neither set fails the
  #     build with a misleading `[options] location of system modules is
  #     not set in conjunction with -source 17` diagnostic that looks like
  #     a JDK/Windows toolchain problem and is not. `OnlyNullMarked=true`
  #     is the fix; see intent doc's Clarifications correction entry.
  #
  # Open questions:
  #   - None. Resolved in the intent doc's Clarifications section.
