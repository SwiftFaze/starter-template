Feature: Mod loader failure path
  ModLoader.load(...) wraps any failure to read or parse mod content in a
  ModLoadException, so callers get one consistent exception type rather
  than a raw I/O or JSON parsing failure.

  Scenario: Loading a mod with a malformed building resource throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed building file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  Scenario: Loading a mod with a malformed mod.json throws ModLoadException
    Given a mods directory containing mod "broken-pack" with a malformed mod.json file
    When the mods directory is loaded
    Then a ModLoadException is thrown wrapping the underlying cause

  # Non-goals:
  #   - Any functional/behavior change to ModLoader — this is failure-path
  #     test coverage only, mirroring the prior BuildingLoader/
  #     BuildingException coverage, migrated to ModLoader/ModLoadException
  #     as part of the mod-loader phase-1 refactor.
  #   - The happy path (loading valid mod content) — covered by
  #     specs/features/mod-loader.feature and ModLoaderIT.
  #
  # Open questions:
  #   None outstanding.
