Feature: Terminal-style UI component framework
  A shared component framework — a base widget contract, a keyboard
  focus/navigation manager, and one consistent "selected" highlight style
  — replaces each panel's hand-rolled InputMap/ActionMap wiring and ad hoc
  highlight color. Three concrete widgets (list, button, popup/modal) are
  built on it, proven in isolation and by migrating ClassSandboxPanel off
  the deleted SelectableMenu. This file used to also prove the framework
  end-to-end by rebuilding the in-game inventory screen through EastPanel;
  that proof case was removed alongside EastPanel — see the trailing
  Risks note.

  Scenario: Navigating a list widget down moves the selection to the next item
    Given a list widget with items "Inventory", "Help", "Journal" and "Inventory" selected
    And the list widget has keyboard focus
    When the "Down" key is pressed
    Then the selected item is "Help"

  Scenario: Moving up from the first item wraps to the last item
    Given a list widget with items "Inventory", "Help", "Journal" and "Inventory" selected
    And the list widget has keyboard focus
    When the "Up" key is pressed
    Then the selected item is "Journal"

  Scenario: Moving down from the last item wraps to the first item
    Given a list widget with items "Inventory", "Help", "Journal" and "Journal" selected
    And the list widget has keyboard focus
    When the "Down" key is pressed
    Then the selected item is "Inventory"

  Scenario: Confirming a list widget's selection with Enter
    Given a list widget with items "Inventory", "Help", "Journal" and "Inventory" selected
    And the list widget has keyboard focus
    When the "Enter" key is pressed
    Then the confirmed item is "Inventory"

  Scenario: A list widget's items come from a pluggable data source, not a hardcoded list
    Given a list widget backed by a data source currently containing "Iron Sword", "Plain Shield"
    When the data source's contents change to "Iron Sword", "Plain Shield", "Health Potion"
    And the list widget is refreshed
    Then the list widget's items are "Iron Sword", "Plain Shield", "Health Potion"

  Scenario: Confirming a button widget invokes its action
    Given a button widget labeled "Close" with an action registered
    And the button widget has keyboard focus
    When the "Enter" key is pressed
    Then the button's action was invoked

  Scenario: ClassSandboxPanel's initial selection is highlighted and its stats shown
    Given a class sandbox panel is showing
    Then the first class's label is colored "#eeb392"
    And the stats label shows the first class's computed stats

  Scenario: Moving ClassSandboxPanel's selection down highlights the next class
    Given a class sandbox panel is showing
    When the down-bound action fires
    Then the previously selected class's label is white
    And the newly selected class's label is colored "#eeb392"
    And the stats label shows the newly selected class's computed stats

  Scenario: Moving ClassSandboxPanel's selection up from the first class wraps to the last
    Given a class sandbox panel is showing
    When the up-bound action fires
    Then the last class's label is colored "#eeb392"
    And the stats label shows the last class's computed stats

  # Non-goals:
  #   - Testing the base widget contract/interface directly — it has no
  #     independent runtime behavior; it's exercised indirectly through the
  #     concrete list/button/popup scenarios above.
  #   - Left-right / tab-style movement between widgets — nothing in the
  #     real screen being rebuilt has a natural left-right layout, so
  #     there's no concrete case to prove.
  #   - A navigable menu widget in the rebuilt screen at all (see
  #     Clarifications) — the inventory popup opens directly off the
  #     keyboard inventory toggle now, no Up/Down/Enter-through-a-menu
  #     step in between, so there's no menu<->popup focus transition or
  #     modal-capture-blocks-the-menu-behind-it behavior to prove either.
  #   - Disabled-widget styling — the shared style/theme constants support
  #     it as a convention hook, but no widget in the rebuilt real screen is
  #     ever disabled, so there's nothing concrete to prove end-to-end.
  #   - Table widget, radio group widget, pattern-validated text fields —
  #     tracked separately in #35.
  #   - Any mouse/pointer handling — this game is keyboard-only by design.
  #   - A deeper redesign of ClassSandboxPanel beyond the mechanical
  #     SelectableMenu -> list-widget swap — tracked in milestone "3. Dev
  #     sandbox framework".
  #
  # Risks:
  #   - This feature supersedes scenarios in four existing files, which
  #     must be removed/updated in the same change (not just added here) or
  #     mvn verify goes red: keyboard-input-and-menu-navigation.feature (4
  #     SelectableMenu scenarios), ui-panel-rendering-and-composition.feature
  #     (3 MenuPanel cancel/confirm-through-EastPanel scenarios),
  #     class-sandbox-panel-selection.feature (3 ClassSandboxPanel
  #     scenarios, all three carried over unchanged above since the visible
  #     behavior doesn't change, only what's underneath it), and
  #     data-driven-item.feature (1 "EastPanel wires real core item data"
  #     scenario). specs/features/README.md's rows for all four need
  #     updating in the same change, not just the new row for this file.
  #   - UiPanelRenderingAndCompositionSteps.java's inventory-item step defs
  #     (itsInventoryPanelDisplaysTheItem,
  #     itsInventoryPanelNoLongerDisplaysThePlaceholderText) are also used
  #     by data-driven-item.feature's scenario, not just
  #     ui-panel-rendering-and-composition.feature's — don't delete them
  #     wholesale when migrating the latter; both files' migrations need to
  #     land together.
  #   - MenuPanel itself (the sidebar's I/H/J/M/P/O list) was deleted
  #     entirely after Step 4.5 manual playtest, on top of the popup's
  #     layered-overlay rework above — decided too late to avoid a second
  #     pass over ui-panel-rendering-and-composition.feature's EastPanel
  #     composition scenario (menu-panel assertion dropped) and
  #     EastPanelTest (menu-cancel tests dropped). The inventory toggle
  #     (the "I" key, GamePanel -> EastPanel.toggleInventory) is untouched
  #     and still the only way to open/close it.
  #   - Real Swing focus-transfer (does a widget actually become the AWT
  #     focus owner) is not simulated headlessly here, matching the
  #     existing precedent in keyboard-input-and-menu-navigation.feature —
  #     "keyboard focus" Given/Then steps model the framework's internal
  #     focus-manager state directly; true cross-component focus transfer
  #     is exercised via the manual playtest (this repo's Step 4.5).
  #
  # Open questions:
  #   - None outstanding.
  #
  # Post-merge amendment (2026-09-01, during codex-ui's #113 build):
  #   - PopupWidget's Close button was removed (from PopupWidget itself, and
  #     the now-dead `remove(getCloseButton())` call in CompactPopupWidget,
  #     which never had one visible anyway) — it never responded to a click
  #     in this keyboard-only game, and Escape already dismisses every
  #     popup, so it was a purely decorative element that looked
  #     interactive but wasn't. `open()` now focuses the popup itself
  #     instead of a Close button child. The two scenarios above that
  #     exercised it were updated/removed to match: "Toggling the inventory
  #     open focuses the popup's Close button" lost its now-nonexistent
  #     assertion (renamed to describe what it still proves), and
  #     "Confirming the popup's Close button closes it and restores game
  #     focus" was deleted outright — its only replacement would be a
  #     verbatim duplicate of the Escape scenario already above it.
  #     ButtonWidget itself (and its own standalone "Confirming a button
  #     widget invokes its action" scenario above) is untouched — it's a
  #     generic framework primitive, not exclusively tied to the Close
  #     button's former role, and is proven directly rather than through
  #     any popup now.
  #
  # Removal note (later, unrelated cleanup):
  #   - The six scenarios proving this framework end-to-end through the
  #     rebuilt in-game inventory screen ("Toggling the inventory open...",
  #     "Dismissing the popup with Escape...", "...displays real loaded
  #     item data...", the two Up/Down item-list scenarios, and "...is
  #     layered above the game view and sidebar...") were removed when
  #     EastPanel/NorthPanel/SouthPanel/PlayerInfoPanel/TerminalPanel were
  #     deleted as unrelated early-scaffolding cleanup.
  #     The list/button/popup framework itself is untouched and still
  #     proven by the scenarios above and by ClassSandboxPanel — only its
  #     inventory-popup proof case is gone, pending a reimplemented
  #     composition root.
