# UI styling rules

Concrete layout, typography, and color rules for every Swing panel/widget in
`ui/`, `ui/widget/`, and `sandbox/` — the dev-console/class-sandbox tooling
follows the same rules as player-facing screens, no separate "editor" style.
`ui-widgets.md` covers what the widget library and theming system *are*;
this doc covers the concrete numbers a new panel or widget should use so
spacing/type/color don't drift screen to screen. Not mechanically enforced
(no PMD/ArchUnit rule backs any of this, same caveat as SLAP in
`.claude/workflow.md`) — self-applied the same way, checked at review time
by eye.

## Layout & spacing — "grid style, components don't touch"

Three fixed sizes, not an open palette:

| Token | Value | Use |
|---|---|---|
| Outer padding | `10px` all sides | Gap between a top-level view's own border and its content — the space around the *edge* of a panel/popup/dev-console window. |
| Component gap | `8px` | Gap between two sibling components stacked or grid-arranged inside a view (e.g. a header sitting above a table, a table sitting above a search field). Components are laid out so this gap always exists — never flush/touching, and never left to default `BoxLayout`/`BorderLayout` zero-gap behavior. |
| Internal padding | `4px` vertical / `8px` horizontal | Padding *inside* one component, between its border and its own content (a table cell's text, a row label) — smaller than the component gap since it's framing a single piece of content, not separating two components. |

These match what's already in place at the two components built together
most recently: `DevConsolePanel`'s outer `BorderFactory.createEmptyBorder(10,
10, 10, 10)` (see `sandbox/DevConsolePanel.java`) and `TableWidget`'s per-cell
`BorderFactory.createEmptyBorder(4, 8, 4, 8)` (`widget/TableWidget.java`).
Component gap is most often realized as a margin baked into the upper
component's own border rather than a spacer between them — see
`HeaderWidget`'s `BOTTOM_MARGIN`, which gives any consumer stacking content
below it in a `BoxLayout` an 8px gap for free without the consumer having to
add its own spacing. Prefer that pattern (the component owns its own
trailing margin) over ad hoc `Box.createVerticalStrut(8)` calls scattered
across consumers.

```java
// Outer padding on a top-level view:
setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

// Internal padding on a single component's content (e.g. a table cell):
BorderFactory.createEmptyBorder(4, 8, 4, 8);
```

A handful of pre-existing screens predate this doc and don't match it
exactly — `SettingsScreenPanel`/`SettingsKeybindsPanel` use `20, 40, 20, 40`
outer padding around their bordered content panel, and per-row padding
varies between `ListWidget` (`2, 4, 2, 4`) and `CodexPanel` rows
(`6, 8, 6, 8`); `SettingsScreenPanel` and `SettingsKeybindsPanel` rows both
use `2, 8, 2, 8` (a UI-compliance audit found the two screens had quietly
drifted to different row-padding values and re-aligned them). That's
existing debt, not a second valid convention — new components use the
values above; retrofitting the older screens to match is optional cleanup,
not required by this doc.

`ListDetailLayoutUtility`'s section-label border (`10, 0, 4, 0`) and
details-panel border (`4, 10, 0, 0`) are asymmetric on purpose, not a drift
from the internal-padding standard above — like `HeaderWidget`'s
`BOTTOM_MARGIN`, they're a component owning its own one-sided spacing
against a neighbor (space above a section label, space left of a details
divider) rather than padding framing a single component's own content on
all sides. Same pattern, just not yet pulled into a named constant.
`DetailsPaneWidget.standalone()` (the dev-console sandbox panels, which have no
list beside them) drops both the divider and that padding, so its tables sit
flush under the panel's header like `ClassDetailPanel`'s; Codex and Inventory
use the default constructor, which keeps both. A `BoxLayout` column must give
every child the same `alignmentX` (`HeaderWidget` and `TableWidget` are
`LEFT_ALIGNMENT`): mixed alignments offset the children against each other. Details tables are capped at their
own rows' height, so a table's left border never runs on into empty space.

## Typography

Everything renders in `Font.MONOSPACED` — no other font family is used
anywhere in `ui/`. Five sizes cover every existing use; a new component
picks one of these rather than a bespoke size:

| Role | Size | Weight | Existing example |
|---|---|---|---|
| Display (h1, rare — full-screen hero text only) | `48pt` | Plain | `TitleScreenPanel`'s title art |
| Page header (h1, a screen's own heading) | `24pt` | Bold | `SettingsScreenPanel`/`SettingsKeybindsPanel` header label |
| Section header (h2, a component's own title bar) | `20pt` | Bold | `HeaderWidget` (`sandbox/ClassDetailPanel`'s class-name heading, `sandbox/DevConsolePanel`'s "Dev Console" title) |
| Body (p, default text — list/table rows, buttons, field input) | `16pt` | Plain | `TableWidget`, `ListWidget`, `ButtonWidget`, `RadioGroupWidget`, `PatternFieldWidget`'s input text |
| Small (captions, hint bars, compact popups) | `14pt` | Plain | `ControlsHintBarWidget`, `CompactPopupWidget` |
| Micro (inline field labels only) | `12pt` | Plain | `PatternFieldWidget`'s floating label |

Body (`16pt` plain) is the default for anything that isn't explicitly a
header or a caption — when in doubt, use it rather than introducing a new
size.

## Color

Full rationale for the theming system (the 11 keys, `WidgetColorTheme`,
`ModLoader.loadThemes`, `applyTheme`) lives in `ui-widgets.md`'s "Widget
theming" section — this is the short version as a rule to follow:

- **Never introduce a hardcoded `java.awt.Color` literal** in `ui/`,
  `ui/widget/`, or `sandbox/`. Every color a component needs must resolve
  to one of `WidgetTheme`'s theme keys (`SELECTED_HIGHLIGHT`,
  `SELECTED_TEXT`, `NORMAL_TEXT`, `DIMMED_TEXT`, `BACKGROUND`,
  `INVALID_HIGHLIGHT`, `VALID_HIGHLIGHT`, `TABLE_HEADER_BACKGROUND`,
  `BORDER`, `SCROLLBAR_THUMB`, `ACCENT`, `WINDOW_BORDER`), read through its
  accessor (`SELECTED_HIGHLIGHT` → `WidgetTheme.selectedHighlight()`). Gameplay/world rendering
  (`Player`, `WorldScene`, `GamePanel`) is exempt — that's game content, not
  UI chrome.
- **Check for an existing fit before adding a key.** Most new UI needs a
  role this list already names — a panel border is `BORDER`, the outermost
  window/frame edge is `WINDOW_BORDER` (kept visually distinct from `BORDER`
  so the true window edge stays bright regardless of what internal panel
  borders are themed to), a validation-state color is
  `INVALID_HIGHLIGHT`/`VALID_HIGHLIGHT`, a highlight that isn't row selection
  is `ACCENT`. Reach for the existing key whose *semantic role* matches, not
  whichever one happens to look close enough.
- **A new key is only justified when both hold:** it's a genuinely distinct
  semantic role none of the 11 existing keys cover, *and* it will be reused
  across more than one widget/screen — not a one-off literal needed by a
  single component. The theme is a small, curated set of roles, not a
  swatch book; resist adding `SOME_PANEL_SPECIFIC_BLUE` for one caller.
- **Adding a key touches three places in the same change:** the `Palette`
  component, its accessor and its `applyTheme` mapping in `WidgetTheme.java`, the key's name
  in `WidgetColorTheme.REQUIRED_KEYS`, and the actual color value in
  `mods/core/themes/default.json`. A theme missing a required key throws
  `ModLoadException` at load time, so these three can't drift out of sync.
