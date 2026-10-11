Feature: Veil application icon
  The game shows the user-supplied Veil icon instead of Java's default
  coffee cup — on the game window and the dev console window at runtime
  (loaded from the classpath resource icons/veil.png, so dev and packaged
  runs behave the same), and on the installed application/shortcuts of each
  jpackage installer (packaging/icons/veil.ico, veil.png, veil.icns).

  Runtime scenarios are Cucumber-tested. Installer scenarios are a
  build-pipeline concern with no Java path, tagged @manual-verification per
  installer-mods-bundling.feature's precedent. Out of scope: creating or
  changing the icon art. The version display is covered separately by
  app-version-display.feature.

  Scenario: The game window uses the Veil icon
    When the game window is shown
    Then the game window's icon is the bundled Veil icon

  Scenario: The dev console window uses the Veil icon
    Given the dev console is enabled
    When the dev console window is built
    Then the dev console window's icon is the bundled Veil icon

  Scenario: A missing icon resource leaves the default icon and does not crash
    Given the bundled icon resource is absent
    When the game window is shown
    Then the game window has no custom icon
    And a warning about the missing icon is logged

  @manual-verification
  Scenario Outline: An installed build shows the Veil icon
    Given a player has installed Veil via the <installer> installer
    When the player looks at the <surface>
    Then it shows the Veil icon, not Java's default

    Examples:
      | installer    | surface                                  |
      | Windows .exe | Start Menu shortcut and desktop shortcut |
      | Windows .exe | taskbar entry of the running game         |
      | Debian .deb  | applications menu entry                   |
      | macOS .pkg   | Applications folder and Dock entry        |

# Non-goals: icon art direction, per-screen icons, a tray icon.
# Risks: jpackage silently falls back to the default icon if --icon points at
#   a missing file or the wrong format for that OS — only the manual
#   installer check catches that.
# Open questions: none.
