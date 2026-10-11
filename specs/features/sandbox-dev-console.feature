Feature: Sandbox dev-console framework
  The dev-only sandbox (launched only via the standalone Sandbox.run.xml
  run configuration, never from Main.java) generalizes from a single
  hardcoded class/stats browser into a searchable results table of every
  registered provider's individual entries (a mod namespace, category, and
  name per row), so a future provider (items, monsters, quests, ...) is a
  small addition rather than a rewrite of the search/results/keybinding
  plumbing.

  Background:
    Given the dev console is running with the "Classes" provider registered

  Scenario: The top-level results table always shows every provider's individual entries
    Then the results include an entry named "Mage"
    And the results include an entry named "Warrior"

  Scenario: Each result shows its mod namespace, category, and name
    Then the "Mage" result has namespace "core" and category "Classes"

  Scenario: Typing filters results by substring, case-insensitively
    When the search text is set to "mag"
    Then the results include an entry named "Mage"
    And the results do not include an entry named "Warrior"

  Scenario: Search matches on mod namespace or category, not just name
    When the search text is set to "core"
    Then the results include an entry named "Mage"
    And the results include an entry named "Warrior"
    When the search text is set to "class"
    Then the results include an entry named "Mage"
    And the results include an entry named "Warrior"

  Scenario: Opening a search result jumps straight to its detail
    When "Mage" is opened
    Then the opened detail panel is shown

  Scenario: Escape returns from an opened detail panel to the top-level results
    Given "Mage" is opened
    When the back action is triggered
    Then the results include an entry named "Mage"

  Scenario: No results for the current search text
    When the search text is set to "zzz"
    Then the results are empty

  # Non-goals:
  #   - Spawning/editing anything (players, items, stats, monsters), combat
  #     simulation, or quest triggering - tracked in issue #27.
  #   - Any in-game hotkey/overlay access - stays a separate dev-only entry
  #     point, never reachable from a running game session.
  #     SUPERSEDED 2026-09-06 by sandbox-spawn-edit.feature: issue #27's
  #     "Player" provider needs a live reference to the actual running
  #     Player, so a dev-only F1 keybind now opens this same dev-console
  #     framework from inside Main.java, alongside the running game. This
  #     provider framework itself is unchanged; only its reachability from
  #     a running game session changed. It still never reaches the
  #     packaged/jpackage build - see class-stats-sandbox.feature's
  #     Non-goals for that half.
  #   - Persisting sandbox state or interacting with save files.
  #
  # Related:
  #   - class-stats-sandbox.feature covers the "Classes" data itself
  #     (class list, computed stats) in detail, unchanged by this refactor
  #     - this feature covers the provider-framework search/results shell.
  #
  # Clarifications:
  #   - Back keybinding reuses Keybindings.MENU_CANCEL (Escape) - the
  #     codebase's existing back/cancel/dismiss convention, not a new key.
  #   - The top-level results table always shows every provider's
  #     individual entries directly (never just a provider name to open),
  #     so search finds specific things ("mage") rather than categories.
  #   - Opening a result jumps straight to that entry's own detail
  #     (pre-selected), not a generic "browse everything" screen first.
