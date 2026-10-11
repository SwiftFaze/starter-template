Feature: Data-driven NPCs with JSON-authored patrol movement
  An NPC is defined entirely in a JSON file under mods/<mod-id>/npcs/ (id, name,
  symbol, color, spawn point, optional patrol) and loaded through the same
  ModLoader/ModRegistry mechanism as tiles, items and quests. A loaded NPC is a
  drawable, positioned entity in the world and a tick consumer: on each turn of
  the turn scheduler it advances one tile along its patrol route, either
  pacing back and forth ("ping_pong", the default) or circling ("loop"). An NPC
  whose next tile is blocked (non-walkable tile, the player, or another NPC)
  waits in place and retries the same step on the next turn. NPCs are not solid
  to the player.

  Covers: the NPC JSON schema and its load-time validation, spawning registered
  NPCs into the game world, and generic patrol stepping on turns.
  Supersedes: nothing. Turn advancement itself (blocked player moves, pausing,
  scheduler reset) stays owned by turn-tick-engine.feature; this file only
  covers what an NPC does when it is stepped. ModLoader's generic collision and
  override rules are owned by mod-loader.feature and are not repeated except for
  one NPC-specific check.
  Out of scope: any NPC's dialogue or quest content (intro-quest issue);
  NPCs being solid to the player or interacting with the player; pathfinding
  or routing around obstacles; NPCs placed per scene; checking at load time that
  spawn or waypoint tiles are walkable; a shared Player/Npc base class; pixel
  rendering (verified by the Step 4.5 playtest).

  QA: none - NPC movement is only observable in a running game window after
  several moves; key replays do not assert on-screen glyphs. Covered by the
  Step 4.5 playtest.

  Scenario: Loading an NPC with a patrol from a mods directory
    Given a mods directory containing the "core" mod with an NPC declaring id "core:guard", name "Guard", symbol "G", color 200,50,50, spawn (3, 4), and a "ping_pong" patrol of waypoints (3, 4) and (6, 4)
    When the mods directory is loaded
    Then an NPC with ID "core:guard" is available
    And its name is "Guard"
    And its symbol is "G" and its color is 200,50,50
    And its spawn is (3, 4)
    And its patrol mode is "ping_pong" with waypoints (3, 4) and (6, 4)

  Scenario: An NPC with no patrol is loaded as stationary
    Given a mods directory containing the "core" mod with an NPC declaring id "core:statue", name "Statue", symbol "S", color 150,150,150, spawn (2, 2), and no patrol
    When the mods directory is loaded
    Then an NPC with ID "core:statue" is available
    And it has no patrol

  Scenario: A patrol that declares no mode defaults to ping_pong
    Given a mods directory containing the "core" mod with an NPC declaring id "core:guard", spawn (1, 1), and a patrol of waypoints (1, 1) and (1, 3) with no mode
    When the mods directory is loaded
    Then the patrol mode of NPC "core:guard" is "ping_pong"

  Scenario Outline: An invalid NPC definition fails to load
    Given a mods directory containing the "core" mod with an NPC declaring id "core:broken" whose <defect>
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming NPC "core:broken", the reason, and the file it came from

    Examples:
      | defect                                                                 |
      | spawn is missing                                                       |
      | patrol mode is "zigzag"                                                |
      | patrol has only one waypoint                                           |
      | patrol has a diagonal leg from (1, 1) to (2, 2)                        |
      | patrol first waypoint (1, 1) differs from spawn (5, 5)                 |
      | loop patrol of (1, 1), (4, 1), (4, 3) closes diagonally back to (1, 1) |

  Scenario: A mod declaring a colliding NPC ID without an override field fails to load
    Given a mods directory containing the "core" mod with an NPC declaring id "core:guard"
    And the mods directory also contains mod "npc-pack" with an NPC declaring id "core:guard" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:guard" and both mods "core" and "npc-pack"

  Scenario: Loading a mod with a malformed NPC file throws ModLoadException
    Given a mods directory containing mod "broken-npc-pack" with a malformed NPC file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  Scenario: A loaded NPC is drawn in the world at its spawn point
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with symbol "G" and color 200,50,50 is defined with spawn (3, 4) and no patrol
    When the NPCs are spawned into the game world
    Then the world draws an NPC at (3, 4) with symbol "G" and color 200,50,50
    And the NPC is registered with the turn scheduler

  Scenario: A ping_pong NPC advances one tile per turn along its leg
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    When 2 turns pass
    Then the NPC is at (5, 4)

  Scenario: A ping_pong NPC reverses at the end of its route
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (5, 4) is spawned
    When 4 turns pass
    Then the NPC is at (3, 4)
    When 1 turn passes
    Then the NPC is at (4, 4)

  Scenario: A ping_pong NPC with three waypoints turns at the corner then paces back
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4), (5, 4) and (5, 6) is spawned
    When 4 turns pass
    Then the NPC is at (5, 6)
    When 3 turns pass
    Then the NPC is at (4, 4)

  Scenario: A loop NPC returns from its last waypoint to its first
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "loop" patrol of waypoints (3, 4), (5, 4), (5, 6) and (3, 6) is spawned
    When 4 turns pass
    Then the NPC is at (5, 6)
    When 2 turns pass
    Then the NPC is at (3, 6)
    When 2 turns pass
    Then the NPC is at (3, 4)

  Scenario: A stationary NPC never moves
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:statue" with spawn (2, 2) and no patrol is spawned
    When 3 turns pass
    Then the NPC is at (2, 2)

  Scenario: An NPC patrols when the player moves, not on its own
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    Then the NPC is at (3, 4)
    When the player steps right
    Then the NPC is at (4, 4)

  Scenario: A blocked player move does not move NPCs
    Given a turn-ticking game on a map where tile (1, 0) is not walkable, with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    When the player steps right
    Then the NPC is at (3, 4)

  Scenario: An NPC waits when a wall blocks its next tile
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    And map tile (4, 4) is not walkable
    When 3 turns pass
    Then the NPC is at (3, 4)

  Scenario: An NPC resumes its patrol once the wall is gone
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    And map tile (4, 4) is not walkable
    And 2 turns have passed
    When map tile (4, 4) becomes walkable
    And 1 turn passes
    Then the NPC is at (4, 4)

  Scenario: An NPC waits rather than step onto the player
    Given a turn-ticking game on an all-walkable map with the player at (4, 4)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    When 1 turn passes
    Then the NPC is at (3, 4)

  Scenario: An NPC does not skip a waypoint while blocked
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4), (4, 4) and (4, 6) is spawned
    And map tile (4, 4) is not walkable
    When 3 turns pass
    Then the NPC is at (3, 4)

  Scenario: An NPC waits rather than step onto another NPC
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:stander" with spawn (4, 4) and no patrol is spawned
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    When 2 turns pass
    Then the NPC "core:guard" is at (3, 4)
    And the NPC "core:stander" is at (4, 4)

  Scenario: NPCs act in registration order within a turn
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:first" with a "ping_pong" patrol of waypoints (4, 4) and (7, 4) is spawned
    And an NPC "core:second" with a "ping_pong" patrol of waypoints (5, 4) and (8, 4) is spawned
    When 1 turn passes
    Then the NPC "core:first" is at (4, 4)
    And the NPC "core:second" is at (6, 4)

  Scenario: Exiting to the main menu respawns NPCs at their spawn points
    Given a turn-ticking game on an all-walkable map with the player at (0, 0)
    And an NPC "core:guard" with a "ping_pong" patrol of waypoints (3, 4) and (6, 4) is spawned
    And 2 turns have passed
    When the game state is reset by exiting to the main menu and the NPCs are spawned again
    Then the NPC is at (3, 4)
    And exactly one NPC is registered with the turn scheduler

  # Non-goals:
  # - Dialogue, quest hooks, or any specific NPC content (intro-quest issue).
  # - NPCs blocking the player: player movement still checks only tile
  #   walkability, so the player may stand on an NPC's tile. Making NPCs solid
  #   would let a waiting NPC trap the player (a blocked move advances no turn).
  # - Pause behavior and scheduler semantics beyond the one blocked-move
  #   scenario above: owned by turn-tick-engine.feature.
  # - Walkability of spawn/waypoint tiles is not checked at load time (needs a
  #   scene); an NPC on a bad tile just waits or renders in place.
  # - The shipped example NPC under mods/core/npcs/ and Main.java wiring are
  #   verified by the Step 4.5 playtest.

  # Risks:
  # - A scheduler snapshot is stepped, so spawning NPCs during a tick cannot
  #   corrupt iteration; NPCs are spawned at world setup, not mid-turn.
  # - "Another NPC occupies the tile" needs a way for an NPC to see other NPCs;
  #   keep that out of Player/WorldScene (e.g. an occupancy check supplied at
  #   spawn) so Player movement stays untouched.

  # Open questions:
  # - None; see the intent doc's Clarifications.
