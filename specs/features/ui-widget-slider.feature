Feature: Slider widget
  A bounded-value slider widget built on the shared Widget/WidgetTheme
  framework from ui-component-framework.feature. Left/Right adjusts the
  current value by a fixed step within a caller-supplied [min, max]
  range. New scope surfaced by issue #54 (settings screen's brightness/
  volume items).
  Unlike the list/table/radio-group widgets, a slider does not wrap
  around at its ends: min and max are hard bounds, not cyclic.

  Scenario: Moving right increases the value by one step
    Given a slider widget ranging 0 to 10 with step 1 and value 5
    And the slider widget has keyboard focus
    When the "Right" key is pressed
    Then the slider's value is 6

  Scenario: Moving left decreases the value by one step
    Given a slider widget ranging 0 to 10 with step 1 and value 5
    And the slider widget has keyboard focus
    When the "Left" key is pressed
    Then the slider's value is 4

  Scenario: Moving right at the maximum does not exceed the maximum
    Given a slider widget ranging 0 to 10 with step 1 and value 10
    And the slider widget has keyboard focus
    When the "Right" key is pressed
    Then the slider's value is 10

  Scenario: Moving left at the minimum does not go below the minimum
    Given a slider widget ranging 0 to 10 with step 1 and value 0
    And the slider widget has keyboard focus
    When the "Left" key is pressed
    Then the slider's value is 0

  # Non-goals:
  #   - Wiring this widget into the settings screen — that's
  #     settings-screen.feature's job, not this one's.
  #   - Mouse dragging — this game is keyboard-only by design.
  #   - Persisting the adjusted value, or applying it to real
  #     brightness/volume/rendering/audio systems — no such systems exist
  #     yet.
  #
  # Risks:
  #   - This widget has no precedent elsewhere in the framework (list/
  #     table/radio-group all wrap; this deliberately doesn't) — the
  #     no-wrap behavior was confirmed at Step 3 approval (2026-08-30).
  #
  # Open questions:
  #   - None for this widget specifically.
