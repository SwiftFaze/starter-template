Feature: Controls hint bar
  A single persistent hint bar docked at the bottom of the game window,
  reflecting the currently-valid key bindings for wherever keyboard focus
  is right now. Every screen (Title, Settings, Keybinds, Inventory, Codex,
  in-game/movement view) pushes its own hint list into it via a shared
  push API, and hints update at row/sub-focus granularity as focus moves
  within a screen, not only on screen switches. Contextual world-action
  hints (e.g. "[e]-Open door") are out of scope, tracked in #133.

  Background:
    Given the game window is shown

  Scenario: The hint bar is never blank on a screen with real bindings
    When the title screen is shown
    Then the hint bar is showing at least one hint

  Scenario: The title screen's hints omit self-explanatory Up/Down navigation
    When the title screen is shown
    Then the hint bar shows exactly "[enter]-Select"

  Scenario: The settings screen's hints reflect an action row's bindings
    Given the settings screen is shown
    When "Go Back" is highlighted
    Then the hint bar shows exactly "[enter]-Select", "[escape]-Back"

  Scenario: Moving onto a slider row adds Decrease/Increase hints
    Given the settings screen is shown
    And "Go Back" is highlighted
    When the "Up" key is pressed until "Brightness" is highlighted
    Then the hint bar shows exactly "[left]-Decrease", "[right]-Increase", "[enter]-Select", "[escape]-Back"

  Scenario: Moving off a slider row removes its Decrease/Increase hints
    Given the settings screen is shown
    And "Volume" is highlighted
    When the "Down" key is pressed
    Then the hint bar shows exactly "[enter]-Select", "[escape]-Back"

  Scenario: A radio-cycle row shows Previous/Next hints, distinct from a slider's
    Given the settings screen is shown
    And "Fullscreen" is highlighted
    Then the hint bar shows exactly "[left]-Previous", "[right]-Next", "[enter]-Select", "[escape]-Back"

  Scenario: The keybinds page's hints omit self-explanatory table navigation
    When the keybinds page is shown
    Then the hint bar shows exactly "[enter]-Rebind", "[escape]-Back"

  Scenario: Moving into the keybinds page's footer changes the hint bar's Up/Down/Left/Right hints
    Given the keybinds page is shown
    And the last action row is highlighted
    When the "Down" key is pressed
    Then the hint bar shows exactly "[up]-Back to table", "[left]-Previous", "[right]-Next", "[enter]-Select", "[escape]-Back"

  Scenario: Arming a keybind capture shows only the Set binding hint, not Escape-Cancel
    Given the keybinds page is shown
    When the "Enter" key is pressed
    Then the hint bar shows exactly "[any key]-Set binding"

  Scenario: Opening Inventory replaces the hint bar's content with Inventory's hints
    Given the in-game view is shown
    When the "I" key is pressed
    Then the hint bar shows exactly "[right]-View details", "[d]-Drop", "[escape]-Close"

  Scenario: Switching from Inventory's item list to its details pane updates the hints
    Given the inventory popup is shown with an item selected
    When the "Right" key is pressed
    Then the hint bar shows exactly "[left]-Back to list", "[d]-Drop", "[escape]-Close"

  Scenario: Switching Codex's Left/Right pane updates the hints
    Given the codex popup is shown with an entry selected
    When the "Right" key is pressed
    Then the hint bar shows exactly "[left]-Back to list", "[tab]-Next category", "[shift+tab]-Prev category", "[escape]-Close"

  Scenario: Closing a popup restores the in-game view's hints
    Given the inventory popup is shown
    When the "Escape" key is pressed
    Then the hint bar shows exactly "[i]-Inventory", "[x]-Codex"

  # Non-goals:
  #   - Contextual world-action hints (e.g. "[e]-Open door") - depends on
  #     an interaction system, door/tile state, and facing/adjacency
  #     detection that don't exist yet. Tracked in #133.
  #   - Wiring the hint bar into the *live* composition root
  #     (Main.buildGameCard/buildUIScreens) for Inventory, Codex, and the
  #     in-game view - that composition root is mid-rebuild, so those three
  #     screens' scenarios above exercise the panel classes directly, the
  #     same way settings-screen.feature exercises SettingsScreenPanel
  #     directly rather than through Main. Title/Settings/Keybinds are not
  #     affected by that rebuild and can wire into Main normally.
  #   - Fixing SettingsKeybindsPanel's press-any-key capture to let Escape
  #     actually cancel instead of rebinding to Escape - a pre-existing
  #     behavior question, deliberately not touched here; the hint bar
  #     just describes that behavior accurately as-is.
  #   - Any visual styling of the bar itself (colors, font, layout within
  #     the bar) - covered by this project's Swing visual-verification
  #     step (docs/ui-verification.md), not Cucumber.
  #   - Mouse/pointer interaction - this game is keyboard-only by design.
  #
  # Risks:
  #   - Plain vertical list movement's "[up]-Navigate"/"[down]-Navigate"
  #     is deliberately absent from every scenario below - suppressed as
  #     self-explanatory per the Clarifications' second-round decision.
  #     It is NOT absent from the Keybinds footer scenario, where Up
  #     means "Back to table" (a distinct, non-obvious effect).
  #   - The in-game view's hint bar shows only "[i]-Inventory" and
  #     "[x]-Codex" - the four Z/S/Q/D movement hints were dropped in a
  #     third round of Clarifications as also self-explanatory, which
  #     supersedes the earlier "letters aren't arrow-based, so keep them"
  #     reasoning for the movement hints specifically (that reasoning
  #     still explains why Z/S/Q/D, not arrows, was the intended scheme
  #     in the first place - it just no longer justifies showing them).
  #   - "Moving off a slider row" deliberately uses Volume -> Keybinds,
  #     not Brightness -> Fullscreen: Fullscreen/Font/Theme are radio-cycle
  #     rows that also carry Left/Right hints (Previous/Next), so
  #     Brightness's immediate neighbor wouldn't actually demonstrate the
  #     hints disappearing. Volume -> Keybinds is a genuine slider ->
  #     action-row transition.
  #   - The keybinds page's footer-focus scenario asserts a static
  #     example (last action row -> Down enters the footer at its first
  #     entry, "Go back"); the wording assumes the same
  #     Navigate/Previous/Next/Select/Back vocabulary used elsewhere,
  #     which is a naming choice rather than something derivable from the
  #     code.
  #
  # Open questions:
  #   None remaining - all raised during spec drafting (including a
  #   second round after the first playtest) were resolved before merge.
