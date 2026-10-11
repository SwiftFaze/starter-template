package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.FillLayout;

import javax.swing.JLayeredPane;

/**
 * Assembles the keybinds screen and its modal popups (discard-confirmation
 * and reset-confirmation dialogs) into a single layered content area: the
 * keybinds screen sits at {@link JLayeredPane#DEFAULT_LAYER}, popups at
 * {@link JLayeredPane#POPUP_LAYER} above it, so popups cover the keybinds
 * view instead of living inside its own layout. Mirrors {@link SettingsWindow}'s
 * pattern for the settings card.
 */
public final class SettingsKeybindsWindow {

    private SettingsKeybindsWindow() {
    }

    public static JLayeredPane buildContentArea(SettingsKeybindsPanel keybindsScreen) {
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(keybindsScreen.getPreferredSize());
        layeredPane.setLayout(new FillLayout());

        layeredPane.add(keybindsScreen, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(keybindsScreen.getDiscardConfirmationPopup(), JLayeredPane.POPUP_LAYER);
        layeredPane.add(keybindsScreen.getResetConfirmationPopup(), JLayeredPane.POPUP_LAYER);

        return layeredPane;
    }
}
