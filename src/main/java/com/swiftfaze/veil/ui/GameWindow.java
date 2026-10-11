package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.ui.widget.FillLayout;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Wraps the game view in a layered content area so a future sidebar/popup
 * layer (e.g. a reimplemented inventory/codex shell) can be layered above
 * {@link JLayeredPane#DEFAULT_LAYER} without changing this method's shape.
 */
public final class GameWindow {

    private GameWindow() {
    }

    public static JLayeredPane buildContentArea(GamePanel gamePanel) {
        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.add(gamePanel, BorderLayout.CENTER);

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(mainArea.getPreferredSize());
        layeredPane.setLayout(new FillLayout());

        layeredPane.add(mainArea, JLayeredPane.DEFAULT_LAYER);

        return layeredPane;
    }
}
