Feature: Seeded world generation
  A world seed drives Perlin-noise terrain and a scatter of building
  blueprints in a new GeneratedWorldScene(width, height, seed). Terrain is
  bucketed from the noise value into contiguous ranges of namespaced core
  tiles (low to high: core:water, core:sand, core:mud, core:grass, core:tree,
  core:rock, core:stone, core:snow, core:ice). The seed shuffles the noise
  permutation table, so every seed has its own terrain. Up to 5 buildings,
  each picked from every registered blueprint, sit fully on walkable terrain
  with a walkable tile outside the door and never overlap. The player spawns
  on the walkable tile nearest the map centre outside any building. The same
  seed always reproduces the same terrain, buildings and spawn.

  Each new game takes a random seed unless -Dveil.worldSeed=<n> overrides it,
  and logs "World generated from seed <n>" at INFO so a map can be
  reproduced.

  Supersedes: TileTestScene2 (all-grass fill, one house at a hardcoded
  (130, 130)), which is deleted; GamePanel builds GeneratedWorldScene instead.
  The sandbox kitchen-sink scene is a separate class and is unaffected.

  Out of scope: roads, districts, zoning or any placement rule beyond
  overlap-avoidance; new building blueprints; a settings UI for the seed;
  a GameEvent for world generation.

  QA: none - world generation has no key-driven behaviour; covered by the human playtest

  Scenario: The same seed reproduces the same map
    Given a world generated from seed 12345
    When another world is generated from seed 12345
    Then both worlds have the same tile at every position
    And both worlds have the same buildings at the same positions
    And both worlds spawn the player at the same position

  Scenario: Seeds 256 apart produce different terrain
    Given a world generated from seed 12345
    When another world is generated from seed 12601
    Then the two worlds differ in at least one tile

  Scenario: Generated terrain uses only the core terrain tiles
    Given a world generated from seed 12345
    Then every tile outside a building is one of core:water, core:sand, core:mud, core:grass, core:tree, core:rock, core:stone, core:snow, core:ice

  Scenario Outline: Noise values bucket into contiguous terrain ranges
    When the noise value <noise> is bucketed
    Then the terrain tile is "<tile>"

    Examples:
      | noise | tile       |
      | -1.00 | core:water |
      | -0.80 | core:water |
      | -0.65 | core:sand  |
      | -0.60 | core:mud   |
      | -0.50 | core:grass |
      | -0.30 | core:grass |
      |  0.30 | core:grass |
      |  0.35 | core:tree  |
      |  0.40 | core:rock  |
      |  0.50 | core:stone |
      |  0.80 | core:snow  |
      |  0.90 | core:ice   |
      |  1.00 | core:ice   |

  Scenario: Five buildings are placed on a roomy map
    Given a world generated from seed 12345
    Then the world contains 5 buildings

  Scenario: Buildings never overlap
    Given a world generated from seed 12345
    Then no two placed buildings share a tile

  Scenario: Buildings sit entirely on walkable terrain
    Given a world generated from seed 12345
    Then every tile under each building's footprint was walkable terrain before placement

  Scenario: Every door opens onto walkable ground
    Given a world generated from seed 12345
    Then the tile directly outside each building's door is walkable and not under any building

  Scenario: Buildings stay inside the map
    Given a world generated from seed 12345
    Then every building's footprint lies within the map bounds

  Scenario: Buildings are picked from every registered blueprint
    Given the registered blueprints are "core:small_house_01" and "test:hut"
    When worlds are generated from seeds 1 to 20
    Then both blueprints appear among the placed buildings

  Scenario: A map with too little walkable ground places fewer buildings
    Given terrain where every tile is core:water
    When the world is generated
    Then the world contains 0 buildings
    And generation completes without error

  Scenario: No registered blueprints still generates terrain
    Given no building blueprints are registered
    When a world is generated from seed 12345
    Then the world contains 0 buildings
    And every tile is one of the core terrain tiles

  Scenario: The player spawns at the centre when it is walkable
    Given a generated world whose centre tile is walkable and outside any building
    When the player spawns
    Then the player is at the map centre

  Scenario: The player spawns on the nearest walkable tile when the centre is blocked
    Given a generated world whose centre tile is core:water
    And the nearest walkable tile outside any building is 2 tiles from the centre
    When the player spawns
    Then the player is on that tile

  Scenario: The player never spawns inside a building
    Given a generated world whose centre tile is under a building
    When the player spawns
    Then the player is on a walkable tile outside every building footprint

  Scenario: A new game uses a fresh random seed
    Given no world-seed override is set
    When two new games are started
    Then each game logs "World generated from seed <n>" at INFO
    And the two logged seeds differ

  Scenario: The world-seed override fixes the map
    Given the world-seed override is "12345"
    When a new game is started
    Then the log reads "World generated from seed 12345"
    And the world matches a world generated from seed 12345

  Scenario: A malformed world-seed override falls back to a random seed
    Given the world-seed override is "abc"
    When a new game is started
    Then a warning naming "abc" is logged
    And the game starts with a world generated from a random seed

  # Non-goals:
  #   - Roads, districts, zoning; new building blueprints.
  #   - A settings UI for the seed; a GameEvent for world generation.
  #
  # Risks:
  #   - Any second source of randomness (the old code used an unseeded
  #     Random and a separate Random(1000)) breaks reproducibility. Noise,
  #     blueprint choice and placement must all derive from the one seed.
  #   - Mod-loading order must not leak into blueprint choice; blueprints
  #     are sorted by id before picking.
  #   - "The two logged seeds differ" is probabilistic with 64-bit seeds;
  #     the collision chance is negligible, but the step should inject the
  #     seed source rather than rely on wall-clock randomness.
  #   - Placement uses a bounded number of attempts per building, so a
  #     crowded map can never hang generation.
  #   - The 40-line / complexity-8 budgets will likely split generation
  #     into noise, bucketing, placement and spawn units.
  #
  # Open questions: none. See Clarifications in
  # specs/intent/seeded-world-generation.md.
