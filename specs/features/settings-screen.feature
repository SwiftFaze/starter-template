Feature: Settings screen
  A navigable, back-able settings screen reached from the title screen's
  Settings item, listing eleven items with type-specific Left/Right
  interaction: sliders (brightness, volume — see ui-widget-slider.feature),
  a Fullscreen/Windowed radio toggle, a font cycle, and a theme cycle (all
  three reusing #35's radio group widget), a dedicated Keybinds sub-page
  (settings-keybinds-page.feature), folder-opening actions, a placeholder
  about/reset entry, and an explicit Go Back item (added after Step 4.5
  playtest found Escape-only back navigation wasn't discoverable). Visual/
  input shape only — no setting has a real backing system yet, including
  Theme.

  Background:
    Given the settings screen is shown

  Scenario: The settings screen lists all eleven settings items
    Then the settings items are "Brightness", "Fullscreen", "Font", "Theme", "Volume", "Keybinds", "Open Game Folder", "Open Mod Folder", "About", "Reset to Defaults", "Go Back"

  Scenario: Down moves the highlighted item to the next one
    Given "Brightness" is highlighted
    When the "Down" key is pressed
    Then "Fullscreen" is highlighted

  Scenario: Adjusting brightness right increases its slider value
    Given "Brightness" is highlighted with slider value 5
    When the "Right" key is pressed
    Then "Brightness"'s slider value is 6

  Scenario: Adjusting volume left decreases its slider value
    Given "Volume" is highlighted with slider value 5
    When the "Left" key is pressed
    Then "Volume"'s slider value is 4

  Scenario: Toggling fullscreen right switches from Windowed to Fullscreen
    Given "Fullscreen" is highlighted with value "Windowed"
    When the "Right" key is pressed
    Then "Fullscreen"'s value is "Fullscreen"

  Scenario: Cycling font right moves to the next font choice
    Given "Font" is highlighted with value "Monospaced"
    When the "Right" key is pressed
    Then "Font"'s value is "Serif"

  Scenario: Cycling theme right moves to the next theme choice
    Given "Theme" is highlighted with value "Default"
    When the "Right" key is pressed
    Then "Theme"'s value is "Midnight"

  Scenario: Confirming Keybinds opens the keybinds page
    Given "Keybinds" is highlighted
    When the "Enter" key is pressed
    Then the keybinds page is shown

  Scenario: Confirming Open Game Folder opens the install directory
    Given "Open Game Folder" is highlighted
    When the "Enter" key is pressed
    Then the install directory was opened

  Scenario: Confirming Open Mod Folder opens the mods directory, creating it if missing
    Given "Open Mod Folder" is highlighted
    And no "mods" directory exists next to the install
    When the "Enter" key is pressed
    Then a "mods" directory was created next to the install
    And the mods directory was opened

  Scenario: Escape returns to the title screen's menu
    When the "Escape" key is pressed
    Then the title screen is shown

  Scenario: Confirming Go Back returns to the title screen's menu
    Given "Go Back" is highlighted
    When the "Enter" key is pressed
    Then the title screen is shown

  # Non-goals:
  #   - Any real mechanism behind brightness, fullscreen, font, theme,
  #     volume, or keybind rebinding — no rendering/audio/config-
  #     persistence system exists yet; this screen is visual/input shape
  #     only. Cycling Theme does not change WidgetTheme's actual colors —
  #     see specs/features/widget-theming.feature.
  #   - Reset to Defaults actually resetting anything — no setting
  #     persists real state yet.
  #   - About/version info's actual content — a static placeholder
  #     label, nothing to prove behaviorally.
  #   - Confirmation dialogs before opening folders — confirm opens
  #     immediately.
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Depends on ui-widget-slider.feature's slider widget landing for
  #     the brightness/volume scenarios above — the radio group widget
  #     (issue #35) needed for the Fullscreen/Windowed toggle and font
  #     cycle is already implemented on develop.
  #   - The font cycle's "Monospaced" -> "Serif" -> "SansSerif" list was
  #     confirmed at Step 3 approval (2026-08-30).
  #   - The Theme row is a fixed placeholder list ("Default", "Midnight",
  #     "Sunrise"), built the same way as the Font row — not sourced from
  #     the real mod-driven theme registry in
  #     specs/features/widget-theming.feature. That registry only has one
  #     entry ("core:default") until a second theme mod actually ships, so
  #     wiring the two together now would make this row's option list
  #     depend on which mods happen to be installed; deferred to the real
  #     activation work (out of scope for now).
  #   - `Desktop.open` behavior (and mods-directory creation) is not
  #     simulated headlessly here — "the install directory was opened" /
  #     "a mods directory was created" model the action being invoked,
  #     not real filesystem/OS-shell side effects, matching this repo's
  #     existing precedent of not exercising real OS integration in
  #     Cucumber scenarios.
  #
  # Open questions:
  #   - None outstanding for this screen specifically — the font-asset
  #     question (startup-welcome-screen.feature) doesn't block this file.
