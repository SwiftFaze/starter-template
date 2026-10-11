Feature: ESC opens a pause menu overlay
  Pressing ESC during gameplay opens a pause menu with Resume / Settings / Exit to Main Menu,
  mirroring how Inventory/Codex popups overlay the game. Movement input freezes while paused.

  Scenario: Pressing Escape during gameplay opens the pause menu
    Given a game panel is running
    When escape is pressed
    Then the pause menu is open

  Scenario: The pause menu lists Resume, Settings, and Exit to Main Menu in that order
    Given a game panel is running with pause menu open
    Then the selected menu item is "Resume"
    And moving down selects "Settings"
    And moving down again selects "Exit to Main Menu"

  # Note: The scenario "While the pause menu is open, movement does not move the player"
  # is verified in unit tests (GamePanelTest.pausedFlagPreventMovement) rather than in
  # Cucumber, as coordinating multiple step definition classes in Cucumber is complex.
  # This behavior is still covered by comprehensive unit tests.

  Scenario: Pressing Escape again while paused closes the pause menu and resumes gameplay
    Given a game panel is running with pause menu open
    When escape is pressed
    Then the pause menu is closed
    And the game is no longer paused

  Scenario: Selecting Resume from the pause menu closes the pause menu and resumes gameplay
    Given a game panel is running with pause menu open
    When resume is selected
    Then the pause menu is closed
    And the game is no longer paused

  Scenario: Selecting Settings from the pause menu notifies the host
    Given a game panel is running with pause menu open
    When settings is selected from pause menu
    Then the host was notified of "Settings" selection

  Scenario: Selecting Exit to Main Menu from the pause menu notifies the host
    Given a game panel is running with pause menu open
    When exit to main menu is selected from pause menu
    Then the host was notified of "Exit to Main Menu" selection

  # Non-goals:
  # - The actual CardLayout screen swap from Settings back to pause menu, and Main.java actually
  #   wiring "Exit to Main Menu" to GamePanel.resetState() (Main.java composition-root wiring,
  #   verified via manual playtest, not Cucumber) — GamePanel.resetState() itself (fresh
  #   Player/WorldScene, paused cleared) is covered directly by GamePanelTest instead, since
  #   unlike the old resetGame()/F5 hot-reset it replaced, it's now plain GamePanel logic.

  # Open questions:
  # - Interaction with Inventory/Codex popups already being open when ESC is pressed is resolved per Swing's focus-dispatch priority: pressing ESC while Inventory/Codex holds focus closes that popup (their own dismiss binding at WHEN_ANCESTOR_OF_FOCUSED_COMPONENT wins); pressing ESC again opens pause menu. No extra code needed.
