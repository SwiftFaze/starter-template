Feature: Teleporting the live player from the dev console's Player provider
  Adds editable "X" and "Y" rows to the Player provider's table
  (sandbox-spawn-edit.feature), so the running player can be moved to a
  new tile without respawning. Uses the same interaction as the stat rows:
  arm the row with Enter, step it with Left/Right. Each step moves the live
  player via Player.setPosition, so the game world shows the new position
  on its next frame.

  Covers: the X/Y rows, their stepping, their floor, and that stepping one
  axis leaves the other untouched. Supersedes nothing;
  sandbox-spawn-edit.feature still owns every stat row.
  Excludes: direct numeric entry and x/y tokens for the command bar's
  set/add verbs (#170), walkability or map-bounds validation, and the
  table refreshing when the player moves in-game while the panel is open.

  QA: none - dev-only second window (F1, -Dveil.devConsole) that QaRunner's main-window key replay cannot reach

  Background:
    Given the dev console is running with the "Player" provider attached to the running player
    And the running player is at position 5, 7
    And "Player" is opened

  Scenario: The Player table lists editable X and Y position rows
    Then the displayed "X" value is 5
    And the displayed "Y" value is 7
    And "X" can be armed
    And "Y" can be armed

  Scenario Outline: Arming a coordinate row and stepping it moves the live player along that axis only
    When "<row>" is armed
    And <direction> is pressed
    Then the running player's position is <x>, <y>
    And the displayed "<row>" value is <shown>

    Examples:
      | row | direction | x | y | shown |
      | X   | right     | 6 | 7 | 6     |
      | X   | left      | 4 | 7 | 4     |
      | Y   | right     | 5 | 8 | 8     |
      | Y   | left      | 5 | 6 | 6     |

  Scenario: Stepping a coordinate below 0 has no further effect
    Given the running player is at position 0, 7
    When "X" is armed
    And left is pressed
    Then the running player's position is 0, 7
    And the displayed "X" value is 0

  Scenario: Teleporting moves the game's own player object, not a copy
    Given the game's live player is the same object identity before and after opening the provider
    When "Y" is armed
    And right is pressed
    Then the game's live player is still the same object identity
    And the running player's position is 5, 8

  Scenario: With nothing armed, Left and Right do not move the player
    When left is pressed
    And right is pressed
    Then the running player's position is 5, 7

  # Non-goals:
  #   - Direct numeric entry of a coordinate; deferred to #170's `set`
  #     command rather than a second interaction in this panel.
  #   - Walkability/bounds checks. WorldScene.isWalkable and getTile already
  #     bounds-check, so an off-map or in-wall position is harmless; normal
  #     movement from there simply refuses unwalkable steps.
  #   - Live-refreshing X/Y while the player walks in-game with the panel
  #     open. The stat rows don't live-refresh either.
  #
  # Risks:
  #   - Adding rows shifts PlayerDetailPanel's row-index constants; the
  #     read-only Attack Power/Defense guards must move with them.
  #
  # Open questions:
  #   - None. Decisions are recorded as "A (auto-decided)" in
  #     specs/intent/sandbox-player-teleport.md's Clarifications.
