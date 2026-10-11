Feature: Interactable world objects (chest, door)
  World objects with an interact action and state — a chest that can be
  opened to reveal its contents, and a door that toggles open/closed — share
  one InteractableObject abstraction. The player triggers an interactable
  with the E key: the four orthogonal neighbours are checked in the order
  N, E, S, W, and the first interactable found wins. The player's own tile
  and diagonals are out of range.

  Object definitions live in mods/<mod>/objects/*.json ("type": "chest" with
  "slots" and "contents", or "type": "door" with "open", "symbolOpen",
  "symbolClosed"). Buildings place them through an "objects" list of
  {"id", "x", "y"}; Java scenes place them in code. Doors default to closed.
  Each new object *type* is a Java subclass; only instance data is JSON.

  Covers: the InteractableObject abstraction, Chest and Door behavior, the E
  interact keybinding, the chest contents panel, the objects JSON kind and
  the building "objects" list, and the static "[E] Interact" hint on the
  world screen.

  Supersedes: the purely cosmetic core:door and core:chest tiles
  (mods/core/tiles/door.json, chest.json, CoreTiles.DOOR / CHEST), which are
  deleted; small_house_01's door cell becomes core:wood with a door object
  placed on it.

  Excludes: locking and key requirements; taking items out of a chest into
  the player's inventory (#72); contextual "[E] Open door" hints (#133);
  placing objects from generated worlds (#67).

  Background:
    Given the "core" mod is loaded

  Scenario: Pressing E next to a closed door opens it
    Given a closed door at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 4) is open
    And the tile (5, 4) is walkable
    And the tile (5, 4) shows the door's open symbol

  Scenario: Pressing E next to an open door closes it
    Given an open door at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 4) is closed
    And the tile (5, 4) is not walkable
    And the tile (5, 4) shows the door's closed symbol

  Scenario Outline: Each orthogonal neighbour is in range
    Given a closed door at <door>
    And the player stands at (5, 5)
    When the player presses E
    Then the door at <door> is open

    Examples:
      | door    |
      | (5, 4)  |
      | (6, 5)  |
      | (5, 6)  |
      | (4, 5)  |

  Scenario: Diagonal objects are out of range
    Given a closed door at (6, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (6, 4) is closed

  Scenario: Pressing E with no interactable in range does nothing
    Given a closed door at (9, 9)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (9, 9) is closed
    And no panel is shown

  Scenario: Interaction checks tiles in a fixed order
    Given a closed door at (5, 4)
    And a closed door at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 4) is open
    And the door at (6, 5) is closed

  Scenario: Pressing E in a doorway reaches a neighbouring object
    Given an open door at (5, 5)
    And a closed door at (6, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (6, 5) is open
    And the door at (5, 5) is open

  Scenario: Pressing E in a doorway with nothing adjacent does nothing
    Given an open door at (5, 5)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 5) is open
    And no panel is shown

  Scenario: A door cannot close on an occupant
    Given an open door at (5, 4)
    And another entity stands at (5, 4)
    And the player stands at (5, 5)
    When the player presses E
    Then the door at (5, 4) is open
    And the event log shows "Something is in the way."

  Scenario: A closed door blocks movement
    Given a closed door at (5, 4)
    And the player stands at (5, 5)
    When the player moves up
    Then the player is at (5, 5)

  Scenario: Pressing E next to a chest reveals its contents
    Given a chest at (6, 5) with 3 slots containing "core:health_potion" and "core:bread_loaf"
    And the player stands at (5, 5)
    When the player presses E
    Then the chest panel is shown
    And it lists the names of "core:health_potion" and "core:bread_loaf"
    And it lists 1 slot as "empty"

  Scenario Outline: Closing the chest panel
    Given the chest panel is open
    When the player presses <key>
    Then the chest panel is not shown

    Examples:
      | key |
      | E   |
      | Esc |

  Scenario Outline: Another screen's key does nothing while the chest panel is open
    Given the chest panel is open
    When the player presses <key>
    Then the <screen> is not shown
    And the chest panel is shown

    Examples:
      | key | screen    |
      | I   | inventory |
      | X   | codex     |

  Scenario: A chest blocks movement
    Given a chest at (5, 4) with 1 slot containing nothing
    And the player stands at (5, 5)
    When the player moves up
    Then the player is at (5, 5)

  Scenario: Loading object definitions
    Given a mod declaring a door object "test:gate" with no "open" field
    And the mod declares a chest object "test:crate" with 2 slots containing "core:iron_sword"
    When the mods directory is loaded
    Then object "test:gate" is a door that starts closed
    And object "test:crate" is a chest with 2 slots containing "core:iron_sword"

  Scenario: A building places a door object
    When the mods directory is loaded
    Then building "core:small_house_01" places a door object at (3, 6)
    And that door starts closed
    And the blueprint tile at (3, 6) is "core:wood"

  Scenario: The old door and chest tiles no longer exist
    When the mods directory is loaded
    Then no tile with ID "core:door" is available
    And no tile with ID "core:chest" is available

  Scenario Outline: Invalid object definitions fail to load
    Given a mod declaring <definition>
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming <named>

    Examples:
      | definition                                                              | named                         |
      | a chest "test:crate" with 1 slot containing "core:health_potion" and "core:bread_loaf" | "test:crate" and its slot count |
      | a chest "test:crate" with 2 slots containing "core:no_such_item"       | "core:no_such_item"           |
      | an object "test:thing" with type "lever"                               | the unknown type "lever"      |
      | a building placing object "core:no_such_object"                        | "core:no_such_object"         |
      | a building placing an object outside its blueprint                     | the building and the position |

  Scenario: The world screen's controls hint lists the interact key
    Given the world screen is shown
    Then the controls hint bar includes "[E] Interact"

  # Non-goals:
  #   - Locked doors/chests and keys (intro quest #78 needs a chest holding a
  #     key, not a locked one).
  #   - Moving chest items into the player's inventory — #72.
  #   - Contextual action hints in the controls bar — #133.
  #   - JSON-scriptable behavior for new object types.
  #   - Placing objects in generated worlds — #67 reuses these definitions.
  #
  # Risks:
  #   - Tile is an immutable flyweight shared across the whole grid, so object
  #     state needs a per-position object layer on WorldScene; isWalkable,
  #     renderWorld and renderToGrid must all consult it.
  #   - small_house_01 now needs E to enter — playtest it (Step 4.5).
  #   - ModLoaderIt asserts the house's south-wall cell is the core:door tile;
  #     that assertion changes with this feature.
  #   - No non-player entity exists yet (NPCs are #69), so "A door cannot
  #     close on an occupant" needs a test-only occupant; the occupancy query
  #     should take any positioned entity, not just the player.
  #
  # Open questions: none.
