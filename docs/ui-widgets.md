# UI widget framework

The reusable Swing widget library in `ui/widget/`, and the theming system
that colors it. For how these compose into actual game screens, see
`docs/screens.md`; for the self-describing data contract screens use to
feed list/detail widgets, see `docs/components.md`; for the concrete
spacing/typography/color rules a panel or widget should follow, see
`docs/ui-styling.md`.

**Widget theming** (`mods/<modid>/themes/*.json`): a directory of files, one
theme per file, matching the `tiles/`/`items/`/`quests/` directory-of-many-files
convention (not the `stats.json` singleton) — each file defining all 13 colors
`WidgetTheme` (see below) exposes through accessors (`SELECTED_HIGHLIGHT` →
`WidgetTheme.selectedHighlight()`, and so on): `SELECTED_HIGHLIGHT`,
`SELECTED_TEXT`, `NORMAL_TEXT`, `DIMMED_TEXT`, `BACKGROUND`, `INVALID_HIGHLIGHT`,
`VALID_HIGHLIGHT`, `TABLE_HEADER_BACKGROUND`, `BORDER`, `SCROLLBAR_THUMB`,
`ACCENT`, `WINDOW_BORDER`, `TABLE_HEADER_TEXT` (`WidgetColorTheme.REQUIRED_KEYS`), each an `{r, g, b}` object using
the same color shape tiles already use (`ModLoader.readColor`). `BORDER` was
named `TABLE_BORDER` until the UI color-cleanup sweep below broadened its use
well beyond tables (every panel border in `ui/`) — the old name was misleading
once `CompactPopupWidget`/`RadioGroupWidget` and then general panel chrome all
started reusing it, so it was renamed to the general-purpose `BORDER`. `ACCENT`
(`#eeb392`) was added in that same sweep to capture a hardcoded leftover: it's
the widget library's original `SELECTED_HIGHLIGHT` color from before an earlier
commit switched selection highlighting to neutral gray, and `NorthPanel`'s
title and `ClassSandboxPanel`'s selected-row color still used that literal
directly. `WINDOW_BORDER` (white by default) was added when a UI-compliance
audit caught `Main.java`'s frame content-pane border and
`sandbox/DevConsolePanel`'s own border both hardcoding `Color.WHITE` instead
of resolving to a theme key — a distinct semantic role from `BORDER` (used
for internal panel/widget chrome, gray by default) since the two need to
diverge visually: the outermost window edge stays bright regardless of what
internal borders are themed to, so it can't just reuse `BORDER`. `TABLE_HEADER_TEXT`
(cyan, `#00c2c2`) was added for `TranscriptWidget`'s result-table header row (see
below): the closest existing candidate, `ACCENT`, already has an unrelated
caller (`ClassSandboxPanel`'s selected-row highlight) that recoloring it to
cyan would have dragged along unintentionally, so this got its own key instead
of overloading `ACCENT`'s meaning. Loaded by `ModLoader.loadThemes`/`loadTheme` — shaped like
`loadTiles`/`loadTile`'s directory scan, still routed through
`registerWithCollisionCheck` for id/`overrides` parity with every other content
type — into a `WidgetColorTheme` (id + `Map<String, Color>`,
`WidgetColorTheme.color(key)`) held in `ModRegistry` (`getTheme`/`getAllThemes`),
keyed by namespaced ID. The core mod's default theme lives at
`mods/core/themes/default.json` (id `core:default`); a file's name doesn't need
to match its id, same as tiles/items/quests. A theme missing a required color
key, or with a malformed `{r,g,b}` value, throws `ModLoadException` the same as
any other content type. `Main.loadGame()` loads the mod registry once at
startup and calls `WidgetTheme.applyTheme(...)` with whichever theme owns ID
`core:default`, before any screen/widget is constructed (most read
`WidgetTheme`'s accessors at construction time). No settings/config persistence
system exists yet to pick a non-default theme — the Settings screen's "Theme"
row (see `docs/screens.md`) is a purely visual placeholder, not wired to this
registry. Theming coverage isn't limited to the original widget-library files
either: every hardcoded `Color` literal across `ui/`, `ui/widget/`, and the dev
`sandbox/ClassSandboxPanel` was swept to reference a `WidgetTheme` accessor instead
(gameplay/world rendering — `Player`, `WorldScene`, `GamePanel` — stays
hardcoded, since those colors are game content, not UI chrome). This is the
widget-theming initiative.

A small reusable widget framework lives in `ui/widget/`: `Widget` (base
`JPanel` — themed background via `WidgetTheme.background()`, focusable),
`FocusManager` (a modal-open flag a popup's content can consult), `WidgetTheme`
(13 colors — `selectedHighlight()`/`selectedText()`/`normalText()`/
`dimmedText()`/`background()`/`invalidHighlight()`/`validHighlight()`/
`tableHeaderBackground()`/`border()`/`scrollbarThumb()`/`accent()`/
`windowBorder()`/`tableHeaderText()` — read from an immutable palette held in
one `AtomicReference`, initialised with hardcoded defaults so any widget built
without `ModLoader` ever running still gets sane colors, and swapped whole for
one built from a loaded `WidgetColorTheme` by `applyTheme` at startup; see "Widget theming" above), `ListWidget<T>` (a
keyboard-navigable,
optionally non-wrapping list over a pluggable data source, with
`onConfirm`/`onSelectionChange` callbacks and auto-scroll-into-view of the
selected row), `ButtonWidget` (an Enter-confirmable label), `TableWidget<T>`
(a keyboard-navigable row/column table with row-level confirm; `updateRow()`
replaces one row's data and re-renders just its cells without resetting
selection, unlike `setRows()`; `setSelectedRowAccentColor()` and
`setOtherRowsDimmed()` let a consumer flag the selected row as additionally
"armed" for some other in-progress action, with every other row dimmed to
match — the accent outline paints inside each cell's existing padding rather
than adding new border thickness, so a cell's insets never change between
accented and un-accented (an earlier version reserved extra space instead,
which stopped the whole table resizing on selection but shifted the grid
lines inward and opened a visible gap between rows); used by
`SettingsKeybindsPanel`, see `docs/screens.md`), `RadioGroupWidget<T>`
(a single-select radio group, vertical by default or horizontal on demand;
`selectOption(index)` marks an option confirmed without moving the
highlight/display cursor to it — for restoring a value loaded from disk as
the initial display too, `selectAndHighlightOption(index)` moves both
together, added for `SettingsScreenPanel`'s persisted Fullscreen/Font/Theme
rows, see "Settings persistence" in `docs/screens.md`),
`PatternFieldWidget` (a text-input field validating its content against a
caller-supplied regex pattern; `setPlaceholder()` shows gray hint text
while empty, `setValidityColoringEnabled(false)` turns off the red/green
border and text coloring for a consumer with no pattern to validate
against, and `getTextField()`/`setOnInputChanged()` let a consumer bind
its own keys or react to input without depending on the pattern-matching
behavior — added for `sandbox/DevConsolePanel`'s search field, which
reuses the widget's outlined-field look and block cursor for free-text
search rather than hand-rolling a lookalike),
`HeaderWidget` (a bordered, full-width, center-aligned title bar with
`setTitle()`/`getTitle()` and a bottom margin baked into its own border
so a consumer stacking content below it in a `BoxLayout` gets a gap for
free — added for `sandbox/ClassDetailPanel`'s class-name heading and
`sandbox/DevConsolePanel`'s own "Dev Console" title),
`PopupWidget` (a dismissible overlay,
keyboard-only like the rest of this game — no Close button, since it never
responded to anything but a click; `open()`/`dismiss()` manage visibility and
focus, Escape dismisses it, and `onUp()`/`onDown()`/`onLeft()`/`onRight()`
hooks — bound at `WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`, so they fire whether
the popup itself or a descendant has real Swing focus — let a subclass wire
keyboard navigation to
its own content; `placement()` says where `FillLayout` puts the popup: `FULL_SCREEN` (the default, from `isFullScreen()`), `CENTERED` at its preferred size (`isFullScreen()` returning false), or `BOTTOM_CENTER`, a box along the bottom edge that `DialoguePanel` uses);
`SliderWidget` (a bounded numeric slider with left/right adjustment within a
[min, max] range by fixed steps, with hard bounds—no wrap-around), `FillLayout`
(a `LayoutManager` stretching every child to the parent's full bounds by
default, for `JLayeredPane` overlays; follows each popup's `PopupWidget.placement()`
(full screen, centred, or the bottom-anchored box, sized from the parent within clamps),
`TerminalScrollBarUi` (a flat black-track/solid-thumb `BasicScrollBarUI`
replacing the platform look-and-feel's default scrollbar chrome), and
`ControlsHintBarWidget` (a single persistent bar, one shared instance built
once in `Main.loadGame()` and docked at `BorderLayout.SOUTH` of the game
frame, rendering whichever screen currently has focus's key bindings — see
`docs/screens.md`'s "Controls hint bar" for how screens push into it).
`setHints(List<Hint>)` takes structured `record Hint(String key, String
action)` pairs, not pre-formatted strings — the widget itself turns a raw key
identifier ("up", "escape", "shift+tab") into its displayed keycap label
(`keycapText()`: capitalizes the first letter, special-casing "escape"→"Esc"
and "enter"→"Enter" since those aren't just a capitalized first letter). Each
hint renders as a literal keycap in true reverse video (`NORMAL_TEXT` as the
label's background, `BACKGROUND` as its foreground — an actual color swap of
the theme's two base colors, not `SELECTED_HIGHLIGHT`, since this needs to
read as a terminal-style status line like `nano`'s, not a selected UI
element) beside its plain-text action, wrapping into a compact grid (up to 3
columns, filled column-major — top-to-bottom per column, then the next
column, the same fill order `nano`'s own help bar uses) once a hint list
exceeds one row, rather than one ever-widening `FlowLayout` line. Every
keycap within one `setHints` call shares a single uniform width (the widest
key in that call), so a short key like "Up" and a longer one like "Enter"
render as same-size blocks. `HintAware` (`ui/HintAware.java`, a single
`refreshHints()` method) is implemented by every `CardLayout`-hosted screen
so `Main.navigateTo()` can re-push the newly-focused screen's current hints
on every screen switch — a screen's own key-bound methods already push hints
for in-screen focus changes, but only `Main` knows when a screen switch just
happened.

`TranscriptWidget` (an append-only, auto-scrolling log view — added for
`sandbox/DevConsolePanel`'s replacement of its old live-filtered results
table, see `dev-console-log-transcript.feature`): `appendCommand`/`appendInfo`/
`appendSuccess`/`appendError` each add one plain-text line, with no timestamp
or level word - the line's color alone is the status signal (`DIMMED_TEXT` for
an echoed command, `NORMAL_TEXT` for info, `VALID_HIGHLIGHT` for success,
`INVALID_HIGHLIGHT` for error). `appendResultTable` adds a plain,
column-aligned text listing (padded to each column's widest value, no grid
borders — a "console.table" look, not a second `TableWidget` instance) with
a `TABLE_HEADER_TEXT`-colored header row, unaffected by the above. `entries()`
and `lastResultTable()` expose what's been appended so far (each entry keeps
its `Level` even though the level word itself isn't drawn) for callers (tests)
to assert against, without re-parsing rendered Swing components. Deliberately
does not import
`ui/ListDetailLayoutUtility.buildScrollPane`/`configureDetailsTable` despite
wanting the same behavior — `ModuleDependencyTest.widgetsMustNotDependOnScreens`
forbids a `ui/widget/` class from depending on that utility, which sits
directly in `ui/` — so `DevConsolePanel` (in `sandbox/`, exempt from that
rule) builds the scroll pane itself, and the table's non-selectable/no-wrap
configuration is inlined rather than shared.
