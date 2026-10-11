Feature: Smaller confirmation-style popup variant
  A fixed-size, centered PopupWidget presentation (PopupWidget.isFullScreen()
  false, as opposed to InventoryPanel's existing full-screen popups) with a
  bordered, arrow-accented title bar, hosted by a new JLayeredPane wired
  specifically for the settings card (GameWindow.buildContentArea's own
  JLayeredPane is scoped to the game card and isn't reusable as-is). Proven
  by a concrete Yes/No confirmation dialog wired to the settings screen's
  "Reset to Defaults" item. Reuses the existing PopupWidget base's core
  mechanism (Escape-to-dismiss, onUp/onDown/onLeft/onRight) and #35's radio
  group widget for the Yes/No choice — no new dialog control is built — but
  does not use PopupWidget's inherited Close-button footer: Yes/No is this
  dialog's only confirm/cancel mechanism.

  Scenario: Confirming Reset to Defaults opens the Yes/No confirmation popup
    Given the settings screen is shown
    And "Reset to Defaults" is highlighted
    When the "Enter" key is pressed
    Then the confirmation popup is shown
    And the confirmation popup is not full-screen
    And the confirmation popup's title is "Confirm Reset"
    And the confirmation popup asks "Reset all settings to their defaults?"
    And "No" is highlighted in the confirmation popup

  Scenario: Choosing No on the confirmation popup dismisses it without resetting
    Given the confirmation popup is shown
    And "No" is highlighted in the confirmation popup
    When the "Enter" key is pressed
    Then the confirmation popup is closed
    And the settings screen is shown

  Scenario: Choosing Yes on the confirmation popup dismisses it
    Given the confirmation popup is shown
    And "Yes" is highlighted in the confirmation popup
    When the "Enter" key is pressed
    Then the confirmation popup is closed
    And the settings screen is shown

  Scenario: Left/Right moves the highlighted choice between Yes and No
    Given the confirmation popup is shown
    And "No" is highlighted in the confirmation popup
    When the "Left" key is pressed
    Then "Yes" is highlighted in the confirmation popup

  Scenario: Escape dismisses the confirmation popup without resetting
    Given the confirmation popup is shown
    When the "Escape" key is pressed
    Then the confirmation popup is closed
    And the settings screen is shown

  # Non-goals:
  #   - Reset to Defaults's "Yes" choice actually resetting anything —
  #     no setting persists real state yet, matching #54's own
  #     out-of-scope framing for that item.
  #   - A bigger real-world trigger for Yes/No confirmation (e.g. NPC
  #     dialogue) — explicitly out of scope for issue #99 itself.
  #   - Building the future Close-only "alert" popup (Clarifications
  #     Q4) — only its underlying mechanism must not be precluded.
  #   - A general-purpose popup host usable by every card/screen — only
  #     the settings card gets one (Clarifications Q1).
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #   - No error/failure path exists for this feature: it's UI-only
  #     (no I/O, no persistence, no validation), matching this repo's
  #     other display/interaction-only specs.
  #
  # Risks:
  #   - Post-approval mechanical fix: Yes/No highlight steps were
  #     reworded from bare "{string} is highlighted" to "{string} is
  #     highlighted in the confirmation popup" to avoid colliding with
  #     the existing generic settings-row dispatcher
  #     (UiComponentFrameworkSteps.itemIsHighlighted), which would
  #     otherwise try to find a settings row literally named "No"/"Yes".
  #     Same disambiguation DropConfirmationPopup's own steps already
  #     use. Wording-only, no behavior/scope change.
  #   - The choice of "Reset to Defaults" as the concrete trigger (over
  #     a dev-only sandbox demo) was an autonomous decision — flagged for
  #     confirmation at Step 3 approval.
  #   - The exact title/question wording ("Confirm Reset" / "Reset all
  #     settings to their defaults?") is a minor copy decision, not yet
  #     reviewed by the human — adjustable at Step 3 approval without
  #     changing the feature's shape.
  #   - Real Swing focus-transfer is not simulated headlessly here,
  #     matching this repo's existing Cucumber precedent
  #     (ui-component-framework.feature).
  #
  # Open questions:
  #   - None outstanding — all Step 2 clarification-round questions were
  #     resolved.
