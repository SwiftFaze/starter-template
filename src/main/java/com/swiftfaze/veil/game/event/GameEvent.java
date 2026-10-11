package com.swiftfaze.veil.game.event;

/**
 * A domain transition the game performed, recorded by {@link GameEventLog} for QA
 * replays. Code outside the engine packages builds events through the static
 * factories rather than the record constructors, because ModuleDependencyTest
 * confines engine-class instantiation to Main.
 */
public sealed interface GameEvent {

    /** A main card switch (title, settings, keybinds, game). */
    record ScreenChanged(String from, String to) implements GameEvent {
    }

    /** The title-screen menu's highlighted item changed. */
    record MenuSelectionChanged(String to) implements GameEvent {
    }

    /** The player's position actually changed. */
    record PlayerMoved(int x, int y) implements GameEvent {
    }

    /** A popup (currently only "inventory") opened or closed. */
    record PopupToggled(String name, boolean open) implements GameEvent {
    }

    /** An interact key press found an object in range that refused to change state. */
    record InteractionRefused(String reason) implements GameEvent {
    }

    static GameEvent screenChanged(String from, String to) {
        return new ScreenChanged(from, to);
    }

    static GameEvent menuSelectionChanged(String to) {
        return new MenuSelectionChanged(to);
    }

    static GameEvent playerMoved(int x, int y) {
        return new PlayerMoved(x, y);
    }

    static GameEvent popupToggled(String name, boolean open) {
        return new PopupToggled(name, open);
    }

    static GameEvent interactionRefused(String reason) {
        return new InteractionRefused(reason);
    }
}
