@manual-verification
Feature: Module dependency gate — structural rules for the package graph
  `ModuleDependencyTest` is this repo's only mechanical check on the shape of
  the dependency graph *between* packages. Today it holds two hand-picked
  direction rules (engine -/-> ui, ui.widget -/-> ui screens), already
  specified by `docs/testing.md` § "Module dependency gate" and unchanged
  here. This feature covers the three structural rules added alongside them:
  package cycle freedom, no public mutable static state, and instantiation
  confined to composition roots — all three frozen at today's baseline in one
  shared `FreezingArchRule` store, since a gate red on arrival gets disabled
  within a week regardless of which rule caused it. It runs through the
  existing `mvn verify` — no new command, no new dependency (ArchUnit 1.4.1 is
  already a test-scoped dependency).

  It supersedes nothing. It is tagged `@manual-verification` for the same
  reason `pmd-jacoco-quality-gates.feature` is: the behavior under test is the
  build's own pass/fail, which a Cucumber step running inside that build
  cannot assert on itself. The two scenarios describing the freeze
  mechanism's dynamic behavior (a new cycle failing the build; removing one
  shrinking the store) are demonstrated manually during Step 4.5 and recorded
  in the PR description, not asserted by an automated meta-test — that would
  be testing ArchUnit's own `FreezingArchRule`, library behavior this repo
  does not own.

  Explicitly out of scope: unpicking the six existing package cycles or
  fixing `WidgetTheme`'s/`GamePanel`'s existing violations (the baseline
  records them; each removal shrinks the store on its own), any rule
  mandating interfaces/abstract classes/factories by count or naming
  convention, and any addition to `CLAUDE.md` or `.claude/workflow.md`.

  Background:
    Given `ModuleDependencyTest` carries the two existing direction rules plus the three structural rules
    And all three structural rules are frozen at today's baseline in one shared, committed violation store at `src/test/resources/archunit_store/`

  Scenario: The gate is green on arrival
    Given a clean checkout of `develop` with this change applied
    When `mvn verify` is run
    Then the build succeeds
    And no structural rule reports a violation outside the frozen baseline

  Scenario: The baseline records today's cycles at full package depth, not just the top-level slice
    Given the package graph is sliced with `slices().matching("com.swiftfaze.(**)")`, treating every package at every depth — including the root package `com.swiftfaze.veil` itself — as its own slice
    When `mvn verify` is run
    Then the cycle-freedom rule passes
    And the frozen violation store contains all six pre-existing cycles as accepted violations, including the two involving the root package and the intra-`entities` one that a coarser top-level-only slice would have missed

  Scenario: A newly introduced package cycle fails the build
    Given a class is added whose import welds a package cycle not present in the frozen store
    When `mvn verify` is run
    Then the build fails with ArchUnit's cycle report naming the packages in the cycle
    And the failure names the specific dependency that closed the cycle
    And this is demonstrated manually during Step 4.5, with the added import and the resulting failure recorded in the PR description, then reverted

  Scenario: Fixing an existing cycle shrinks the store automatically
    Given one of the six recorded cycles is removed by an interface extraction
    When `mvn verify` is run
    Then the build succeeds
    And the frozen violation store no longer lists that cycle
    And the store was not hand-edited to achieve this
    And this is demonstrated manually during Step 4.5, with the store diff recorded in the PR description

  Scenario Outline: The static-state rule distinguishes constants from mutable state, with today's violations frozen
    Given a class declares a field that is <field>
    When `mvn verify` is run
    Then the build <outcome>

    Examples:
      | field                                                                    | outcome                                                    |
      | `public static final` — a constant                                      | succeeds                                                   |
      | `private static` and not final                                          | succeeds                                                   |
      | `WidgetTheme`'s 13 pre-existing `public static` non-final `Color` fields | succeeds — recorded in the frozen store as accepted debt   |
      | a newly added `public static` non-final field, anywhere else            | fails with the no-public-mutable-static-state rule         |

  Scenario: Instantiating an engine class from outside the composition root fails the build, with today's violations frozen
    Given a class outside `com.swiftfaze.veil.ui..` and outside `Main` instantiates a class in `game`, `world`, `mods`, or `entities..` directly
    And that instantiation is not one of the pre-existing sites already in the frozen store (`GamePanel`'s five `Player`/`TileTestScene2`/`Camera` sites, `SettingsStore`'s `SettingsRepository` site)
    When `mvn verify` is run
    Then the build fails with the instantiation rule naming the offending class and the class it instantiated

  Scenario: Only Main is a composition root — GamePanel's existing instantiations are frozen debt, not blessed design
    Given `Main` instantiates a class in `game`, `world`, `mods`, or `entities..`
    When `mvn verify` is run
    Then the build succeeds
    But `GamePanel` is not a composition root: its five existing instantiation sites pass only because they are in the frozen store, and a sixth new one outside that store fails

  Scenario: com.swiftfaze.veil.ui.. is excluded from the instantiation target set
    Given a class in `com.swiftfaze.veil.ui..` instantiates a plain UI type such as `JPanel`
    When `mvn verify` is run
    Then the build succeeds
    And the instantiation rule does not fire, because it targets engine construction (`game`/`world`/`mods`/`entities..`), not a general no-`new` rule

  Scenario: The existing direction rules keep working unchanged
    Given engine code outside `ui` (excluding `Main` and `sandbox`) does not depend on `com.swiftfaze.veil.ui`
    And no class in `com.swiftfaze.veil.ui.widget` depends on a screen class sitting directly in `com.swiftfaze.veil.ui`
    When `mvn verify` is run
    Then the build succeeds
    And both pre-existing rules still fail the build when violated

  Scenario: The documentation lands with the rules
    Given this change is complete
    Then `docs/testing.md` § "Module dependency gate" documents the freeze mechanism, the full-depth slicing pattern, the composition-root definition, and how to legitimately update the store
    And `docs/clean-code-gate.md`'s coverage table gains a row for the structural (between-packages) layer
    And `check-clean.sh` section 1's PASS text mentions the structural rules
    And no new command is added — `mvn verify` already runs ArchUnit

  # Non-goals:
  #   - Fixing the six existing cycles, or WidgetTheme's/GamePanel's existing
  #     static-state and instantiation violations. The baseline captures
  #     them; unpicking them is follow-up work, one at a time.
  #   - Making GamePanel a composition root by fiat. Its field-initializer
  #     construction of Player/TileTestScene2/Camera is exactly the wiring
  #     that makes the game untestable without a live panel; freezing those
  #     sites as debt keeps that visible instead of blessing it as design.
  #   - Any rule requiring interfaces, abstract classes, or factories by count
  #     or naming convention — that produces cargo-cult abstraction and
  #     contradicts the existing gate line "no abstraction with a single
  #     caller added 'for later'".
  #   - A general no-`new` rule. The instantiation rule's target set is
  #     game/world/mods/entities.. specifically; com.swiftfaze.veil.ui.. is
  #     excluded so `new JPanel()` and friends are untouched.
  #   - Adding a new Maven dependency, plugin, CI job, or shell command.
  #   - Any addition to CLAUDE.md or .claude/workflow.md — "if it is
  #     mechanically checkable, write the check, not the rule".
  #   - An automated meta-test asserting FreezingArchRule's own dynamic
  #     behavior (new violation fails, fixed violation drops out) — that is
  #     library behavior this repo does not own; covered by manual Step 4.5
  #     demonstration instead.
  #
  # Risks:
  #   - The freeze store is load-bearing for all three structural rules, not
  #     just cycle-freedom. Without it committed in the same change, the
  #     gate is red on arrival on develop and gets disabled within a week.
  #   - A frozen store can hide a genuine regression if a contributor
  #     re-freezes rather than fixes. docs/testing.md's account of
  #     "legitimately updating the store" is the only thing standing between
  #     a ratchet and a rubber stamp.
  #   - Freezing WidgetTheme's 13 fields means a 14th theme colour added the
  #     same way will fail the build until the store is updated — the
  #     intended fix is a private field plus an accessor, not growing the
  #     public mutable static surface further, but that friction lands on
  #     whoever touches WidgetTheme next, not this change's author.
  #   - Full-depth slicing (com.swiftfaze.(**)) means a future package split
  #     inside an already-large package (e.g. splitting entities.player
  #     further) can surface a new intra-module cycle. That is the intended
  #     alarm, not a false positive, but it will read as new friction to
  #     whoever triggers it without this history.
  #
  # Open questions:
  #   - None outstanding. Five were raised and settled via a grilling round
  #     during spec drafting (recorded in specs/intent/module-dependency-
  #     gate.md's Clarifications): (1) cycle slicing at full package depth,
  #     com.swiftfaze.(**), rather than the intent doc's original top-level-
  #     only (*).. ; (2) all three rules share one frozen store, not just
  #     cycle-freedom; (3) the instantiation rule's target set is game/world/
  #     mods/entities.., allowed set is Main alone (not GamePanel); (4) the
  #     freeze mechanism's dynamic behavior is demonstrated manually at Step
  #     4.5, not asserted by an automated meta-test; (5) the store lives at
  #     src/test/resources/archunit_store/, configured via a new
  #     src/test/resources/archunit.properties.
