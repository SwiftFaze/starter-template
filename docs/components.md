# UI component contracts

General rules for how a Swing UI component (a list, a table, a details
pane, or a composite popup built from several of these) should receive
its data and report what the player does with it. These aren't specific
to any one panel — they apply whenever a new component is built or a new
domain type needs to show up in one.

## Rule 1 — a type describes itself; a component never hand-maps a type's shape

A domain type that needs to appear in a list or a details pane implements
a small, generic interface exposing its own display data. The component
rendering it only ever calls that interface — it never contains
type-specific mapping logic (a `switch` over types, a `toEntry(Foo)`
method per type, etc.). Adding a field to a type, or adding a whole new
type, should never require touching a UI component's code.

```java
public interface Identifiable {
    String getId();
    String getName();
}

public record DetailField(String label, String value) {}

public record DetailTable(String label, List<String> columnHeaders, List<List<String>> rows) {}

public interface DetailDescribable {
    List<DetailTable> getDetailTables();
}

public interface Inspectable extends Identifiable, DetailDescribable {}
```

- **`Identifiable`** — for anything that appears in a browsable list. A
  generic list widget accepts any `T` plus a name-renderer function;
  types implementing `Identifiable` let that default to
  `Identifiable::getName` instead of every call site writing its own
  lambda.
- **`DetailDescribable`** — for anything with a details pane, list or no
  list. A type with exactly one instance in the game (e.g. the player's
  own stats) can implement this alone, with nothing to browse.
- **`DetailTable`** — a details pane can show more than one table (e.g.
  base fields plus a second table of effects/bonuses); this shape
  doesn't hardcode how many or what a given type's second table means.
  Its `label` renders as a heading above the table when non-blank (e.g.
  "Effects"); leave it `""` for a table that needs no heading (see
  "Resolved" below for why this lives on the record instead of a
  widget-side convention).
- **`Inspectable`** — convenience union for a type that's both listable
  and detail-renderable. Not required — a type can implement just one of
  the two — but useful as a single generic bound for a widget that wants
  both (`ListWidget<T extends Inspectable>`).

## Rule 2 — the contract stays data-only

No Swing (or other UI-framework) type may appear in any of these
signatures, and a method's output may not vary depending on which
component is asking. If two consumers want to display the same type
differently, that's a sign a field or a narrower method belongs on the
type's own implementation — not that a component should hand-map it
"just this once."

## Rule 3 — opt-in, not universal

Not every domain object is list/detail-shaped. A type that exists only
to be placed or computed (a world instance holding its own x/y, say)
implements neither interface. Don't force every object a registry holds
through this contract just because some of them fit it.

## Rule 4 — internal state stays internal; only cross-component signals leave a widget

A component's own presentation state — which row is currently
highlighted, scroll position, and so on — never needs to leave the
component; nothing outside it should need to know. A component should
expose an explicit output (a callback, an event) only for the signals a
*different* component genuinely needs:

- **A sibling needs to live-preview the current selection** (e.g. a
  details pane that updates as you scroll a list, before committing to
  anything) → the list fires a "selection changed" callback on every
  move. This is a deliberate, continuous signal — only wire it when that
  live-preview behavior is actually wanted, not by default.
- **A sibling needs to take over keyboard ownership** (e.g. moving from
  a list into its details pane) → this is a structural, discrete
  transition, not a stream of updates. It's usually cleanest as a
  template-method hook on the composite (the popup/panel) that a
  specific direction key triggers, rather than a payload-carrying event
  from the child widget itself.

Don't wire an output "just in case a future consumer might want it" —
that's exactly how a component ends up doing another component's job.

## Rule 5 — prefer reading state at the moment it's needed over subscribing to it

If a sibling only needs a component's current state at one discrete
trigger moment (e.g. "what's selected right now, at the instant the
player commits to it"), read it directly then
(`someWidget.getSelectedItem()`) rather than maintaining a live
subscription that fires on every intermediate change nobody asked for.
Reserve the continuous-callback form (Rule 4's first bullet) for cases
where the intermediate values themselves are the point, like true live
preview.

---

## Worked example: Codex and Inventory's details pane

This is where the rules above came from, kept here as a concrete
illustration rather than the subject of the doc.

`InventoryPanel` and `CodexPanel` each hand-wrote their own `FieldRow`
record, `fields`/`effects` `TableWidget` setup, `updateDetails`, and all
Up/Down/Left/Right focus-navigation logic — nothing shared, which is
exactly how `CodexPanel` ended up missing a focus state entirely (its
Effects table renders but was keyboard-unreachable). The per-type field
mapping (`CodexPanel.toEntry(Item|Tile|PlayerClass)`,
`InventoryPanel.fieldRows(Item)`) was hand-written in both panels too —
this is Rule 1's violation directly: `Item`, `Tile`, `PlayerClass` should
each implement `Inspectable` and own their own `getDetailTables()`,
replacing those methods. This isn't a one-off fix either — `ModRegistry`
already tracks six mod-loaded catalog types (`Building`, `Tile`,
`PlayerClass`, `Item`, `Quest`, `WidgetColorTheme`), of which `Quest`
already has no Codex tab at all, and future types (Biome) will hit the
same gap. `Building` is Rule 3's counter-example — it's a placed world
instance (`worldX`/`worldY` + a blueprint), not a catalog entry, and
implements neither interface.

This isn't a new idea for this codebase, either — `Positionable` →
`DrawableAsciiEntity` (see `architecture-engine.md`'s "Rendering contracts")
already applies Rule 1 to world rendering, where `GamePanel` iterates
`entitiesToDraw` generically instead of switching on entity type.

**Rules 4/5 checked against the actual widgets** (not assumed):

- `ListWidget.moveUp()`/`moveDown()` already fire `onSelectionChange(T)`
  on every move (`ListWidget.java:144`) — this is Rule 4's live-preview
  case, confirmed as the intended, already-shipped behavior: the details
  pane updates as you scroll the list, before you commit to anything.
- `TableWidget` has `onConfirm` but no `onSelectionChange` — an existing
  asymmetry with `ListWidget`, worth resolving when the shared details
  widget is built (should it gain parity, or is "only fires on confirm"
  intentional for tables?).
- Left/Right (moving from list into details, or between detail tables)
  is Rule 4's structural-transition case: `PopupWidget.onUp/onDown/
  onLeft/onRight` (`PopupWidget.java:75-85`) are empty template-method
  hooks a subclass overrides directly, not a payload-carrying event —
  `CodexPanel`/`InventoryPanel` each mutate their own private `Focus`
  field in response. `ListWidget` itself has no Left/Right key bindings
  at all. A shared details-pane widget should own this coordination once
  (keyed off however many `DetailTable`s the current `DetailDescribable`
  returned), instead of each popup re-deriving its own `Focus` enum.

**Resolved:**

- `TableWidget` does not gain `onSelectionChange`. Nothing in this case
  needs live-preview *within* a table (unlike `ListWidget`, where the
  details pane needs to update as the list scrolls) — adding it would be
  wiring an output "just in case a future consumer might want it," which
  Rule 4 already warns against. The asymmetry with `ListWidget` is
  intentional, not a gap.
- `DetailTable` carries its own `label` as data, not one the widget
  hardcodes by position (e.g. "table 0 is unlabeled, table 1+ is
  labeled"). A widget-side positional convention is Rule 1's violation
  moved up one level, and breaks the moment a type doesn't fit the
  two-table shape — `PlayerClass` has only one table, and a future type
  could need three, or a labeled first table. Each type states its own
  label per table instead; `""` renders no heading, matching
  `InventoryPanel`'s current unlabeled Fields table.
