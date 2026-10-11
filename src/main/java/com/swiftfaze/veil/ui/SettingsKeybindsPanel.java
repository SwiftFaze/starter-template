package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.config.SettingsConfig;
import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SettingsKeybindsPanel extends JPanel implements HintAware {
    private static final Font ROW_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final List<String> FOOTER_ACTIONS = List.of("Go back", "Reset to Defaults", "Cancel", "Apply");
    private static final List<ControlsHintBarWidget.Hint> TABLE_HINTS =
            List.of(new ControlsHintBarWidget.Hint("enter", "Rebind"), new ControlsHintBarWidget.Hint("escape", "Back"));
    private static final List<ControlsHintBarWidget.Hint> FOOTER_HINTS = List.of(
            new ControlsHintBarWidget.Hint("up", "Back to table"),
            new ControlsHintBarWidget.Hint("left", "Previous"),
            new ControlsHintBarWidget.Hint("right", "Next"),
            new ControlsHintBarWidget.Hint("enter", "Select"),
            new ControlsHintBarWidget.Hint("escape", "Back"));
    private static final List<ControlsHintBarWidget.Hint> CAPTURE_HINTS =
            List.of(new ControlsHintBarWidget.Hint("any key", "Set binding"));

    private record ActionRow(String action, String key) {
    }

    private final List<String> actions;
    private final Map<String, String> keyBindings;
    private final Map<String, String> committedBindings;
    private final Consumer<String> onBack;
    private final ControlsHintBarWidget hintBar;
    private final SettingsStore settingsStore;
    private final TableWidget<ActionRow> actionsTable;
    private final JPanel footerPanel;
    private final ResetConfirmationPopup discardConfirmationPopup;
    private final ResetConfirmationPopup resetConfirmationPopup;

    private boolean footerFocused = false;
    private int footerIndex = 0;
    private boolean popupOpen = false;
    private Runnable pendingDiscardAction = () -> {};

    public SettingsKeybindsPanel(Consumer<String> onBack, ControlsHintBarWidget hintBar, SettingsStore settingsStore) {
        this.onBack = onBack;
        this.hintBar = hintBar;
        this.settingsStore = settingsStore;
        this.actions = List.of("Move up", "Move down", "Move left", "Move right", "Toggle inventory");
        this.keyBindings = new LinkedHashMap<>(settingsStore.config().getKeybinds());
        this.committedBindings = new LinkedHashMap<>(settingsStore.config().getKeybinds());

        setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WidgetTheme.border(), 2),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)));
        setFocusable(true);

        JLabel header = new JLabel("Keybinds");
        header.setForeground(WidgetTheme.normalText());
        header.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionsTable = new TableWidget<>(List.of(ActionRow::action, ActionRow::key), List.of("Action", "Key"));
        actionsTable.setWrapAround(false);
        actionsTable.setAlignmentX(Component.CENTER_ALIGNMENT);

        footerPanel = new JPanel();
        footerPanel.setBackground(WidgetTheme.background());
        footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
        footerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        add(Box.createVerticalGlue());
        add(header);
        add(Box.createVerticalStrut(20));
        add(actionsTable);
        add(footerPanel);
        add(Box.createVerticalGlue());

        actionsTable.setRows(buildRows());
        refreshFooter();
        addKeyListener(new KeybindsKeyListener());

        discardConfirmationPopup = new ResetConfirmationPopup("Discard Changes", "Discard unsaved keybind changes?");
        resetConfirmationPopup = new ResetConfirmationPopup("Confirm Reset", "Reset all keybinds to their defaults?");

        discardConfirmationPopup.setOnYes(() -> {
            revertBindings();
            pendingDiscardAction.run();
        });

        resetConfirmationPopup.setOnYes(this::resetBindingsToDefaults);

        refreshHints();
    }

    private List<ActionRow> buildRows() {
        List<ActionRow> rows = new ArrayList<>();
        for (String action : actions) {
            rows.add(new ActionRow(action, keyBindings.getOrDefault(action, "")));
        }
        return rows;
    }

    private boolean isDirty() {
        return !keyBindings.equals(committedBindings);
    }

    private void revertBindings() {
        keyBindings.clear();
        keyBindings.putAll(committedBindings);
        actionsTable.setRows(buildRows());
    }

    private void resetBindingsToDefaults() {
        SettingsConfig defaults = new SettingsConfig();
        keyBindings.clear();
        keyBindings.putAll(defaults.getKeybinds());
        actionsTable.setRows(buildRows());
    }

    public String getHighlightedActionName() {
        ActionRow row = actionsTable.getSelectedRow();
        return row == null ? "" : row.action();
    }

    public void moveUp() {
        if (footerFocused) {
            footerFocused = false;
            actionsTable.setSelectable(true);
        } else {
            actionsTable.moveUp();
        }
        refreshFooter();
        refreshHints();
    }

    public void moveDown() {
        if (footerFocused) {
            return;
        }
        if (actionsTable.isAtLastRow()) {
            footerFocused = true;
            footerIndex = 0;
            actionsTable.setSelectable(false);
        } else {
            actionsTable.moveDown();
        }
        refreshFooter();
        refreshHints();
    }

    public void moveFooterLeft() {
        if (footerFocused && footerIndex > 0) {
            footerIndex--;
            refreshFooter();
            refreshHints();
        }
    }

    public void moveFooterRight() {
        if (footerFocused && footerIndex < FOOTER_ACTIONS.size() - 1) {
            footerIndex++;
            refreshFooter();
            refreshHints();
        }
    }

    public void confirm() {
        if (!footerFocused) {
            popupOpen = true;
            applyArmedStyle();
        } else {
            String action = getHighlightedFooterAction();
            switch (action) {
                case "Apply" -> handleApply();
                case "Cancel" -> handleCancel();
                case "Go back" -> handleGoBack();
                case "Reset to Defaults" -> handleResetToDefaults();
                default -> {
                    // No footer action highlighted
                }
            }
        }
        refreshHints();
    }

    private void handleApply() {
        committedBindings.clear();
        committedBindings.putAll(keyBindings);
        settingsStore.config().setKeybinds(new LinkedHashMap<>(keyBindings));
        settingsStore.persist();
        onBack.accept("settings");
    }

    private void handleCancel() {
        if (isDirty()) {
            openDiscardConfirmation(() -> {});
        }
    }

    private void handleGoBack() {
        if (isDirty()) {
            openDiscardConfirmation(() -> onBack.accept("settings"));
        } else {
            onBack.accept("settings");
        }
    }

    private void handleResetToDefaults() {
        resetConfirmationPopup.open();
    }

    private void openDiscardConfirmation(Runnable afterDiscard) {
        pendingDiscardAction = afterDiscard;
        discardConfirmationPopup.open();
    }

    public void back() {
        if (isDirty()) {
            openDiscardConfirmation(() -> onBack.accept("settings"));
        } else {
            onBack.accept("settings");
        }
    }

    public String getKeyForAction(String action) {
        return keyBindings.getOrDefault(action, "");
    }

    public void updateKeyForAction(String action, String key) {
        keyBindings.put(action, key);
        actionsTable.updateRow(actions.indexOf(action), new ActionRow(action, key));
        popupOpen = false;
        applyArmedStyle();
        refreshHints();
    }

    public boolean isPopupOpen() {
        return popupOpen;
    }

    public String getHighlightedFooterAction() {
        return FOOTER_ACTIONS.get(footerIndex);
    }

    public void highlightFooterAction(String action) {
        footerFocused = true;
        footerIndex = FOOTER_ACTIONS.indexOf(action);
        actionsTable.setSelectable(false);
        refreshFooter();
        refreshHints();
    }

    public void pressKey(String key) {
        if (popupOpen) {
            updateKeyForAction(getHighlightedActionName(), key);
        }
    }

    public ResetConfirmationPopup getDiscardConfirmationPopup() {
        return discardConfirmationPopup;
    }

    public ResetConfirmationPopup getResetConfirmationPopup() {
        return resetConfirmationPopup;
    }

    private void applyArmedStyle() {
        actionsTable.setSelectedRowAccentColor(popupOpen ? WidgetTheme.validHighlight() : null);
        actionsTable.setOtherRowsDimmed(popupOpen);
    }

    private void refreshFooter() {
        footerPanel.removeAll();
        for (int i = 0; i < FOOTER_ACTIONS.size(); i++) {
            JLabel label = new JLabel(FOOTER_ACTIONS.get(i));
            label.setFont(ROW_FONT);
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            boolean highlighted = footerFocused && i == footerIndex;
            label.setForeground(highlighted ? WidgetTheme.selectedText() : WidgetTheme.normalText());
            label.setBackground(highlighted ? WidgetTheme.selectedHighlight() : WidgetTheme.background());
            label.setOpaque(true);
            footerPanel.add(label);
            if (i < FOOTER_ACTIONS.size() - 1) {
                footerPanel.add(Box.createHorizontalStrut(20));
            }
        }
        revalidate();
        repaint();
    }

    private List<ControlsHintBarWidget.Hint> computeHints() {
        if (popupOpen) {
            return CAPTURE_HINTS;
        }
        return footerFocused ? FOOTER_HINTS : TABLE_HINTS;
    }

    @Override
    public void refreshHints() {
        hintBar.setHints(computeHints());
    }

    public List<String> getAllActions() {
        return actions;
    }

    private class KeybindsKeyListener extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (popupOpen) {
                pressKey(KeyEvent.getKeyText(e.getKeyCode()));
                return;
            }
            handleNavigationKey(e.getKeyCode());
        }

        private void handleNavigationKey(int keyCode) {
            switch (keyCode) {
                case KeyEvent.VK_UP -> moveUp();
                case KeyEvent.VK_DOWN -> moveDown();
                case KeyEvent.VK_LEFT -> moveFooterLeft();
                case KeyEvent.VK_RIGHT -> moveFooterRight();
                case KeyEvent.VK_ENTER -> confirm();
                case KeyEvent.VK_ESCAPE -> back();
                default -> {
                    // Not a navigation key
                }
            }
        }
    }
}
