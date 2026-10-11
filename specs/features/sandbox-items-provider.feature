Feature: Sandbox Items provider
  Adds an "Items" provider to the dev-console provider framework
  (sandbox-dev-console.feature), so every mod-loaded item
  (ModRegistry.getAllItems()) is browsable and inspectable as data without
  loading the real game. Each item is its own top-level result - the flat
  model the "Classes" provider already uses; there is no intermediate
  "Items" browse screen. Opening an item shows the same Inspectable
  DetailTable output InventoryPanel and CodexPanel render (field table,
  plus an Effects table when the item has effects).

  Covers: item entries in the results table, their namespace/category,
  search over them, and the read-only detail panel for one item.
  Supersedes: nothing. The search/results/back shell itself stays covered
  by sandbox-dev-console.feature and is unchanged.
  Out of scope: spawning items into the world or a player's inventory,
  and any interaction with a live Player's inventory. Editing an item's
  base damage is covered by sandbox-edit-item-fields.feature.

  QA: none - no keyboard input of its own; reached through the existing dev-console shell

  Background:
    Given the dev console is running with the "Items" provider registered

  Scenario: Every mod-loaded item is its own top-level result
    Then the results include an entry named "Iron Sword"
    And the results include an entry named "Bread Loaf"
    And there is one result per mod-loaded item

  Scenario: Each item result shows its mod namespace and the "Items" category
    Then the "Iron Sword" result has namespace "core" and category "Items"

  Scenario: Typing part of an item's name finds it
    When the search text is set to "sword"
    Then the results include an entry named "Iron Sword"
    And the results do not include an entry named "Bread Loaf"

  Scenario: Searching the category lists every item
    When the search text is set to "items"
    Then the results include an entry named "Iron Sword"
    And the results include an entry named "Bread Loaf"

  Scenario: Items and classes are listed side by side when both providers are registered
    Given the dev console is running with the "Classes" and "Items" providers registered
    Then the results include an entry named "Mage"
    And the results include an entry named "Iron Sword"

  Scenario: Opening an item shows its field table
    When "Iron Sword" is opened
    Then the opened detail panel is shown
    And the item detail shows field rows:
      | Field             | Value           |
      | ID                | core:iron_sword |
      | Name              | Iron Sword      |
      | Glyph             | /               |
      | Type              | weapon          |
      | Slot              | main_hand       |
      | Base Damage (Min) | 4               |
      | Base Damage (Max) | 9               |

  Scenario: Opening an item with effects also shows its Effects table
    When "Iron Sword" is opened
    Then the item detail shows an "Effects:" table with row "stat_bonus", "strength", "level*1.5+2"

  Scenario Outline: An item without damage or effects shows only what it has
    When "<item>" is opened
    Then the item detail has no "Base Damage (Min)" row
    And the item detail has no "Effects:" table

    Examples:
      | item          |
      | Bread Loaf    |
      | Health Potion |
      | Rusty Key     |

  Scenario: Going back from an item leaves the results table unchanged
    Given "Iron Sword" is opened
    When the back action is triggered
    Then the results include an entry named "Iron Sword"
    And there is one result per mod-loaded item

  Scenario: Searching for something that isn't an item finds nothing
    When the search text is set to "zzz"
    Then the results are empty

  Scenario: Opening an unknown item id is rejected
    When the Items provider is asked for the panel of "core:no_such_item"
    Then it fails with an unknown item id error for "core:no_such_item"

  # Non-goals:
  #   - Spawning an item into the world or a player's inventory - tracked
  #     separately if/when an in-world item/inventory concern exists.
  #   - Editing item fields - see sandbox-edit-item-fields.feature.
  #   - Any change to DevConsolePanel's search/results/keybinding shell.
  #
  # Decisions derived from the codebase (not open questions):
  #   - Registered everywhere the "Classes" provider is: the standalone
  #     ClassSandbox entry point and Main's F1 dev console.
  #   - Entries are keyed by the item's fully-qualified id (e.g.
  #     "core:iron_sword"), which createPanel(id) receives - the
  #     DevConsoleProvider contract uses id, not the display name the issue
  #     text mentions.
  #   - The detail panel reuses the existing DetailsPaneWidget (the shared
  #     Inspectable renderer InventoryPanel/CodexPanel use), not a new
  #     renderer.
  #   - An unknown id throws IllegalArgumentException, matching
  #     ClassSandboxModel's "Unknown class id" behavior.
  #
  # Risks:
  #   - DevConsoleSteps.java is also modified on the in-flight #146 branch
  #     (feat/sandbox-kitchen-sink-scene); adding the "Items" provider to its
  #     providerFor factory may need a merge fix-up when the second PR lands.
  #   - Scenarios read the real mods/core/items data. Renaming or removing
  #     Iron Sword, Bread Loaf, Health Potion or Rusty Key breaks them.
  #
  # Open questions:
  #   - None.
