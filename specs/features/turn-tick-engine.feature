Feature: Turn/tick engine advances the world one turn per successful player move
  Introduces a turn scheduler: every entity registered with it gets exactly one
  chance to act each time the player successfully moves. A blocked move does not
  advance a turn. While any screen is open (inventory, codex, pause menu, and any
  future dialogue/menu screen) the world is paused through one shared mechanism:
  movement input is ignored and no turns are processed. Exiting to the main menu
  clears the scheduler.

  Covers: the scheduler, its turn counter, the shared world-paused state, and a
  no-op counting consumer that proves entities are stepped.
  Supersedes: the pause-menu-only `GamePanel.paused` flag described in
  pause-screen-esc.feature ("Movement input freezes while paused") — that freeze
  is now one case of the shared world-paused state. pause-screen-esc.feature
  still owns the pause menu's own items and navigation.
  Out of scope: any entity that actually does something on a tick (#69); any
  player-visible sign that a turn happened (no event-log entry, no dev-console
  counter); real-time or timed ticking; dev-console repositioning advancing
  turns; the dev console pausing the world.

  QA: none - ticks are not player-visible yet, so a key replay cannot observe
  them; movement and inventory replays already exist (map-movement,
  inventory-toggle).

  Background:
    Given a turn-ticking game with the player at (5, 5)
    And a counting tick consumer is registered with the turn scheduler

  Scenario: A successful move advances the world by one turn
    Given map tile (6, 5) is walkable
    When the player steps right
    Then the turn counter is 1
    And the counting tick consumer has been stepped 1 time

  Scenario: Every registered entity acts once per turn, in registration order
    Given a second counting tick consumer is registered with the turn scheduler
    And map tile (6, 5) is walkable
    When the player steps right
    Then each counting tick consumer has been stepped 1 time
    And the first consumer was stepped before the second

  Scenario: Several successful moves advance one turn each
    Given map tiles (6, 5), (7, 5) and (8, 5) are walkable
    When the player steps right 3 times
    Then the turn counter is 3
    And the counting tick consumer has been stepped 3 times

  Scenario: A blocked move does not advance a turn
    Given map tile (6, 5) is not walkable
    When the player steps right
    Then the player is at position (5, 5)
    And the turn counter is 0
    And the counting tick consumer has been stepped 0 times

  Scenario: Repositioning the player directly does not advance a turn
    When the player is repositioned directly to (8, 8)
    Then the player is at position (8, 8)
    And the turn counter is 0
    And the counting tick consumer has been stepped 0 times

  Scenario Outline: Opening a screen pauses the world
    When the <screen> screen is opened
    Then the world is paused

    Examples:
      | screen     |
      | inventory  |
      | codex      |
      | pause menu |

  Scenario Outline: Movement input is ignored while a screen is open
    Given map tile (6, 5) is walkable
    And the <screen> screen is open
    When the player steps right
    Then the player is at position (5, 5)
    And the turn counter is 0
    And the counting tick consumer has been stepped 0 times

    Examples:
      | screen     |
      | inventory  |
      | codex      |
      | pause menu |

  Scenario Outline: Closing the last open screen resumes ticking
    Given map tile (6, 5) is walkable
    And the <screen> screen is open
    When the <screen> screen is closed
    And the player steps right
    Then the world is not paused
    And the turn counter is 1

    Examples:
      | screen     |
      | inventory  |
      | codex      |
      | pause menu |

  Scenario: Switching directly between screens keeps the world paused
    Given the inventory screen is open
    When the codex screen is opened
    Then the inventory screen is no longer open
    And the world is paused

  Scenario: Exiting to the main menu clears the turn scheduler
    Given map tile (6, 5) is walkable
    And the player has stepped right
    When the game state is reset by exiting to the main menu
    Then the turn counter is 0
    And no tick consumers are registered
    And the world is not paused

  # Non-goals:
  # - Any consumer with real behavior (NPC patrols, timed effects) — #69.
  # - Player-visible turn feedback (event-log line, dev-console turn counter) —
  #   per Clarifications; the Step 4.5 playtest checks only that movement,
  #   blocked moves, and opening/closing screens still feel right.
  # - Dialogue popup / standalone quest-log screen: they don't exist yet. When
  #   added, they must join the shared world-paused state like the screens above.
  # - Re-wiring Main.java's composition root is verified by playtest, not here,
  #   consistent with pause-screen-esc.feature.
  # - Dev-console repositioning (X/Y rows, snapshot restore) advancing turns:
  #   only keyboard movement via GamePanel.MoveAction is a turn.
  # - The dev console pausing the world: it is a separate frame outside the
  #   game's screens and does not join the world-paused state.
  # - Settings opened from the pause menu needs no scenario of its own: the
  #   pause menu stays open underneath, so the world is already paused.

  # Risks:
  # - Today inventory/codex block movement only via modal focus capture. Moving
  #   the source of truth to an explicit paused state means the pause menu's
  #   existing setPaused(true/false) calls must route through the same mechanism,
  #   or closing one screen could unpause the world while another is still open.
  # - A consumer registering or unregistering entities during a tick could
  #   mutate the list mid-iteration; the scheduler should step a snapshot.

  # Open questions:
  # - None.
