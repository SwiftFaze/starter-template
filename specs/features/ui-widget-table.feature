Feature: Table widget
  A keyboard-navigable, full-width table widget (rows/columns of
  terminal-style cells with visible cell/table borders, and an optional
  header row) built on the shared Widget/WidgetTheme framework from
  ui-component-framework.feature. Also supports a non-selectable/
  non-highlighted mode, used for whichever table isn't currently the
  active navigation target. Proven in isolation below. Its former real
  consumers — the rebuilt inventory popup's field/value and effects
  tables — were removed alongside EastPanel; see the trailing Risks note.

  QA: none - comment-only rename (TerminalScrollBarUi, #219); no keyboard behavior changed

  Scenario: Navigating a table widget down moves the selection to the next row
    Given a table widget with rows "Sword", "Shield", "Potion" and row 1 selected
    And the table widget has keyboard focus
    When the "Down" key is pressed
    Then the selected row is 2

  Scenario: Navigating a table widget right moves the selection to the next column
    Given a table widget with columns "Name", "Type", "Value" and column 1 selected
    And the table widget has keyboard focus
    When the "Right" key is pressed
    Then the selected column is 2

  Scenario: Moving up from the first row wraps to the last row
    Given a table widget with rows "Sword", "Shield", "Potion" and row 1 selected
    And the table widget has keyboard focus
    When the "Up" key is pressed
    Then the selected row is 3

  Scenario: Moving right from the last column wraps to the first column
    Given a table widget with columns "Name", "Type", "Value" and column 3 selected
    And the table widget has keyboard focus
    When the "Right" key is pressed
    Then the selected column is 1

  Scenario: A table widget can be configured to stop instead of wrap
    Given a table widget with rows "Sword", "Shield", "Potion" and row 1 selected
    And the table widget's wrap-around is disabled
    And the table widget has keyboard focus
    When the "Up" key is pressed
    Then the selected row is 1

  Scenario: Confirming a table widget's selection with Enter confirms the whole row
    Given a table widget with rows "Sword", "Shield", "Potion" and row 2 selected
    And the table widget has keyboard focus
    When the "Enter" key is pressed
    Then the confirmed row is "Shield"

  # Non-goals:
  #   - Cell-level confirm (as opposed to row-level) — decided against
  #     during spec drafting.
  #   - Scrolling behavior specifics — TerminalScrollBarUi is reused
  #     as-is from the existing framework, nothing new to prove there.
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Real Swing focus-transfer is not simulated headlessly here,
  #     matching the existing precedent in ui-component-framework.feature
  #     — "keyboard focus" Given/Then steps model internal state directly.
  #   - TableWidget only exposes the isAtFirstRow()/isAtLastRow()/
  #     moveToStart()/moveToEnd() primitives a consumer needs to build
  #     multi-table navigation; it never knew about any other table
  #     itself — that fall-through boundary logic always lived in the
  #     consumer, not here.
  #
  # Removal note (later, unrelated cleanup):
  #   - This file used to also cover two real consumers in the rebuilt
  #     inventory popup's details pane (a field/value table and an
  #     effects table, forming one continuous Left/Right/Up/Down-navigable
  #     region with the item list) — nine scenarios in total. Those
  #     consumers were removed when EastPanel/NorthPanel/SouthPanel/
  #     PlayerInfoPanel/TerminalPanel were deleted as unrelated
  #     early-scaffolding cleanup.
  #     TableWidget itself is untouched and still fully proven in
  #     isolation above.
  #
  # Open questions:
  #   - None outstanding for this widget.
