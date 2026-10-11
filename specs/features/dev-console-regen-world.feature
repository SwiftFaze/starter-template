Feature: Dev console regen verb for regenerating the world
  The dev-console command bar gains a `regen [seed]` verb that replaces the
  running game's world with a freshly generated one (seeded-world-generation
  .feature's GeneratedWorldScene). With no argument it draws a fresh random
  seed; with a numeric argument it uses that seed. Either way it logs
  "World generated from seed <n>" at INFO and writes a SUCCESS transcript
  line naming the seed, so a dev can flip through maps and note a good one.

  The running player is kept - stats, class and inventory are untouched -
  and moved to the new world's spawn point. Pause state is untouched.
  `regen` joins tab completion like the other verbs.

  Supersedes: nothing.

  Out of scope: a keybinding for regen; regenerating outside the dev
  console; persisting or listing past seeds; resetting the player.

  QA: none - a typed dev-console verb with no new keybinding; the command bar's key handling is covered by the dev-console procedures

  Background:
    Given the dev console is attached to a running game

  Scenario: regen with a seed rebuilds the world from that seed
    When the command bar is set to "regen 12345"
    Then the running game's world matches a world generated from seed 12345
    And the transcript's last line is a SUCCESS line "World regenerated from seed 12345"
    And the log reads "World generated from seed 12345"

  Scenario: regen without a seed uses a fresh random seed
    Given the next fresh world seed is 777
    When the command bar is set to "regen"
    Then the running game's world matches a world generated from seed 777
    And the transcript's last line is a SUCCESS line "World regenerated from seed 777"

  Scenario: regen keeps the player and moves them to the new spawn
    Given the running player has 42 Strength and is at a position other than the new world's spawn
    When the command bar is set to "regen 12345"
    Then the running player is the same player with 42 Strength
    And the running player is at the spawn point of a world generated from seed 12345

  Scenario: regen ignores the world-seed override
    Given the world-seed override is "999"
    And the next fresh world seed is 777
    When the command bar is set to "regen"
    Then the running game's world matches a world generated from seed 777

  Scenario Outline: A malformed regen leaves the world unchanged
    Given the running game's world was generated from seed 12345
    When the command bar is set to "<command>"
    Then the transcript's last line is an ERROR line "<error>"
    And the running game's world matches a world generated from seed 12345

    Examples:
      | command      | error                               |
      | regen abc    | Not a valid seed: abc               |
      | regen 1 2    | Usage: regen [seed]                 |
      | regen 1.5    | Not a valid seed: 1.5               |

  Scenario: regen is offered by tab completion
    Given the command field contains "reg"
    Then the suggestion overlay is showing with candidates "regen"

  # Non-goals:
  #   - A keybinding, a seed history, or a settings UI for the seed.
  #   - Resetting the player (Exit to Main Menu already does a full reset).
  #
  # Risks:
  #   - "regen ignores the world-seed override": -Dveil.worldSeed would
  #     otherwise make bare `regen` rebuild the same map every time, which
  #     defeats the command. The override only applies to new games.
  #   - The dev console is in `sandbox`; ArchUnit forbids it constructing
  #     engine classes, so the regen must reach GamePanel through a callback
  #     wired in Main.
  #   - Regenerating a 250x250 world runs on the EDT; if it's slow, the
  #     console will stall for that long.
  #
  # Open questions: none. See Clarifications in
  # specs/intent/seeded-world-generation.md.
