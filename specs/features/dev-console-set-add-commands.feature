Feature: Dev console set/add/subtract commands for live stat mutation
  The dev-console command bar (#186's parser/transcript, #187's
  completion) gains three verbs, `set <entry> <field> <value>`,
  `add <entry> <field> <value>`, and `subtract <entry> <field> <value>`,
  so a dev can jump straight to mutating a specific field on a live entity
  without opening its detail panel and arming a row. Field names are
  abbreviation tokens (str/dex/con/int/wis/luck/maxhp/maxmana/hp/mana),
  matching #170's confirmed `str` example. `add`/`subtract` take a
  positive-only magnitude - the other verb handles the opposite direction,
  never a negative value. `default` resets a field to its class-attribute
  base value, valid only for the six attributes plus Max HP/Max Mana (not
  Current HP/Current Mana, which have no class-default to reset to). Only
  the "Player" provider wires up field setters for v1. Reuses
  PlayerDetailPanel's existing floor/clamping rules (see
  sandbox-spawn-edit.feature) rather than defining new ones. Successful
  and failed mutations are both written as a levelled line to #186's
  transcript. The non-numeric "Class" field is out of scope - rejected as
  an unsupported field, same as the read-only Attack Power/Defense rows.

  Background:
    Given the dev console is running with the "Player" provider attached to the running player

  Scenario Outline: set overwrites a numeric field to an exact value
    Given the running player's "<field>" is <initial>
    When the command bar is set to "set player <token> <value>"
    Then the running player's "<field>" value is <value>
    And the transcript's last line is a SUCCESS line for "<field>" set to <value>

    Examples:
      | field        | token   | initial | value |
      | Strength     | str     | 10      | 15    |
      | Dexterity    | dex     | 10      | 0     |
      | Max HP       | maxhp   | 100     | 150   |
      | Current HP   | hp      | 100     | 40    |

  Scenario Outline: add adds a positive magnitude to a numeric field's current value
    Given the running player's "<field>" is <initial>
    When the command bar is set to "add player <token> <magnitude>"
    Then the running player's "<field>" value is <after>
    And the transcript's last line is a SUCCESS line for "<field>" set to <after>

    Examples:
      | field        | token | initial | magnitude | after |
      | Strength     | str   | 10      | 5         | 15    |
      | Current Mana | mana  | 50      | 10        | 60    |

  Scenario Outline: subtract removes a positive magnitude from a numeric field's current value
    Given the running player's "<field>" is <initial>
    When the command bar is set to "subtract player <token> <magnitude>"
    Then the running player's "<field>" value is <after>
    And the transcript's last line is a SUCCESS line for "<field>" set to <after>

    Examples:
      | field     | token | initial | magnitude | after |
      | Dexterity | dex   | 10      | 3         | 7     |
      | Max Mana  | maxmana | 50    | 6         | 44    |

  Scenario Outline: A negative value on add or subtract is rejected with an error transcript line
    When the command bar is set to "<verb> player str <negative>"
    Then the running player's "Strength" value is unchanged
    And the transcript's last line is an ERROR line for value "<negative>"

    Examples:
      | verb     | negative |
      | add      | -5       |
      | subtract | -5       |

  Scenario Outline: set/add/subtract below a field's floor clamps to the floor, same as arm+Left editing
    Given the running player's "<field>" is <initial>
    When the command bar is set to "set player <token> <below floor>"
    Then the running player's "<field>" value is <floor>

    Examples:
      | field      | token | initial | below floor | floor |
      | Strength   | str   | 10      | -5          | 0     |
      | Max HP     | maxhp | 100     | 0           | 1     |
      | Current HP | hp    | 100     | -10         | 0     |

  Scenario: set on Max HP below current HP clamps Current HP down to match, same as arm+Left editing
    Given the running player's "Current HP" is 100
    And the running player's "Max HP" is 100
    When the command bar is set to "set player maxhp 49"
    Then the running player's "Max HP" value is 49
    And the running player's "Current HP" value is 49

  Scenario: subtract on Max Mana below current mana clamps Current Mana down to match
    Given the running player's "Current Mana" is 50
    And the running player's "Max Mana" is 50
    When the command bar is set to "subtract player maxmana 10"
    Then the running player's "Max Mana" value is 40
    And the running player's "Current Mana" value is 40

  Scenario Outline: default resets an attribute or Max HP/Max Mana to its class-attribute base value
    Given the running player's class is "Warrior"
    And the running player's "<field>" has been changed from its class default
    When the command bar is set to "set player <token> default"
    Then the running player's "<field>" is Warrior's level-0 base <field>

    Examples:
      | field    | token |
      | Strength | str   |
      | Max HP   | maxhp |

  Scenario Outline: default is rejected for Current HP/Current Mana - no class-default to reset to
    When the command bar is set to "set player <token> default"
    Then the transcript's last line is an ERROR line for value "default"

    Examples:
      | token |
      | hp    |
      | mana  |

  Scenario: set/add/subtract on the non-numeric Class field is rejected as an unsupported field
    When the command bar is set to "set player class warrior"
    Then the running player's class value is unchanged
    And the transcript's last line is an ERROR line for field "class"

  Scenario: set/add/subtract on a read-only field is rejected with an error transcript line
    When the command bar is set to "set player attackpower 999"
    Then the running player's "Attack Power" value is unchanged
    And the transcript's last line is an ERROR line for field "attackpower"

  Scenario: set/add/subtract naming an unknown field is rejected with an error transcript line
    When the command bar is set to "set player nosuchfield 5"
    Then the transcript's last line is an ERROR line for field "nosuchfield"

  Scenario: set/add/subtract naming an unknown entry is rejected with an error transcript line
    When the command bar is set to "set nosuchentry str 5"
    Then the transcript's last line is an ERROR line for entry "nosuchentry"

  Scenario: set/add/subtract with a non-numeric, non-"default" value is rejected with an error transcript line
    When the command bar is set to "set player str notanumber"
    Then the running player's "Strength" value is unchanged
    And the transcript's last line is an ERROR line for value "notanumber"

  # Non-goals:
  #   - The command bar shell itself: the parser, the transcript, and
  #     namespace:id entry addressing - tracked in #186.
  #   - Positional Tab completion and command history as a general
  #     mechanism - tracked in #187. This file only covers set/add/subtract
  #     registering their arguments into that mechanism, not the
  #     mechanism's own behavior.
  #   - Any provider other than "Player" wiring up field setters - no other
  #     stateful/editable provider exists yet.
  #   - PlayerDetailPanel's existing arm+Left/Right editing - unchanged,
  #     covered by sandbox-spawn-edit.feature. set/add/subtract coexists as
  #     a power-user fast path alongside it; neither supersedes the other.
  #   - The non-numeric "Class" field - rejected as unsupported, same path
  #     as the read-only Attack Power/Defense rows.
  #
  # Risks:
  #   - This file specs against #186/#187's command-bar interface before
  #     either is implemented (#170 depends on both landing first). The
  #     exact token grammar, transcript line format, and completion
  #     registration API are assumed here from #170's issue text and may
  #     need revision once #186/#187 actually land.
  #   - `subtract` is a scope addition beyond issue #170's original text,
  #     added during this file's own clarification pass - the issue itself
  #     will need a follow-up edit or comment noting the added verb.
  #
  # Open questions:
  #   - Whether editing via command bar should live-update
  #     PlayerDetailPanel's table if it happens to be open at the same
  #     time (unlikely given the command bar is top-level-only), or that's
  #     a non-issue given the two surfaces don't overlap.
  #   - Exact transcript line wording/format - this file only asserts
  #     level (SUCCESS/ERROR) and which token the error names, not the
  #     literal message text, pending #186 landing.
