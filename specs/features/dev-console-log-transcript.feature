Feature: Dev console log transcript
  The dev console's top-level view stops being a live-filtered, selectable results
  table and becomes an append-only log transcript, driven by two explicit commands:
  `search <term>` (filters, prints a numbered result table into the transcript) and
  `edit <namespace:id>` (opens that entry's detail screen). `DevConsoleEntry` gains a
  stable id so entries are addressed as `namespace:id` instead of by display name.
  Implicit bare-text search is removed - a bare word with no recognized verb, or any
  other unrecognized/malformed input, appends an error line instead of running
  anything. Previous output is never replaced; it accumulates for the life of the
  console panel (built once at startup - a close/reopen via F1 does not clear it).
  Every typed command is itself echoed as a dimmed COMMAND-level line immediately
  before whatever it produces, so the transcript reads as a full record of what was
  typed and what happened, not just the results.

  This file supersedes sandbox-dev-console.feature's live-as-you-type scenarios
  ("Typing filters results by substring, case-insensitively", "Opening a search
  result jumps straight to its detail", "No results for the current search text"),
  replacing them with the explicit command grammar below. sandbox-dev-console's
  provider framework itself (`DevConsoleProvider`, `entries()`) and its Escape/detail
  panel plumbing are unchanged and out of scope here.

  Out of scope: Tab completion and command history (#187); `set`/`add` mutation verbs
  (#170); any change to how the console is launched or gated
  (`-Dveil.devConsole=true`, F1); tailing the game's real slf4j/logback log lines into
  the transcript (command output only, for this issue - #187 or a later issue);
  addressing an entry by result-row number (e.g. `edit 1` - `edit <namespace:id>` is
  the only form here).

  Background:
    Given the dev console is running with the "Classes" provider registered

  Scenario: Searching appends a summary line and a numbered result table to the transcript
    When the command "search classes" is entered
    Then the transcript's last entry is an info line reporting 2 results for "classes"
    And the transcript's most recent result table includes a row with id "core:mage", name "Mage", category "Classes", and mod "core"
    And the transcript's most recent result table includes a row with id "core:warrior", name "Warrior", category "Classes", and mod "core"

  Scenario: A search with no matches reports zero results and no table
    When the command "search zzz" is entered
    Then the transcript's last entry is an info line reporting 0 results for "zzz"
    And the transcript has no result table

  Scenario: Addressing an entry by namespace:id opens its detail screen
    When the command "edit core:mage" is entered
    Then the opened detail panel is shown

  Scenario: The typed command itself is echoed as a transcript line before its result
    When the command "search classes" is entered
    Then the transcript's first entry is a command line for "search classes"
    And the transcript's last entry is an info line reporting 2 results for "classes"

  Scenario: Escape returns to the console view without clearing the transcript
    Given the command "search classes" has been entered
    And the command "edit core:mage" has been entered
    When the back action is triggered
    Then the console view is shown
    And the transcript still contains an info line reporting 2 results for "classes"

  Scenario: Output accumulates across commands rather than replacing prior output
    When the command "search classes" is entered
    And the command "edit core:mage" is entered
    And the back action is triggered
    And the command "flibberty" is entered
    Then the transcript contains an info line reporting 2 results for "classes"
    And the transcript contains an error line for "flibberty"

  Scenario Outline: Unrecognized or malformed input appends an error line instead of running a command
    When the command "<input>" is entered
    Then the transcript's last entry is an error line for "<input>"
    And the transcript has no result table

    Examples:
      | input           |
      | mage            |
      | flibberty       |
      | edit            |
      | edit core:ghost |
      | edit 1          |
      | search          |

  # Non-goals:
  #   - Tab completion and command history - tracked in #187.
  #   - `set`/`add` mutation verbs - tracked in #170.
  #   - Any change to how the console is launched or gated (-Dveil.devConsole=true, F1).
  #
  # Related:
  #   - sandbox-dev-console.feature covers the provider framework itself
  #     (DevConsoleProvider, entries()) - unchanged here. This file supersedes its
  #     live-as-you-type filtering and type-then-Enter opening scenarios (see the
  #     Feature description above) with the explicit search/edit command grammar.
  #   - sandbox-spawn-edit.feature and class-stats-sandbox.feature are unaffected -
  #     they exercise provider/detail-panel behavior below the console shell this
  #     file covers.
  #
  # Risks:
  #   - Exact summary-line wording (singular/plural phrasing, e.g. "1 Result" vs.
  #     "2 Results") is left as a cosmetic implementation detail, not pinned by
  #     these scenarios beyond the numeric count.
  #
  # Clarifications (see specs/intent/dev-console-log-transcript.md for full rationale):
  #   - Transcript shows command output only - no logback tailing in this issue.
  #   - `edit <namespace:id>` is the only address form - `edit 1` (row-number
  #     shorthand) is malformed input here, not a valid address.
