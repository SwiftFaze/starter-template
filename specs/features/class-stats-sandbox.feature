Feature: Class/stats sandbox
  A standalone, dev-only tool lists all player classes and shows each
  one's computed stats, so class balance can be checked without playing
  the game. It is not part of the packaged/jpackage build.

  Scenario: Listing available classes
    Given the class sandbox is running
    Then the class list includes "Warrior" and "Mage"

  Scenario: Selecting a class shows its computed stats
    Given the class sandbox is running
    When "Warrior" is selected
    Then the displayed attack power is 35
    And the displayed defense is 17
    And the displayed max HP is 120
    And the displayed max mana is 20

  Scenario: Editing a class's JSON is reflected on the next run without recompiling
    Given the "mage" class JSON has been edited to set max HP to 80
    When the class sandbox is started fresh and "Mage" is selected
    Then the displayed max HP is 80

  # Non-goals:
  #   - A playable test arena, world, or movement — nothing consumes
  #     combat stats in gameplay yet, so there'd be nothing real to
  #     exercise.
  #   - Wiring the sandbox into Main.java or the packaged build.
  #     PARTIALLY SUPERSEDED 2026-09-06 by sandbox-spawn-edit.feature: a
  #     dev-only F1 keybind now opens the dev-console framework from
  #     inside Main.java (issue #27's live "Player" provider needs a
  #     reference to the actual running Player). This class-stats-sandbox
  #     feature (ClassSandbox, the standalone entry point) is itself
  #     unchanged and still not wired into Main.java. The "packaged build"
  #     half of this Non-goal still holds in full: the dev-only keybind is
  #     excluded from the jpackage/installer build, same as ClassSandbox
  #     always was.
  #
  # Risks:
  #   - Accidentally including ClassSandbox as a launchable entry point in
  #     the jpackage build — Verification explicitly checks for this.
  #
  # Open questions:
  #   - None outstanding — the shared TerminalPanel/SelectableMenu
  #     questions raised here were resolved in
  #     keyboard-input-and-menu-navigation.feature.
