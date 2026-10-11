Feature: Pattern-validated text field widget
  A keyboard-driven text/form-field widget built on the shared
  Widget/WidgetTheme framework from ui-component-framework.feature,
  validating its current input against a caller-supplied regex pattern
  and surfacing valid/invalid state via a new
  WidgetTheme.INVALID_HIGHLIGHT color. No real in-game screen consumes it
  yet — the first real consumer and its actual validation pattern are
  still unidentified.

  Scenario: Typing input that matches the pattern shows valid state
    Given a pattern field with pattern "^[A-Za-z]+$" and empty input
    And the pattern field has keyboard focus
    When the characters "Rob" are typed
    Then the pattern field's input is "Rob"
    And the pattern field is in the valid state

  Scenario: Typing input that fails the pattern shows invalid state
    Given a pattern field with pattern "^[A-Za-z]+$" and empty input
    And the pattern field has keyboard focus
    When the characters "Rob1" are typed
    Then the pattern field's input is "Rob1"
    And the pattern field is in the invalid state

  Scenario: An empty pattern field's validity reflects whether the pattern matches an empty string
    Given a pattern field with pattern "^[A-Za-z]+$" and empty input
    Then the pattern field is in the invalid state

  Scenario: Correcting invalid input back to a match returns the field to valid state
    Given a pattern field with pattern "^[A-Za-z]+$" and input "Rob1"
    And the pattern field is in the invalid state
    And the pattern field has keyboard focus
    When the last character is deleted
    Then the pattern field's input is "Rob"
    And the pattern field is in the valid state

  # Non-goals:
  #   - Wiring this widget into any real in-game screen, or deciding any
  #     real validation pattern (e.g. character name rules) — no
  #     consumer exists yet; this is still an open question.
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Real Swing focus-transfer and real keyboard character input are
  #     not simulated headlessly here, matching the existing precedent in
  #     ui-component-framework.feature — steps model the widget's
  #     internal input/validation state directly.
  #   - This widget has no real consumer to validate its shape against;
  #     the WidgetTheme.INVALID_HIGHLIGHT styling decision was made
  #     autonomously and should be revisited once a real consumer exists.
  #
  # Open questions:
  #   - What's the first real consumer of this widget, and what pattern
  #     will it actually validate against? Still unresolved — this is the
  #     one open question this feature file cannot close on its own.
