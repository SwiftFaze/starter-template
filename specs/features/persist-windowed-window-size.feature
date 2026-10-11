Feature: Persist windowed game window size in settings
  The game remembers the Windowed-mode window's width and height across
  restarts. A new "WindowWidth"/"WindowHeight" pair on SettingsConfig
  persists whatever size the window last had in Windowed mode when the
  app quit; the next launch (and any Fullscreen -> Windowed switch
  mid-session) restores that size, clamped to the current screen's
  bounds, instead of the default pack() size.

  Scenario: A fresh install with no saved window size defaults to letting the window size itself
    Given no settings file exists next to the install
    When the settings file is loaded
    Then the loaded "WindowWidth" is 0
    And the loaded "WindowHeight" is 0

  Scenario: A saved window size survives being read back after relaunch
    Given the settings file has "WindowWidth" set to 1024
    And the settings file has "WindowHeight" set to 768
    When the settings file is loaded
    Then the loaded "WindowWidth" is 1024
    And the loaded "WindowHeight" is 768

  Scenario: A settings file missing the saved window size falls back to the default-size sentinel
    Given the settings file has "Volume" set to 8
    When the settings file is loaded
    Then the loaded "WindowWidth" is 0
    And the loaded "WindowHeight" is 0

  Scenario: A corrupt settings file falls back to the default-size sentinel without crashing
    Given the settings file next to the install is corrupt
    When the settings file is loaded
    Then the loaded "WindowWidth" is 0
    And the loaded "WindowHeight" is 0

  # Non-goals:
  #   - Actually capturing the live window's pixel size when the app exits
  #     (via the new shutdown hook), actually applying a restored size to a
  #     real JFrame at launch or on a Fullscreen -> Windowed switch, and
  #     actually clamping a restored size to a real GraphicsDevice's
  #     screen bounds — none of this is exercised by Cucumber, same
  #     reasoning fullscreen-windowed-toggle.feature already documented
  #     ("a live JFrame isn't constructed in headless tests",
  #     UiComponentFrameworkSteps.theGameWindowIsShown()). Unlike that
  #     feature, there is no existing test-double seam for window *size*
  #     analogous to onWindowModeChanged/lastWindowMode for window *mode*
  #     — per this feature's Clarifications (Q1), adding one was
  #     explicitly declined in favor of scoping Cucumber to the
  #     settings.json round-trip only. Verified instead by manual playtest
  #     (CLAUDE.md's Step 4.5: resize the window, quit, relaunch, confirm
  #     the size returns; shrink the screen/switch monitor to confirm
  #     clamping) and the implementing agent's own render-and-inspect
  #     check for the Swing-facing pieces (docs/ui-verification.md).
  #   - The two exit paths (JFrame's default EXIT_ON_CLOSE via the OS
  #     close button, and the title screen's explicit System.exit(0) Exit
  #     menu item) both funnel into one JVM shutdown hook per this
  #     feature's Clarifications (Q4) — the hook's registration and its
  #     coverage of both paths is a Step 4 implementation detail, not
  #     something Cucumber can observe.
  #   - Leaving a previously-saved Windowed size untouched when the app
  #     quits while in Fullscreen mode — this is "the shutdown hook only
  #     captures when the live mode is Windowed," a real-window/real-mode
  #     behavior with no settings.json-only expression; deferred to manual
  #     playtest the same as the rest of this list.
  #   - Independent-axis clamping to [MIN_WINDOW_WIDTH, screenBounds.width]
  #     / [MIN_WINDOW_HEIGHT, screenBounds.height] (this feature's
  #     Clarifications Q5) — same real-GraphicsDevice dependency as above.
  #   - Persisting window *position*, or persisting any Fullscreen-mode
  #     dimension — both explicitly out of scope per the intent doc.
  #
  # Risks:
  #   - New step definitions needed: "the settings file is loaded" (When)
  #     and "the loaded {string} is {int}" (Then) — neither exists yet.
  #     The existing "the settings file has {string} set to {int}" Given
  #     and "the settings file now has {string} set to {int}" Then only
  #     needed their key-name switch statements extended with
  #     "WindowWidth"/"WindowHeight" cases, but a Then that reloads from
  #     disk after a Given that just wrote the same value via the same
  #     SettingsStore round-trips trivially without exercising a genuine
  #     "relaunch reads a fresh store" path — hence the new "loaded"
  #     wording, which constructs a brand-new SettingsRepository/
  #     SettingsConfig the same way SettingsStore's constructor does at
  #     real startup, without building any screen or JFrame.
  #   - These new steps most naturally extend
  #     UiComponentFrameworkSteps.java (where every other settings-file
  #     Given/Then already lives), which several other feature files also
  #     depend on — per .claude/workflow.md's Step 5 guidance, run
  #     `mvn clean test` twice in a row after the change and require
  #     identical results.
  #   - Field defaults: two plain int fields (windowWidth/windowHeight),
  #     sentinel 0 meaning "nothing saved yet, let the window pack() to
  #     its natural size" — Gson's default int value for a missing/absent
  #     field is already 0, so no separate migration path is needed for
  #     settings.json files written before this feature existed (this
  #     feature's Clarifications Q2/Q3).
  #
  # Open questions:
  #   - None outstanding — all 5 clarification-round questions (testability
  #     boundary, field shape, default sentinel, capture trigger mechanism,
  #     clamping precedence) were resolved.
