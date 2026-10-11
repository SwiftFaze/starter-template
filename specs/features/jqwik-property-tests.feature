@manual-verification
Feature: jqwik property-based tests
  Every existing test in the repo is an example test: fixed inputs, fixed
  expected output. That proves the one case someone thought of; the
  interesting failures live in the cases nobody did. This feature wires
  jqwik alongside JUnit 5 and adds a first suite of properties — invariants
  checked over generated inputs rather than a handful of fixed examples —
  targeting the highest-value invariants that exist in the codebase today:

  - `WorldScene.getTile`/`isWalkable` never go out of bounds or throw, for
    any world size and coordinate (the real "camera stays sane" safety net,
    since `Camera` itself does no world-bounds clamping — see
    `camera-behavior.feature`'s Non-goals).
  - `PlayerClass.applyStatsAtLevel` is deterministic for any well-formed
    growth-curve expression and level.
  - `Stats.getAttackPower`/`getDefense` never go negative for non-negative
    attributes.

  It supersedes nothing — properties are additive, existing example tests
  in `PlayerClassTest`, `WorldSceneTest`, `ClassSandboxModelTest`, and
  `CameraTest` stay as documentation of specific cases. It does not cover
  world-generation determinism (no seeded world generation exists yet —
  deferred to land alongside #67), stateful/model-based property testing,
  or converting any existing example test or Cucumber scenario into a
  property.

  Background:
    Given jqwik is added as a test-scope dependency, registering its own JUnit Platform test engine
    And Surefire discovers `@Property` test classes alongside existing `@Test` classes and the Cucumber suite under `mvn test`, with no separate Surefire configuration
    And each property runs a deliberately capped number of tries, not jqwik's default, to bound the added wall-clock time in the agent's inner loop
    And CI runs jqwik with a fixed seed configuration

  Scenario: Properties run under mvn test alongside existing JUnit tests
    Given a clean checkout of `develop` with this change applied
    When `mvn test` is run
    Then jqwik's `@Property` tests execute and report pass/fail alongside the existing `@Test` and Cucumber results
    And no existing JUnit Jupiter or Cucumber test is skipped or altered

  Scenario: World tile lookups never go out of bounds, for any generated world size and position
    Given a `WorldScene` of any generated width and height, both positive and bounded for test speed
    When `getTile` and `isWalkable` are called with any generated x/y coordinate, including coordinates outside the world's bounds
    Then `getTile` returns null and `isWalkable` returns false whenever the coordinate falls outside [0, width) x [0, height)
    And neither method throws, for any generated width, height, and coordinate

  Scenario: A deliberately broken bounds check fails the property with a reproducible counterexample
    Given the bounds check in `getTile`/`isWalkable` is deliberately weakened, for example an off-by-one on the upper bound
    When the world-bounds-safety property runs
    Then the property fails
    And the failure output includes a shrunk counterexample naming the world size and coordinate that broke it
    And the failure output includes the seed needed to reproduce that counterexample
    # This is a one-time demonstration performed during implementation to
    # prove the failure-reporting path actually works — the deliberate
    # break is reverted immediately afterward, not left in the codebase.

  Scenario: Stat computation is deterministic for any generated growth curve and level
    Given a generated `PlayerClass.StatCurve` whose `growthCalc` is a well-formed expression in `CalcExpressionParser`'s grammar (`+ - * / ( ) level <number>`, or no growth curve at all)
    And a generated level
    When `applyStatsAtLevel` is called twice with that same stat curve and level, on two fresh `Stats` instances
    Then both calls produce identical attribute values
    And neither call throws, for any generated well-formed growth curve and level

  Scenario: Combat-relevant derived stats never go negative for non-negative attributes
    Given a `Stats` instance with any generated non-negative strength, dexterity, and constitution
    When `getAttackPower` and `getDefense` are read
    Then both values are greater than or equal to zero, for any generated non-negative inputs

  Scenario: Compile/test-time impact is measured and stated
    Given this change is complete
    Then the PR description states the `mvn test` wall-clock time added by the property suite, and the try-count cap chosen for each property

  Scenario: The when-to-use convention lands in documentation
    Given this change is complete
    Then `docs/testing-property-based.md` states when a property test is the right tool versus an example test
    And it states the assert-vs-boolean-return convention this issue settles for `@Property` methods, for issue #215 to target
    And it is linked from `docs/testing.md`'s "Unit tests" section, mirroring how "Acceptance and approval tests" links to its own file

  Scenario: Mutation score on the covered classes is recorded before and after
    Given this change is complete
    Then the PR description records the PIT mutation score for `WorldScene`, `PlayerClass`, and `Stats` from before this change
    And the PR description records the same classes' mutation score after this change, to check whether the properties measurably improved the kill rate

  # Non-goals:
  #   - World-generation determinism properties (same seed => same world) and
  #     the HashSet-iteration nondeterminism demonstration. No seeded world
  #     generation exists yet (confirmed 2026-09-12: no `seed`/`Random`
  #     anywhere in `com.swiftfaze.veil.world`) — issue #67 (seeded
  #     biome/terrain generation) is still open. This is explicit follow-up
  #     work to be written alongside #67, not retrofitted after it.
  #   - Camera-to-world clamping as new behavior. `Camera.centerOn` has no
  #     world-bounds awareness and `camera-behavior.feature`'s own Non-goals
  #     section states the current unclamped behavior is intentional. The
  #     property target above is `WorldScene`'s existing out-of-bounds
  #     safety net, not a new clamping feature on `Camera`.
  #   - A general combat-resolution/damage formula. No such class exists —
  #     `class-stats-sandbox.feature`'s Non-goals already state nothing
  #     consumes `Stats.getAttackPower`/`getDefense` in gameplay yet. The
  #     property target above is those two existing derived-stat getters,
  #     not a fabricated attacker-vs-defender damage function.
  #   - Growth-direction monotonicity as an enforced invariant. `growthCalc`
  #     is an arbitrary mod-authored expression with nothing preventing a
  #     decreasing curve, and shipped content (`warrior.json`, `mage.json`)
  #     has no growth curves at all today. The property target above is
  #     `computeStat`'s determinism/correctness, not growth direction.
  #   - Converting any existing example test (`PlayerClassTest`,
  #     `WorldSceneTest`, `ClassSandboxModelTest`, `CameraTest`) into a
  #     property. Properties are additive; a passing example test documents
  #     a specific case and stays.
  #   - Stateful/model-based property testing.
  #   - Cucumber scenarios for the properties themselves — Gherkin is the
  #     acceptance layer and stays example-based; the scenarios above
  #     describe what the property suite must guarantee as the
  #     human-reviewed spec of record, but the suite is implemented as
  #     jqwik `@Property` unit tests, not Cucumber step definitions (hence
  #     this file's `@manual-verification` tag — same treatment as
  #     `pit-mutation-threshold.feature` and `error-prone-nullaway.feature`).
  #
  # Risks:
  #   - jqwik registers its own JUnit Platform test engine; a misconfigured
  #     Surefire `<includes>` naming pattern could leave property test
  #     classes silently undiscovered while `mvn test` still reports green.
  #     The "properties run alongside existing tests" scenario above is not
  #     sufficient proof by itself — the deliberately-broken-bounds-check
  #     scenario (a property that must fail) is what actually demonstrates
  #     the suite is live and its failure-reporting path works.
  #   - Property tests are slower than example tests per input, and this
  #     runs in `mvn test`, the agent's inner loop. An uncapped try count
  #     could make the inner loop noticeably slower without anyone deciding
  #     that tradeoff explicitly — hence the deliberate cap in Background
  #     and the wall-clock-impact scenario above.
  #   - `CalcExpressionParser` is a hand-rolled recursive-descent parser; a
  #     generated-expression `Arbitrary` that produces a syntactically
  #     malformed string (not just a semantically odd one) would throw
  #     `IllegalArgumentException` from the tokenizer/parser itself, which
  #     is a different failure mode than the determinism property is meant
  #     to check. The `Arbitrary` must only generate well-formed expressions
  #     in the parser's grammar.
  #
  # Open questions:
  #   - None outstanding. All three scope gaps between the source issue
  #     (#200) and the actual codebase — camera clamping, stat monotonicity,
  #     combat formula bounds — were resolved by the user; see
  #     specs/intent/jqwik-property-tests.md's Clarifications section.
