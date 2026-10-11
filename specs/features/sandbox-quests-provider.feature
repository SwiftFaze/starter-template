Feature: Sandbox Quests provider
  Adds a read-only "Quests" provider to the dev-console provider framework
  (sandbox-dev-console.feature), so every mod-loaded quest is its own
  searchable top-level result and opens straight to its objective and
  reward data - checking quest JSON without loading the real game.

  Covers: the Quests provider's entries (one per quest from
  ModRegistry.getAllQuests(), no intermediate "Quests" browse screen), its
  registration in both dev-console entry points (the F1 in-game console and
  the standalone ClassSandbox), and the detail view built from Quest's
  Inspectable DetailTables.
  Supersedes: nothing.
  Excludes: the search/results/back shell itself (sandbox-dev-console.feature),
  quest loading/validation (data-driven-quest.feature), and anything that
  touches a live player's QuestLog.

  QA: none - dev-only console emits no GameEvents for QaRunner to match; covered by Cucumber and the Step 4.5 playtest

  Background:
    Given the dev console is running with the "Quests" provider registered

  Scenario: Every mod-loaded quest is its own top-level result
    Then the results include an entry named "Goblin Slayer"
    And the "Goblin Slayer" result has namespace "core" and category "Quests"

  Scenario: A quest is findable by typing part of its name
    When the search text is set to "goblin"
    Then the results include an entry named "Goblin Slayer"

  Scenario: Opening a quest shows its objective
    When "Goblin Slayer" is opened
    Then the quest detail shows these fields:
      | Field            | Value              |
      | ID               | core:goblin_slayer |
      | Name             | Goblin Slayer      |
      | Objective Type   | kill               |
      | Objective Target | core:goblin        |
      | Objective Count  | 5                  |

  Scenario: Opening a quest shows each reward as a row, with "-" for fields that don't apply
    When "Goblin Slayer" is opened
    Then the quest detail has a "Rewards:" table with these rows:
      | Type | ID              | Count | Calc     |
      | item | core:iron_sword | 1     | -        |
      | xp   | -               | -     | level*25 |

  Scenario: An objective with no target or count shows a placeholder and the loader's default
    Given a loaded quest "Explore the Wilds" whose objective has no target and no count
    When "Explore the Wilds" is opened
    Then the quest detail shows these fields:
      | Field            | Value |
      | Objective Target | -     |
      | Objective Count  | 0     |

  Scenario: A quest with no rewards shows no rewards table
    Given a loaded quest "Scout the Road" with no rewards
    When "Scout the Road" is opened
    Then the quest detail has no "Rewards:" table

  Scenario: Returning to the results leaves no quest state behind
    Given "Goblin Slayer" is opened
    When the back action is triggered
    Then the results include an entry named "Goblin Slayer"
    And no player's quest log has changed

  Scenario: No quests loaded contributes no results
    Given no quests are loaded
    Then the results do not include any entry in category "Quests"

  Scenario Outline: The Quests provider is available from both dev-console entry points
    Given the <entry point> dev console is built
    Then its providers include the "Quests" provider

    Examples:
      | entry point          |
      | F1 in-game           |
      | standalone sandbox   |

  # Non-goals:
  #   - Triggering/completing a quest against a live player - no
  #     quest-engine/trigger system exists yet.
  #   - Resolving a reward's calc string to a number - shown raw.
  #     CalcExpressionParser does exist, but a read-only browse has no
  #     player level to evaluate against (clarified 2026-09-30; the issue's
  #     "no evaluator exists" premise was wrong).
  #   - Any change to DevConsolePanel's search/results/keybinding shell.
  #
  # Risks:
  #   - Quest's reward/objective fields are null where they don't apply;
  #     rendering must map null to "-", never print "null".
  #   - The F1 console and ClassSandbox build their provider lists
  #     separately (Main.java, ClassSandbox.java), so registration can drift
  #     between them - hence the Scenario Outline.
  #
  # Open questions: none.
