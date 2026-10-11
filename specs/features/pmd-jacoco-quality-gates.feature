@manual-verification
Feature: PMD and JaCoCo CI-enforced quality gates
  `mvn verify` enforces the function-size/complexity/parameter/duplication
  and coverage budgets documented in `.claude/workflow.md`, instead of
  relying on the agent's own judgment to follow them. Pure Swing
  layout/wiring classes with no real branching logic are excluded from
  both gates, the same way they're already excluded from PIT mutation
  testing.

  Coverage is enforced at two levels: a repo-wide `BUNDLE` line-coverage
  ratio (85%, unchanged), and a per-`SOURCEFILE` line-coverage floor (lower
  than the bundle floor by design — it exists to catch a brand-new class
  shipping at 0% coverage, not to force 85% on every thin class). Both
  levels also carry a `BRANCH` coverage floor alongside their `LINE` floor,
  closing the gap where line coverage alone can't tell a fully-exercised
  `if` from one that only ever took its true branch. Exact floor values are
  set from a measured baseline (see `docs/testing.md` § "Code quality
  gates").

  The per-file scope is `SOURCEFILE`, not `CLASS`, and that is load-bearing:
  JaCoCo's `CLASS` element counts compiled `.class` files, so every nested
  and anonymous class is judged as its own unit. Measured on `develop`, the
  worst such rows are incidental inner classes of already-tested widgets,
  which forces any green `CLASS` floor down to ~0% — a gate that catches
  nothing. `SOURCEFILE` judges the unit somebody actually adds (a `.java`
  file) and needs no per-inner-class exclusion list.

  These floors ratchet up, never down. That is not a convention anyone has
  to remember: `.claude/tools/check-quality-gates.sh` fails any PR that
  lowers a threshold or adds an exclusion.

  Scenario: A compliant codebase passes verify
    Given no non-excluded function exceeds cyclomatic complexity 8, 40 lines, or 4 parameters
    And no non-excluded code block duplicates another at or above PMD CPD's default 100-token threshold
    And repo-wide line coverage across non-excluded classes is at least 85%
    And repo-wide branch coverage across non-excluded classes is at least the configured bundle branch floor
    And every non-excluded source file individually meets the configured file-level line and branch floors
    When `mvn verify` is run
    Then the build succeeds

  Scenario Outline: A single-metric PMD violation fails the build
    Given a non-excluded function <violates>
    When `mvn verify` is run
    Then the build fails with a PMD violation report identifying the offending function and rule

    Examples:
      | violates                                   |
      | has cyclomatic complexity greater than 8   |
      | is longer than 40 lines                    |
      | takes more than 4 parameters               |

  Scenario: Duplicate code fails the build
    Given two non-excluded code blocks duplicate each other at or above PMD CPD's default 100-token threshold
    When `mvn verify` is run
    Then the build fails with a PMD CPD violation report identifying both duplicate locations

  Scenario: Repo-wide coverage below threshold fails the build
    Given repo-wide line coverage across non-excluded classes is below 85%
    When `mvn verify` is run
    Then the build fails with a JaCoCo `BUNDLE` coverage-check violation

  Scenario: Repo-wide branch coverage below threshold fails the build
    Given repo-wide branch coverage across non-excluded classes is below the configured bundle branch floor
    When `mvn verify` is run
    Then the build fails with a JaCoCo `BUNDLE` `BRANCH` coverage-check violation

  Scenario: A new zero-coverage class fails the build even with the bundle ratio intact
    Given a new non-excluded class ships in its own source file with no unit tests
    And the repo-wide bundle line-coverage ratio still meets 85% because the rest of the corpus carries it
    When `mvn verify` is run
    Then the build fails with a JaCoCo `SOURCEFILE`-level coverage-check violation identifying that file

  Scenario: A source file below the file-level branch floor fails the build
    Given a non-excluded source file has line coverage above the file-level line floor but branch coverage below the file-level branch floor
    When `mvn verify` is run
    Then the build fails with a JaCoCo `SOURCEFILE`-level `BRANCH` coverage-check violation identifying that file

  Scenario: A nested class is judged as part of its enclosing source file, not on its own
    Given a non-excluded source file whose enclosing class is well covered
    And that file contains a nested or anonymous class with no coverage of its own
    And the file's aggregate line and branch coverage still meet the file-level floors
    When `mvn verify` is run
    Then the build succeeds
    And no coverage-check violation is reported against the nested class as a separate unit

  Scenario: Excluding a source file also excludes its nested classes
    Given a source file on the pure-Swing-layout/wiring exclusion list contains an anonymous inner class
    When `mvn verify` is run
    Then JaCoCo evaluates no `SOURCEFILE` floor against that file
    And the inner class's lines and branches are not counted toward the repo-wide `BUNDLE` ratio

  Scenario: Excluded pure-layout classes are not held to any coverage gate
    Given a class on the pure-Swing-layout/wiring exclusion list has no unit test coverage and would otherwise exceed a PMD budget
    When `mvn verify` is run
    Then PMD does not fail the build over that class's complexity, length, parameter count, or duplication
    And JaCoCo does not count that class's lines or branches toward or against the repo-wide `BUNDLE` threshold
    And JaCoCo does not evaluate that class against the `SOURCEFILE`-level line or branch floor

  Scenario: workflow.md's coverage wording matches what's actually enforced
    Given `.claude/workflow.md`'s constraints section
    Then it states coverage is enforced repo-wide (e.g. "85% repo-wide line coverage"), not "on changed files"

  # Non-goals:
  #   - Moving PIT mutation testing into CI as a gate — stays a manual
  #     Step 6 command (mvn org.pitest:pitest-maven:mutationCoverage).
  #   - CRAP score (complexity x coverage composite) — no maintained tool
  #     for it against JaCoCo.
  #   - SpotBugs, SonarQube, or any static analysis tool beyond PMD.
  #   - True changed-files-only coverage enforcement (needs a diff-coverage
  #     tool beyond plain JaCoCo) — repo-wide is the accepted reading.
  #   - Writing new unit tests for a class purely to pad the coverage
  #     number once it's confirmed to be pure layout with no real logic.
  #
  # Risks:
  #   - PIT's <targetClasses> allowlist in pom.xml, the starting point for
  #     the new exclusion list, was found stale during spec drafting: it
  #     named two classes that no longer exist (SelectableMenu, MenuPanel
  #     — superseded by the widget-framework work in
  #     ui-component-framework.md), and its complement would otherwise
  #     have excluded several classes that are clearly not "pure layout
  #     with no logic to unit test" — e.g. ListWidget (146 lines),
  #     TableWidget (351 lines), RadioGroupWidget (285 lines), and
  #     SliderWidget (91 lines), all of which already have dedicated
  #     *Test.java files and real selection/scroll/bounds logic. Resolved
  #     via the intent doc's Clarifications: the exclusion list is built
  #     hybrid-style (start from PIT's list, pull back in any class with
  #     an existing dedicated unit test, exclude untested classes only
  #     after confirming they're genuinely pure layout), and this issue
  #     also removes PIT's two dead entries as a drive-by fix.
  #   - Retrofitting the gates onto the existing codebase "green by the
  #     time it's done" may still require some new test-writing for
  #     currently-untested classes that turn out to have real logic —
  #     actual size unknown until Step 4 runs PMD/JaCoCo for real and
  #     applies the hybrid derivation per class.
  #
  # Open questions:
  #   - None outstanding — the exclusion-list derivation method and the
  #     PIT stale-entry cleanup were both settled via a grilling round
  #     during spec drafting.
  #
  # --- Added for issue #196 (class-level + branch floors) ---
  #
  # Non-goals (this addition):
  #   - Raising the repo-wide 85% line floor — untouched by this issue.
  #   - PIT's own mutationThreshold — that's a separate concept (mutation
  #     score, not JaCoCo coverage) covered by pit-mutation-threshold.feature.
  #   - Reconciling the three separate PMD/JaCoCo/PIT exclusion lists that
  #     currently drift against each other — explicitly out of scope per
  #     the source issue; the new SOURCEFILE rule reuses the existing
  #     JaCoCo BUNDLE exclusion list rather than deriving a new one. (The
  #     ratchet check in quality-gate-ratchet.feature does make additions
  #     to any of the three visible in one place, which is not the same as
  #     unifying them.)
  #
  # Open questions (this addition):
  #   - None. Two were resolved during drafting, both recorded in
  #     specs/intent/coverage-gate-floors.md's Clarifications section:
  #     whether the BRANCH limit applies to BUNDLE as well as the new rule
  #     (it does), and what granularity the new rule uses (SOURCEFILE, not
  #     the CLASS the source issue named — see
  #     specs/intent/coverage-gate-floors-class-scope.md for the
  #     measurement that settled it; issue #196 amended to match).
  #   - Floor values are measured, not pending: BUNDLE branch 0.78
  #     (baseline 78.98%), SOURCEFILE line 0.35 and branch 0.50 (worst
  #     non-excluded file, FillLayout.java, at 35.7% / 50.0%).
