@manual-verification
Feature: Error Prone expanded beyond its default check set
  Error Prone (`error_prone_core`, compiler plugin, see
  `error-prone-nullaway.feature`) currently runs only its ON_BY_DEFAULT
  checks, plus NullAway and the four checks `pmd-full-rule-catalogue.feature`
  restored to ERROR. Default-on checks whose severity is WARNING never fail
  the build (there is no `-Werror`): 18 such warnings across 7 checks pass
  silently as of 2026-10-01. This feature audits every check Error Prone
  ships that does not currently fail the build — the default-on WARNING
  tier, the experimental tier and the off-by-default suggestion tier —
  decides each one ADD or EXCLUDE with a recorded reason, and enables the
  ADD set at ERROR. Error Prone's dataflow analysis finds
  defects of a different kind than the PMD catalogue, so this complements
  `pmd-full-rule-catalogue.feature` rather than overlapping it.

  It extends `error-prone-nullaway.feature`'s Error Prone configuration and
  supersedes that file's non-goal "Error Prone's optional/experimental check
  sets — only the default set". It supersedes nothing else.

  Governing principle, carried over from #215: a check is set to what is
  correct, never demoted to fit existing code. Unlike PMD, an Error Prone
  check at ERROR fails `javac` with no diff-scoping, so every violation the
  ADD set finds in `src/main` and `src/test` is fixed by hand in this
  change — no `@SuppressWarnings`, no auto-patching — and recorded in the
  impacts file.

  EXCLUDE is allowed only for an objective reason (intent Clarifications):
  the check has no surface here (Android/Dagger/Guice/Flogger/AutoValue/GWT
  etc.), it contradicts another enabled Error Prone check or PMD rule, it
  needs a new dependency, or it is deprecated or a no-op in the pinned
  version. A noisy or style-only check is ADD.

  Tagged `@manual-verification` for the same reason as the other gate
  features: the behavior under test is `mvn compile`'s own pass/fail, which
  a Cucumber step running inside that build cannot assert on itself.

  Explicitly out of scope: the four checks #215 already restored; NullAway
  configuration and `@NullMarked` coverage; new analysis tools; Error Prone
  version bumps.

  QA: none - build tooling, no keyboard input

  Background:
    Given every Error Prone check in the pinned `error_prone_core` version that is not already at ERROR has a recorded ADD or EXCLUDE decision with a one-line reason
    And each ADD check is enabled with its own `-Xep:<Check>:ERROR` flag
    And no `-XepAllDisabledChecksAsWarnings`, `-XepAllSuggestionsAsWarnings`, `-XepPatchChecks` or `-XepPatchLocation` flag is configured

  Scenario: A clean checkout is green with the expanded set
    Given a clean checkout of `develop` with this change applied
    When `mvn clean test-compile` is run
    Then the build succeeds
    And no Error Prone diagnostic is reported from any ADD check

  Scenario Outline: A violation of an ADD check fails the compile
    Given a change in <source set> introduces a violation of an ADD check from the <tier> tier
    When `mvn clean test-compile` is run
    Then the build fails with an Error Prone error naming the check, file and line

    Examples:
      | source set | tier                     |
      | src/main   | default-on WARNING       |
      | src/test   | default-on WARNING       |
      | src/main   | experimental/suggestion  |
      | src/test   | experimental/suggestion  |

  Scenario: The default-on WARNING checks no longer pass silently
    Given the 7 default-on WARNING checks that reported findings on 2026-10-01
    When this change is complete
    Then each of them is ADD at ERROR, or EXCLUDE for one of the four objective reasons
    And `mvn clean test-compile` reports no Error Prone warning from any ADD check

  Scenario: Every EXCLUDE names an objective reason
    Given the recorded decisions
    When each EXCLUDE is read
    Then its reason is one of: no surface in this codebase, contradicts another enabled check or PMD rule, needs a new dependency, deprecated or no-op
    And no check is excluded only for being noisy or style-only

  Scenario: Checks needing a new dependency are excluded and listed
    Given a check that only works with annotations from a dependency the project does not declare (e.g. `Var` and `@Var` from `error_prone_annotations`)
    When it is audited
    Then it is EXCLUDE with the reason "needs new dependency"
    And it appears in a list of such checks for a follow-up issue
    And `pom.xml` gains no new dependency

  Scenario: An EXCLUDE check stays off
    Given a check recorded as EXCLUDE
    And a change introduces code that check would flag
    When `mvn clean test-compile` is run
    Then no diagnostic from that check is reported

  Scenario: A check new in a later Error Prone version is not enabled silently
    Given the enable mechanism is opt-in per check
    When `error_prone_core` is bumped to a version that ships a new check
    Then that check is not enabled at ERROR until it is given its own decision and flag

  Scenario: Existing violations are fixed, not suppressed or demoted
    Given the ADD set was measured against `src/main` and `src/test` before any fix
    When this change is complete
    Then every measured violation has been fixed by hand
    And no `@SuppressWarnings` naming an Error Prone check was added
    And no ADD check was demoted below ERROR or scoped to a subset of packages or source sets

  Scenario: The Windows single-arg constraint still holds
    Given the Error Prone and NullAway flags have grown
    When `pom.xml`'s `maven-compiler-plugin` configuration is read
    Then the Error Prone/NullAway flags are either one `<arg>` with no embedded newline, or read from an args file or Maven property
    And `mvn clean test-compile` succeeds on Windows with `<fork>true</fork>`

  Scenario: The impacts file records every violation fixed
    Given the ADD set's violations were measured before fixing
    When the impacts file is written
    Then every ADD check with at least one violation has an entry naming the check, file(s), count, and the fix applied

  Scenario: Compile time impact is measured and stated
    Given this change is complete
    Then the PR description states the `mvn clean test-compile` wall-clock time before and after

  Scenario: The documentation lands with the gate
    Given this change is complete
    Then `docs/testing-quality-gates.md` describes the expanded set and how to decide a check added by a future Error Prone version
    And the pom's Error Prone comment block no longer calls the four restored checks "demoted"

  # Non-goals:
  #   - The four checks restored by #215 (EnumOrdinal, StringCaseLocaleUsage,
  #     MissingSummary, ImmutableEnumChecker).
  #   - NullAway configuration or widening @NullMarked coverage.
  #   - New analysis tools or dependencies; Error Prone version bumps.
  #   - Auto-patching (-XepPatchChecks); all fixes are by hand.
  #   - Adding error_prone_annotations or any other dependency; checks that
  #     need one are listed for a follow-up issue instead.
  #   - Asserting a specific check or violation count — counts live in the
  #     impacts file.
  #
  # Risks:
  #   - Every fix happens in this change, so the diff may be large and touch
  #     player-visible code paths; any behaviour-changing fix needs flagging
  #     in the impacts file and the playtest.
  #   - Some experimental checks are slow; compile time is the agent's inner
  #     loop (see the compile-time scenario).
  #   - A long flag list on one <arg> is hard to review; an args file must
  #     still survive the Windows batch-file fork (google/error-prone#4256).
  #
  # Open questions:
  #   - None. Resolved in the intent doc's Decisions and Clarifications
  #     (WARNING tier included; objective-only EXCLUDE reasons; dependency-
  #     needing checks excluded and listed).
