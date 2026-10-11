@manual-verification
Feature: Quality gates ratchet up, never down
  The coverage and mutation floors in `pom.xml` only constrain behaviour if
  weakening them is harder than satisfying them. Today it is not: an agent
  that trips a gate has a cheaper path than writing a test — lower a
  `<minimum>`, or add its own class to an `<excludes>` list with a
  plausible-sounding comment. Neither is detected, and both land as one
  quiet line inside a 400-line `pom.xml` that no reviewer diffs closely.

  This feature makes weakening a gate a build failure.
  `.claude/tools/check-quality-gates.sh` compares the PR's gate
  configuration against the base branch and fails on any change in the
  weaker direction: a lowered JaCoCo floor, a lowered PIT
  mutationThreshold, or a newly added exclusion in any of the three
  PMD/JaCoCo/PIT lists. Strengthening a gate always passes.

  This is the same ratchet that `.claude/tools/check-instruction-budget.sh`
  already applies to instruction-file budgets, and it exists for the same
  reason CLAUDE.md gives for that one: "if it is mechanically checkable,
  write the check, not the rule". Issue #196 originally asked only that the
  ratchet be documented in `docs/testing.md`; prose is not enforcement
  against the actor the gates exist to constrain.

  Weakening a gate stays possible — it just stops being silent. The check
  fails the PR, so lowering a floor becomes an explicit act a human has to
  approve, with the reason recorded in the PR.

  Background:
    Given `.claude/tools/check-quality-gates.sh` runs as a `repo-hygiene.yml` job on every PR
    And it compares the head branch's gate configuration against the base branch's

  Scenario: An unchanged gate configuration passes
    Given a PR that does not touch any threshold or exclusion list
    When the quality-gate check runs
    Then it passes

  Scenario: Lowering a JaCoCo coverage floor fails the build
    Given a PR that lowers a JaCoCo `<minimum>` below its value on the base branch
    When the quality-gate check runs
    Then it fails, naming the gate, its base-branch value, and the proposed value

  Scenario: Raising a JaCoCo coverage floor passes
    Given a PR that raises a JaCoCo `<minimum>` above its value on the base branch
    When the quality-gate check runs
    Then it passes

  Scenario: Lowering the PIT mutation threshold fails the build
    Given a PR that lowers `<mutationThreshold>` below its value on the base branch
    When the quality-gate check runs
    Then it fails, naming the threshold, its base-branch value, and the proposed value

  Scenario Outline: Adding an exclusion to any gate's list fails the build
    Given a PR that adds an entry to <list>
    When the quality-gate check runs
    Then it fails, naming the added entry and the list it was added to
    And the failure message states that an exclusion is a gate weakening and needs a stated reason in the PR

    Examples:
      | list                                     |
      | JaCoCo's `jacoco-check` `<excludes>`     |
      | PMD's `<excludes>`                       |
      | PIT's `<excludedClasses>`                |

  Scenario: Removing an exclusion passes
    Given a PR that removes an entry from any of the three exclusion lists
    When the quality-gate check runs
    Then it passes

  Scenario: Narrowing PIT's target classes fails the build
    Given a PR that removes an entry from PIT's `<targetClasses>`
    When the quality-gate check runs
    Then it fails, naming the removed target
    And the failure message states that un-targeting a class removes it from mutation testing entirely

  Scenario: Release automation and the promotion PR are exempt
    Given a PR from a `release-please--branches--*` branch or from `develop`
    When the quality-gate check runs
    Then it skips, reporting the branch as exempt

  # Non-goals:
  #   - Blocking a weakening outright. The check makes it visible and
  #     human-approved, not impossible; a genuinely justified floor drop
  #     (e.g. a large excluded subsystem being deleted) must remain
  #     possible without editing the check itself.
  #   - Verifying that an exclusion is *justified* — that a class really is
  #     pure layout with no logic. Not mechanically checkable; the check
  #     only forces the claim to be made out loud in a PR a human reads.
  #   - Reconciling or unifying the three exclusion lists — out of scope
  #     per issue #196. This check makes additions to all three visible in
  #     one place, which is not the same thing.
  #   - Ratcheting the instruction-file budgets — already owned by
  #     .claude/tools/check-instruction-budget.sh.
  #   - The floor and threshold values themselves — owned by
  #     pmd-jacoco-quality-gates.feature and pit-mutation-threshold.feature
  #     respectively. This file covers only the direction of travel.
  #
  # Risks:
  #   - The check parses `pom.xml`. A structural reformat of that file
  #     (reindentation, reordering) could make it misread values or miss a
  #     list. It must fail loudly if it cannot find a gate it expects to
  #     find, rather than passing by default — a check that silently stops
  #     checking is worse than no check.
  #
  # Open questions:
  #   - None.
