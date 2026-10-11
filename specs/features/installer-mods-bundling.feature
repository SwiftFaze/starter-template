@manual-verification
Feature: Installer build bundles mods/core alongside the executable
  The jpackage installer pipeline stages the repository's checked-in
  mods/core content into the packaged application so a fresh install has
  working buildings, tiles, classes, and stats out of the box, matching a
  dev run from the repo root.

  This entire feature is a build-pipeline / OS-installer packaging
  concern with no Java code path for Cucumber to exercise, unlike every
  other .feature file in this repo (see specs/features/mod-loader.feature,
  which set this precedent for the same concern: "verified manually, not
  exercised via Cucumber"). None of the scenarios below have step
  definitions and none are intended to gain any — this file is the
  human-reviewed spec of record only. Verification happens by actually
  building and running each installer, as an extension of the repo's
  Step 4.5 manual-playtest gate, not via `mvn test`/`mvn verify`.

  Scenario: Release pipeline bundles core mod content alongside the packaged jar
    Given the "Build shaded jar" step has produced target/Veil-<version>-app.jar
    And the repository's mods/core directory exists with its checked-in content
    When the "Build installer" step runs jpackage with --app-content mods
    Then the installed application's top-level directory contains mods/core with the same files as the repository's mods/core
    And the installed application's top-level directory still contains the executable, as before

  Scenario Outline: A fresh install loads core content from its normal launch entry point
    Given a player has installed Veil via the <installer> installer
    When the player launches the game via its <launch path>
    Then the game's working directory resolves to the installation directory
    And mods/core loads successfully, with its buildings, tiles, classes, and stats present

    Examples:
      | installer   | launch path             |
      | Windows .exe | Start Menu shortcut     |
      | Windows .exe | desktop shortcut        |
      | Windows .exe | direct .exe launch      |
      | Debian .deb  | applications menu entry |
      | macOS .pkg   | applications menu entry |

  Scenario: A dev run from the repo root is unaffected
    Given the repository's checked-in mods/core directory
    When the game is run via mvn compile exec:java
    Then mods/core loads successfully, as it does today

  # Non-goals:
  #   - Any change to where ModLoader looks for mods/ at runtime, or a
  #     fallback search path (e.g. resolving relative to the jar's own
  #     location instead of cwd). Stayed out of scope: a direct Windows
  #     .exe launch was verified (locally, via jpackage --type app-image)
  #     to have its working directory resolve to the top-level install
  #     directory, matching ModLoader's existing cwd-relative resolution.
  #   - Bundling anything beyond mods/core — third-party mods are a
  #     player-installed concern, not something the installer ships.
  #   - In-game mod management UI — already out of scope per the original
  #     mod-loader intent.
  #
  # Risks:
  #   - None of these scenarios have a Java code path for Cucumber step
  #     definitions to exercise (no application code invokes jpackage or
  #     installs the app) — unlike every other .feature file in this repo.
  #     Deliberate: this file is a manually-verified spec of record,
  #     matching the precedent set by specs/features/mod-loader.feature
  #     for this exact concern. No step definitions should be added for
  #     any scenario here, including the staging scenario, even though it
  #     alone wouldn't require an actual installer run to check.
  #
  # Open questions:
  #   - Resolved for the "Windows .exe / direct .exe launch" row of the
  #     Examples table: verified locally. The
  #     remaining four rows (Start Menu shortcut, desktop shortcut, and
  #     both Debian/macOS launch paths) were not independently verified —
  #     doing so needs WiX (Windows installer) or a Linux/macOS
  #     environment, neither available where this was implemented. Nothing
  #     about the --app-content placement fix is shortcut- or
  #     installer-type-specific, so this is a low-risk gap, accepted by
  #     the human rather than blocking on it.
