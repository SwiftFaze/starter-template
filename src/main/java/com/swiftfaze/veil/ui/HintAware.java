package com.swiftfaze.veil.ui;

/**
 * Implemented by CardLayout-hosted screens Main navigates between (Title,
 * Settings, Keybinds), so Main can re-push the newly active screen's
 * current hints into the shared ControlsHintBarWidget on every
 * navigateTo() call - a screen's own key-bound methods push hints for
 * in-screen focus changes, but only Main knows when a screen switch just
 * happened.
 */
public interface HintAware {
    void refreshHints();
}
