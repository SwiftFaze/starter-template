# Mod Format Reference

The mod format is an explicit JSON contract, with one schema per file type committed under `docs/schemas/`. This document explains the overall structure and validates your mod files against the schemas.

## Directory Structure

A mod is a directory under `mods/<mod-id>/` with this structure:

```
mods/<mod-id>/
├── mod.json              # Manifest (required) — mod id and dependencies
├── stats.json            # Stat registry (core mod only) — stat names available to classes and items
├── tiles/                # Tile definitions (optional)
│   ├── grass.json
│   ├── stone.json
│   └── ...
├── buildings/            # Building blueprints (optional)
├── classes/              # Player class definitions (optional)
├── items/                # Item definitions (optional)
├── objects/              # Interactable world objects: chests, doors (optional)
├── quests/               # Quest definitions (optional)
├── npcs/                 # NPC definitions, with optional patrol routes and dialogue (optional)
├── dialogues/            # Branching NPC conversations (optional)
└── themes/               # UI color themes (optional)
```

## File Types and Schemas

Each file type has a corresponding JSON Schema under `docs/schemas/`:

- **`mod.json`** — [`mod.schema.json`](../schemas/mod.schema.json): The mod manifest
- **`stats.json`** — [`stats.schema.json`](../schemas/stats.schema.json): Stat registry (for core mod only)
- **Tile files** — [`tile.schema.json`](../schemas/tile.schema.json): Individual tile definitions under `tiles/`
- **Building files** — [`building.schema.json`](../schemas/building.schema.json): Building blueprints under `buildings/`
- **Class files** — [`class.schema.json`](../schemas/class.schema.json): Player class definitions under `classes/`
- **Object files** — [`object.schema.json`](../schemas/object.schema.json): Chests (`"type": "chest"`) and doors (`"type": "door"`) under `objects/`; a building places them through its `"objects"` list of `{"id", "x", "y"}`
- **Item files** — [`item.schema.json`](../schemas/item.schema.json): Item definitions under `items/`
- **Quest files** — [`quest.schema.json`](../schemas/quest.schema.json): Quest definitions under `quests/`
- **NPC files** — [`npc.schema.json`](../schemas/npc.schema.json): NPC definitions under `npcs/`. An optional `"dialogue"` names a dialogue id from `dialogues/`. A `patrol` is `ping_pong` (default) or `loop` over orthogonal legs; the first waypoint must equal `spawn`. Loader-side checks beyond the schema (leg shape, spawn match) fail the mod load with the NPC id and file.
- **Dialogue files** — [`dialogue.schema.json`](../schemas/dialogue.schema.json): Branching conversations under `dialogues/`, as `{"id", "start", "nodes": {<node id>: {"text", "choices": [{"text", "next"?}]}}}`. A choice with no `next` ends the conversation. An NPC opts in with `"dialogue": "<dialogue id>"`. The loader also rejects, naming the dialogue, its file, and the node or choice: a `start` that is not a node, a `next` that is not a node, a node with no choices or more than 9, blank text, and a node the start cannot reach. An NPC naming an unknown dialogue id fails the load too.
- **Theme files** — [`theme.schema.json`](../schemas/theme.schema.json): UI color themes under `themes/`

Read the schema files directly for the authoritative field-by-field specification. This document explains concepts that span multiple schemas.

## Namespaced IDs

Content IDs (for tiles, buildings, classes, items, quests, and themes) follow a namespaced format:

```
<mod-id>:<content-name>
```

Examples:
- `core:grass` — a tile from the core mod
- `core:warrior` — a class from the core mod
- `expanded-quests:dragon-slayer` — a quest from an expanded-quests mod

Tile, building, class, item, quest, and theme IDs must match the pattern `^[a-z0-9][a-z0-9_-]*:[a-z0-9][a-z0-9_-]*$`. The mod ID itself (in `mod.json`) is not namespaced.

## The `overrides` Field

Every content type (tiles, buildings, classes, items, quests, themes) supports an optional `overrides` field:

```json
{
  "id": "mymod:my_tile",
  "overrides": "core:some_tile",
  ...
}
```

If present, this content will replace another content with the same ID from a loaded mod. Without `overrides`, registering a content ID that already exists will fail with a collision error. `overrides` is a string holding the ID being overridden.

## Validation and Error Messages

When a mod file fails validation, you'll see a field-level error message naming:
- The file path
- The offending field or path (e.g., `/colors/BORDER/r`)
- What was expected (type, required, pattern, etc.)
- What was found

Example:

```
Failed to load tile from file: mods/mymod/tiles/grass.json
  /walkable - required by schema
  /color - expected object, but found string
```

**Unknown fields are rejected.** A typo in a field name (e.g., `overides` instead of `overrides`) will cause an error. Refer to the schema for the exact field names.

## Validation During Mod Loading

Mod files are validated against their schemas during `ModLoader.load()`, which is called whenever:
- The game starts (dev, test, or shipped)
- A test calls `ModLoader.load(modsRoot)`
- Cucumber scenarios exercise mod loading

Validation is not optional — if a file violates its schema, loading fails with a diagnostic error, not a silent no-op.

## Additional Resources

- [`docs/README.md`](README.md) — main documentation index
- [`docs/architecture.md`](architecture.md) — engine and mod registry design
