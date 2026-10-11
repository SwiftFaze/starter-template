Feature: Approval tests over the rendered ASCII glyph grid
  Veil's product is a grid of glyphs, and today nothing asserts on it —
  `.claude/workflow.md` Step 4 says outright that no test asserts on pixels
  or rendered text. This feature adds a rendering seam that produces the
  visible tile grid as text with no `Graphics2D` involved (extracted from
  `WorldScene.renderWorld`'s `g2d.drawString` loop, which keeps calling this
  seam and then drawing each glyph), clipped to the camera's viewport —
  not the full world tile array `renderWorld` currently loops over — plus a
  homegrown approval-test mechanism: render a fixed scene, compare the text
  against a committed approved fixture at
  `src/test/resources/approved/<scenario-name>.approved.txt`, and write a
  sibling `<scenario-name>.received.txt` on mismatch.

  It covers the seam itself, the compare/mismatch/re-approve mechanics of
  the approval helper, and a starting suite of scenarios chosen because
  they're the kind of defect a human playtest reliably misses: a viewport
  overhanging the world edge, a building's full footprint, and
  entity-over-tile draw ordering. All fixture scenes use small hand-built
  `WorldScene`/`Tile`/`Building`/`DrawableAsciiEntity` test doubles (matching
  `WorldSceneTest`'s existing convention), never the real mod-loaded
  `TileTestScene2`/`ModLoader` content — kept small, deterministic, and
  insulated from unrelated mod-content changes.

  It supersedes nothing — this is new coverage, not a replacement for any
  existing `.feature` file. It narrows what CLAUDE.md's Step 4.5 human
  playtest is *for* (feel, not regression) without removing that step.

  Explicitly out of scope: Swing/AWT pixel or font rendering (the seam
  stops at the glyph grid — whether `drawString` puts a glyph at the right
  baseline stays a human/visual check), colour (glyph grid only — colour is
  a `WidgetTheme` concern), replacing the human playtest, and any clamping
  behavior on `Camera` — it performs none today and none is being added
  here (see `camera-behavior.feature`'s Non-goals).

  Background:
    Given a WorldScene rendering seam that produces the visible tile grid as text, clipped to the camera's viewport, with no Graphics2D involved
    And hand-built test-double Tile, WorldScene, Building, and DrawableAsciiEntity fixtures, not real mod-loaded content

  Scenario: A scene's text rendering matches its approved fixture
    Given a fixed test scene with known tile placements
    And an approved fixture already committed at src/test/resources/approved/ for that scene
    When the scene is rendered through the text seam
    And the result is compared against the approved fixture
    Then the comparison passes
    And no .received.txt file is written

  Scenario: A rendering change that alters the glyph grid fails the suite and shows the diff
    Given a fixed test scene with known tile placements
    And an approved fixture already committed at src/test/resources/approved/ for that scene
    When one glyph in the renderer is deliberately changed
    And the scene is rendered through the text seam
    And the result is compared against the approved fixture
    Then the comparison fails
    And a sibling <scenario-name>.received.txt file is written next to the approved fixture, containing the new grid
    And the failure output shows both the approved and received grids so the difference is visible
    And this is demonstrated manually during Step 4.5, with the deliberate change and the resulting failure recorded in the PR description, then reverted

  Scenario: Re-approving a changed fixture is one explicit command, never automatic
    Given a <scenario-name>.received.txt file exists next to an approved fixture because the two differ
    When `mvn compile exec:java -Dexec.mainClass=...` is run for the approval re-approve tool
    Then the approved fixture is replaced with the received content
    And the .received.txt file is removed
    And a normal `mvn test` run never performs this replacement on its own

  Scenario: A viewport overhanging the world edge renders off-map cells as a defined blank glyph
    Given a small test scene smaller than the camera's viewport
    And the camera positioned so part of its viewport falls outside the world bounds
    When the scene is rendered through the text seam
    Then the rendered grid matches an approved fixture where every off-map cell is a space, with no exception thrown and no garbage glyph

  Scenario: A building's full footprint renders correctly in the glyph grid
    Given a test scene with a minimal hand-built Building blueprint placed on it
    When the scene is rendered through the text seam
    Then the rendered grid matches an approved fixture showing the building's full footprint, corners included

  Scenario: An entity glyph occludes the tile glyph beneath it
    Given a test scene with a tile at a given position
    And a minimal hand-built DrawableAsciiEntity positioned on that same tile
    When the scene and its entities are rendered through the text seam
    Then the rendered grid shows the entity's glyph at that position, not the tile's

  Scenario Outline: Viewport size boundaries render the correct number of columns and rows
    Given a camera with a viewport <width> tiles wide and <height> tiles tall
    When a fixed test scene larger than the viewport is rendered through the text seam
    Then the rendered grid is exactly <width> columns by <height> rows, matching an approved fixture

    Examples:
      | width | height |
      | 5     | 5      |
      | 20    | 16     |

  # Non-goals:
  #   - Swing/AWT pixel or font rendering — the seam stops at the glyph grid.
  #   - Colour — glyph grid only; colour stays a WidgetTheme concern.
  #   - Replacing the human playtest (CLAUDE.md Step 4.5).
  #   - Any clamping behavior on Camera — it performs none today, and none
  #     is being added by this feature (see camera-behavior.feature's
  #     Non-goals). The "viewport overhangs world edge" scenario above
  #     documents the current unclamped behavior; it does not request
  #     clamping be added.
  #   - Rendering the full world tile array. The text seam is clipped to
  #     the camera's viewport; WorldScene.renderWorld's own full-world loop
  #     (a separate latent perf concern) is unchanged by this feature.
  #   - Real mod-loaded content (TileTestScene2/ModLoader) in any approval
  #     fixture — all scenes/entities/buildings here are hand-built test
  #     doubles.
  #
  # Risks:
  #   - The rendering seam must not violate ModuleDependencyTest — if it
  #     needs a new package, its dependency direction has to satisfy the
  #     existing ArchUnit rules rather than get a carve-out.
  #   - Approved fixtures must be committed as LF via .gitattributes, or a
  #     CRLF checkout on Windows fails every comparison byte-for-byte.
  #   - Clipping the text seam to the viewport (rather than matching
  #     renderWorld's current full-world loop) means the seam and
  #     renderWorld compute overlapping but not identical bounds until/
  #     unless renderWorld itself is later changed to iterate only the
  #     viewport — implementation must keep both correct against the same
  #     camera-offset math, not silently diverge.
  #
  # Open questions:
  #   - None outstanding. Six were raised and settled via a grilling round
  #     during spec drafting (recorded in
  #     specs/intent/approval-tests-glyph-grid.md's Clarifications): (1)
  #     edge-case scenario reframed as "viewport overhangs world edge" with
  #     blank off-map glyphs, not camera clamping (Camera has none); (2)
  #     text seam clips to camera viewport, not the full world grid; (3)
  #     approved fixtures live at src/test/resources/approved/, named
  #     <scenario-name>.approved.txt / .received.txt; (4) all fixture
  #     scenes use hand-built Tile/WorldScene/Building test doubles, never
  #     real mod-loaded content; (5) the entity-over-tile scenario uses a
  #     minimal hand-built DrawableAsciiEntity, not the real Player class;
  #     (6) re-approval is a small Java tool run via exec:java, matching
  #     the sandbox precedent in root CLAUDE.md.
