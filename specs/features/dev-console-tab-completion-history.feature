Feature: Dev console positional Tab completion and command history
  The dev-console command bar (#186's parser/transcript) gains positional,
  context-sensitive Tab completion and Up/Down command history. Position 0
  always completes verbs (search/edit/set/add/subtract/snapshot/restore). Later positions
  complete against whatever that verb declares for that argument: `search`
  completes against the deduplicated set of matching entry names,
  namespaces, and categories (the same three fields it filters against);
  `edit` completes against entries' full `namespace:id` tokens, since that's
  the exact form it needs to resolve; `set`/`add`/`subtract`'s entry
  argument completes against the local-id form of entries whose provider
  exposes a field mutator (#170/#189 - currently only "player"), and their
  field argument completes against that entry's mutable field tokens;
  `set`/`add`/`subtract`'s value argument completes only the `default`
  keyword, and only when the verb is `set` and the named field has a
  class-default (per #189/#170). A trailing space always starts a new,
  empty-prefix argument at the next position (e.g. "set player " completes
  the field-name argument, listing every field, rather than re-suggesting
  "player") - completion always tracks the argument actually being typed,
  never the one just finished.

  Candidates render live, on every keystroke, in a floating suggestion
  overlay anchored above the command field (styled like Claude Code's own
  `/`-command menu) - not printed into the transcript. Zero candidates for
  the current trailing token: no overlay. One or more candidates: the
  overlay opens listing all of them, one highlighted (defaults to the
  first). While the overlay is open, Up/Down move the highlight among its
  candidates (wrapping at the ends) instead of navigating history; Tab or
  Enter accepts the highlighted candidate - filling it into the field and
  closing the overlay, without executing the command (a second Enter, now
  with the overlay closed, runs it); Escape closes the overlay without
  changing the field's text. While no overlay is open, Up/Down navigate
  command history as below, Enter executes the typed command, and Tab does
  nothing. Recalling a history entry (Up/Down with no overlay open) never
  itself reopens an overlay for the recalled text, even if it would
  otherwise have candidates - only genuine typing triggers live filtering.

  Up/Down command history (unaffected by the overlay redesign above): Up
  recalls backward, most recent first; Down walks forward and, past the
  newest entry, restores whatever draft was being typed before history
  navigation started. History records on Enter for any non-empty command
  that differs from the immediately previous entry (shell-style adjacent
  dedup only, no global move-to-top reordering) - including a zero-result
  command, since that's the one most worth recalling and fixing. History is
  in-memory, process lifetime only.

  This file defines the completion/history mechanism only - the verbs
  themselves (`search`/`edit` from #186, `set`/`add`/`subtract` from
  #170/#189) are unchanged and out of scope here. Supersedes the Tab/
  history portion of #171 (closed as superseded): #171's "Tab completes
  only the first keyword, never entry names" is reversed by the per-
  position design above, and its Ctrl+Up/Ctrl+Down results-table
  navigation is dead now that the console has no selectable table
  (dev-console-log-transcript.feature). Also supersedes this same file's
  own first version (2026-09-08), which printed multi-candidate matches as
  a pipe-separated transcript line instead of a floating overlay - replaced
  after seeing the requested UX matched Claude Code's own suggestion menu.

  Background:
    Given the dev console is running with the "Classes" and "Player" providers registered

  Scenario Outline: A suggestion overlay opens live, as the typed prefix gets one or more candidates
    Given the command field contains "<typed>"
    Then the suggestion overlay is showing with candidates "<candidates>"

    Examples:
      | typed             | candidates                            |
      | sea                | search                                |
      | s                  | search, set, snapshot, subtract       |
      | search c           | Classes, core                         |
      | edit core:         | core:mage, core:warrior, core:player  |
      | set player m       | maxhp, maxmana, mana                  |
      | set player str d   | default                                |

  Scenario: A trailing space starts a new, empty-prefix argument instead of re-suggesting the just-completed word
    Given the command field contains "set player "
    Then the suggestion overlay is showing with candidates "str, dex, con, int, wis, luck, maxhp, maxmana, hp, mana"

  Scenario Outline: No suggestion overlay opens when the current token has no completion source
    Given the command field contains "<typed>"
    Then the suggestion overlay is not showing

    Examples:
      | typed              | reason                                       |
      | set player str 9   | a literal numeric value, nothing to complete |
      | set player hp d    | Current HP has no class-default to offer     |
      | add player str d   | default is only valid for the set verb       |

  Scenario: Tab does nothing when no overlay is open
    Given the command field contains "set player str 9"
    When Tab is pressed
    Then the command field text is "set player str 9"

  Scenario Outline: Tab accepts the highlighted (default: first) candidate, filling it in and closing the overlay
    Given the command field contains "<typed>"
    When Tab is pressed
    Then the command field text is "<completed>"
    And the suggestion overlay is not showing

    Examples:
      | typed             | completed              |
      | sea                | search                 |
      | set p              | set player             |
      | set player s       | set player str         |
      | set player str d   | set player str default |

  Scenario: Live suggestions still refresh after accepting one, proving the field stays wired for further typing
    Given the command field contains "sea"
    When Tab is pressed
    And the command field contains "search c"
    Then the suggestion overlay is showing with candidates "Classes, core"

  Scenario: Enter accepts the highlighted candidate without executing the command
    Given the command field contains "sea"
    When Enter is pressed
    Then the command field text is "search"
    And the suggestion overlay is not showing
    And the transcript has no entries

  Scenario: Down moves the overlay's highlight to the next candidate instead of navigating history while it is open
    Given the command field contains "s"
    When Down is pressed
    And Tab is pressed
    Then the command field text is "set"

  Scenario: The overlay's highlight wraps from the last candidate back to the first
    Given the command field contains "s"
    When Down is pressed
    And Down is pressed
    And Down is pressed
    And Down is pressed
    And Tab is pressed
    Then the command field text is "search"

  Scenario: Escape closes the overlay without changing the field's text
    Given the command field contains "sea"
    When Escape is pressed
    Then the suggestion overlay is not showing
    And the command field text is "sea"

  Scenario: Recalling a history entry does not reopen an overlay for a recalled word that would otherwise have candidates
    Given the command "search classes" has been entered
    When Up is pressed
    Then the command field text is "search classes"
    And the suggestion overlay is not showing

  Scenario: Up recalls the most recently entered command
    Given the command "search classes" has been entered
    When Up is pressed
    Then the command field text is "search classes"

  Scenario: Repeated Up walks further back through history, most recent first
    Given the command "search classes" has been entered
    And the command "edit core:mage" has been entered
    When Up is pressed
    Then the command field text is "edit core:mage"
    When Up is pressed
    Then the command field text is "search classes"

  Scenario: Down past the newest history entry restores the in-progress draft
    Given the command "search classes" has been entered
    And the command field contains "unsent draft"
    When Up is pressed
    Then the command field text is "search classes"
    When Down is pressed
    Then the command field text is "unsent draft"

  Scenario: A zero-result command is still recorded in history
    Given the command "search zzz" has been entered
    When Up is pressed
    Then the command field text is "search zzz"

  Scenario: A blank command is not recorded in history
    Given the command "search classes" has been entered
    And the command "" is entered
    Then the command history size is 1

  Scenario: Adjacent duplicate commands are recorded only once
    Given the command "search classes" has been entered
    And the command "search classes" has been entered
    And the command "edit core:mage" has been entered
    Then the command history size is 2

  Scenario: A non-adjacent repeat is recorded again rather than reordering the earlier entry
    Given the command "search classes" has been entered
    And the command "edit core:mage" has been entered
    And the command "search classes" has been entered
    Then the command history size is 3
    When Up is pressed
    Then the command field text is "search classes"
    When Up is pressed
    Then the command field text is "edit core:mage"
    When Up is pressed
    Then the command field text is "search classes"

  # Non-goals:
  #   - The search/edit/set/add/subtract/snapshot/restore verbs' own behavior -
  #     #186, #170, #189, #144. This file only covers the completion/history
  #     contract those verbs register into.
  #   - Persisting history across process restarts.
  #   - Mouse/click interaction with the overlay - keyboard only (Up/Down/
  #     Tab/Enter/Escape), matching every other dev-console interaction.
  #
  # Risks:
  #   - Candidate ordering within the overlay is not pinned for the
  #     "opens live" scenarios beyond membership - "default: first" in the
  #     accept scenarios refers to whichever ordering the implementation
  #     produces (alphabetical, case-insensitive, per DevConsoleCompletion's
  #     existing dedupeAndSort), not a separately-specified order.
  #   - This file specs the overlay widget before it exists - exact pixel
  #     positioning/sizing is an implementation + visual-verification
  #     concern (docs/ui-verification.md), not pinned by these scenarios,
  #     which only assert candidate membership, highlight movement, and
  #     accept/dismiss behavior.
  #   - Suppressing live-filter re-triggering on the panel's own
  #     programmatic field updates (history recall, accept-fill) is an
  #     implementation necessity to make "Up/Down go to history when no
  #     overlay is open" and "accepting doesn't immediately reopen the
  #     overlay for its own result" both true - covered by the "Recalling a
  #     history entry closes any open overlay" scenario, but the exact
  #     mechanism (e.g. detaching/reattaching a DocumentListener around
  #     programmatic setText calls) is not pinned here.
  #
  # Clarifications (see specs/intent/dev-console-tab-completion-history.md
  # for full rationale):
  #   - Candidates render in a floating overlay above the command field
  #     (like Claude Code's own `/`-command menu), not a transcript line -
  #     supersedes this file's original (2026-09-08) transcript-line
  #     design.
  #   - The overlay live-filters on every keystroke, not just on Tab.
  #   - Tab and Enter are equivalent "accept the highlighted candidate"
  #     actions whenever the overlay is open; Enter only executes the
  #     typed command when no overlay is open.
  #   - The overlay always opens for one or more candidates, including
  #     exactly one - there is no more "auto-fill immediately, no overlay"
  #     special case for a single match.
  #   - set/add/subtract's entry-token argument is completion-enabled,
  #     scoped to entries with a field mutator, using the local-id form.
  #   - edit completes to full namespace:id; search completes to matching
  #     names/namespaces/categories - each verb completes to what its
  #     argument actually needs to resolve.
  #   - default is offered as a value-position candidate only for the set
  #     verb on a field with a class-default.
