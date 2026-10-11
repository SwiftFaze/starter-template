# Architecture: dev console

The dev-only, command-driven inspector framework in `sandbox/` and its
providers. Overview: [`architecture.md`](architecture.md).

**Dev console framework** (`sandbox/DevConsole*.java`): a pluggable
framework for dev-only inspectors. `DevConsolePanel` is a command-driven
interface — an append-only `TranscriptWidget` log (see `docs/ui-widgets.md`)
plus a single command field, not a live-filtered table. Every typed command is
echoed into the transcript as a dimmed `COMMAND` line before whatever it
produces. `DevConsoleCommandRunner` parses one typed line into a verb +
argument and dispatches it: `search <term>` filters registered provider
entries (each a namespaced id/category/name from `DevConsoleProvider.entries()`)
and writes a summary line plus a result table into the transcript; `edit
<namespace:id>` resolves an entry by its stable id and opens its detail panel
(via `DevConsoleProvider.createPanel(id)` — entries are addressed by id, not
display name, so two providers can't collide on a shared name). `set <entry>
<field> <value>`, `add <entry> <field> <value>`, and `subtract <entry> <field>
<value>` mutate a live entity's field directly from the command bar — entries
are addressed by either their full id or just the part after the namespace
(`player`, not `core:player`, via `DevConsoleModel.findByEntryToken`). A
provider opts in by returning a `DevConsoleFieldMutator` from
`DevConsoleProvider.fieldMutator(id)`; `PlayerSandboxProvider` (via
`PlayerFieldMutator`/`PlayerField`) and `ItemSandboxProvider` (via
`ItemFieldMutator`/`ItemField`, editing `mindmg`/`maxdmg` by swapping in an immutable
`Item.withBaseDamage` copy; min above max is rejected) do. Player edits reuse the same
field floors/clamping rules as `PlayerDetailPanel`'s arm+Left/Right editing.
Every mutation writes a SUCCESS or ERROR line to the transcript.
`regen [seed]` replaces the running game's world (`GamePanel.regenerateWorld`) via the
`WorldRegenerator` seam `Main` hands the panel: a bare `regen` draws a fresh random seed, a numeric
argument uses it, a non-number is an ERROR line, and a console with no game attached reports
`WorldRegenerator.NO_GAME_MESSAGE`. Success logs the seed and writes a SUCCESS line naming it. `reload`
re-reads `mods/` without restarting: `DevConsoleModel.reload()` calls each
provider's `DevConsoleProvider.reload()` (default no-op; mod-backed providers
override it), then rebuilds the entry list from every provider's `entries()`.
The new list replaces the old only if every provider succeeds, so a
`ModLoadException` leaves the previous entries in place and is reported as an
ERROR line. `snapshot <entry> <name>` and `restore <entry> <name>` save and
reload a named in-memory copy of an entity's state for the dev-console session;
a provider opts in by returning a `DevConsoleSnapshotter` from
`DevConsoleProvider.snapshotter(id)` (`PlayerSandboxProvider` does, via
`PlayerSnapshotter`, capturing the player's position and the ten editable
`Stats` fields). Re-snapshotting a name overwrites it; restoring an unknown
name writes an ERROR line. Anything else
writes a specific error line (unknown command, missing argument, or an id that
resolves to nothing) instead of running anything; a bare word alone is not a
search. Multiple providers can register entries together: `ClassSandboxProvider`
(each player class, opening `ClassDetailPanel`), `PlayerSandboxProvider` (the
running player, see below), `ItemSandboxProvider` (each mod-loaded item,
`Item.getDetailTables()` in `ItemDetailPanel`'s `DetailsPaneWidget`; base damage is editable
by command or by arming a Base Damage row with Enter and stepping it with Left/Right, each step
going through the same `ItemFieldMutator`),
`TileSandboxProvider` (see "Tiles provider" below; tile, quest and item panels are all an
`InspectableDetailPanel` - a `HeaderWidget` title over a standalone `DetailsPaneWidget`)
and `QuestSandboxProvider` (each mod-loaded quest, read-only:
`Quest.getDetailTables()` in a `DetailsPaneWidget`), and `KitchenSinkProvider`
(one "Kitchen Sink" entry opening `KitchenSinkPreviewPanel`: a live preview of
a `KitchenSinkScene` (`world/`, every tile in `ModRegistry.getAllTiles()` in a
10-wide grid, built through `KitchenSinkScene.holding(...)`) drawn through
`WorldScene.renderWorld`, a marker moved by the movement keys via
`KitchenSinkModel` (walkability from `WorldScene.isWalkable`), and a
`W`-toggled walkability tint behind the glyphs. The preview's Camera is fixed
and does not follow the marker). The framework is wired
into `Main.java` behind a dev-only system property gate: `mvn compile
exec:java -Dveil.devConsole=true` enables the F1 keybind to toggle a floating
dev console frame alongside the running game. The packaged/installer build
does not include the property, so players never see it.

**Completion and history**: the command field supports live-filtering Tab
completion, context-sensitive by argument position — position 0 completes
verb names, position 1 entry identifiers/search terms, positions 2-3 field
names and values for mutations. `DevConsoleCompletion.candidates(commandLine)`
returns matches for the trailing token on every keystroke;
`apply(commandLine, candidate)` fills the chosen one in. Candidates render in
`SuggestionOverlayWidget`, a floating popup above the command field (styled
like Claude Code's own `/`-command menu; see its own javadoc for why it's a
persistent `JLayeredPane` child rather than a `javax.swing.Popup`) — Up/Down
move its highlight, Tab/Enter accepts, Escape dismisses. With no overlay
open, Up/Down instead navigate `DevConsoleCommandHistory`: shell-style
adjacent dedup, draft restored past the newest entry. A provider's
`DevConsoleFieldMutator` exposes completable field tokens via `fieldTokens()`.
Programmatic field updates (history recall, accept-fill) detach/reattach the
live-filter `DocumentListener` around `setText` so only real typing filters.

**Class sandbox** (now part of the dev console): `ClassSandboxModel` wraps
`PlayerClassLoader.loadAll()` and exposes class names plus computed `Stats`
per class (via `PlayerClass.applyBaseStats`, no duplicated formulas);
`ClassDetailPanel` (a `JPanel` using `HeaderWidget` + `TableWidget`) shows
the selected class's stats at levels 0, 5, 10, 15 and 20 (one row per stat,
one column per level, via `ClassSandboxModel.computedStats(name, level)`)
with Up/Down navigation. Editing a class's JSON
and re-launching the sandbox picks up the change with no recompile, since
`PlayerClassLoader` reads the resource fresh on every `ClassSandboxModel`
construction — there is no static caching of loaded classes anywhere in
this path.

**Player sandbox** (live in-game editor): `PlayerSandboxProvider` holds a
`Supplier<Player>` (not a direct reference) to always read whichever player
is currently running in the game. `PlayerDetailPanel` shows that player's
editable stats (all ten base attributes, max/current HP/mana, and class), the
player's X/Y tile position (teleport, floored at 0), plus read-only derived
stats (attack power, defense), using the same `TableWidget` row-navigation
and Left/Right-to-adjust interaction pattern as `SettingsKeybindsPanel`. Editing a stat changes it on the live object
immediately; the game's next frame sees the change. Cycling the Class field
reapplies that class's level-0 base stats to the same player object
(via `PlayerInfo.setPlayerClass`). Since `PlayerSandboxProvider` uses a
supplier rather than holding a direct reference, it survives `GamePanel
.resetState()` — when that method replaces the player object with a fresh
one, the supplier returns the new object immediately, so edits stay attached
to the live game player even across a "New Game" restart without restarting
the dev console.

**Tiles provider**: `TileSandboxProvider` exposes every mod-loaded tile as
its own top-level entry (namespace from the id before `:`, category "Tiles")
opening a `DetailsPaneWidget` over the tile's `Inspectable` detail tables
(ID, symbol, color, walkable). It holds the tiles as `Inspectable`s and
deliberately does not import `com.swiftfaze.veil.world.Tile`: a `sandbox` ->
`world` edge would add a new path to the frozen ArchUnit cycle store. The
provider list is built in `Main.buildDevConsoleProviders` (F1 console) and
`ClassSandbox.providers()` (standalone sandbox); register new providers in
both.

