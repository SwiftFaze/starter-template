# Architecture: mod-loaded content

The world/tile model and every kind of content `ModLoader` loads into the
`ModRegistry` from `mods/` — tiles, buildings, player classes, items, quests.
The JSON contract itself is in [`mod-format.md`](mod-format.md). Overview:
[`architecture.md`](architecture.md).

**World representation** (`world/WorldScene.java`): an abstract base holding
a `Tile[width][height]` grid. The game's scene is `GeneratedWorldScene`
(`game/worldgen/`), built from a seed (see "Seeded world generation" below);
`GamePanel` builds one per new game. `Tile` (`world/Tile.java`) is a plain
glyph/color/walkability data class — no longer an enum — with instances
loaded from JSON (`mods/core/tiles/*.json`) via `ModLoader` into a
`ModRegistry`, keyed by namespaced ID (`core:grass`, etc.). `CoreTiles`
(`world/CoreTiles.java`) exposes those IDs as String constants for
production call sites (`registry.getTile(CoreTiles.GRASS)`) so they keep
some compile-time safety without `Tile` itself being an enum. Walkability
and rendering are entirely data-driven off these tile definitions, there's
no separate collision or sprite system. This is phase 2 of the
data-driven-mod-content initiative.

**Seeded world generation** (`game/worldgen/`, `world/PerlinNoise`, `TerrainBuckets`,
`WorldSeeds`): `new GeneratedWorldScene(width, height, seed)` derives the whole map
from one `long`. Terrain is `PerlinNoise` (the seed only shuffles its permutation
table) bucketed by `TerrainBuckets` into core tiles, water through ice.
`BuildingScatter` then places up to 5 registered blueprints on walkable ground without
overlap, keeping the tile outside each door (`DoorApproaches`) walkable and free.
`SpawnFinder` picks the walkable, building-free tile nearest the centre. The same seed
gives the same map. A test can pass `GeneratedWorldScene.Sources` to supply its own
registry and noise field. `WorldSeeds.fromSystemProperty` picks each new game's seed:
`-Dveil.worldSeed=<n>` if set, else random (a malformed value logs a warning and falls
back to random). `GamePanel` logs `World generated from seed <n>` at INFO.

**Buildings** are authored as JSON blueprints under `mods/core/buildings/`
(a flat 2D array of namespaced tile IDs — `{"id", "name", "type", "width",
"height", "tiles": [...]}`) and loaded by `ModLoader.load(modsRoot)` into a
`ModRegistry`, from which a `Building` (a `Tile[][]` blueprint plus a world
X/Y offset) is looked up by its namespaced `"id"` (e.g. `core:small_house_01`)
and stamped into a scene via `WorldScene.placeBuilding`. `ModLoader` reads
content from an external `mods/` directory (resolved relative to the JVM's
working directory) rather than the classpath — `core` is itself just a mod
living at `mods/core/`, loaded through the same path any third-party mod
would use. `ModLoader` makes two full passes over mods in dependency order:
first to load all tiles from `tiles/*.json` into a registry, then to load
all buildings from `buildings/*.json` with tile references resolved against
the tile registry. This is phase 2 of a larger data-driven-mod-content
initiative.

**Interactable objects** (`world/object/`): `InteractableObject` is the
abstraction for anything on a tile the player triggers with E (`Chest`,
`Door`); each new object *type* is a Java class, only instance data is JSON
(`mods/<mod>/objects/*.json`, read by `ObjectReader`). `ModLoader` loads
objects after items (chests reference items) and before buildings, which
place them via `Building.ObjectPlacement`; `WorldScene.placeBuilding` stamps
a fresh `newInstance()` per placement so state is never shared between
buildings. `GamePanel` checks the four neighbours N, E, S, W and calls the
first object's `interact`; a chest's `SHOW_CONTENTS` result is routed to the
UI through `GameListener.openChest`.

**Player RPG data** (`entities/player/`): `PlayerInfo` composes `Level`,
`Stats`, and a `PlayerClass`. `PlayerClass` is a plain data holder (name +
base stat values + optional per-level growth curves) loaded from JSON via
the mod system (`mods/core/classes/warrior.json`, `mage.json`, etc.), using
the same `ModLoader` and `ModRegistry` as tiles and buildings — see "World
representation" above. `CoreClasses` exposes class IDs as String constants
(`core:warrior`, `core:mage`) for production call sites. Class growth is
defined via `calc` expressions (plain arithmetic: `+ - * /`, parentheses,
the `level` variable, and numeric literals — no embedded scripting), validated
at load time against a stat registry (`mods/core/stats.json`), and exposed
via `PlayerClass.applyStatsAtLevel(stats, level)` to compute stat values at
any level (currently level 0 only in gameplay, since no level-up trigger
exists yet). `PlayerInfo` defaults new players to `core:warrior`. Only the
eight base-stat *values* and growth curves are data-driven — `Stats`' fields
and its derived `getAttackPower`/`getDefense` formulas stay plain Java. The
eight registered stats are `strength`, `dexterity`, `constitution`,
`intelligence`, `wisdom`, `luck`, `maxHp`, `maxMana`. This data isn't wired
into gameplay yet — it used to also feed a `PlayerInfoPanel` display,
removed along with the rest of the early UI shell (see `docs/screens.md`'s
"UI shell" note).

**Items** (`entities/items/Item.java`): a plain data holder (name, glyph,
type, slot, base damage min/max, and an `effects` list of `{type, stat,
calc}` entries) loaded from JSON under `mods/core/items/*.json` via the
same `ModLoader`/`ModRegistry` mechanism as tiles/buildings/classes. Only
`stat_bonus` is a supported effect type so far. `effects[].stat` is
validated at load time against the same stat registry
(`mods/core/stats.json`) classes use, and `effects[].calc` is parsed with
`CalcExpressionParser` for syntactic validity — but, unlike `PlayerClass`,
nothing evaluates an item's `calc` to a number yet, since no equip/
inventory-management system exists to consume it. `InventoryPanel` takes
`ModRegistry.getAllItems()`'s result via `showItems(List<Item>)` — that
wiring now happens in `Main.wirePopups` rather than `EastPanel`'s
constructor, removed along with the rest of the early UI shell (see
`docs/screens.md`'s "UI shell" note). This is phase 4 of the
data-driven-mod-content initiative.

**Quests** (`entities/quests/Quest.java`): a plain data holder (name, an
`objective` — `{type, target, count}`, fixed to `"kill"` this slice — and
a `rewards` list of `{type, id?, count?, calc?}` entries, `item` or `xp`)
loaded from JSON under `mods/core/quests/*.json` via the same
`ModLoader`/`ModRegistry` mechanism as tiles/buildings/classes/items.
`ModLoader` loads quests after items within the same mod-load pass, since
`rewards[].type: "item"` entries validate their `id` against the item
registry already populated earlier in that pass; an unresolved item ID
fails loading immediately, matching the tile/class/item unregistered-
reference pattern. `rewards[].type: "xp"` entries parse their `calc` with
`CalcExpressionParser` for syntactic validity only, same as items —
nothing evaluates it to a number yet, and no combat/monster system exists
to detect a `kill` objective being satisfied either. Minimal per-player
quest state (`entities/player/QuestLog.java`, an unvalidated
`Map<String, QuestLog.State>` of not-started/offered/active/complete,
defaulting unseen quest IDs to not-started) is composed onto `PlayerInfo`
alongside `stats`/`playerClass`, with no order validation on transitions
and no persistence across restarts — no game-save/progress system exists in
the project yet (distinct from the settings-only persistence in
`com.swiftfaze.veil.config`, see `docs/screens.md`'s "Settings persistence" —
per-installation config, not per-playthrough save data, different lifecycle).
This is phase 5 of the data-driven-mod-content initiative.

**NPCs** (`entities/npc/`): `NpcDefinition` (id, name, symbol, color, spawn tile, optional
`Patrol`) is immutable registry content loaded from `mods/<mod>/npcs/*.json` by the same
`ModLoader`/`ModRegistry` mechanism as the other types (`ModRegistry.getNpc` / `getAllNpcs`,
which returns definitions in load order). `ModLoader` loads NPCs last within a mod's pass,
validating each file against `npc.schema.json` and then checking what the schema cannot: a
patrol's first waypoint must equal `spawn`, and every leg (including last-to-first for `loop`)
must be a straight horizontal or vertical line. A failure throws `ModLoadException` naming the
NPC id and file; a malformed file does the same. `patrol.mode` is `ping_pong` (default) or
`loop` (`PatrolMode`). Definitions are not live entities: `Main` calls
`GamePanel.spawnNpcs(registry)` after building the game card and again after
`resetState()` (exit to main menu), which for each definition creates a fresh
`NonPlayerCharacter`, adds it to `entitiesToDraw`, and registers its `onTurn` with the
`TurnScheduler` after any earlier NPCs, so NPCs move in spawn order. Each turn an NPC takes
one step toward its current waypoint. **Blocked-step rule:** `GamePanel.canNpcEnter` allows a
step only onto a walkable tile holding neither the player nor another NPC; when the step is
refused the NPC stays put and retries the same step next turn, never skipping or
re-targeting a waypoint. An NPC with no patrol never moves. NPCs are not solid: they do not
block the player, and an NPC may share the player's tile. Talking to one is covered below.

**Dialogues** (`entities/dialogue/`, `mods/DialogueReader`): a `Dialogue` (id, start node id,
nodes) is registry content from `mods/<mod>/dialogues/*.json`. `ModLoader` loads every dialogue
across all mods before any NPC, so an NPC's optional `dialogue` id resolves to the `Dialogue`
held by its `NpcDefinition`; an unknown id is a `ModLoadException`. `DialogueReader` checks the
graph the schema cannot (see `mod-format.md`). A `Conversation` is one run through a dialogue:
the chat log and the current node. `GamePanel` builds a new one each time the player presses E
next to an NPC that has a dialogue (N, E, S, W order; on one tile an NPC beats an object; an NPC
without a dialogue is skipped) and fires `GameListener.openDialogue`. `DialoguePanel` (`ui/`)
shows it, and `DialoguePanelListener` holds `WorldPause.DIALOGUE` while it is open. The engine
builds the `Conversation` rather than the popup, because `ui/` may not construct engine classes
(`ModuleDependencyTest`).
