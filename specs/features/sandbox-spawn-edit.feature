Feature: Live in-game dev console for editing the running player's stats
  A dev-only keybind opens a second window (the dev-console framework from
  sandbox-dev-console.feature) alongside the running game, with a "Player"
  provider that holds a direct reference to the actual running Player -
  not a spawned copy - so editing a stat there is edited on the exact
  object the game loop reads next frame. Never reachable in a packaged
  build.

  Background:
    Given the dev console is running with the "Player" provider attached to the running player
    And "Player" is opened

  Scenario: The Player entry's table lists every editable field plus the read-only combat stats
    Then the table includes editable rows "Class", "Strength", "Dexterity", "Constitution", "Intelligence", "Wisdom", "Luck", "Max HP", "Max Mana", "Current HP", "Current Mana"
    And the table includes read-only rows "Attack Power", "Defense"

  Scenario Outline: Arming a row and pressing Right increases that field on the live player by 1
    Given the running player's "<field>" is <initial>
    When "<field>" is armed
    And right is pressed
    Then the running player's "<field>" value is <after right>
    And the displayed "<field>" value is <after right>

    Examples:
      | field        | initial | after right |
      | Strength     | 10      | 11          |
      | Dexterity    | 10      | 11          |
      | Constitution | 10      | 11          |
      | Intelligence | 10      | 11          |
      | Wisdom       | 10      | 11          |
      | Luck         | 10      | 11          |
      | Max HP       | 100     | 101         |
      | Max Mana     | 50      | 51          |
      | Current HP   | 100     | 101         |
      | Current Mana | 50      | 51          |

  Scenario Outline: Arming a row and pressing Left decreases that field on the live player by 1
    Given the running player's "<field>" is <initial>
    When "<field>" is armed
    And left is pressed
    Then the running player's "<field>" value is <after left>

    Examples:
      | field        | initial | after left |
      | Strength     | 10      | 9          |
      | Max HP       | 100     | 99         |
      | Current Mana | 50      | 49         |

  Scenario Outline: Decrementing at the field's floor has no further effect
    Given the running player's "<field>" is <floor>
    When "<field>" is armed
    And left is pressed
    Then the running player's "<field>" value is <floor>

    Examples:
      | field        | floor |
      | Strength     | 0     |
      | Max HP       | 1     |
      | Max Mana     | 1     |
      | Current HP   | 0     |
      | Current Mana | 0     |

  Scenario: Decreasing Max HP below the current HP clamps Current HP down to match
    Given the running player's "Current HP" is 100
    And the running player's "Max HP" is 100
    When "Max HP" is armed
    And "Max HP" is decreased to 49
    Then the running player's "Current HP" value is 49

  Scenario: Decreasing Max Mana below the current mana clamps Current Mana down to match
    Given the running player's "Current Mana" is 50
    And the running player's "Max Mana" is 50
    When "Max Mana" is armed
    And "Max Mana" is decreased to 44
    Then the running player's "Current Mana" value is 44

  Scenario: Attack Power and Defense are read-only and update live as their underlying attributes change
    Given the running player's "Dexterity" is 0
    And the running player's "Strength" is 10
    And the displayed "Attack Power" value is 20
    When "Strength" is armed
    And right is pressed
    Then the displayed "Attack Power" value is 22
    And "Attack Power" cannot be armed

  Scenario: Cycling the Class row reapplies that class's base stats to the live player at level 0
    Given the running player's class is "Warrior"
    And the running player's "Strength" has been changed from its class default
    When "Class" is armed
    And right is pressed
    Then the running player's class value is "Mage"
    And the running player's "Strength" is Mage's level-0 base strength

  Scenario: Editing a stat does not spawn or copy a player - it changes the object the game itself owns
    Given the game's live player is the same object identity before and after opening the provider
    When "Max HP" is armed
    And right is pressed
    Then the game's live player is still the same object identity
    And the game's live player's "Max HP" reflects the edit

  Scenario: Returning to the top-level search list does not reset or discard the live player's edits
    Given the running player's "Strength" has been edited away from its class default
    When the back action is triggered
    And "Player" is opened again
    Then the running player's "Strength" still reflects the edit

  # Non-goals:
  #   - Spawning a fresh/disconnected Player with a chosen class or starting
  #     position - dropped from scope entirely; there is nothing to spawn,
  #     the provider always attaches to whichever player the game already
  #     has running. Supersedes the original 2026-08-29 framing of this
  #     issue (see specs/intent/sandbox-spawn-edit.md's Revision history).
  #   - Item spawning/equipping, combat simulation, quest triggering,
  #     monster/NPC spawning - no such systems exist in the codebase yet.
  #   - Persisting an edited player to a save file.
  #   - The actual Main.java wiring that opens/closes the second JFrame on
  #     the F1 keybind, and the dev-only build-gating mechanism itself
  #     (excluding it from the jpackage/installer build) - this is
  #     composition-root wiring in the same vein as pause-screen-esc
  #     .feature's Non-goals ("Main.java actually wiring X... verified via
  #     manual playtest, not Cucumber"). The GamePanel-level notification
  #     that the keybind was pressed (a new GameListener.toggleDevConsole()
  #     default method, mirroring the existing toggleInventory/toggleCodex/
  #     togglePause) is expected to be covered by a GamePanelTest unit
  #     test, not this file.
  #   - Auto-pausing the game while the console is open - explicitly not
  #     wanted; the game keeps running so an edit's effect is visible live.
  #   - Cross-process/IPC access - same-process/same-JVM only.
  #
  # Clarifications:
  #   - Floors: attributes (Strength/Dexterity/Constitution/Intelligence/
  #     Wisdom/Luck) floor at 0; Max HP/Max Mana floor at 1 (0 is a
  #     degenerate/undefined state, no combat/death system exists to
  #     interpret it); Current HP/Current Mana floor at 0. No ceiling on
  #     any field.
  #   - Decreasing Max HP/Max Mana below the current Current HP/Current
  #     Mana clamps current down to match, rather than letting current
  #     temporarily exceed max.
  #   - The Main.java keybind is F1 - no collision with Keybindings.java's
  #     existing Z/S/Q/D/arrows/I/X/Tab/Enter/Escape bindings.
  #   - sandbox-dev-console.feature's and class-stats-sandbox.feature's
  #     Non-goals were updated in this same change to note they're
  #     superseded by this file - see those files' own Non-goals sections.
  #
  # Open questions:
  #   - None outstanding. Category/entry naming for the new provider (this
  #     file assumes a single DevConsoleEntry named "Player") is left to
  #     Step 4 implementation as a cosmetic, easily-changed detail.
  #
  # Post-approval fix (2026-09-06, found during Step 4/5 implementation):
  #   - Every "Then the running player's X is N" line was reworded to
  #     "Then the running player's X value is N" (and the Class row's to
  #     "class value is"). The original wording was IDENTICAL to the
  #     corresponding "Given the running player's X is N" precondition
  #     text used elsewhere in this same file - Cucumber-JVM matches step
  #     text regardless of @Given/@When/@Then keyword, so a Given-position
  #     "set the field" step and a Then-position "assert the field" step
  #     sharing identical text cannot both be implemented (there is no
  #     shared idempotent behavior for these two, unlike e.g.
  #     UiComponentFrameworkSteps.theConfirmationPopupIsShown()'s
  #     build-or-verify trick, since set and assert are genuinely
  #     different actions). This is exactly the duplicate-step-definition
  #     hazard .claude/workflow.md's Step 5 section warns about, and it
  #     was caught the same way that doc describes: cascading UndefinedStep
  #     failures across unrelated feature files (174 errors across the
  #     whole suite) traced back to `grep -ohE '@(Given|When|Then)\("[^"]*"\)' ...`
  #     finding these two duplicate literals.
