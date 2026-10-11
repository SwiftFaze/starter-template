Feature: Camera behavior
  The camera centers the viewport on a target world position. It holds no
  state beyond the last computed offset, and performs no clamping to map
  bounds — the offset it produces is used as-is by every renderer that
  translates world coordinates into screen coordinates. The viewport itself
  is resizable after construction, tracking the game panel's live pixel size
  so a resizable Windowed frame reveals more or less of the map around the
  player as it's resized; resizing below a small minimum clamps to that floor rather than shrinking
  the viewport to zero or negative tiles.

  Background:
    Given a camera with a viewport 10 tiles wide and 10 tiles tall

  Scenario: Centering on a target position offsets the viewport by half its size
    When the camera centers on position (20, 20)
    Then the camera's offset is (15, 15)

  Scenario Outline: Centering offset follows target minus half the viewport
    When the camera centers on position (<targetX>, <targetY>)
    Then the camera's offset is (<offsetX>, <offsetY>)

    Examples:
      | targetX | targetY | offsetX | offsetY |
      | 20      | 20      | 15      | 15      |
      | 25      | 13      | 20      | 8       |
      | 0       | 0       | -5      | -5      |

  Scenario: Re-centering replaces the previous offset with no smoothing
    Given the camera has centered on position (20, 20)
    When the camera centers on position (30, 30)
    Then the camera's offset is (25, 25)

  Scenario: Centering near a map edge is not clamped to the map bounds
    When the camera centers on position (2, 2)
    Then the camera's offset is (-3, -3)

  Scenario: Resizing the viewport changes subsequent centering offsets
    When the viewport is resized to 20 tiles wide and 16 tiles tall
    And the camera centers on position (100, 100)
    Then the camera's offset is (90, 92)

  Scenario: Resizing the viewport below the minimum clamps to a 5x5 floor
    When the viewport is resized to 2 tiles wide and 1 tiles tall
    And the camera centers on position (50, 50)
    Then the camera's offset is (48, 48)

  # Non-goals:
  #   - Edge-of-map clamping, zoom, panning, or floor/depth-aware behavior —
  #     out of scope; the scenario above documents the current unclamped
  #     behavior, it does not request clamping be added.
  #   - Pixel-level Swing rendering output — only the Camera domain object's
  #     offset math is covered, not Graphics2D calls. How GamePanel derives
  #     the resize call's width/height from its own live pixel size (and
  #     when it's invoked — e.g. a ComponentListener vs. reading
  #     getWidth()/getHeight() at paint time) is covered by
  #     fullscreen-windowed-toggle.feature's Non-goals, not here — this file
  #     only covers what the Camera object itself does once given new
  #     dimensions.
  #
  # Risks:
  #   - None identified for the original centering scenarios; Camera has no
  #     external dependencies and no branching logic beyond the two
  #     arithmetic assignments in centerOn.
  #   - The 5x5-tile minimum-viewport floor is this spec's own concrete
  #     choice for the "small sane minimum" agreed during Step 2 drafting.
  #
  # Open questions:
  #   - None. Camera has no validation/error paths (no exceptions, no bounds
  #     checks) — there is no meaningful failure/error scenario to add here;
  #     the unclamped-offset and below-minimum-clamped scenarios above are
  #     the closest analogues to edge cases this class has.
