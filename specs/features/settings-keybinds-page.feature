Feature: Settings keybinds page
  A dedicated page (opened from the settings screen's Keybinds item)
  listing every rebindable action and its current key, with a "press any
  key" popup to change a binding's display, and a footer with Go back,
  Reset to Defaults, Cancel, and Apply (left to right, added after Step
  4.5 playtest feedback). This file covers only the page's static shape
  (listing actions, opening/capturing the press-any-key popup) — real,
  distinct footer-action semantics (Apply persists, Cancel/Go back discard
  pending edits with a confirmation gate, Reset to Defaults gets its own
  confirmation) live in settings-persistence.feature, which supersedes
  this file's former "Reset to Defaults is the only real action, Apply/
  Cancel/Go back behave identically" framing. Still no real rebinding into
  input dispatch (Keybindings.java's KeyStroke constants are unrelated
  and untouched).

  Background:
    Given the keybinds page is shown

  Scenario: The keybinds page lists every rebindable action with its current key
    Then the keybinds page lists "Move up" bound to "Up"
    And the keybinds page lists "Move down" bound to "Down"
    And the keybinds page lists "Move left" bound to "Left"
    And the keybinds page lists "Move right" bound to "Right"
    And the keybinds page lists "Toggle inventory" bound to "I"

  Scenario: Confirming an action opens a press-any-key popup
    Given "Move up" is highlighted
    When the "Enter" key is pressed
    Then the press-any-key popup is shown

  Scenario: Pressing a key while the popup is open updates that action's displayed keybind
    Given "Move up" is highlighted
    And the press-any-key popup is shown
    When the "W" key is pressed
    Then the press-any-key popup is closed
    And the keybinds page lists "Move up" bound to "W"

  Scenario: Confirming Reset to Defaults resets all keybinds without leaving the page
    Given "Move up" is highlighted
    And the press-any-key popup is shown
    When the "W" key is pressed
    And "Reset to Defaults" is highlighted in the footer
    And the "Enter" key is pressed
    And the reset confirmation popup is shown
    And "Yes" is highlighted in the reset confirmation popup
    And the "Enter" key is pressed
    Then the reset confirmation popup is closed
    And the keybinds page lists "Move up" bound to "Up"

  # Non-goals:
  #   - Real, distinct Apply/Cancel/Go back/Reset to Defaults semantics —
  #     out of scope for this file now that settings-persistence.feature
  #     covers them; this file only proves the page's static shape (the
  #     action list, the press-any-key popup) still works.
  #   - Actually changing Keybindings.java's real KeyStroke constants, or
  #     any other rebinding side effect — display-only.
  #   - Validating for duplicate/conflicting key assignments — no real
  #     rebinding exists yet to conflict with.
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Real Swing focus-transfer and real keyboard capture (the
  #     press-any-key popup accepting arbitrary keys, not just
  #     MENU_CONFIRM/MENU_CANCEL) are not simulated headlessly here,
  #     matching this repo's existing Cucumber precedent.
  #
  # Open questions:
  #   - None outstanding for this page specifically.
