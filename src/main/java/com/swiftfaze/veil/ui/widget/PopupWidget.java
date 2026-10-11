package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.input.Keybindings;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class PopupWidget extends Widget {

    private final JPanel contentPanel;
    private Runnable onDismiss = () -> {
    };
    private final List<Runnable> dismissListeners = new ArrayList<>();
    private FocusManager focusManager;

    public PopupWidget() {
        setLayout(new BorderLayout());
        setVisible(false);

        contentPanel = new JPanel();
        contentPanel.setBackground(WidgetTheme.background());
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        add(contentPanel, BorderLayout.CENTER);

        // Without this, Java's default focus-traversal handling consumes Tab/Shift+Tab on
        // whichever component currently holds keyboard focus (the popup itself, or a content
        // widget a subclass has explicitly focused, e.g. DropConfirmationPopup's radio choice)
        // before this popup's own onUp/onDown/onLeft/onRight routing (or a subclass's own Tab
        // binding, e.g. CodexPanel's tab switcher) ever sees the key event — silently kicking
        // keyboard focus onto some other descendant (typically the first focusable list/table)
        // whose own WHEN_FOCUSED bindings then intercept subsequent arrow-key presses instead.
        setFocusTraversalKeysEnabled(false);

        bindKeys();
    }

    public void setFocusManager(FocusManager focusManager) {
        this.focusManager = focusManager;
    }

    public void setOnDismiss(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }

    /**
     * Adds a listener run on every dismiss, after {@link #setOnDismiss}'s callback. Unlike
     * that single callback, several can be added, so a wiring layer can't overwrite another's.
     */
    public void addDismissListener(Runnable listener) {
        dismissListeners.add(listener);
    }

    public void addContent(JComponent component) {
        contentPanel.add(component);
    }

    public void removeAllContent() {
        contentPanel.removeAll();
    }

    /** Where {@link FillLayout} puts a popup inside its parent. */
    public enum Placement {
        /** Stretched over the whole parent. */
        FULL_SCREEN,
        /** Centred at its preferred size. */
        CENTERED,
        /** A box along the bottom edge, sized from the parent (the RPG dialogue box). */
        BOTTOM_CENTER
    }

    /**
     * Returns true if this popup should stretch to fill the parent container,
     * false if it should be sized and centered at its preferred size.
     * Subclasses can override to present smaller, compact popups.
     */
    public boolean isFullScreen() {
        return true;
    }

    /**
     * The placement {@link FillLayout} honours. Defaults from {@link #isFullScreen()}; a popup
     * that needs another placement overrides this directly.
     */
    public Placement placement() {
        return isFullScreen() ? Placement.FULL_SCREEN : Placement.CENTERED;
    }

    public void open() {
        setVisible(true);
        if (focusManager != null) {
            focusManager.captureModally();
        }
        requestFocusInWindow();
    }

    public void dismiss() {
        setVisible(false);
        if (focusManager != null) {
            focusManager.restoreFocus();
        }
        onDismiss.run();
        for (Runnable listener : dismissListeners) {
            listener.run();
        }
    }

    /**
     * Hook for a subclass with scrollable/navigable content (e.g. an item
     * list) to move its own selection. No-op by default, since not every
     * popup has anything to navigate.
     */
    protected void onUp() {
    }

    protected void onDown() {
    }

    protected void onLeft() {
    }

    protected void onRight() {
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_CANCEL, "popup-dismiss");
        inputMap.put(Keybindings.MENU_UP, "popup-up");
        inputMap.put(Keybindings.MENU_DOWN, "popup-down");
        inputMap.put(Keybindings.MENU_LEFT, "popup-left");
        inputMap.put(Keybindings.MENU_RIGHT, "popup-right");

        actionMap.put("popup-dismiss", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dismiss();
            }
        });

        actionMap.put("popup-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUp();
            }
        });

        actionMap.put("popup-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDown();
            }
        });

        actionMap.put("popup-left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onLeft();
            }
        });

        actionMap.put("popup-right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onRight();
            }
        });
    }
}
