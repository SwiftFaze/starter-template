Feature: Sandbox Tiles provider
  Adds a read-only "Tiles" provider to the dev-console provider framework
  (sandbox-dev-console.feature), so every mod-loaded tile is its own
  searchable top-level result and opens straight to its glyph, color and
  walkability - checking tile data without loading a world scene that
  happens to use it.

  Covers: the Tiles provider's entries (one per tile from
  ModRegistry.getAllTiles(), no intermediate "Tiles" browse screen), its
  registration in both dev-console entry points (the F1 in-game console and
  the standalone ClassSandbox), and the detail view built from Tile's
  Inspectable DetailTables.
  Supersedes: nothing.
  Excludes: the search/results/back shell itself (sandbox-dev-console.feature),
  tile loading/validation (data-driven-tile.feature), and rendering or
  walking a live scene of tiles (sandbox-kitchen-sink-scene.feature).

  QA: none - dev-only console emits no GameEvents for QaRunner to match; covered by Cucumber

  Background:
    Given the dev console is running with the "Tiles" provider registered

  Scenario: Every mod-loaded tile is its own top-level result
    Then the results include an entry named "grass"
    And the results include an entry named "water"
    And the "grass" result has namespace "core" and category "Tiles"
    And the Tiles provider contributes one result per loaded tile

  Scenario: A tile is findable by typing part of its name
    When the search text is set to "gra"
    Then the results include an entry named "grass"
    And the results do not include an entry named "water"

  Scenario Outline: Opening a tile shows its glyph, color and walkability
    When "<tile>" is opened
    Then the opened detail panel is shown
    And the tile detail shows these fields:
      | Field    | Value      |
      | ID       | <id>       |
      | Symbol   | <symbol>   |
      | Color    | <color>    |
      | Walkable | <walkable> |

    Examples:
      | tile  | id         | symbol | color             | walkable |
      | grass | core:grass | ⡐      | rgb(179, 224, 160) | true     |
      | water | core:water | ⠭      | rgb(174, 191, 232) | false    |

  Scenario: Returning to the results leaves no tile state behind
    Given "water" is opened
    When the back action is triggered
    Then the results include an entry named "grass"
    And the results include an entry named "water"

  Scenario: No tiles loaded contributes no results
    Given the dev console is running with a Tiles provider that has no tiles
    Then the results are empty

  Scenario: Opening an id the Tiles provider does not know is rejected
    Then the Tiles provider rejects opening "core:no_such_tile"

  Scenario Outline: The Tiles provider is available from both dev-console entry points
    Then the <entry point> dev console's results include an entry named "grass"

    Examples:
      | entry point        |
      | F1 in-game         |
      | standalone sandbox |

  # Non-goals:
  #   - Editing tile data from the console (read-only).
  #   - Drawing the glyph in its own color, or any live scene preview -
  #     sandbox-kitchen-sink-scene.feature owns rendered previews.
  #   - Any change to DevConsolePanel's search/results/keybinding shell.
  #
  # Risks:
  #   - The F1 console and ClassSandbox build their provider lists
  #     separately (Main.java, ClassSandbox.java), so registration can drift
  #     between them - hence the entry-point Scenario Outline.
  #   - A direct sandbox -> world import adds a new frozen ArchUnit cycle
  #     path; the provider works through Inspectable instead (intent doc
  #     Clarifications).
  #
  # Open questions: none.
