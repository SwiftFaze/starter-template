Feature: Dev console snapshot/restore of the running player's position and stats
  The dev-console command bar (#186's parser/transcript) gains two verbs,
  `snapshot <entry> <name>` and `restore <entry> <name>`, so a dev can
  capture the running player's position plus its ten editable Stats fields
  (Strength, Dexterity, Constitution, Intelligence, Wisdom, Luck, Max HP,
  Max Mana, Current HP, Current Mana) into a named in-memory slot and write
  them back later in the same session, instead of re-entering every value by
  hand. Slot names are free-form single tokens; there is no slot limit; and
  snapshotting an existing name overwrites it. Restore targets whichever
  player is live when it runs (the game view shows the new position on the
  next frame, and the Player panel shows restored values when opened) and
  never changes the player's class. Only the "Player" provider supports these
  verbs. Error lines follow dev-console-set-add-commands.feature's
  conventions.

  Out of scope: persistence to disk (closing the dev console discards every
  snapshot); snapshotting class identity or anything beyond position + Stats;
  multiple concurrent players; tab-completing snapshot names.

  QA: none - dev-only console window behind -Dveil.devConsole, which the
  key-replay runner does not launch; scenarios drive the command bar directly

  Background:
    Given the dev console is running with the "Player" provider attached to the running player

  Scenario: Restoring a snapshot writes the captured position and stats back
    Given the running player is at position (4, 7)
    And the running player's "Strength" is 18
    And the running player's "Current HP" is 60
    When the command bar is set to "snapshot player boss"
    And the running player is moved to position (10, 2)
    And the running player's "Strength" is 3
    And the running player's "Current HP" is 5
    And the command bar is set to "restore player boss"
    Then the running player is at position (4, 7)
    And the running player's "Strength" value is 18
    And the running player's "Current HP" value is 60
    And the transcript's last line is a SUCCESS line for snapshot "boss" restored

  Scenario: Taking a snapshot reports success and changes nothing
    Given the running player's "Strength" is 18
    When the command bar is set to "snapshot player boss"
    Then the running player's "Strength" value is unchanged
    And the transcript's last line is a SUCCESS line for snapshot "boss" saved

  Scenario Outline: Every editable stat round-trips through a snapshot
    Given the running player's "<field>" is <captured>
    When the command bar is set to "snapshot player slot"
    And the running player's "<field>" is <changed>
    And the command bar is set to "restore player slot"
    Then the running player's "<field>" value is <captured>

    Examples:
      | field        | captured | changed |
      | Strength     | 18       | 1       |
      | Dexterity    | 14       | 2       |
      | Constitution | 16       | 3       |
      | Intelligence | 12       | 4       |
      | Wisdom       | 11       | 5       |
      | Luck         | 9        | 6       |
      | Max HP       | 150      | 20      |
      | Max Mana     | 80       | 10      |
      | Current HP   | 70       | 1       |
      | Current Mana | 30       | 0       |

  Scenario: Opening Player after a restore shows the restored values
    Given the running player's "Strength" is 18
    When the command bar is set to "snapshot player boss"
    And the running player's "Strength" is 3
    And the command bar is set to "restore player boss"
    And "Player" is opened
    Then the displayed "Strength" value is 18

  Scenario: Separate named slots are kept independently
    Given the running player's "Strength" is 18
    When the command bar is set to "snapshot player strong"
    And the running player's "Strength" is 2
    And the command bar is set to "snapshot player weak"
    And the command bar is set to "restore player strong"
    Then the running player's "Strength" value is 18

  Scenario: Snapshotting an existing name overwrites it
    Given the running player's "Strength" is 18
    When the command bar is set to "snapshot player boss"
    And the running player's "Strength" is 2
    And the command bar is set to "snapshot player boss"
    And the running player's "Strength" is 7
    And the command bar is set to "restore player boss"
    Then the running player's "Strength" value is 2

  Scenario: Restore does not change the player's class
    Given the running player's class is "Warrior"
    And the command bar is set to "snapshot player boss"
    And the running player's class is "Mage"
    When the command bar is set to "restore player boss"
    Then the running player's class value is "Mage"

  Scenario: Restoring an unknown snapshot name is an error and changes nothing
    Given the running player's "Strength" is 18
    When the command bar is set to "restore player nosuchslot"
    Then the running player's "Strength" value is unchanged
    And the transcript's last line is an ERROR line for snapshot "nosuchslot"

  Scenario Outline: A verb without a snapshot name prints its usage
    When the command bar is set to "<command>"
    Then the transcript's last line is an ERROR line reading "<usage>"

    Examples:
      | command          | usage                             |
      | snapshot player  | Usage: snapshot <entry> <name>    |
      | restore player   | Usage: restore <entry> <name>     |
      | snapshot         | Usage: snapshot <entry> <name>    |

  Scenario: An unknown entry is an error
    When the command bar is set to "snapshot nosuchentry boss"
    Then the transcript's last line is an ERROR line for entry "nosuchentry"

  Scenario: An entry without snapshot support is an error
    Given the dev console also has the "Classes" provider attached
    When the command bar is set to "snapshot warrior boss"
    Then the transcript's last line is an ERROR line reading "Entry does not support snapshots: warrior"

# Non-goals: disk persistence; class snapshot; multiple players; name completion.
# Risks: "Restore" onto a replaced player (GamePanel.resetState) must hit the
#   provider's Supplier<Player>, not a cached reference - the class-change
#   scenario covers the "live now" read only indirectly.
# Open questions: none (see intent Clarifications, all auto-decided).
