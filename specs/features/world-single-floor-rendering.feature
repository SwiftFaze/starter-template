Feature: Single-floor world rendering
  The world is a flat, single-layer map. The Z-axis/floor system and
  fog-of-war have been removed: the only active scene ever used one
  Z-layer, so the multi-floor machinery was dead weight.

  Background:
    Given a world scene 10 tiles wide and 10 tiles tall

  Scenario: The world scene has no floor/depth dimension
    Then looking up a tile takes only an (x, y) position, not a floor

  Scenario: A player moves onto a walkable tile
    Given a player at position (5, 5)
    And tile (5, 4) is walkable
    When the player moves up
    Then the player's position is (5, 4)

  Scenario: A player is blocked by a non-walkable tile
    Given a player at position (5, 5)
    And tile (6, 5) is not walkable
    When the player moves right
    Then the player's position is still (5, 5)

  Scenario: Loading a building blueprint from JSON
    When building "small_house_01.json" is loaded
    Then the building has a single 2D tile layer
    And the building's width and height match the JSON's "width" and "height" fields

  # Non-goals:
  #   - Re-adding any multi-floor, ascend/descend, or fog/brightness behavior.
  #   - Visual rendering/pixel output (Swing painting isn't covered by
  #     Cucumber here; only the WorldScene/Player/Building domain model is).
  #
  # Risks:
  #   - small_house_01.json and BuildingLoaderIT must be updated together
  #     (schema changes from {"layers": [{"tiles": [...]}]} to a flat
  #     {"tiles": [...]}) or the integration test breaks.
  #
  # Open questions:
  #   - None — schema shape is fully specified in the intent doc.
