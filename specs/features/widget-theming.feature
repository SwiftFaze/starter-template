Feature: Mod-driven color theming for the UI widget library
  Widget colors move from hardcoded WidgetTheme Color constants to
  mod-shaped theme files under mods/<modid>/themes/*.json, loaded through
  the same ModLoader/mods/ mechanism tiles/buildings/classes/items/quests
  already use, so reskinning the widget library no longer requires
  editing Java. Every hardcoded UI color across ui/ and ui/widget/ (plus
  the dev ClassSandboxPanel) is migrated to reference WidgetTheme instead
  of a literal — the 2026-08-31 scope expansion was from "the widget
  library's original 10 colors" to "every hardcoded UI color".

  Scenario: Loading the core mod's default theme populates all twelve widget colors
    Given a mods directory containing the "core" mod with a theme declaring id "core:default" and all twelve widget colors
    When the mods directory is loaded
    Then a theme with ID "core:default" is available
    And WidgetTheme's colors match the "core:default" theme's colors exactly

  Scenario: A second mod can ship its own theme without activating it
    Given a mods directory containing the "core" mod with a theme declaring id "core:default" and all twelve widget colors
    And the mods directory also contains mod "midnight-pack" with a theme declaring id "midnight-pack:midnight" and all twelve widget colors
    When the mods directory is loaded
    Then a theme with ID "core:default" is available
    And a theme with ID "midnight-pack:midnight" is available
    And WidgetTheme's colors still match the "core:default" theme's colors

  Scenario: A mod declaring a colliding theme ID without an override field fails to load
    Given a mods directory containing the "core" mod with a theme declaring id "core:default" and all twelve widget colors
    And the mods directory also contains mod "retexture-pack" with a theme declaring id "core:default" and no "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the colliding ID "core:default" and both mods "core" and "retexture-pack"

  Scenario: A mod declaring a colliding theme ID with an explicit override replaces the earlier definition
    Given a mods directory containing the "core" mod with a theme declaring id "core:default" and all twelve widget colors
    And the mods directory also contains mod "retexture-pack" with a theme declaring id "core:default", a "SELECTED_HIGHLIGHT" color of (10, 20, 30), and the rest of the twelve widget colors, whose "overrides" field names "core:default"
    When the mods directory is loaded
    Then a theme with ID "core:default" is available
    And its "SELECTED_HIGHLIGHT" color is (10, 20, 30)

  Scenario: A theme missing a required color key fails to load
    Given a mods directory containing mod "broken-pack" with a theme declaring id "broken-pack:incomplete" that omits "BORDER"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the missing color key "BORDER" and the file for theme "broken-pack:incomplete"

  Scenario: A theme with a malformed color resource throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed theme file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  # Non-goals:
  #   - Fonts — colors only.
  #   - Gameplay/world rendering colors (Player's glyph color, WorldScene's
  #     fallback tile color, GamePanel's own viewport chrome) — not UI,
  #     per the user's 2026-08-31 clarification; these stay hardcoded.
  #   - Theme activation/switching (choosing which loaded theme actually
  #     applies to WidgetTheme) — no settings/config persistence system
  #     exists yet. WidgetTheme always applies whichever theme owns ID
  #     "core:default" once loaded; a second mod's theme loading without
  #     error is what proves the pattern, not that it renders.
  #   - The visual-only "Theme" entry on the Settings screen — a fixed
  #     placeholder cycle (not sourced from this theme registry, see
  #     settings-screen.feature's Risks), part of the existing Settings
  #     screen concept rather than a new scenario here.
  #   - An automated per-file check that no ui/ or ui/widget/ file
  #     hardcodes a Color literal — this is a one-time sweep verified by
  #     code review/grep at implementation time, not a Gherkin scenario;
  #     ModLoader-level scenarios above can't observe Swing call sites.
  #
  # Risks:
  #   - WidgetTheme's fields are `static final` Color constants today;
  #     they become mutable statics populated once at startup from the
  #     loaded "core:default" theme. Every consumer across ui/ and
  #     ui/widget/ (plus ClassSandboxPanel) keeps referencing them by
  #     static field name — no behavior change, only where each hardcoded
  #     literal used to be inline now reads WidgetTheme.FIELD instead.
  #   - Themes live under mods/<modid>/themes/*.json — a directory of
  #     files, one theme per file, matching the tiles/items/quests
  #     convention (not the stats.json singleton) — so ModLoader's new
  #     loadThemes/loadTheme functions are shaped like loadTiles/loadTile,
  #     still routed through registerWithCollisionCheck for id/overrides
  #     parity with every other content type. The core mod's default
  #     theme file is mods/core/themes/default.json (id "core:default").
  #   - The 7-vs-10-color scope question (issue #106 named only 7;
  #     WidgetTheme had 10 at implementation time) was resolved via
  #     grilling.
  #   - TABLE_BORDER renamed to BORDER, and a new ACCENT color added
  #     (2026-08-31) once the sweep reached panels
  #     using that border color outside any table, and a leftover
  #     hardcoded accent color (#eeb392) in NorthPanel/ClassSandboxPanel.
  #     All 11 keys must be present in every theme file. A 12th key,
  #     WINDOW_BORDER, was added later (2026-09-05 UI compliance audit)
  #     once Main.java's frame border and DevConsolePanel's own border
  #     were both found hardcoding Color.WHITE instead of resolving to a
  #     theme key. A 13th key, TABLE_HEADER_TEXT, was added
  #     (dev-console-log-transcript.feature, 2026-09-07) for the dev
  #     console's new result-table header color - the closest existing key
  #     (ACCENT) already has an unrelated caller (ClassSandboxPanel's
  #     selected-row highlight) that a shared color would have dragged
  #     along unintentionally.
  #
  # Open questions:
  #   None outstanding.
