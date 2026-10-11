@manual-verification
Feature: Full PMD rule catalogue as a strict Clean Code gate
  `.pmd-clean-code.xml` currently enforces 52 PMD rules, picked ad hoc over
  time; the other 272 rules in PMD 7's catalogue have never been audited
  against this codebase. This feature covers auditing all 324 native rules
  and adding 196 of the 200 verdicted ADD (4 exist only on PMD's unreleased
  `main` branch as of this change - see Clarification C7 - and are tracked
  in `impacts.md` for a later PMD release), vendoring 19 jPinpoint
  `XPathRule` definitions with no PMD-native equivalent, fixing three rule
  references on deprecated PMD 7 paths, restoring four demoted Error Prone
  checks to ERROR together with the 22 call-sites that restoration breaks
  (C9 corrected this from the issue's originally-measured 10), and scoping
  `AvoidInstantiatingObjectsInLoops` to the render path. It supersedes
  nothing — `.pmd-clean-code.xml` and `check-clean.sh`'s diff-scoping already
  exist and are unchanged in kind, only in rule count.

  The governing principle, set by the human on 2026-09-12 and recorded in the
  intent doc's Clarifications (C0-C10): rules are set to what is correct, not
  to what the codebase currently passes. No threshold is loosened, no rule
  dropped, and no exclusion added in order to accommodate existing code.
  Where a correct rule flags real code, that is an `impacts.md` finding to
  plan and fix later. That is why `LawOfDemeter` (73 measured violations) and
  both jPinpoint suppression-hygiene rules (3 measured violations) are in,
  and why the seven Phase 2 rules land at PMD defaults rather than at
  thresholds derived from the current baseline. The same principle extended
  to a version gap discovered during implementation (C7): 18 of the 200
  native rules only exist from PMD 7.2x onward, so `pom.xml` now pins PMD
  7.27.0 (the latest stable release) instead of `maven-pmd-plugin`'s default
  7.17.0, rather than dropping all 22 unresolved rule names.

  It is tagged `@manual-verification` for the same reason
  `pmd-jacoco-quality-gates.feature` is: the behavior under test is
  `check-clean.sh`/`mvn verify`'s own pass/fail, which a Cucumber step
  running inside that same build cannot assert on itself.

  Explicitly out of scope: fixing any violation the expanded ruleset surfaces
  against the existing codebase (that's `impacts.md`'s job, as input to
  future planning) — the sole exception being the 22 Error Prone call-sites,
  which have to be fixed for the restored ERROR severities to compile at all;
  the ~120 jPinpoint rules with no surface here (Spring/Reactor/JPA/SOAP/
  HttpClient); the 66 native PMD rules with no surface here (EJB/Android/
  J2EE/JDBC/crypto/JUnit 3-4/deprecated aliases); the 4 native rules not yet
  in any released PMD version (C7).

  Scenario: The expanded ruleset loads with every rule reference resolved
    Given `.pmd-clean-code.xml` contains its existing 52 rules plus the 196 newly-added native PMD rules and 19 vendored jPinpoint rules
    When the ruleset is loaded
    Then every rule reference resolves to a known rule definition
    And the number of active rules matches the configured total

  Scenario Outline: A known-violating fixture fails its rule family
    Given a fixture source file containing a violation from the <rule family> family
    When `check-clean.sh --all` is run against it
    Then the violation is reported against that fixture

    Examples:
      | rule family        |
      | complexity         |
      | naming             |
      | dead code          |
      | error handling     |
      | performance        |
      | vendored jPinpoint |

  Scenario: A clean fixture produces no violations
    Given a fixture source file that violates none of the active rules
    When `check-clean.sh --all` is run against it
    Then no violation is reported against that fixture

  Scenario: Diff scoping still excludes pre-existing violations on untouched lines
    Given a file with a pre-existing violation of a newly-added rule, on a line not touched by the current change
    And a different file where that same rule fires on a line the current change added
    When `check-clean.sh` is run in its default (diff-scoped) mode
    Then the untouched file's violation is not reported
    And the newly-added line's violation is reported

  Scenario: check-clean.sh --all completes at the new rule count
    Given the expanded ruleset (52 existing + 196 native + 19 jPinpoint rules) is active
    When `bash .claude/tools/check-clean.sh --all` is run against the whole repository
    Then it completes without timing out
    And produces a per-rule violation count for every active rule

  Scenario: No rule's threshold is derived from the current violation baseline
    Given the seven measured Phase 2 rules are active at their configured thresholds
    When each rule's configured properties are compared against PMD's defaults
    Then every threshold is PMD's default, or the project's pre-existing tuned value where that is stricter
    And no threshold was set from the `check-clean.sh --all` baseline
    And the measured baseline counts appear in `impacts.md` instead

  Scenario: LawOfDemeter is active despite its measured existing-code conflict
    Given `LawOfDemeter` was measured against the whole repository before the rule was added
    When the expanded ruleset is loaded
    Then `LawOfDemeter` is active at PMD's default settings
    And its measured violations are recorded in `impacts.md` with file and count
    And no exclusion, property override, or file-path carve-out was added for it

  Scenario: Both jPinpoint suppression-hygiene rules are active
    Given the repository's three existing `@SuppressWarnings("PMD.ExcessiveParameterList")` annotations
    When the expanded ruleset is loaded
    Then both `UsingSuppressWarnings` and `UsingSuppressWarningsHighRisk` are active
    And neither rule's `ruleIdMatches` or `ruleIdNotMatches` property has been narrowed from the jPinpoint default
    And each of the three annotations is recorded in `impacts.md`, with the recorded fix being to narrow `ExcessiveParameterList` so it does not fire on an `@Override` of a third-party interface method

  Scenario: Every vendored jPinpoint rule names its source
    Given the 19 vendored jPinpoint rules in `.pmd-clean-code.xml`
    When the file is read
    Then each vendored rule's XML carries a comment naming jPinpoint as the source and the rule's origin
    And the file records jPinpoint's Apache 2.0 licence once

  Scenario: The three deprecated PMD 7 rule references are replaced without losing coverage
    Given `AvoidCatchingGenericException` has moved from `category/java/design.xml` to `category/java/errorprone.xml`
    When `AvoidCatchingNPE` and `AvoidCatchingThrowable` are deleted as subsumed by it
    Then its `typesThatShouldNotBeCaught` covers NullPointerException, Exception, RuntimeException, Throwable and Error
    And a fixture catching `NullPointerException` is still reported
    And a fixture catching `Throwable` is still reported

  Scenario: The four demoted Error Prone checks are restored to ERROR and the build compiles
    Given `EnumOrdinal`, `StringCaseLocaleUsage`, `MissingSummary` and `ImmutableEnumChecker` are set to ERROR in the Error Prone compiler argument
    And the 22 existing call-sites those checks flag have been fixed (10 in src/main, 12 in src/test - measurement corrected in C9)
    When `mvn clean test-compile` is run over the whole repository
    Then it succeeds with no Error Prone diagnostic from any of the four checks
    And no check was re-demoted, suppressed, or scoped to a subset of packages

  Scenario: AvoidInstantiatingObjectsInLoops is scoped to the render path only
    Given the rule's repo-wide violation count has been measured and recorded in `impacts.md`
    And `GamePanel` and the widget paint-path classes are in the render-scoped rule routing
    And a violation of `AvoidInstantiatingObjectsInLoops` exists in a render-scoped class
    And an equivalent violation exists in a class outside the render path
    When `check-clean.sh` evaluates both
    Then the render-scoped violation is reported
    And the out-of-render-path violation is not reported under this rule

  Scenario: impacts.md records every violation the expanded ruleset measures
    Given `check-clean.sh --all` has been run against the expanded ruleset
    When `impacts.md` is written
    Then every rule with at least one violation has an entry naming the rule, file and count, the correct fix in one sentence, whether it changes player-visible behavior, and a rough size
    And no entry in `impacts.md` has been fixed as part of this change, apart from the 22 Error Prone call-sites the restored ERROR severities require

  Scenario: docs/clean-code-gate.md stays in step with the ruleset's sections
    Given `.pmd-clean-code.xml`'s description states its sections map 1:1 onto `docs/clean-code-gate.md`
    Then `docs/clean-code-gate.md`'s rule table reflects the new sections, rule counts, and the three vendored-jPinpoint/deprecated-path/render-scoping notes above

  # Non-goals:
  #   - Fixing any violation the expanded ruleset surfaces against the
  #     existing codebase — that's impacts.md's job as input to future
  #     planning, not this change's. The 22 Error Prone call-sites are the
  #     one exception, and only because Error Prone has no diff-scoping.
  #   - The ~120 jPinpoint rules with no surface here (Spring/Reactor/JPA/
  #     SOAP/HttpClient), and the 66 native PMD rules with no surface here.
  #   - The 4 native rules not yet in any released PMD version (C7).
  #   - Binding any part of this ruleset to `mvn verify` directly —
  #     `.pmd-clean-code.xml` stays diff-scoped via check-clean.sh, the same
  #     as its existing 52 rules.
  #   - Asserting any specific violation count. Counts live in impacts.md and
  #     change as the codebase is fixed; a scenario pinned to one would have
  #     to be loosened later, which is the failure mode this feature exists
  #     to prevent.
  #
  # Risks:
  #   - Restoring the four Error Prone checks changes a javac/Error Prone
  #     compiler argument, which has no diff-scoping analogous to
  #     check-clean.sh's: every future violation anywhere in the repo fails
  #     the build immediately. That is the intended, accepted cost.
  #   - The ImmutableEnumChecker fix restructures PlayerField (dropping the
  #     Spec record for switch expressions) rather than annotating it
  #     @Immutable — the annotation route needs a new compile-scope
  #     dependency and would still fail on the unannotated
  #     ToIntFunction/ObjIntConsumer field types.
  #   - The StringCaseLocaleUsage fix (.toLowerCase(Locale.ROOT) in
  #     ModLoader) changes mod content-type matching under Turkish-family
  #     locales. This is a behaviour change, in the correct direction, and is
  #     recorded in impacts.md as such rather than avoided.
  #   - LawOfDemeter at PMD defaults is known to flag some fluent/builder
  #     chains. Measured here at 73 violations across 21 files, not the
  #     "hundreds" the decision file assumed. If false positives prove real,
  #     the lever is a later evidence-backed narrowing issue, never a
  #     pre-emptive exclusion.
  #
  # Open questions:
  #   - None. All five are resolved in the intent doc's Clarifications
  #     section (C0-C6).
