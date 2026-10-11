# Architecture

Veil is a 2D ASCII-tile desktop RPG built with Java 17 Swing (no game
engine). Rendering draws Unicode/ASCII glyphs with `Graphics2D.drawString`
onto a `JPanel`.

This file covers the game engine and data model. UI is documented
separately: `docs/ui-widgets.md` covers the reusable Swing widget
framework and theming, `docs/screens.md` covers how those widgets
compose into the game's actual screens, and `docs/components.md` covers
the self-describing list/detail data contract screens use to feed those
widgets. See `docs/README.md` for the full doc index.

This engine/widgets/screens layering is mechanically enforced, not just
documented: `ModuleDependencyTest` (ArchUnit) fails the build if engine
code depends on the UI layer, or if a widget depends on a screen. See
`docs/testing-module-dependency.md` for the exact rule and why `Main` and
`sandbox` are excluded.

## Where to read next

This file is only the overview. Each area has its own doc — add new detail
there, not here:

- [`architecture-engine.md`](architecture-engine.md) — entry point/window
  assembly, `GamePanel` and `Camera`, player movement, rendering contracts
  (`DrawableAsciiEntity`), keyboard input, the game event log, `GameConst`.
- [`architecture-mod-content.md`](architecture-mod-content.md) — the
  `WorldScene`/`Tile` world model and mod-loaded content via
  `ModLoader`/`ModRegistry`: tiles, buildings, player classes/stats, items,
  quests.
- [`architecture-dev-console.md`](architecture-dev-console.md) — the F1 dev
  console (`-Dveil.devConsole=true`): command runner, completion/history, and
  each `DevConsoleProvider` (class sandbox, player sandbox, ...).

## Package map

Under `com.swiftfaze.veil`:

- `Main`, `ui/`, `component/` — window assembly, Swing screens/widgets, and the
  list/detail data contract (UI layer).
- `world/`, `entities/`, `mods/`, `input/`, `game/`, `config/`, `exceptions/`,
  `render/` (`Camera`, `DrawableAsciiEntity`, `Positionable`; depends on
  nothing else in the project) and the root `GameConst` — the engine; never
  depends on the UI layer.
- `sandbox/` — dev-only tooling, excluded from the layering rule.
