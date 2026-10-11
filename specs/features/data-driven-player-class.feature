Feature: Data-driven PlayerClass (JSON base stats + growth curves)
  Player class definitions move from classpath JSON loaded by
  PlayerClassLoader to mods/core/classes/*.json, loaded through the same
  ModLoader/mods/ mechanism tiles and buildings already use, keyed by a
  namespaced ID, with per-level stat growth expressed as calc strings
  validated against a stat vocabulary registry.

  Scenario: Loading the core Warrior class from a mods directory
    Given a mods directory containing the "core" mod with a class declaring id "core:warrior", name "Warrior", base strength 15, dexterity 10, constitution 14, intelligence 6, wisdom 6, luck 8, max HP 120, and max mana 20
    When the mods directory is loaded
    Then a class with ID "core:warrior" is available
    And its name is "Warrior"
    And its base max HP is 120
    And its base max mana is 20

  Scenario: Loading the core Mage class from a mods directory
    Given a mods directory containing the "core" mod with a class declaring id "core:mage", name "Mage", base strength 6, dexterity 9, constitution 8, intelligence 16, wisdom 14, luck 7, max HP 70, and max mana 100
    When the mods directory is loaded
    Then a class with ID "core:mage" is available
    And its name is "Mage"
    And its base max HP is 70
    And its base max mana is 100

  Scenario: A class's stat value grows across levels according to its growth calc
    Given a mods directory containing the "core" mod with a class declaring id "core:test_class" with base strength 10 and a strength growth calc of "level*2"
    When the mods directory is loaded
    Then the class "core:test_class"'s strength at level 0 is 10
    And the class "core:test_class"'s strength at level 5 is 20

  Scenario: A stat with no growth calc stays at its base value at every level
    Given a mods directory containing the "core" mod with a class declaring id "core:test_class" with base luck 8 and no growth calc for luck
    When the mods directory is loaded
    Then the class "core:test_class"'s luck at level 10 is 8

  Scenario: A non-integer growth result rounds half-up to the nearest stat value
    Given a mods directory containing the "core" mod with a class declaring id "core:test_class" with base strength 0 and a strength growth calc of "level*1.5+2"
    When the mods directory is loaded
    Then the class "core:test_class"'s strength at level 5 is 10

  Scenario: A class's growth calc referencing an unregistered stat fails to load
    Given a mods directory containing the "core" mod with a class declaring id "core:test_class" with a growth calc for stat "endurance" of "level*2"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming class "core:test_class", stat "endurance", and the file it came from

  Scenario: A mod declaring a colliding class ID without an override field fails to load
    Given a mods directory containing the "core" mod with a class declaring id "core:warrior", name "Warrior", base strength 15, dexterity 10, constitution 14, intelligence 6, wisdom 6, luck 8, max HP 120, and max mana 20
    And the mods directory also contains mod "reskin-pack" with a class declaring id "core:warrior" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:warrior" and both mods "core" and "reskin-pack"

  Scenario: A mod declaring a colliding class ID with an explicit override replaces the earlier definition
    Given a mods directory containing the "core" mod with a class declaring id "core:warrior", name "Warrior", base strength 15, dexterity 10, constitution 14, intelligence 6, wisdom 6, luck 8, max HP 120, and max mana 20
    And the mods directory also contains mod "reskin-pack" with a class declaring id "core:warrior", name "Berserker", and the same base stats, whose "overrides" field names "core:warrior"
    When the mods directory is loaded
    Then a class with ID "core:warrior" is available
    And its name is "Berserker"

  Scenario: Loading a mod with a malformed class resource throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed class file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  # Non-goals:
  #   - Items, quests, maps — separate follow-on issues.
  #   - Any runtime level-up trigger or mechanic — Level.java/PlayerInfo.java
  #     are untouched; growth is a directly-callable capability, not a wired
  #     game event.
  #   - Re-testing "a new player defaults to Warrior" — already covered by
  #     specs/features/default-player-class.feature, which must keep
  #     passing unmodified against the ModRegistry-backed PlayerClass.
  #   - Extending the stat registry to derived stats (attackPower, defense)
  #     — out of scope for now; items would need this later.
  #
  # Risks:
  #   - core:warrior/core:mage base stat values must exactly match today's
  #     src/main/resources/classes/warrior.json / mage.json numbers, or this
  #     is an unintended balance change.
  #   - specs/features/class-stats-sandbox.feature and
  #     specs/features/class-sandbox-panel-selection.feature currently
  #     depend on PlayerClass/PlayerClassLoader via ClassSandboxModel; both
  #     must keep passing unmodified (Gherkin text unchanged) once
  #     ClassSandboxModel moves to the ModRegistry-backed PlayerClass.
  #   - specs/features/data-driven-player-classes.feature and the old
  #     classpath-based PlayerClassLoader mechanism are superseded by this
  #     file and by mods/core/classes/*.json; the old feature file,
  #     PlayerClassLoader, and src/main/resources/classes/*.json are
  #     removed in the same change.
  #   - `level` is 0-indexed, matching Level.getCurrentLevel()'s existing
  #     convention (PlayerInfoPanel already displays "LV " + currentLevel
  #     with no offset, and Level starts currentLevel at 0).
  #
  # Open questions:
  #   None outstanding (growth calc semantics, calc grammar scope,
  #   rounding rule, and optional base/growth fields all resolved).
