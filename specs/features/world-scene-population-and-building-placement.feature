Feature: World scene population and building placement
  A scene is composed from a base tile fill plus zero or more building
  blueprints stamped into it at a world offset.

  Background:
    Given an empty world scene 10 tiles wide and 10 tiles tall

  Scenario: Filling a scene sets every tile to the fill type
    When the scene is filled with grass
    Then every tile in the scene is GRASS

  Scenario: Placing a building stamps its blueprint into the scene at its world offset
    Given the scene is filled with grass
    And a building with the following blueprint:
      | WALL  | WOOD |
      | STONE | DOOR |
    And the building's world position is set to (3, 4)
    When the building is placed in the scene
    Then tile (3, 4) is WALL
    And tile (4, 4) is WOOD
    And tile (3, 5) is STONE
    And tile (4, 5) is DOOR

  Scenario: Placing a building overwrites tiles already in the scene
    Given the scene is filled with grass
    And tile (6, 6) is water
    And a building with the following blueprint:
      | WALL |
    And the building's world position is set to (6, 6)
    When the building is placed in the scene
    Then tile (6, 6) is WALL

  Scenario: Placing a building with an empty blueprint leaves the scene unchanged
    Given the scene is filled with grass
    And a building with an empty blueprint
    And the building's world position is set to (2, 2)
    When the building is placed in the scene
    Then tile (2, 2) is GRASS

  # Non-goals:
  #   - BuildingLoader JSON parsing and blueprint width/height correctness
  #     (already covered by world-single-floor-rendering.feature).
  #   - Basic tile walkability (already covered by the same existing feature).
  #   - Player movement, world generation, tile physics, camera behavior, and UI.
  #   - Out-of-bounds building placement: placeBuilding currently does no
  #     bounds-checking (unlike fillRegion), so a footprint extending past
  #     scene bounds throws ArrayIndexOutOfBoundsException. Not asserted here
  #     per the intent doc's explicit scope exclusion.
  #   - TileTestScene2 is deliberately not used as a fixture, so it stays
  #     free to keep changing without breaking this spec. All fixtures here
  #     (scene, building, blueprint) are built directly by the scenario steps.
  #
  # Risks:
  #   - None identified — no shared JSON fixture dependency, unlike
  #     world-single-floor-rendering.feature's small_house_01.json/
  #     BuildingLoaderIT coupling.
  #
  # Open questions:
  #   - None — the absence of a failure/error scenario (out-of-bounds
  #     placement is out of scope per the intent doc) was confirmed
  #     acceptable; see intent.md's Clarifications section.
