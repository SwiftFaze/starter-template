@manual-verification
Feature: PMD parameter-count gate — existing violations remediated
  `pmd-jacoco-quality-gates.feature` already specifies that any function
  taking more than 4 parameters fails `mvn verify` (PMD's
  `ExcessiveParameterList` rule, `minimum="4"` in `.pmd-minimal.xml`). This
  feature covers bringing the codebase into compliance with that
  already-specified gate: the 17 methods that predate the tightened
  threshold and currently violate it. 15 are decomposed with no behavior
  change, 1 is suppressed as a documented JDK-interface-override exception,
  and 1 — `DrawableAsciiEntity.render`, a project-owned public interface —
  has its contract itself changed to fit the gate, since neither of its two
  overrides (`Player.render`, `WorldScene.render`) could reduce their own
  parameter count without an interface change. This last point moves the
  issue onto `.claude/workflow.md`'s high-risk path (the doc names
  `DrawableAsciiEntity`'s public contract as a trigger by itself), so this
  spec is a blocking approval gate before implementation, not a standard-
  path formality.

  Scenario: The codebase satisfies the tightened parameter-count gate
    Given `.pmd-minimal.xml`'s ExcessiveParameterList rule is set to minimum 4
    When `mvn verify` is run
    Then the build succeeds
    And PMD reports zero ExcessiveParameterList violations

  Scenario: The one new documented interface-override exception is suppressed, not refactored
    Given `PatternFieldWidget.AllowedCharacterFilter.replace` overrides `javax.swing.text.DocumentFilter.replace(FilterBypass, int, int, String, AttributeSet)`
    Then it carries `@SuppressWarnings("PMD.ExcessiveParameterList")` with a comment naming the interface it overrides
    And it is the only new occurrence of that suppression added by this change, alongside the pre-existing `Border.paintBorder` overrides in `RadioGroupWidget` and `TableWidget`

  Scenario: DrawableAsciiEntity's render contract is fixed at the source, not suppressed at each override
    Given `Player.render` and `WorldScene.render` both override `DrawableAsciiEntity.render(Graphics2D g2d, int tileWidth, int tileHeight, int cameraX, int cameraY)`, a fixed 5-parameter public interface signature neither override can shrink on its own
    When `DrawableAsciiEntity.render` is changed to `render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera)`, reusing the existing `com.swiftfaze.veil.Camera` type in place of raw `cameraX`/`cameraY` ints
    Then `Player.render` and `WorldScene.render` are updated to the new 4-parameter signature
    And `GamePanel`'s call sites pass the existing `camera` object directly instead of unpacking `camera.getX()`/`camera.getY()` into ints first
    And neither `Player.render` nor `WorldScene.render` carries `@SuppressWarnings("PMD.ExcessiveParameterList")`

  Scenario Outline: A previously-violating production method is decomposed, not suppressed
    Given <class>'s <method> previously exceeded 4 parameters
    When its parameter-count refactor is complete
    Then it does not carry `@SuppressWarnings("PMD.ExcessiveParameterList")`
    And it takes 4 or fewer parameters

    Examples:
      | class              | method                |
      | Main                | wirePopups            |
      | Main                | buildUIScreens        |
      | Main                | handleMenuSelection   |
      | Main                | configureAndShowFrame |
      | ModLoader           | loadBuildings         |
      | ModLoader           | loadBuilding          |
      | ModLoader           | loadClasses           |
      | ModLoader           | loadClass             |
      | ModLoader           | loadItems             |
      | ModLoader           | loadItem              |
      | ModLoader           | loadQuests            |
      | ModLoader           | loadQuest             |
      | WorldScene          | fillRegion            |
      | WorldScene          | renderWorld           |

  Scenario: The refactor changes no observable behavior
    Given the existing acceptance test suite covering Main's UI wiring, Player rendering, ModLoader's mod-loading pipeline, and WorldScene rendering
    When the parameter-count refactor and the DrawableAsciiEntity contract change are applied
    Then every pre-existing acceptance scenario for those areas continues to pass unchanged
    And a manual playtest shows zero functional difference

  # Non-goals:
  #   - Specifying the gate mechanism itself (threshold, what triggers a
  #     failure) — that's `pmd-jacoco-quality-gates.feature`. This feature
  #     is scoped to remediating the current 17 violations against that
  #     already-specified gate.
  #   - Weakening `.pmd-minimal.xml`'s threshold back to 5, or setting
  #     `pmd.failOnViolation=false` anywhere — both would silently disable
  #     the gate rather than satisfy it.
  #   - Any new `@SuppressWarnings("PMD.ExcessiveParameterList")` beyond
  #     the one documented `AllowedCharacterFilter.replace` override.
  #   - Making tile size a variable concept (minimap, zoom, etc.).
  #     `DrawableAsciiEntity.render` keeps `tileWidth`/`tileHeight` as
  #     explicit parameters rather than folding them into `GameConst`
  #     lookups inside each implementation, precisely so this refactor
  #     doesn't quietly decide that question — it stays a live parameter,
  #     just not bundled with camera position.
  #   - Introducing a new value type for camera position — `Camera`
  #     already exists and already carries `x`/`y`; reuse it instead of
  #     adding a parallel record/DTO for the same data.
  #
  # Risks:
  #   - `ModLoader`'s 8 violations share an obvious common shape (each
  #     `load<Type>`/`load<Type>s` pair) — worth one shared refactor
  #     pattern across all 8 rather than 8 independent ad hoc fixes.
  #   - Decomposing `ModLoader` and `Main` is expected to raise
  #     `GodClass`/`TooManyMethods` advisories from the Clean Code gate
  #     (`bash .claude/tools/check-clean.sh`). Advisories don't block, but
  #     are a signal about the underlying class shape, not noise to
  #     suppress.
  #   - `DrawableAsciiEntity.render`'s signature change has a small but
  #     real blast radius beyond `Player`/`WorldScene`/`GamePanel`: existing
  #     unit tests call the old 5-int signature directly
  #     (`PlayerTest.java:107`, `WorldSceneTest.java:94` and `:102`) and
  #     must be updated in lockstep or the build won't compile, independent
  #     of the PMD gate itself.
  #   - This is this repo's first case of a parameter-count fix changing a
  #     project-owned public interface rather than suppressing an override
  #     or decomposing a free-standing method — confirmed via `workflow.md`
  #     to be exactly the kind of change its high-risk-path trigger list
  #     names, so it needs human approval of this spec before Step 4 starts,
  #     unlike the rest of this same issue's changes.
  #
  # Open questions:
  #   - None outstanding. Two were raised and settled via a grilling round
  #     during spec drafting (recorded in specs/intent/pmd-parameter-count-
  #     gate.md's Clarifications): (1) Player.render/WorldScene.render fix
  #     as an interface change rather than a suppression, accepting the
  #     high-risk-path consequence; (2) the interface change reuses the
  #     existing Camera type and keeps tileWidth/tileHeight as explicit
  #     parameters, rather than dropping them or inventing a new bundled
  #     value type.
