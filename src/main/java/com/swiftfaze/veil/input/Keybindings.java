package com.swiftfaze.veil.input;

import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

public final class Keybindings {

    public static final KeyStroke MOVE_UP_Z = KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0);
    public static final KeyStroke MOVE_UP_ARROW = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
    public static final KeyStroke MOVE_DOWN_S = KeyStroke.getKeyStroke(KeyEvent.VK_S, 0);
    public static final KeyStroke MOVE_DOWN_ARROW = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
    public static final KeyStroke MOVE_LEFT_Q = KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0);
    public static final KeyStroke MOVE_LEFT_ARROW = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    public static final KeyStroke MOVE_RIGHT_D = KeyStroke.getKeyStroke(KeyEvent.VK_D, 0);
    public static final KeyStroke MOVE_RIGHT_ARROW = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    public static final KeyStroke TOGGLE_INVENTORY = KeyStroke.getKeyStroke(KeyEvent.VK_I, 0);
    public static final KeyStroke TOGGLE_CODEX = KeyStroke.getKeyStroke(KeyEvent.VK_X, 0);
    public static final KeyStroke TOGGLE_DEV_CONSOLE = KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
    public static final KeyStroke TOGGLE_WALKABILITY = KeyStroke.getKeyStroke(KeyEvent.VK_W, 0);
    public static final KeyStroke INTERACT = KeyStroke.getKeyStroke(KeyEvent.VK_E, 0);
    public static final KeyStroke NEXT_TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
    public static final KeyStroke PREV_TAB = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK);

    public static final KeyStroke MENU_UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
    public static final KeyStroke MENU_DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
    public static final KeyStroke MENU_LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    public static final KeyStroke MENU_RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    public static final KeyStroke MENU_CONFIRM = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    public static final KeyStroke MENU_CANCEL = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    public static final KeyStroke DROP_ITEM = KeyStroke.getKeyStroke(KeyEvent.VK_D, 0);

    public static final String ACTION_MOVE_UP = "move-up";
    public static final String ACTION_MOVE_DOWN = "move-down";
    public static final String ACTION_MOVE_LEFT = "move-left";
    public static final String ACTION_MOVE_RIGHT = "move-right";
    public static final String ACTION_TOGGLE_INVENTORY = "toggle-inventory";
    public static final String ACTION_TOGGLE_CODEX = "toggle-codex";
    public static final String ACTION_TOGGLE_DEV_CONSOLE = "toggle-dev-console";
    public static final String ACTION_TOGGLE_WALKABILITY = "toggle-walkability";
    public static final String ACTION_INTERACT = "interact";
    public static final String ACTION_TOGGLE_PAUSE = "toggle-pause";
    public static final String ACTION_NEXT_TAB = "next-tab";
    public static final String ACTION_PREV_TAB = "prev-tab";

    public static final String ACTION_MENU_UP = "menu-up";
    public static final String ACTION_MENU_DOWN = "menu-down";
    public static final String ACTION_MENU_LEFT = "menu-left";
    public static final String ACTION_MENU_RIGHT = "menu-right";
    public static final String ACTION_MENU_CONFIRM = "menu-confirm";
    public static final String ACTION_MENU_CANCEL = "menu-cancel";
    public static final String ACTION_DROP_ITEM = "drop-item";

    public static final String ACTION_DEV_CONSOLE_COMPLETE = "dev-console-complete";
    public static final String ACTION_DEV_CONSOLE_HISTORY_UP = "dev-console-history-up";
    public static final String ACTION_DEV_CONSOLE_HISTORY_DOWN = "dev-console-history-down";
    public static final String ACTION_DEV_CONSOLE_DISMISS_OVERLAY = "dev-console-dismiss-overlay";

    private Keybindings() {
    }
}
