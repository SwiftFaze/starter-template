package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.FillLayout;

import javax.swing.JLayeredPane;

/**
 * Assembles the settings screen and its modal popups (e.g. the reset confirmation
 * dialog) into a single layered content area: the settings screen sits at
 * {@link JLayeredPane#DEFAULT_LAYER}, popups at {@link JLayeredPane#POPUP_LAYER}
 * above it, so a popup covers the settings view instead of living inside its
 * own layout. Mirrors {@link GameWindow}'s pattern for the game card.
 */
public final class SettingsWindow {

    private SettingsWindow() {
    }

    public static JLayeredPane buildContentArea(SettingsScreenPanel settingsScreen) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(settingsScreen.getPreferredSize());
        layeredPane.setLayout(new FillLayout());

        layeredPane.add(settingsScreen, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(settingsScreen.getResetConfirmationPopup(), JLayeredPane.POPUP_LAYER);

        return layeredPane;
    }
}
