Feature: In-game version display
  The running build's version is shown at the bottom-right of the window's
  bottom strip (the controls hint bar, docked at BorderLayout.SOUTH of the
  game frame), so a player or tester can tell which build they're running.
  The version comes from the Maven project version, filtered at build time
  into a bundled version.properties on the classpath and read at startup, so
  a dev run (mvn compile exec:java) and a packaged install show the same
  string. Because the bar is frame-level, the version shows on every screen.

  Supersedes issue #16's original placement in SouthPanel, which was removed
  before this landed (077d19f). Out of scope: commit hash, build timestamp,
  any text beyond "v<version>". The app icon is covered separately by
  app-icon.feature.

  Scenario: The version string is shown at the bottom-right of the hint bar
    Given the bundled version is "0.5.0-beta.39"
    When the game window is shown
    Then the hint bar's version label reads "v0.5.0-beta.39"
    And the version label sits at the hint bar's right edge

  Scenario: The version stays visible as hints change between screens
    Given the bundled version is "0.5.0-beta.39"
    And the game window is shown
    When the settings screen is shown
    Then the hint bar's version label reads "v0.5.0-beta.39"
    And the hint bar still shows the settings screen's hints

  Scenario: The version label uses the theme's dimmed text color
    Given the bundled version is "0.5.0-beta.39"
    When the game window is shown
    Then the version label's color is the theme's dimmed text color

  Scenario: The classpath version.properties carries the real Maven project version
    When version.properties is read from the classpath
    Then its version equals the project's pom.xml version

  Scenario Outline: A missing or unfiltered version shows no version text
    Given the bundled version.properties is <state>
    When the game window is shown
    Then the hint bar's version label is empty
    And a warning about the missing version is logged

    Examples:
      | state                                        |
      | absent                                       |
      | present without a version key                |
      | present with the unfiltered "${project.version}" |

  @manual-verification
  Scenario Outline: An installed build shows the same version as its release
    Given a player has installed Veil <version> via the <installer> installer
    When the player launches the game
    Then the bottom-right of the hint bar reads "v<version>"

    Examples:
      | installer    | version        |
      | Windows .exe | 0.5.0-beta.40  |
      | Debian .deb  | 0.5.0-beta.40  |
      | macOS .pkg   | 0.5.0-beta.40  |

# Non-goals: commit hash, build timestamp, a version on the title-screen art,
#   an "About" dialog.
# Risks: the filtered <resource> block must not filter other resources (JSON
#   schemas / logback.xml) — a pom change that widens filtering would mangle them.
# Open questions: none.
