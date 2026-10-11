@manual-verification
Feature: QA key-replay procedures check key-driven behaviour in the real window
  The Step 4.5 human playtest is fully manual. Veilclj's `bb qa <slug>`
  replays a key script in the real window and checks the game event log
  (game-event-log.feature) against expectations, so the human only has to
  judge feel and rendering.

  Each procedure is two files:
    - `specs/qa/<slug>.keys`: Veilclj's format, one key per line, `#`
      comments.
    - `specs/qa/<slug>.json`: `{"script": …, "expect": [{event…}, …]}`.

  `com.swiftfaze.veil.testing.qa.QaRunner` (test scope) boots the game
  through `Main`'s startup wiring with the event log enabled, waits for focus
  the same way `GamePanelRealKeyEventTest.waitForFocus` does, and dispatches
  real `KeyEvent`s through the focused component, so input goes through
  Swing's focus machinery. It then checks that `expect` appears in the log as
  an in-order subsequence.

  Run with `mvn -q test-compile exec:java -Dexec.classpathScope=test
  -Dexec.mainClass=…QaRunner "-Dexec.args=<slug>"` or `--all` (see
  `docs/testing.md`, "QA runs"). It opens a window, so it is local-only: not
  in CI and not in `check-clean.sh`. That is why this file is
  `@manual-verification`: the runner is its own check, verified by running it.
  The parsing and matching classes (`KeyScript`, `QaProcedure`,
  `EventMatcher`) have unit tests.

  Each key is dispatched as a KEY_PRESSED then a KEY_RELEASED event to the
  current focus owner, re-read per key, with the event queue drained between
  keys. `main` ends in `System.exit` because Swing's non-daemon event thread
  would otherwise keep `exec:java` alive forever.

  Coordinates in the scenarios below, such as (6, 5), are illustrative. The
  procedures use the real start position (125, 125), so `map-movement`
  expects (126, 125) then (127, 125).

  First procedures: `main-menu`, `map-movement`, `inventory-toggle`.

  QA: none - this is the QA tooling itself, verified by running QaRunner
  Covers: the procedure file format, the runner, the pass/fail rule, the
  `QA: none - <reason>` opt-out, and the QA step in `implement-issue`.
  Supersedes: nothing. It narrows the Step 4.5 human playtest to feel and
  rendering but does not replace it.
  Out of scope: running in CI or headless, pixel or rendering checks, and
  generating procedures from `.feature` files.

  Scenario: A procedure whose expected events all occur passes
    Given the `map-movement` procedure presses Right twice and expects `PlayerMoved` at (6, 5) then (7, 5)
    When QaRunner runs `map-movement`
    Then it reports the procedure as passed

  Scenario: Extra events between expected ones do not fail a procedure
    Given a procedure that expects `ScreenChanged` "title" to "game" then `PlayerMoved` at (6, 5)
    And the real run also records `MenuSelectionChanged` events in between
    When QaRunner runs it
    Then it reports the procedure as passed

  Scenario: Expected events out of order fail a procedure
    Given a procedure that expects `PlayerMoved` at (7, 5) before `PlayerMoved` at (6, 5)
    When QaRunner runs it
    Then it reports the procedure as failed

  Scenario: A wrong expectation fails with a readable diff
    Given the `map-movement` procedure's expectation is changed to `PlayerMoved` at (9, 9)
    When QaRunner runs `map-movement`
    Then it reports the procedure as failed
    And the output shows the first expected event that was not matched and the events actually recorded after the last match

  Scenario: `--all` runs every procedure and fails if any fails
    Given procedures `main-menu`, `map-movement` and `inventory-toggle`, where one has a wrong expectation
    When QaRunner runs with `--all`
    Then it runs all three
    And it reports one pass/fail line per procedure
    And it exits non-zero

  Scenario: Comments and blank lines in a key script are ignored
    Given a `.keys` file with `#` comment lines and blank lines between key lines
    When QaRunner runs it
    Then only the key lines are dispatched

  Scenario: An unknown key name fails before the window opens
    Given a `.keys` file containing the line "NOT_A_KEY"
    When QaRunner runs it
    Then it fails, naming the file, the line number and the unknown key
    And no game window is opened

  Scenario: A procedure missing either file fails
    Given `specs/qa/foo.keys` exists but `specs/qa/foo.json` does not
    When QaRunner runs `foo`
    Then it fails, naming the missing file

  Scenario: The window never gaining focus fails instead of hanging
    Given the game window does not gain focus within the focus timeout
    When QaRunner runs a procedure
    Then it fails, saying focus was never gained
    And it does not dispatch any keys

  Scenario: A feature with no key-driven behaviour opts out
    Given a `.feature` file whose `Feature:` block contains `QA: none - no keyboard input`
    When the implement-issue QA step checks for a procedure
    Then it accepts the opt-out and names the reason

  Scenario: A changed feature with neither a procedure nor an opt-out blocks the handoff
    Given a changed `.feature` file with no `specs/qa/<slug>.keys` and no `QA: none` line
    When the implement-issue QA step runs
    Then it stops before the hardener handoff
    And it names the feature and asks for a procedure or a `QA: none - <reason>` line

  # Non-goals:
  #   - Replacing the human playtest. It still judges feel and rendering.
  #   - Asserting on what was drawn. Only the event log is checked.
  #   - Exact-sequence matching. In-order subsequence is deliberate, so a
  #     new event type doesn't break every existing procedure.
  #
  # Risks:
  #   - Focus is the known failure mode for real key events on this
  #     platform (fix/window-focus-on-launch). The focus timeout must fail
  #     loudly, never pass with zero keys sent.
  #   - Key timing: dispatching faster than the game processes input could
  #     drop or merge moves. The runner may need to wait for the event
  #     queue to drain between keys.
  #
  # Open questions:
  #   - None. A missing procedure with no opt-out blocks the handoff (intent,
  #     Clarifications).
