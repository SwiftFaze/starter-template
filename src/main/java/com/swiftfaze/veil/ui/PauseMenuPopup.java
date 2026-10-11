package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.PopupWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * ESC-triggered pause overlay: Resume / Settings / Exit to Main Menu, following
 * the same PopupWidget overlay pattern as InventoryPanel/CodexPanel.
 */
public class PauseMenuPopup extends PopupWidget {

    public static final String RESUME = "Resume";
    public static final String SETTINGS = "Settings";
    public static final String EXIT_TO_MAIN_MENU = "Exit to Main Menu";

    private final ListWidget<String> menuList;
    private Consumer<String> onMenuSelect = item -> { };

    public PauseMenuPopup() {
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0, WidgetTheme.border());
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        setBorder(BorderFactory.createCompoundBorder(bottomLine, padding));

        addContent(makeTitleLabel());

        menuList = new ListWidget<>(s -> s);
        menuList.setWrapAround(false);
        menuList.setFocusTraversalKeysEnabled(false);
        menuList.setItems(List.of(RESUME, SETTINGS, EXIT_TO_MAIN_MENU));
        addContent(menuList);

        bindConfirmKey();
    }

    public void setOnMenuSelect(Consumer<String> onMenuSelect) {
        this.onMenuSelect = onMenuSelect;
    }

    public String getSelectedItem() {
        return menuList.getSelectedItem();
    }

    @Override
    protected void onUp() {
        menuList.moveUp();
    }

    @Override
    protected void onDown() {
        menuList.moveDown();
    }

    private void bindConfirmKey() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        inputMap.put(Keybindings.MENU_CONFIRM, "pause-confirm");
        actionMap.put("pause-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmSelection();
            }
        });
    }

    private void confirmSelection() {
        String selected = menuList.getSelectedItem();
        if (RESUME.equals(selected)) {
            dismiss();
        } else if (selected != null) {
            onMenuSelect.accept(selected);
        }
    }

    private static JLabel makeTitleLabel() {
        JLabel titleLabel = new JLabel("Paused");
        titleLabel.setForeground(WidgetTheme.normalText());
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return titleLabel;
    }
}
