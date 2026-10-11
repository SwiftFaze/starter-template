Feature: Class stats across a level range
  The dev console's "Classes" provider opens a per-class detail view
  (ClassDetailPanel). This view shows the class's computed stats at a fixed
  set of levels (0, 5, 10, 15, 20), with one row per stat and one column per
  level, so a class's growth curve can be checked without playing to those
  levels. Every value is exactly what PlayerClass.applyStatsAtLevel produces
  for that level.

  Supersedes: the detail view's old single "Value" column, which only ever
  showed level 0.
  Out of scope: ClassSandboxPanel and class-stats-sandbox.feature, which
  keep showing level 0 unchanged; editing the curve itself (mod JSON); and
  a live player's level (sandbox-spawn-edit.feature).

  QA: none - no new keyboard input; the view only gains level columns

  Background:
    Given a class "Grower" with base strength 10 growing by "level * 2" and base max HP 100

  Scenario: The table has one column per sampled level
    When the detail view for "Grower" is opened
    Then the detail table's columns are "Stat, Lv 0, Lv 5, Lv 10, Lv 15, Lv 20"
    And the detail table's rows are the stats "Attack Power, Defense, Max HP, Max Mana, Strength, Dexterity, Constitution, Intelligence, Wisdom, Luck"

  Scenario Outline: Stats grow along the class's curve
    When the detail view for "Grower" is opened
    Then the "<stat>" row reads "<values>"

    Examples:
      | stat         | values               |
      | Strength     | 10, 20, 30, 40, 50   |
      | Attack Power | 20, 40, 60, 80, 100  |

  Scenario: A stat with no growth curve is the same at every level
    When the detail view for "Grower" is opened
    Then the "Max HP" row reads "100, 100, 100, 100, 100"
    And the "Luck" row reads "0, 0, 0, 0, 0"

  Scenario: The level 0 column matches the class's starting stats
    Given the core classes are loaded
    When the detail view for "Warrior" is opened
    Then the "Attack Power" row reads "35, 35, 35, 35, 35"
    And the "Max HP" row reads "120, 120, 120, 120, 120"

  Scenario: Opening the detail view for an unknown class fails
    When the detail view for "Nonexistent" is opened
    Then opening the detail view fails with "Unknown class: Nonexistent"

  # Non-goals:
  #   - A user-adjustable level range. Fixed levels are enough until the
  #     game defines a max level (see the intent doc's Clarifications).
  #   - Changing ClassSandboxPanel, which is only a Cucumber fixture.
  #
  # Risks:
  #   - The shipped classes (Warrior, Mage) have no growth curves, so on
  #     real data every column is identical. This is correct and not a bug.
  #     Growth is covered by the "Grower" fixture class instead.
  #
  # Open questions:
  #   - None outstanding.
