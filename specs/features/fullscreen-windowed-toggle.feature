Feature: Fullscreen/Windowed toggle applies live to the game window
  The Settings screen's Fullscreen/Windowed radio row now drives the real
  game window instead of only updating its own display: toggling it applies
  immediately, Reset to Defaults applies it too, and launching with an
  already-persisted Fullscreen/Windowed value applies it at startup. The F5
  hot-reset dev feature is removed entirely as part of this change — real
  launches already pick up the persisted window mode directly, making F5's
  dispose/rebuild cycle both redundant and an awkward extra path to keep
  correct.

  Scenario: Toggling Fullscreen right switches the live game window to Fullscreen
    Given the settings screen is shown
    And "Fullscreen" is highlighted with value "Windowed"
    When the "Right" key is pressed
    Then "Fullscreen"'s value is "Fullscreen"
    And the game window switches to "Fullscreen" mode

  Scenario: Toggling Fullscreen left switches the live game window back to Windowed
    Given the settings file has "Fullscreen" set to "Fullscreen"
    And the settings screen is shown
    And "Fullscreen" is highlighted with value "Fullscreen"
    When the "Left" key is pressed
    Then "Fullscreen"'s value is "Windowed"
    And the game window switches to "Windowed" mode

  Scenario: Launching with a persisted Fullscreen setting starts the game window in Fullscreen mode
    Given the settings file has "Fullscreen" set to "Fullscreen"
    When the settings screen is shown
    Then the game window switches to "Fullscreen" mode

  Scenario: Confirming Reset to Defaults switches the live game window back to Windowed
    Given the settings file has "Fullscreen" set to "Fullscreen"
    And the settings screen is shown
    And the confirmation popup is shown
    And "Yes" is highlighted in the confirmation popup
    When the "Enter" key is pressed
    Then "Fullscreen"'s value is "Windowed"
    And the game window switches to "Windowed" mode

  # Non-goals:
  #   - Real JFrame decoration/undecoration, actual GraphicsDevice fullscreen
  #     bounds, actual pixel-level resizing, or minimum-size enforcement —
  #     none of this is exercised by Cucumber ("a live JFrame isn't
  #     constructed in headless tests", see
  #     UiComponentFrameworkSteps.theGameWindowIsShown()); verified instead
  #     by manual playtest (CLAUDE.md's Step 4.5) and the implementing
  #     agent's own render-and-inspect check for Swing changes
  #     (docs/ui-verification.md). These scenarios only assert that the
  #     correct mode string ("Windowed"/"Fullscreen") reaches the callback
  #     Main.java wires to the real frame.
  #   - Camera viewport resizing to track the panel's live pixel size, and
  #     its minimum-size floor — Camera is a plain domain object tested
  #     independently of any Swing container in camera-behavior.feature;
  #     covered there, not duplicated here.
  #   - Persisting the chosen window mode across full application restarts —
  #     that mechanism (SettingsStore/SettingsConfig) already exists and is
  #     covered by settings-persistence.feature. This feature only covers
  #     applying whatever value is already persisted/selected to the real
  #     window.
  #   - Which physical monitor/GraphicsDevice Fullscreen mode maximizes to on
  #     a multi-monitor setup — an implementation detail
  #     (frame.getGraphicsConfiguration().getDevice()), not automatable or
  #     spec-worthy at this level.
  #   - The F5 hot-reset dev feature's removal itself has no scenario here —
  #     it was never covered by an automated test to begin with (no existing
  #     .feature file references F5/resetGame). Confirmed instead via manual
  #     playtest that F5 no longer does anything once Main.resetGame,
  #     Main.keyListen, and the F5 keybinding are deleted.
  #
  # Risks:
  #   - The new "the game window switches to {string} mode" step is added to
  #     the existing shared UiComponentFrameworkSteps.java, which several
  #     other feature files also depend on — per .claude/workflow.md's Step
  #     5 guidance, run `mvn clean test` twice in a row after the change and
  #     require identical results.
  #   - The existing `itemIsHighlightedWithValue` step only navigates to the
  #     named row by highlighting it; it does not drive or verify the stated
  #     value. Scenarios above needing a non-default starting value
  #     ("Fullscreen") route around this by writing the value directly into
  #     the settings file first and letting construction pick it up, the
  #     same pattern settings-persistence.feature already uses, rather than
  #     relying on that step to set state.
  #   - Exact callback shape threaded from SettingsScreenPanel through to
  #     Main.java (a plain Consumer<String>, or a small dedicated type) is
  #     left for Step 4.
  #
  # Open questions:
  #   - None outstanding — all Step 2 clarification-round questions
  #     (initial-launch application, Reset to Defaults live-applying,
  #     minimum window size, and the mid-implementation F5-removal request)
  #     were resolved.
