package com.swiftfaze.veil.input;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeybindingsTest {

    @Test
    void movementAndToggleKeyStrokesAreAllDistinct() {
        List<KeyStroke> keys = List.of(
                Keybindings.MOVE_UP_Z, Keybindings.MOVE_UP_ARROW,
                Keybindings.MOVE_DOWN_S, Keybindings.MOVE_DOWN_ARROW,
                Keybindings.MOVE_LEFT_Q, Keybindings.MOVE_LEFT_ARROW,
                Keybindings.MOVE_RIGHT_D, Keybindings.MOVE_RIGHT_ARROW,
                Keybindings.TOGGLE_INVENTORY
        );

        assertEquals(keys.size(), Set.copyOf(keys).size());
    }

    @Test
    void actionNamesAreAllDistinct() {
        List<String> actionNames = List.of(
                Keybindings.ACTION_MOVE_UP, Keybindings.ACTION_MOVE_DOWN,
                Keybindings.ACTION_MOVE_LEFT, Keybindings.ACTION_MOVE_RIGHT,
                Keybindings.ACTION_TOGGLE_INVENTORY,
                Keybindings.ACTION_MENU_UP, Keybindings.ACTION_MENU_DOWN,
                Keybindings.ACTION_MENU_CONFIRM
        );

        assertEquals(actionNames.size(), Set.copyOf(actionNames).size());
    }
}
