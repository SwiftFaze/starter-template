Feature: Data-driven quests (JSON schema + minimal quest-state tracking)
  Quest definitions live in mods/core/quests/*.json, loaded through the
  same ModLoader/mods/ mechanism tiles, buildings, classes, and items
  already use, keyed by a namespaced ID, with a fixed-vocabulary "kill"
  objective and item/xp rewards — item rewards validated against the item
  registry and xp rewards' calc strings parsed at load time — plus a
  minimal per-player quest state (not started / offered / active /
  complete) that survives at least the current play session.

  Scenario: Loading a core quest from a mods directory
    Given a mods directory containing the "core" mod with an item declaring id "core:iron_sword", name "Iron Sword", glyph "/", type "weapon", slot "main_hand", base damage min 4 and max 9, and no effects
    And the mods directory also contains the "core" mod with a quest declaring id "core:goblin_slayer", name "Goblin Slayer", objective type "kill" on target "core:goblin" with count 5, an item reward of "core:iron_sword" count 1, and an xp reward with calc "level*25"
    When the mods directory is loaded
    Then a quest with ID "core:goblin_slayer" is available
    And its name is "Goblin Slayer"
    And its objective is a "kill" on target "core:goblin" with count 5
    And it has 2 rewards: an item reward of "core:iron_sword" count 1, and an xp reward with calc "level*25"

  Scenario: A quest with no rewards declares an empty rewards list
    Given a mods directory containing the "core" mod with a quest declaring id "core:explorer", name "Explorer", objective type "kill" on target "core:rat" with count 1, and no rewards
    When the mods directory is loaded
    Then a quest with ID "core:explorer" is available
    And it has no rewards

  Scenario: A quest reward referencing an unregistered item fails to load
    Given a mods directory containing the "core" mod with a quest declaring id "core:phantom_reward", objective type "kill" on target "core:goblin" with count 1, and an item reward of "core:nonexistent_sword" count 1
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming quest "core:phantom_reward", item "core:nonexistent_sword", and the file it came from

  Scenario: A quest xp reward with an unparseable calc string fails to load
    Given a mods directory containing the "core" mod with a quest declaring id "core:broken_reward", objective type "kill" on target "core:goblin" with count 1, and an xp reward with calc "level*"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming quest "core:broken_reward" and the file it came from

  Scenario: A quest declaring an unsupported objective type fails to load
    Given a mods directory containing the "core" mod with a quest declaring id "core:mystery_quest", objective type "gather" on target "core:herb" with count 3, and no rewards
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming quest "core:mystery_quest", objective type "gather", and the file it came from

  Scenario: A mod declaring a colliding quest ID without an override field fails to load
    Given a mods directory containing the "core" mod with a quest declaring id "core:goblin_slayer", name "Goblin Slayer", objective type "kill" on target "core:goblin" with count 5, and no rewards
    And the mods directory also contains mod "quest-pack" with a quest declaring id "core:goblin_slayer" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:goblin_slayer" and both mods "core" and "quest-pack"

  Scenario: A mod declaring a colliding quest ID with an explicit override replaces the earlier definition
    Given a mods directory containing the "core" mod with a quest declaring id "core:goblin_slayer", name "Goblin Slayer", objective type "kill" on target "core:goblin" with count 5, and no rewards
    And the mods directory also contains mod "quest-pack" with a quest declaring id "core:goblin_slayer", name "Slay the Goblins", and the same objective and rewards, whose "overrides" field names "core:goblin_slayer"
    When the mods directory is loaded
    Then a quest with ID "core:goblin_slayer" is available
    And its name is "Slay the Goblins"

  Scenario: Loading a mod with a malformed quest resource throws ModLoadException
    Given a mods directory containing mod "broken-quest-pack" with a malformed quest file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  Scenario: A new player's quest state defaults to not started
    Given a new player
    Then the player's quest state for "core:goblin_slayer" is "not started"

  Scenario Outline: Setting a player's quest state changes it
    Given a new player
    When the player's quest state for "core:goblin_slayer" is set to "<state>"
    Then the player's quest state for "core:goblin_slayer" is "<state>"

    Examples:
      | state    |
      | offered  |
      | active   |
      | complete |

  # Non-goals:
  #   - Maps — separate follow-on issue (#26's phasing, phase 6).
  #   - Quest-giving NPCs / dialogue UX — no NPC concept exists in the
  #     codebase yet; this feature covers the schema and state model only.
  #   - Any mechanism that actually transitions quest state in response to
  #     gameplay (e.g. detecting a "kill" objective's target being
  #     defeated) — no combat/monster/enemy system exists anywhere in the
  #     codebase today. This feature provides the schema, load-time
  #     validation, and a state container/API a future combat system can
  #     call into, not the combat system itself.
  #   - objective.type values beyond "kill" — more types are additive
  #     later work, not this slice.
  #   - rewards[].type values beyond "item" and "xp".
  #   - Persisting quest state across game restarts (save/load) — no
  #     save/load system exists anywhere in the codebase; see
  #     Clarifications in the intent doc (self-resolved: session-only for
  #     this phase).
  #   - Any UI surface for quests (a quest log panel, dialogue prompts).
  #   - Validating a quest's objective "target" against anything (e.g. a
  #     monster registry) — no such registry exists; "target" is an
  #     unvalidated opaque string reference this slice.
  #   - Validating a player's quest state changes against the loaded quest
  #     registry (ModRegistry) — QuestLog tracks arbitrary quest IDs
  #     unvalidated, matching how PlayerInfo doesn't cross-validate
  #     playerClass against the registry post-load either.
  #   - Enforcing an ordered state-machine transition (not started ->
  #     offered -> active -> complete) — this slice is a free-form setter;
  #     see Clarifications in the intent doc (self-resolved).
  #
  # Risks:
  #   - Quest loading must run after item loading within ModLoader.load(),
  #     since rewards[].id resolution (item type) depends on the item
  #     registry already being populated for the same mod-load pass —
  #     mirrors items loading after the stat registry for the same reason
  #     (see data-driven-item.feature's Risks).
  #   - mods/core/quests/goblin_slayer.json ships as real core content and
  #     references mods/core/items/iron_sword.json by ID — renaming or
  #     removing that item later breaks quest loading, not just this
  #     fixture, the same way mods/core/items/iron_sword.json already is a
  #     load-bearing fixture for data-driven-item.feature's EastPanel
  #     scenario.
  #
  # Open questions:
  #   None outstanding. All four open questions in this pass were
  #   self-resolved per explicit instruction rather than reviewed by the
  #   human before spec approval; flagged here for visibility at PR
  #   review time.
