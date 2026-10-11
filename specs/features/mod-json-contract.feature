Feature: Mod JSON format is an explicit, schema-backed contract
  The mod-loading JSON format is one of .claude/workflow.md's three named
  high-risk triggers, and today it exists only as an emergent property of
  ModLoader's hand-written Gson tree-API parsing (loadTile, loadBuilding,
  loadClass, loadItem, loadQuest, loadTheme, and their helpers) — there is
  no schema a third-party mod author can read or validate against, the
  parser IS the spec, and failures are either unattributable (a raw wrapped
  NPE naming the file but not the field) or silent (a typo'd optional field
  key loads with the wrong semantics and no diagnostic).

  This feature makes the format explicit: a committed JSON Schema per mod
  file type under docs/schemas/ (mod.json, stats.json, tiles/, buildings/,
  classes/, items/, quests/, themes/), validated with the new
  networknt/json-schema-validator dependency (see
  specs/intent/mod-json-contract.md's Clarifications for why a library was
  chosen over hand-written validation, and why that library specifically).
  ModLoader's parsing methods gain field-level diagnostics — naming the
  file, the offending field, and what was expected versus found — replacing
  today's opaque wrapped-NPE messages, and reject unknown fields instead of
  silently ignoring a typo.

  It covers, via Cucumber scenarios exercising ModLoader.load(...) exactly
  like specs/features/mod-loader.feature already does: a missing required
  field, a field with the wrong type, an unknown/typo'd field, a
  malformed content id, a mod.json dependsOn naming a mod that isn't
  loaded, and a building blueprint referencing a tile id that doesn't
  resolve — each asserting the specific diagnostic produced, not just that
  some ModLoadException was thrown.

  It does NOT cover, and defers to non-Cucumber test layers (see Non-goals):
  the schemas themselves as static committed artifacts, the test that
  validates mods/core/** against them, unknown-field rejection's schema
  mechanics, the schema/parser-drift demonstration, or narrowing
  ModLoader's catch (Exception e) blocks so a genuine Veil bug isn't
  misreported as bad mod data.

  It supersedes nothing: specs/features/building-loader-failure-path.feature
  covers a different failure mode (JSON that fails to *parse* at all, e.g.
  "{ not valid json") and stays exactly as it is — this feature covers JSON
  that parses fine but violates the schema (wrong shape, wrong type, unknown
  field), which is new ground, not a replacement.

  Background:
    Given a mods directory containing mod "core" so the mods directory is never empty

  Scenario Outline: A mod file missing a required field fails to load with a field-level diagnostic
    Given a mods directory containing mod "broken-pack" with a <fileType> file missing the required field "<field>"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the file, the missing required field "<field>", and that it is required

    Examples:
      | fileType | field     |
      | tile     | walkable  |
      | item     | slot      |
      | quest    | objective |

  Scenario: A field with the wrong type fails to load with a field-level diagnostic
    Given a mods directory containing mod "broken-pack" with a tile file whose "color" field is a string instead of an object
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the file, the field "color", the expected type "object", and the type actually found

  Scenario: An unknown field is rejected instead of silently ignored
    Given a mods directory containing mod "broken-pack" with a tile file that has an "overides" field instead of the recognized "overrides" field
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the file and the unknown field "overides"

  Scenario: A content id that doesn't follow the required namespace:name pattern fails to load
    Given a mods directory containing mod "broken-pack" with a tile file whose id is "notnamespaced" with no namespace separator
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the file and the malformed id "notnamespaced"

  Scenario: A mod.json dependsOn naming a mod that never loads fails with a diagnostic naming the file
    Given a mods directory containing mod "broken-pack" whose mod.json declares a "dependsOn" of "nonexistent-mod"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the mod.json file, mod "broken-pack", and the unresolved dependency "nonexistent-mod"

  Scenario: A building blueprint referencing a tile id that doesn't resolve fails to load naming the file
    Given a mods directory containing mod "broken-pack" with a building whose blueprint references tile id "nonexistent:tile"
    When the mods directory is loaded
    Then loading fails with a ModLoadException naming the building's file, the building's id, and the unresolved tile id "nonexistent:tile"

  @manual-verification
  Scenario: Renaming a schema-covered field without updating the schema fails the build
    Given the "walkable" field in Tile and ModLoader#loadTile is deliberately renamed without updating the tile schema under docs/schemas/
    When mvn verify is run
    Then the mods/core/** schema-validation test fails
    And this is demonstrated manually during Step 4.5, with the deliberate rename and the resulting failure recorded in the PR description, then reverted

  # Non-goals:
  #   - The schemas themselves as committed artifacts (docs/schemas/*.json)
  #     — reviewed as files, not exercised via Gherkin.
  #   - The test validating every file under mods/core/** against its
  #     schema — a JUnit/integration test (parallel to ModLoaderIT, which
  #     already loads real mods/core content off disk), not a Cucumber
  #     scenario; "shipped content can't drift from the spec" is a build-
  #     time guarantee, not mod-author-observable behavior.
  #   - Narrowing ModLoader's catch (Exception e) blocks so a genuine bug
  #     in Veil's own loader code isn't misreported as a ModLoadException
  #     about bad mod data — verified by a targeted unit test asserting
  #     which exception types each loadX method's catch clause now
  #     accepts, not a Gherkin-observable behavior (there's no way to
  #     script "Veil has a bug" as a Given).
  #   - JSON that fails to parse at all (invalid JSON syntax) — that's
  #     specs/features/building-loader-failure-path.feature's concern,
  #     unaffected by this feature.
  #   - Any change to the mod format itself — this documents and enforces
  #     the existing shape; a format change is separate, high-risk-path
  #     work.
  #   - Replacing Gson tree-API parsing with POJO binding — a larger,
  #     separate refactor; the schemas are valuable either way.
  #   - A schema version field and migration handling.
  #   - Runtime schema validation on every load in the shipped game.
  #   - docs/README.md indexing the new mod-format reference, and the
  #     .claude/workflow.md high-risk-trigger note update — documentation
  #     tasks (Step 7), not Gherkin-observable behavior.
  #
  # Risks:
  #   - The "wrong type" and "malformed id" step definitions are new
  #     ModLoaderSteps.java Given steps, not variants of existing ones —
  #     unlike the missing-required-field Outline, which can reuse the
  #     existing "and no {string} field" family of steps' underlying
  #     fixture-writing machinery with a tweak to actually omit a required
  #     field (today's "and no {string} field" steps are all used for
  #     *optional* fields like "overrides", so they don't yet cover
  #     omitting a genuinely required one).
  #   - The building-blueprint-unresolved-tile-id scenario changes an
  #     existing message ("Building 'X' references unknown tile ID: Y" in
  #     ModLoader.readBlueprint) to also name the file — check no other
  #     .feature file (e.g. mod-loader.feature) asserts on today's exact
  #     wording before changing it.
  #   - networknt/json-schema-validator's transitive dependencies must not
  #     collide with Gson's version or introduce a conflicting Jackson
  #     version already absent from this repo — check `mvn dependency:tree`
  #     after adding it.
  #
  # Open questions:
  #   None outstanding — settled via a grilling round during spec drafting,
  #   recorded in specs/intent/mod-json-contract.md's Clarifications:
  #   (1) networknt/json-schema-validator over hand-written validation or
  #   everit-org/json-schema; (2) schemas live under docs/schemas/.
