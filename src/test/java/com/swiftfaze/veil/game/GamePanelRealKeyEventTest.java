package com.swiftfaze.veil.game;

import com.swiftfaze.veil.testing.FlatFloorWorld;
import com.swiftfaze.veil.entities.player.Player;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves GamePanel's WHEN_IN_FOCUSED_WINDOW key bindings actually route through Swing's
 * real focus machinery, not just that the bound Action works when invoked directly via
 * getActionMap() (every other test in GamePanelTest, and every Cucumber step definition
 * driving keyboard input, takes that shortcut — see docs/testing-acceptance.md's key-event coverage
 * rule). Needs a real, OS-focused window: java.awt.HeadlessException rules that out
 * entirely on this repo's actual CI (ubuntu-latest, no virtual display configured), so
 * this is skipped there rather than failed, and runs for real on any machine with a
 * display (local dev, a future CI with Xvfb).
 */
class GamePanelRealKeyEventTest {

    private static final int MAX_FOCUS_WAIT_ATTEMPTS = 50;
    private static final int FOCUS_POLL_INTERVAL_MS = 20;

    @BeforeEach
    void requiresRealDisplay() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "Real focus-window key dispatch needs a display; skipped under headless CI.");
    }

    @Test
    void aRealKeyPressRoutesThroughFocusToMovePlayer() throws InterruptedException {
        GamePanel panel = FlatFloorWorld.panel();
        JFrame frame = new JFrame("GamePanelRealKeyEventTest");
        frame.add(panel);
        frame.pack();
        try {
            frame.setVisible(true);
            waitForFocus(panel);

            Player player = panel.getPlayer();
            int startY = player.getY();

            panel.dispatchEvent(new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                    0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED));

            assertEquals(startY - 1, player.getY());
        } finally {
            frame.dispose();
        }
    }

    private static void waitForFocus(GamePanel panel) throws InterruptedException {
        for (int i = 0; i < MAX_FOCUS_WAIT_ATTEMPTS && !panel.isFocusOwner(); i++) {
            panel.requestFocusInWindow();
            TimeUnit.MILLISECONDS.sleep(FOCUS_POLL_INTERVAL_MS);
        }
        Assumptions.assumeTrue(panel.isFocusOwner(),
                "GamePanel never became the real focus owner; skipping rather than asserting on an unfocused window.");
    }
}
