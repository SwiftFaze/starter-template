Feature: Settings persistence
  Loads settings.json (Brightness, Fullscreen, Font, Theme, Volume, and
  Keybinds) once at startup as the initial state of the Settings and
  Keybinds screens, falling back to today's hardcoded defaults for
  anything missing or unparsable. The main Settings screen writes on
  every change, including a newly-wired Reset to Defaults. The Keybinds
  page keeps its pending-edit table but gives Apply/Cancel/Go back/Escape
  real, distinct meaning for the first time: Apply is the only action
  that writes, Cancel/Go back/Escape discard pending edits back to the
  last-applied state (confirming first if there's something to lose), and
  its own Reset to Defaults only stages the defaults as a pending edit,
  still requiring Apply to persist.

  Scenario: A fresh install with no settings file uses today's hardcoded defaults
    Given no settings file exists next to the install
    When the settings screen is shown
    Then "Brightness" is highlighted with slider value 5
    And "Fullscreen"'s value is "Windowed"
    And "Font"'s value is "Monospaced"
    And "Theme"'s value is "Default"
    And "Volume"'s slider value is 5

  Scenario: Values saved in the settings file become the settings screen's initial state
    Given the settings file has "Brightness" set to 8
    And the settings file has "Fullscreen" set to "Fullscreen"
    And the settings file has "Font" set to "Serif"
    And the settings file has "Theme" set to "Midnight"
    And the settings file has "Volume" set to 2
    When the settings screen is shown
    Then "Brightness" is highlighted with slider value 8
    And "Fullscreen"'s value is "Fullscreen"
    And "Font"'s value is "Serif"
    And "Theme"'s value is "Midnight"
    And "Volume"'s slider value is 2

  Scenario: A settings file missing some values falls back to defaults for just those values
    Given the settings file has "Volume" set to 8
    When the settings screen is shown
    Then "Brightness" is highlighted with slider value 5
    And "Volume"'s slider value is 8

  Scenario: A corrupt settings file falls back to every default without crashing
    Given the settings file next to the install is corrupt
    When the settings screen is shown
    Then "Brightness" is highlighted with slider value 5
    And "Fullscreen"'s value is "Windowed"
    And "Volume"'s slider value is 5

  Scenario: Keybinds saved in the settings file become the keybinds page's initial state
    Given the settings file has "Move up" bound to "W"
    When the keybinds page is shown
    Then the keybinds page lists "Move up" bound to "W"

  Scenario: A settings file missing a keybind falls back to that action's default
    Given the settings file has "Toggle inventory" bound to "O"
    When the keybinds page is shown
    Then the keybinds page lists "Move up" bound to "Up"
    And the keybinds page lists "Toggle inventory" bound to "O"

  Scenario: Adjusting a slider writes the change to the settings file immediately
    Given the settings screen is shown
    And "Volume" is highlighted with slider value 5
    When the "Left" key is pressed
    Then "Volume"'s slider value is 4
    And the settings file now has "Volume" set to 4

  Scenario: Toggling a radio row writes the change to the settings file immediately
    Given the settings screen is shown
    And "Fullscreen" is highlighted with value "Windowed"
    When the "Right" key is pressed
    Then "Fullscreen"'s value is "Fullscreen"
    And the settings file now has "Fullscreen" set to "Fullscreen"

  Scenario: Confirming Yes on the settings screen's Reset to Defaults resets every row and persists it
    Given the settings file has "Volume" set to 8
    And the settings file has "Fullscreen" set to "Fullscreen"
    And the settings screen is shown
    And the confirmation popup is shown
    And "Yes" is highlighted in the confirmation popup
    When the "Enter" key is pressed
    Then the confirmation popup is closed
    And the settings screen is shown
    And "Volume"'s slider value is 5
    And "Fullscreen"'s value is "Windowed"
    And the settings file now has "Volume" set to 5
    And the settings file now has "Fullscreen" set to "Windowed"

  Scenario: Choosing No on the settings screen's Reset to Defaults changes nothing
    Given the settings file has "Volume" set to 8
    And the settings screen is shown
    And the confirmation popup is shown
    And "No" is highlighted in the confirmation popup
    When the "Enter" key is pressed
    Then "Volume"'s slider value is 8
    And the settings file now has "Volume" set to 8

  Scenario: Confirming Apply writes the pending rebind to the settings file
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Apply" is highlighted in the footer
    When the "Enter" key is pressed
    Then the settings screen is shown
    And the settings file now has "Move up" bound to "W"

  Scenario: Cancel with no pending changes reverts immediately without a confirmation popup
    Given the keybinds page is shown
    And "Cancel" is highlighted in the footer
    When the "Enter" key is pressed
    Then the discard confirmation popup is not shown
    And the keybinds page is shown

  Scenario: Cancel with pending changes asks to discard first
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Cancel" is highlighted in the footer
    When the "Enter" key is pressed
    Then the discard confirmation popup is shown

  Scenario: Confirming Yes on Cancel's discard popup reverts the pending rebind and stays on the page
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Cancel" is highlighted in the footer
    And the "Enter" key is pressed
    And the discard confirmation popup is shown
    And "Yes" is highlighted in the discard confirmation popup
    When the "Enter" key is pressed
    Then the discard confirmation popup is closed
    And the keybinds page is shown
    And the keybinds page lists "Move up" bound to "Up"

  Scenario: Choosing No on Cancel's discard popup keeps the pending rebind and stays on the page
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Cancel" is highlighted in the footer
    And the "Enter" key is pressed
    And the discard confirmation popup is shown
    And "No" is highlighted in the discard confirmation popup
    When the "Enter" key is pressed
    Then the discard confirmation popup is closed
    And the keybinds page is shown
    And the keybinds page lists "Move up" bound to "W"

  Scenario: Go back with no pending changes leaves immediately without a confirmation popup
    Given the keybinds page is shown
    And "Go back" is highlighted in the footer
    When the "Enter" key is pressed
    Then the discard confirmation popup is not shown
    And the settings screen is shown

  Scenario: Confirming Yes on Go back's discard popup discards the pending rebind and leaves the page
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Go back" is highlighted in the footer
    And the "Enter" key is pressed
    And the discard confirmation popup is shown
    And "Yes" is highlighted in the discard confirmation popup
    When the "Enter" key is pressed
    Then the discard confirmation popup is closed
    And the settings screen is shown

  Scenario: Choosing No on Go back's discard popup keeps editing on the page
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Go back" is highlighted in the footer
    And the "Enter" key is pressed
    And the discard confirmation popup is shown
    And "No" is highlighted in the discard confirmation popup
    When the "Enter" key is pressed
    Then the discard confirmation popup is closed
    And the keybinds page is shown
    And the keybinds page lists "Move up" bound to "W"

  Scenario: Escape with pending changes asks to discard, and confirming discards and leaves the page
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And the "Escape" key is pressed
    And the discard confirmation popup is shown
    And "Yes" is highlighted in the discard confirmation popup
    When the "Enter" key is pressed
    Then the discard confirmation popup is closed
    And the settings screen is shown

  Scenario: Confirming Reset to Defaults on the keybinds page asks first
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Reset to Defaults" is highlighted in the footer
    When the "Enter" key is pressed
    Then the reset confirmation popup is shown

  Scenario: Applying after Reset to Defaults persists the restored default
    Given the settings file has "Move up" bound to "W"
    And the keybinds page is shown
    And "Reset to Defaults" is highlighted in the footer
    And the "Enter" key is pressed
    And the reset confirmation popup is shown
    And "Yes" is highlighted in the reset confirmation popup
    And the "Enter" key is pressed
    And the reset confirmation popup is closed
    And the keybinds page lists "Move up" bound to "Up"
    And "Apply" is highlighted in the footer
    When the "Enter" key is pressed
    Then the settings screen is shown
    And the settings file now has "Move up" bound to "Up"

  Scenario: Choosing No on Reset to Defaults leaves the pending rebind untouched
    Given the keybinds page is shown
    And "Move up" is highlighted
    And the press-any-key popup is shown
    And the "W" key is pressed
    And "Reset to Defaults" is highlighted in the footer
    And the "Enter" key is pressed
    And the reset confirmation popup is shown
    And "No" is highlighted in the reset confirmation popup
    When the "Enter" key is pressed
    Then the reset confirmation popup is closed
    And the keybinds page lists "Move up" bound to "W"

  # Non-goals:
  #   - Any real mechanism behind brightness, fullscreen, font, theme, or
  #     volume beyond persisting the chosen value — none of them affect
  #     real rendering/audio/window state today (settings-screen.feature's
  #     own non-goals), except whatever the separate Fullscreen/Windowed
  #     live-toggle work wires up on its own branch. This feature persists
  #     whatever value the relevant widget currently reads/writes; it adds
  #     no new live effects itself.
  #   - Making Keybinds rebinding retarget real input dispatch.
  #     SettingsKeybindsPanel.keyBindings is a display-only label map,
  #     unconnected to the real Keybindings KeyStroke constants every
  #     other panel binds directly. This feature persists that display
  #     map faithfully; it does not wire it into real key dispatch.
  #   - Fixing KeybindsKeyListener's pre-existing capture-popup bug
  #     (Escape, while the press-any-key popup is open, rebinds the
  #     action to "Escape" instead of canceling capture) — already
  #     flagged and deliberately deferred in controls-hint-bar.md's
  #     Clarifications as its own follow-up issue.
  #   - Any game-save/progress-tracking system (player position,
  #     inventory, quest state) — different lifecycle, its own future
  #     issue per the source issue (#135).
  #   - Any mouse/pointer handling — this game is keyboard-only by design.
  #
  # Risks:
  #   - Exact settings.json key/shape (e.g. whether Keybinds nest under a
  #     "keybinds" object keyed by action name) is left for Step 4.
  #   - Whether "on change" for main-screen sliders/radios means "write on
  #     every Left/Right keypress" or "write once when focus leaves that
  #     row" is left for Step 4 to settle; the former (every keypress) is
  #     this spec's assumption, matching the screen having no separate
  #     commit step today.
  #   - Exact wording of the two new Yes/No popups on the keybinds page
  #     (discard confirmation, reset confirmation), and whether they reuse
  #     ResetConfirmationPopup's widget/host mechanism as-is or need their
  #     own, is a Step 4 detail — same status confirmation-popup-variant.md
  #     left its own popup's copy in.
  #   - The main Settings screen's Reset to Defaults and the keybinds
  #     page's new Reset to Defaults both reuse the "Yes/No confirmation"
  #     pattern from confirmation-popup-variant.feature, but this spec
  #     names the keybinds page's two popups separately ("discard
  #     confirmation popup", "reset confirmation popup") to disambiguate
  #     them from each other and from the main screen's "confirmation
  #     popup" — a step-definition/naming detail, not a behavior
  #     difference.
  #   - Post-Step-4 mechanical fix (Step 5): "the settings file has X set
  #     to Y" / "...bound to Y" is a Given (writes the file as setup); its
  #     Then-position uses ("Volume"'s slider value changed and so did the
  #     persisted file) were reworded to "the settings file now has X set
  #     to Y" / "...now has X bound to Y" — same wording split rationale
  #     as the Yes/No highlight steps in confirmation-popup-variant.feature
  #     (Cucumber matches step text regardless of Given/When/Then, so one
  #     literal text can only ever back one method; the write and the
  #     read-and-assert behaviors are genuinely different operations, not
  #     just a different label on the same one). Wording-only, no
  #     behavior/scope change — this repo's real filesystem I/O (temp
  #     install directories, real settings.json read/write) is actually
  #     exercised in these steps, unlike settings-screen.feature's
  #     folder-opening scenarios, which only model the action being
  #     invoked.
  #
  # Open questions:
  #   - None outstanding — all Step 2 clarification-round questions were
  #     resolved.
