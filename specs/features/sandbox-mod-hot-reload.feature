Feature: Dev console reload verb for mod hot-reload
  The dev-console command bar (#171's verb shell) gains a `reload` verb
  that asks every registered provider to refresh its mod-loaded data and
  then rebuilds the console's entry list, so edits to mod JSON show up
  without restarting the game or sandbox. Providers get a no-op-by-default
  refresh hook; providers that already re-read mods/ on every entries()
  call (Classes) need nothing more. A failed load keeps the previous
  entries. The live running player is never touched by reload.
  Out of scope: hot-reloading Java code, file watching/auto-reload on save,
  refreshing game-side mod data outside the dev console, and a dedicated
  reload keybinding.
  QA: none - a typed dev-console verb with no new keybinding; the command bar's key handling is covered by the dev-console procedures

  Background:
    Given the dev console is running with a provider whose entries can change

  Scenario: Reload picks up an entry added since the console started
    Given the provider starts with the entry "core:mage"
    And the provider's data now also contains the entry "core:rogue"
    When the command bar is set to "reload"
    Then searching for "rogue" finds the entry "core:rogue"
    And the transcript's last line is a SUCCESS line "Reloaded mods: 2 entries"

  Scenario: Reload drops an entry removed since the console started
    Given the provider starts with the entries "core:mage" and "core:rogue"
    And the provider's data now only contains the entry "core:mage"
    When the command bar is set to "reload"
    Then searching for "rogue" finds no entries
    And the transcript's last line is a SUCCESS line "Reloaded mods: 1 entries"

  Scenario: Reload asks every provider to refresh
    Given a second provider is also registered
    When the command bar is set to "reload"
    Then every registered provider was asked to refresh exactly once

  Scenario: Without reload the console keeps its startup entries
    Given the provider starts with the entry "core:mage"
    And the provider's data now also contains the entry "core:rogue"
    When the command bar is set to "search rogue"
    Then the transcript reports 0 results for "rogue"

  Scenario: A failed reload keeps the previous entries
    Given the provider starts with the entry "core:mage"
    And the provider's next refresh fails with "Failed to load tile from file: bad.json"
    When the command bar is set to "reload"
    Then searching for "mage" finds the entry "core:mage"
    And the transcript's last line is an ERROR line "Reload failed: Failed to load tile from file: bad.json"

  Scenario: Reload with arguments is rejected
    When the command bar is set to "reload now"
    Then no provider was asked to refresh
    And the transcript's last line is an ERROR line "Usage: reload"

  Scenario: Reload leaves the running player untouched
    Given the dev console also has the "Player" provider attached to the running player
    And the running player's "Strength" is 17
    When the command bar is set to "reload"
    Then the running player's "Strength" value is 17
    And the running player is the same player instance as before the reload
