Feature: Startup welcome screen
  A title screen shown before the game view: "VEIL" (Delta Corps Priest 1
  font, with a graceful fallback — see below) and a menu (Continue, New,
  Load, Settings, Exit), replacing today's direct-to-world launch. Built
  on the current ListWidget/ButtonWidget framework, not the deleted
  MenuPanel/SelectableMenu the source issue's text refers to.

  Scenario: Launching the game shows the title screen
    Given the game is launched
    Then the title screen is shown
    And the title text is "VEIL"
    And the title menu lists "Continue", "New", "Load", "Settings", "Exit"

  Scenario: Selecting New from the title screen navigates to the game view
    Given the title screen is shown
    And "New" is highlighted in the title menu
    When the "Enter" key is pressed
    Then the game view is shown

  Scenario: Selecting Settings from the title screen opens the settings screen
    Given the title screen is shown
    And "Settings" is highlighted in the title menu
    When the "Enter" key is pressed
    Then the settings screen is shown

  Scenario: Selecting Exit from the title screen triggers the exit action
    Given the title screen is shown
    And "Exit" is highlighted in the title menu
    When the "Enter" key is pressed
    Then the exit action is triggered

  Scenario Outline: Confirming a non-functional placeholder menu item does nothing
    Given the title screen is shown
    And "<item>" is highlighted in the title menu
    When the "Enter" key is pressed
    Then the title screen is still shown

    Examples:
      | item     |
      | Continue |
      | Load     |

  Scenario: The title falls back to the default terminal font when Delta Corps Priest 1 is not bundled
    Given no Delta Corps Priest 1 font resource is bundled
    When the title screen is built
    Then the title text uses the default monospaced terminal font

  # Non-goals:
  #   - Continue/Load actually loading a save — no save system exists.
  #   - Any decorative ASCII art, logo, or border — v1 is title text +
  #     menu only.
  #   - Asserting the actual Delta Corps Priest 1 glyphs render
  #     correctly — that font file isn't in the repo yet, and is a
  #     manual/visual concern once it's supplied, not something
  #     a Gherkin scenario can usefully check either way.
  #   - Exit's actual window-close behavior beyond "the game exits" —
  #     standard JFrame close, nothing new to prove.
  #   - Any mouse/pointer handling — this game is keyboard-only by
  #     design.
  #
  # Risks:
  #   - Depends on ui-widget-slider.feature's slider widget landing for
  #     the settings screen's brightness/volume items — this screen's own
  #     scenarios don't touch it, but the Settings navigation target
  #     (settings-screen.feature) does. The radio group widget (issue #35)
  #     needed for the Fullscreen/Windowed toggle and font cycle is
  #     already implemented on develop.
  #   - Real Swing focus-transfer and real window launch are not
  #     simulated headlessly, matching this repo's existing Cucumber
  #     precedent (ui-component-framework.feature).
  #
  # Open questions:
  #   - Where the actual Delta Corps Priest 1 .ttf comes from.
