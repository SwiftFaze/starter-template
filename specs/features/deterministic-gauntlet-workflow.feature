@manual-verification
Feature: Deterministic-gauntlet alignment of the agentic workflow
  `.claude/workflow.md` and root `CLAUDE.md` are brought closer to Robert
  C. Martin's own description of how he runs agentic coding: a real PMD
  fix-loop before acceptance tests, a mechanically-enforced module
  dependency rule alongside PMD/JaCoCo/PITest, documented parallel
  fan-out guidance, a documented rationale for tunable complexity/
  coverage thresholds, a default agile slice-by-slice implementation loop
  (full intent -> spec -> approval reserved for high-risk work), and
  `specs/intent/` no longer treated as a permanently indexed artifact.

  Scenario: A compliant module layering passes verify
    Given no class outside `com.swiftfaze.veil.ui` imports from `com.swiftfaze.veil.ui`
    And no class in `com.swiftfaze.veil.ui.widget` imports from a screen class directly in `com.swiftfaze.veil.ui`
    When `mvn verify` is run
    Then the build succeeds

  Scenario Outline: A module-layering violation fails the build
    Given <violation>
    When `mvn verify` is run
    Then the build fails with an ArchUnit violation report identifying the offending class and the violated dependency rule

    Examples:
      | violation                                                                          |
      | a class in `game`, `world`, `entities`, `input`, `mods`, `exceptions`, or `sandbox` imports a class from `com.swiftfaze.veil.ui` |
      | a class in `com.swiftfaze.veil.ui.widget` imports a screen class directly in `com.swiftfaze.veil.ui` |

  Scenario: A screen class depending on a widget class is allowed
    Given a screen class directly in `com.swiftfaze.veil.ui` imports a class from `com.swiftfaze.veil.ui.widget`
    When `mvn verify` is run
    Then the ArchUnit module-dependency check does not fail the build over that import

  Scenario: Step 4's handoff requires closing the PMD loop before acceptance tests
    Given `.claude/workflow.md`'s Step 4 (Implementation) section
    Then it states the implementing agent must run PMD, fix any violations, and rerun PMD until clean before proceeding to Step 5 (Acceptance tests)
    And this is described as a deterministic loop the agent must satisfy, not a one-time self-check against the `uncle-bob-craft` checklist alone

  Scenario: workflow.md documents parallel fan-out as a normal option
    Given `.claude/workflow.md`'s Step 4 (Implementation) section
    Then it documents running independent Step 4 implementations in parallel (separate worktrees or separate fresh Haiku agents) as a normal option for multiple ready tickets, not only as an edge-case exception

  Scenario: workflow.md documents the complexity/coverage thresholds as a tunable dial
    Given `.claude/workflow.md`'s Constraints section
    Then it states the function-length, cyclomatic-complexity, parameter-count, and coverage thresholds are a deliberate, adjustable dial for agent-authored code, not a fixed constant
    And it states that future changes to these numbers must be a documented, deliberate experiment rather than undocumented drift

  Scenario Outline: The implementation path depends on feature risk category
    Given a feature classified as "<risk category>"
    Then `.claude/workflow.md` requires "<required path>" before implementation code is written

    Examples:
      | risk category                                          | required path                                                          |
      | standard (not auth/payments/data-integrity/public API) | a short intent doc, then an agile slice-by-slice implementation loop (implement a slice, look at the result, reconcile the intent doc, continue) — no human-approved `.feature` file required before code exists |
      | high-risk (auth/payments/data-integrity/public API)    | the full intent -> Gherkin spec -> human approval gate, unchanged from today |

  Scenario: specs/intent/ is not an indexed artifact
    Given `specs/intent/README.md`
    Then it does not require an intent doc add, remove, or rename to update any index in the same change
    And it explains that intent docs are local scratch which nothing needs to persist past the feature they fed

  # Non-goals:
  #   - Any actual game feature or player-visible behavior change.
  #   - The PMD/JaCoCo gate implementation itself (already delivered by
  #     pmd-jacoco-quality-gates.feature / #123).
  #   - Moving PIT mutation testing into CI as a gate, or any other change
  #     to Step 6 — untouched by this issue.
  #   - Changing `specs/features/*.feature`'s status as an executable,
  #     indexed artifact — only `specs/intent/*.md`'s ceremony is reduced.
  #   - Imposing or relaxing any TDD (test-driven development) discipline
  #     on implementing agents — neither `.claude/workflow.md` nor root
  #     `CLAUDE.md` currently mandates strict test-first TDD, so there is
  #     nothing to change here (confirmed during spec drafting).
  #
  # Risks:
  #   - The real package layout has no literal "screens" subpackage —
  #     screen classes (TitleScreenPanel, SettingsScreenPanel, CodexPanel,
  #     InventoryPanel, SettingsKeybindsPanel, GameWindow, SettingsWindow,
  #     and panel-composition classes) sit flat in com.swiftfaze.veil.ui
  #     alongside the ui.widget subpackage. Resolved via the intent doc's
  #     Clarifications: "screens" is defined as "classes directly in
  #     com.swiftfaze.veil.ui, excluding ui.widget" for ArchUnit purposes.
  #   - Flipping the default implementation path away from full
  #     intent->spec->approval is a significant process change; the
  #     high-risk carve-out (auth/payments/data-integrity/public API)
  #     must stay unambiguous so it isn't accidentally skipped for
  #     something that should get the full gate.
  #
  # Open questions:
  #   - None outstanding — settled via a grilling round during spec
  #     drafting on 2026-09-01, cross-referenced against the primary-source
  #     Robert C. Martin interview transcript.
