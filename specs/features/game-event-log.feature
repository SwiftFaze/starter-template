Feature: Game event log records what the game did
  A QA replay (qa-key-replay.feature) needs to know what the game *did*, not
  just what it drew. `GameListener` only carries UI callbacks
  (`updatePlayer`, `toggleInventory`, ...), so nothing records domain
  transitions.

  `com.swiftfaze.veil.game.event.GameEvent` is a sealed interface of records,
  and `GameEventLog` is an in-memory, append-only list of them, injected
  where each transition happens. It is a no-op by default. Starting the game
  with `-Dveil.qaLog=<path>` enables it: each event is written and flushed to
  that file as one JSON line (Gson) the moment it is appended, so a crashed
  or killed run still leaves a usable partial log. The types live in the engine (`game.event`), so `Main` and
  `ui` may depend on them and ArchUnit's direction holds.

  The initial event set is only what the first QA procedures need (main
  menu, map movement, inventory toggle):
    - `ScreenChanged(from, to)`: a `Main` card switch (`title`, `settings`,
      `keybinds`, `game`)
    - `MenuSelectionChanged(to)`: the title-screen menu's selection moves
    - `PlayerMoved(x, y)`: the player's position actually changes
    - `PopupToggled(name, open)`: the inventory popup opens or closes

  QA: none - no keyboard input of its own; recorded through the key-driven procedures
  Covers: the event types, when each fires, the no-op default, and the
  `-Dveil.qaLog` file.
  Supersedes: nothing. `GameListener` keeps its UI callbacks.
  Out of scope: a `GameOver` event (there is no game-over state yet), and any
  event no QA procedure needs yet. Events are added with the procedure that
  needs them, never in advance.

  Background:
    Given an enabled game event log

  Scenario: Starting a new game from the title screen records one screen change
    Given the title screen is showing
    When the player chooses "New Game"
    Then the log contains exactly one `ScreenChanged` from "title" to "game"

  Scenario: Moving the title-screen menu selection records each move
    Given the title screen is showing with the first option selected
    When the player presses Down twice
    Then the log contains two `MenuSelectionChanged` events, in order, naming the second and third options

  Scenario: A successful move records the new position once
    Given the player is at (5, 5) on an open floor
    When the player presses the move-right key
    Then the log contains exactly one `PlayerMoved` at (6, 5)

  Scenario: A blocked move records nothing
    Given the player is at (5, 5) with a wall to the right
    When the player presses the move-right key
    Then the log contains no `PlayerMoved` event

  Scenario: Toggling the inventory records open then closed
    Given the game screen is showing with the inventory closed
    When the player presses the inventory key twice
    Then the log contains `PopupToggled` "inventory" open, then `PopupToggled` "inventory" closed

  Scenario: The log is append-only
    Given a log that has recorded two events
    Then its events cannot be removed or replaced through the log's API

  Scenario: The log does nothing unless enabled
    Given the game starts without `-Dveil.qaLog`
    When the player moves and toggles the inventory
    Then no events are kept and no log file is written

  Scenario: `-Dveil.qaLog` writes each event as it happens
    Given the game starts with `-Dveil.qaLog` pointing at a file
    When the player moves right once
    Then the file already has one line for that `PlayerMoved` before the game exits
    And the line is a JSON object with its event type, `x` and `y`

  Scenario: A killed run still leaves the events recorded so far
    Given the game starts with `-Dveil.qaLog` pointing at a file
    And the player has moved right twice
    When the game process is killed without a clean shutdown
    Then the file holds both `PlayerMoved` lines

  Scenario: An unwritable log path fails loudly
    Given `-Dveil.qaLog` points at a directory that does not exist
    When the first event is recorded
    Then the game reports an error naming the path
    And the game itself keeps running

  # Non-goals:
  #   - Replacing GameListener, or routing UI updates through the log.
  #   - Persisting the log in normal play. It exists for QA runs.
  #   - Events for codex, pause, dev console or settings changes until a QA
  #     procedure needs them.
  #
  # Risks:
  #   - Injection points span Main (card switches), the title screen (menu),
  #     GamePanel/Player (movement) and ui.PopupToggleListener (inventory).
  #     A missed call site means a transition the log silently doesn't see.
  #     The Step 4.5 playtest should confirm menu, movement and inventory
  #     behave the same with and without -Dveil.qaLog.
  #   - "Exactly once" matters: a transition that fires both a key action
  #     and a listener callback could double-log.
  #
  #   - Writing on every append means file I/O on the EDT per event. That's
  #     acceptable at key-press rates and only with -Dveil.qaLog set.
  #
  # Open questions:
  #   - None. Writes happen on every append (intent, Clarifications).
