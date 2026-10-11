Feature: Data-driven Tile (JSON definitions + registry)
  Tile definitions move from a hardcoded Java enum to JSON, loaded through
  the same ModLoader/mods/ mechanism buildings already use, keyed by a
  namespaced ID.

  Scenario: Loading a walkable tile definition
    Given a mods directory containing the "core" mod with a tile declaring id "core:grass", symbol "⡐", color (179, 224, 160), and walkable true
    When the mods directory is loaded
    Then a tile with ID "core:grass" is available
    And its symbol is "⡐"
    And its color is (179, 224, 160)
    And it is walkable

  Scenario: Loading a non-walkable tile definition
    Given a mods directory containing the "core" mod with a tile declaring id "core:water", symbol "⠭", color (174, 191, 232), and walkable false
    When the mods directory is loaded
    Then a tile with ID "core:water" is available
    And it is not walkable

  Scenario: A building's tiles array resolves namespaced tile IDs
    Given a mods directory containing the "core" mod with a tile declaring id "core:stone", symbol "⠿", color (88, 88, 92), and walkable false
    And the mods directory also contains a building declaring id "core:test_building" whose blueprint is a single tile "core:stone"
    When the mods directory is loaded
    Then the building "core:test_building"'s blueprint at (0, 0) references tile "core:stone"

  Scenario: A mod declaring a colliding tile ID without an override field fails to load
    Given a mods directory containing the "core" mod with a tile declaring id "core:grass", symbol "⡐", color (179, 224, 160), and walkable true
    And the mods directory also contains mod "retexture-pack" with a tile declaring id "core:grass" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:grass" and both mods "core" and "retexture-pack"

  Scenario: A mod declaring a colliding tile ID with an explicit override replaces the earlier definition
    Given a mods directory containing the "core" mod with a tile declaring id "core:grass", symbol "⡐", color (179, 224, 160), and walkable true
    And the mods directory also contains mod "retexture-pack" with a tile declaring id "core:grass", symbol "#", color (0, 0, 0), and walkable true, whose "overrides" field names "core:grass"
    When the mods directory is loaded
    Then a tile with ID "core:grass" is available
    And its symbol is "#"

  Scenario: Loading a mod with a malformed tile resource throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed tile file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  # Non-goals:
  #   - PlayerClass, items, quests, maps — separate follow-on issues
  #     (#50, #51, #52, #53).
  #   - Embedded scripting — fixed JSON vocabulary only, per #26.
  #   - A general stat/vocabulary registry (core:stats.json) — tiles have
  #     no numeric "calc" fields, so it isn't needed here.
  #   - Non-boolean walkability (e.g. movement cost) — resolved as boolean
  #     only for this phase.
  #   - New scenarios in this file for WorldScene.fillAll/Player
  #     movement/GamePanel rendering — those call sites migrate to a
  #     CoreTiles String-ID constants class + registry lookups (see
  #     Clarifications), but specs/features/world-single-floor-rendering.feature
  #     and specs/features/world-scene-population-and-building-placement.feature
  #     already exercise that behavior at the Gherkin level and need no
  #     text changes, only step-definition changes — no new scenarios
  #     belong in *this* file for it.
  #   - Wiki documentation content itself — a Step 7 deliverable, not a
  #     Gherkin-testable behavior.
  #
  # Risks:
  #   - This phase is far more invasive than the ModLoader/buildings phase
  #     (#48): Tile is referenced by 12 files today as compile-time enum
  #     constants, not just JSON. All 12 need updating to the
  #     CoreTiles/registry pattern in the same change.
  #   - mods/core/buildings/small_house_01.json's "tiles" array currently
  #     holds raw enum names ("STONE", "WOOD", "DOOR"); it must be migrated
  #     to namespaced IDs ("core:stone", "core:wood", "core:door") in the
  #     same change, or ModLoader's existing building-parsing breaks.
  #   - specs/features/world-single-floor-rendering.feature and
  #     specs/features/world-scene-population-and-building-placement.feature
  #     must keep passing unmodified (Gherkin text unchanged) once their
  #     step definitions move off direct Tile enum references.
  #
  # Open questions:
  #   None outstanding.
