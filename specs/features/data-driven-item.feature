Feature: Data-driven items (JSON schema + minimal InventoryPanel wiring)
  Item definitions live in mods/core/items/*.json, loaded through the same
  ModLoader/mods/ mechanism tiles, buildings, and classes already use, keyed
  by a namespaced ID, with stat_bonus effects validated against the existing
  stat vocabulary registry at load time — and EastPanel wires at least one
  real core: item into InventoryPanel instead of its hardcoded stub labels.

  Scenario: Loading a core item from a mods directory
    Given a mods directory containing the "core" mod with an item declaring id "core:iron_sword", name "Iron Sword", glyph "/", type "weapon", slot "main_hand", base damage min 4 and max 9, and a "stat_bonus" effect on stat "strength" with calc "level*1.5+2"
    When the mods directory is loaded
    Then an item with ID "core:iron_sword" is available
    And its name is "Iron Sword"
    And its glyph is "/"
    And its base damage is 4 to 9
    And it has one effect: a "stat_bonus" on stat "strength" with calc "level*1.5+2"

  Scenario: An item with no effects declares an empty effects list
    Given a mods directory containing the "core" mod with an item declaring id "core:plain_shield", name "Plain Shield", glyph "]", type "armor", slot "off_hand", base damage min 0 and max 0, and no effects
    When the mods directory is loaded
    Then an item with ID "core:plain_shield" is available
    And it has no effects

  Scenario: An item effect referencing an unregistered stat fails to load
    Given a mods directory containing the "core" mod with an item declaring id "core:cursed_ring" with a "stat_bonus" effect on stat "endurance" with calc "level*2"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming item "core:cursed_ring", stat "endurance", and the file it came from

  Scenario: An item effect with an unparseable calc string fails to load
    Given a mods directory containing the "core" mod with an item declaring id "core:broken_amulet" with a "stat_bonus" effect on stat "luck" with calc "level*"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming item "core:broken_amulet" and the file it came from

  Scenario: A mod declaring a colliding item ID without an override field fails to load
    Given a mods directory containing the "core" mod with an item declaring id "core:iron_sword", name "Iron Sword", glyph "/", type "weapon", slot "main_hand", base damage min 4 and max 9, and a "stat_bonus" effect on stat "strength" with calc "level*1.5+2"
    And the mods directory also contains mod "reskin-pack" with an item declaring id "core:iron_sword" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:iron_sword" and both mods "core" and "reskin-pack"

  Scenario: A mod declaring a colliding item ID with an explicit override replaces the earlier definition
    Given a mods directory containing the "core" mod with an item declaring id "core:iron_sword", name "Iron Sword", glyph "/", type "weapon", slot "main_hand", base damage min 4 and max 9, and a "stat_bonus" effect on stat "strength" with calc "level*1.5+2"
    And the mods directory also contains mod "reskin-pack" with an item declaring id "core:iron_sword", name "Rusty Sword", and the same base damage and effects, whose "overrides" field names "core:iron_sword"
    When the mods directory is loaded
    Then an item with ID "core:iron_sword" is available
    And its name is "Rusty Sword"

  Scenario: Loading a mod with a malformed item resource throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed item file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  # Non-goals:
  #   - Quests, maps — separate follow-on issues.
  #   - Inventory management (pickup, drop, equip, stack, use/consume) — no
  #     Player inventory concept exists yet; out of scope for this slice.
  #   - Validating "type"/"slot" values against a fixed vocabulary — the
  #     issue only calls for validating effects[].stat against the stat
  #     registry; type/slot are free-form strings for this slice.
  #   - Extending the stat registry (mods/core/stats.json) itself — items
  #     reference existing registry stats only.
  #   - effects[].type values beyond "stat_bonus" — more types are
  #     additive, later work.
  #   - A callable API to evaluate an item's effects[].calc to a number
  #     (e.g. at a given player level) — this slice only validates
  #     effects[].stat against the registry and parses effects[].calc for
  #     syntactic validity. No equip system exists yet to consume a
  #     computed value.
  #
  # Risks:
  #   - mods/core/items/iron_sword.json ships as real core content and
  #     becomes a load-bearing test fixture for the EastPanel scenario
  #     above, the same way mods/core/classes/mage.json already is for
  #     PlayerInfoTest.settingPlayerClassReappliesBaseStats — renaming or
  #     removing it later breaks that scenario, not just documentation.
  #   - EastPanel gains a ModLoader.load(Paths.get("mods")) call in its own
  #     constructor (mirroring PlayerInfo/TileTestScene2's existing
  #     self-contained pattern) rather than receiving a ModRegistry from
  #     Main.java — this means "new EastPanel()" in tests now does real
  #     file I/O against the checked-in mods/ directory, same as
  #     PlayerInfoTest already accepts for PlayerInfo.
  #
  # Open questions:
  #   None outstanding (effects[].type vocabulary, calc-evaluation
  #   scope, and InventoryPanel's wiring mechanism all resolved).
