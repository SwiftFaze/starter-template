Feature: Sandbox kitchen-sink tile scene with walkability overlay
  A dev-console provider (the framework from sandbox-dev-console.feature)
  contributing exactly one top-level entry: a curated WorldScene holding
  every tile in ModRegistry.getAllTiles(), side by side. Opening it shows a
  live preview drawn through WorldScene's real rendering path, with a marker
  the movement keys drive and a toggleable walkability overlay taken from
  WorldScene.isWalkable(x, y).

  Supersedes: nothing. Unlike TileTestScene2, this is a new, separate scene.
  TileTestScene2 is the live game's scene (GamePanel) and this feature does
  not touch it.

  Out of scope: a scene picker, or loading arbitrary WorldScenes; any link
  to the Player provider or to a running game's Player.

  QA: none - dev-console sandbox panel, not a game screen QaRunner can drive; covered by the human playtest

  Background:
    Given the dev console is running with the "Kitchen Sink" provider registered

  Scenario: The kitchen-sink scene is one top-level result
    Then the results include exactly one entry from the "Kitchen Sink" provider
    And that entry has namespace "sandbox", id "sandbox:kitchen-sink", category "Scenes" and name "Kitchen Sink"

  Scenario: Opening the entry shows a live preview of the curated scene
    When the kitchen-sink entry is opened
    Then the opened detail panel is shown
    And the scene contains every tile in the mod registry
    And the marker starts on a walkable tile

  Scenario Outline: Movement keys move the marker one tile onto walkable ground
    Given the kitchen-sink entry is opened
    And the marker stands on a walkable tile whose <direction> neighbour is walkable
    When the <key> key is pressed
    Then the marker has moved one tile <direction>

    Examples:
      | direction | key   |
      | up        | Z     |
      | up        | Up    |
      | down      | S     |
      | down      | Down  |
      | left      | Q     |
      | left      | Left  |
      | right     | D     |
      | right     | Right |

  Scenario: The marker cannot move onto an unwalkable tile
    Given the kitchen-sink entry is opened
    And the marker's right neighbour is unwalkable
    When the Right key is pressed
    Then the marker has not moved

  Scenario: The marker cannot leave the scene's bounds
    Given the kitchen-sink entry is opened
    And the marker stands on the scene's left edge
    When the Left key is pressed
    Then the marker has not moved

  Scenario: The walkability overlay toggles on and off
    Given the kitchen-sink entry is opened
    And the walkability overlay is off
    When the W key is pressed
    Then every cell is marked walkable or unwalkable as WorldScene.isWalkable reports it
    And the marking is a background tint, so each tile's glyph is still drawn
    When the W key is pressed
    Then no walkability marking is drawn

  Scenario: Leaving and reopening the preview starts it fresh
    Given the kitchen-sink entry is opened
    And the marker has moved away from its start position
    And the walkability overlay is on
    When the back action is triggered
    And the kitchen-sink entry is opened
    Then the marker is at its start position
    And the walkability overlay is off

  Scenario: An empty tile registry still opens the entry
    Given the mod registry contains no tiles
    When the kitchen-sink entry is opened
    Then the opened detail panel shows "No tiles registered"
    And no preview is drawn

  # Non-goals:
  #   - Scene switching or picking; editing tiles in the preview.
  #   - Changing TileTestScene2 or GamePanel's live scene.
  #
  # Risks:
  #   - The first live render+input loop in the sandbox. The dev console's own
  #     arrow/Escape bindings must not steal keys from the preview.
  #   - The 40-line / complexity-8 budgets may force a render loop split into
  #     several small functions.
  #
  # Open questions: none. Tile layout is left to the implementer, bounded by
  # the two invariants in the "Opening the entry" scenario (see intent
  # Clarifications). Not every tile is guaranteed reachable by the marker.
