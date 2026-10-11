Feature: Radio group widget
  A single-select radio group widget built on the shared
  Widget/WidgetTheme framework from ui-component-framework.feature.
  Vertical (Up/Down) layout by default, matching every other widget's
  convention; an optional horizontal (Left/Right) variant is available
  for callers that need it, sharing the new MENU_LEFT/MENU_RIGHT
  keybindings with the table widget. Its former real consumer — the
  rebuilt inventory popup's "Drop item?" confirmation (a horizontal
  Yes/No instance) — was removed alongside EastPanel; see the trailing
  Risks note.

  Scenario: Navigating a vertical radio group down moves the highlighted option to the next one
    Given a radio group with options "Warrior", "Mage", "Rogue" and "Warrior" highlighted
    And the radio group has keyboard focus
    When the "Down" key is pressed
    Then the highlighted option is "Mage"

  Scenario: Moving up from the first option wraps to the last option
    Given a radio group with options "Warrior", "Mage", "Rogue" and "Warrior" highlighted
    And the radio group has keyboard focus
    When the "Up" key is pressed
    Then the highlighted option is "Rogue"

  Scenario: Confirming a radio group's highlighted option with Enter selects it
    Given a radio group with options "Warrior", "Mage", "Rogue" and "Mage" highlighted
    And the radio group has keyboard focus
    When the "Enter" key is pressed
    Then the selected option is "Mage"

  Scenario: Selecting a new option deselects the previous one
    Given a radio group with options "Warrior", "Mage", "Rogue" and "Warrior" selected
    And the radio group has keyboard focus
    When the "Down" key is pressed
    And the "Enter" key is pressed
    Then the selected option is "Mage"
    And "Warrior" is not selected

  Scenario: A horizontal radio group navigates with Left/Right instead of Up/Down
    Given a horizontal radio group with options "Windowed", "Fullscreen" and "Windowed" highlighted
    And the radio group has keyboard focus
    When the "Right" key is pressed
    Then the highlighted option is "Fullscreen"

  # Non-goals:
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Real Swing focus-transfer is not simulated headlessly here,
  #     matching the existing precedent in ui-component-framework.feature.
  #   - This orientation question was answered three times over the
  #     course of drafting (vertical-only, then horizontal-only after
  #     #54, then vertical-by-default-with-a-horizontal-option here).
  #
  # Removal note (later, unrelated cleanup):
  #   - This file used to also cover DropConfirmationPopup's "Drop item?"
  #     confirmation (a horizontal Yes/No radio group), driven through the
  #     rebuilt in-game inventory screen via EastPanel — six scenarios in
  #     total. That real consumer was removed when EastPanel/NorthPanel/
  #     SouthPanel/PlayerInfoPanel/TerminalPanel were deleted as unrelated
  #     early-scaffolding cleanup.
  #     DropConfirmationPopup itself still exists and is unaffected as a
  #     class, but has nothing wiring it into the live game right now, so
  #     there's nothing left to acceptance-test through it.
  #
  # Open questions:
  #   - None outstanding for this widget.
