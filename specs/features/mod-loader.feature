Feature: Mod loader (external mods/ directory)
  Building content moves from a classpath resource loaded directly by
  BuildingLoader to a ModLoader that reads namespaced content from an
  external mods/ directory, with core itself loading through the exact
  same path as any third-party mod.

  Background:
    Given a mods directory containing the "core" mod with a building declaring id "core:small_house_01"

  Scenario: Loading core's own content produces a namespaced building ID
    When the mods directory is loaded
    Then a building with ID "core:small_house_01" is available

  Scenario: A third-party mod's content loads alongside core
    Given the mods directory also contains mod "goblin-pack" with a building declaring id "goblin-pack:goblin_den"
    When the mods directory is loaded
    Then a building with ID "core:small_house_01" is available
    And a building with ID "goblin-pack:goblin_den" is available

  Scenario: A mod declaring a colliding ID without an override field fails to load
    Given the mods directory also contains mod "retexture-pack" with a building declaring id "core:small_house_01" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:small_house_01" and both mods "core" and "retexture-pack"

  Scenario: A mod declaring a colliding ID with an explicit override replaces the earlier definition
    Given the mods directory also contains mod "retexture-pack" with a building declaring id "core:small_house_01" and an "overrides" field of "core:small_house_01"
    When the mods directory is loaded
    Then a building with ID "core:small_house_01" is available
    And its blueprint matches the one from mod "retexture-pack", not "core"

  Scenario: A mod that depends on another mod loads after it
    Given the mods directory also contains mod "goblin-pack" with a building declaring id "goblin-pack:goblin_den"
    And mod "goblin-pack" declares a "dependsOn" of "core"
    When the mods directory is loaded
    Then mod "core" finishes loading before mod "goblin-pack" starts loading

  # Non-goals:
  #   - Data-driving Tile, PlayerClass, items, quests, or maps — separate
  #     follow-on issues (#49, #50, #51, #52, #53).
  #   - Embedded scripting or the "calc" expression parser — buildings have
  #     no numeric tuning fields, so this doesn't arise until the Tile/
  #     PlayerClass phases.
  #   - A stat/vocabulary registry (core:stats.json) — not needed for
  #     buildings.
  #   - In-game UI for browsing/enabling/disabling mods — mods/ contents
  #     load unconditionally at startup.
  #   - The jpackage installer bundling a mods/ folder alongside the
  #     executable — a release/build packaging concern (docs/release.md),
  #     verified manually, not exercised via Cucumber.
  #   - A "mods/ directory doesn't exist at all" scenario — core always
  #     ships as a present mods/core/ folder (checked into the repo for
  #     dev, packaged for release), so a missing mods/ directory is a
  #     broken install, not a supported/tested case.
  #   - mod.json's displayName/version/description fields — not part of
  #     this phase's schema; only "id" and "dependsOn" are load-bearing.
  #   - Exactly how mods/ path resolution is implemented (working-
  #     directory-relative, per the Clarifications) — an implementation
  #     detail, not a distinct behavior scenario.
  #
  # Risks:
  #   - specs/features/world-single-floor-rendering.feature and
  #     world-scene-population-and-building-placement.feature currently
  #     depend on small_house_01 loading via today's classpath
  #     BuildingLoader; both must keep passing unmodified once ModLoader
  #     replaces that path (same coexistence concern noted in
  #     data-driven-player-classes.feature for default-player-class.feature).
  #   - specs/features/building-loader-failure-path.feature currently
  #     asserts BuildingException for a missing/malformed *classpath*
  #     resource fixture; now that ModLoadException replaces
  #     BuildingException's role for building loads and loading moves to
  #     an external mods/ directory, that spec needs reconciling
  #     (updating its fixture path and expected exception type) during
  #     implementation.
  #   - Building JSON gains a new required "id" field (e.g.
  #     "core:small_house_01"); this must be added to
  #     mods/core/buildings/small_house_01.json without disturbing its
  #     existing "name"/"width"/"height"/"tiles" fields, since
  #     WorldSingleFloorRenderingSteps reads "width"/"height" directly
  #     from that JSON.
  #
  # Open questions:
  #   None outstanding.
