package com.swiftfaze.veil.ui;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.config.SettingsStore;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.RadioGroupWidget;
import com.swiftfaze.veil.ui.widget.SliderWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SettingsScreenPanel extends JPanel implements HintAware {
    private static final Font ROW_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final List<ControlsHintBarWidget.Hint> TAIL_HINTS =
            List.of(new ControlsHintBarWidget.Hint("enter", "Select"), new ControlsHintBarWidget.Hint("escape", "Back"));

    private final List<SettingsRow> rows;
    private final JPanel rowsPanel;
    private int selectedIndex = 0;
    private final Consumer<String> onBack;
    private final Consumer<String> onOpenFolder;
    private final ControlsHintBarWidget hintBar;
    private final ResetConfirmationPopup resetConfirmationPopup;
    private final SettingsStore settingsStore;
    private Consumer<String> onWindowModeChanged = mode -> { };
    private String backTarget = "title";

    private static class SettingsRow {
        final String name;
        final Object widget; // SliderWidget, RadioGroupWidget<String>, or null for actions

        SettingsRow(String name, @Nullable Object widget) {
            this.name = name;
            this.widget = widget;
        }

        String displayValue() {
            if (widget instanceof SliderWidget slider) {
                return slider.getDisplayText();
            } else if (widget instanceof RadioGroupWidget<?> radio) {
                return String.valueOf(radio.getHighlightedOption());
            }
            return "";
        }
    }

    public SettingsScreenPanel(Consumer<String> onBack, Consumer<String> onOpenFolder, ControlsHintBarWidget hintBar, SettingsStore settingsStore) {
        this.onBack = onBack;
        this.onOpenFolder = onOpenFolder;
        this.hintBar = hintBar;
        this.settingsStore = settingsStore;
        this.rows = new ArrayList<>();

        setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(WidgetTheme.border(), 2),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)));
        setFocusable(true);

        JLabel header = new JLabel("Settings");
        header.setForeground(WidgetTheme.normalText());
        header.setFont(new Font(Font.MONOSPACED, Font.BOLD, 24));
        header.setAlignmentX(Component.CENTER_ALIGNMENT);

        rowsPanel = new JPanel();
        rowsPanel.setBackground(WidgetTheme.background());
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        rowsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(header);
        add(Box.createVerticalStrut(20));
        add(rowsPanel);
        add(Box.createVerticalGlue());

        initializeRows();
        refresh();
        bindKeys();

        resetConfirmationPopup = new ResetConfirmationPopup();
        resetConfirmationPopup.setOnYes(this::resetAllToDefaults);
    }

    public void setOnWindowModeChanged(Consumer<String> onWindowModeChanged) {
        this.onWindowModeChanged = onWindowModeChanged;
        onWindowModeChanged.accept(settingsStore.config().getFullscreen());
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, "settings-up");
        inputMap.put(Keybindings.MENU_DOWN, "settings-down");
        inputMap.put(Keybindings.MENU_LEFT, "settings-left");
        inputMap.put(Keybindings.MENU_RIGHT, "settings-right");
        inputMap.put(Keybindings.MENU_CONFIRM, "settings-confirm");
        inputMap.put(Keybindings.MENU_CANCEL, "settings-cancel");

        actionMap.put("settings-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        actionMap.put("settings-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
        actionMap.put("settings-left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveLeft();
            }
        });
        actionMap.put("settings-right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveRight();
            }
        });
        actionMap.put("settings-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirm();
            }
        });
        actionMap.put("settings-cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                back();
            }
        });
    }

    private void initializeRows() {
        rows.clear();

        var config = settingsStore.config();

        rows.add(new SettingsRow("Brightness", new SliderWidget(0, 10, 1, config.getBrightness())));

        RadioGroupWidget<String> fullscreenRadio = new RadioGroupWidget<>(s -> s, /* horizontal= */ true);
        fullscreenRadio.setOptions(List.of("Windowed", "Fullscreen"));
        fullscreenRadio.selectAndHighlightOption(List.of("Windowed", "Fullscreen").indexOf(config.getFullscreen()));
        rows.add(new SettingsRow("Fullscreen", fullscreenRadio));

        RadioGroupWidget<String> fontRadio = new RadioGroupWidget<>(s -> s, /* horizontal= */ true);
        fontRadio.setOptions(List.of("Monospaced", "Serif", "SansSerif"));
        fontRadio.selectAndHighlightOption(List.of("Monospaced", "Serif", "SansSerif").indexOf(config.getFont()));
        rows.add(new SettingsRow("Font", fontRadio));

        RadioGroupWidget<String> themeRadio = new RadioGroupWidget<>(s -> s, /* horizontal= */ true);
        themeRadio.setOptions(List.of("Default", "Midnight", "Sunrise"));
        themeRadio.selectAndHighlightOption(List.of("Default", "Midnight", "Sunrise").indexOf(config.getTheme()));
        rows.add(new SettingsRow("Theme", themeRadio));

        rows.add(new SettingsRow("Volume", new SliderWidget(0, 10, 1, config.getVolume())));
        rows.add(new SettingsRow("Keybinds", null));
        rows.add(new SettingsRow("Open Game Folder", null));
        rows.add(new SettingsRow("Open Mod Folder", null));
        rows.add(new SettingsRow("About", null));
        rows.add(new SettingsRow("Reset to Defaults", null));
        rows.add(new SettingsRow("Go Back", null));
    }

    public String getHighlightedItemName() {
        return rows.get(selectedIndex).name;
    }

    public ResetConfirmationPopup getResetConfirmationPopup() {
        return resetConfirmationPopup;
    }

    public void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            refresh();
        }
    }

    public void moveDown() {
        if (selectedIndex < rows.size() - 1) {
            selectedIndex++;
            refresh();
        }
    }

    public void moveLeft() {
        SettingsRow row = rows.get(selectedIndex);
        if (row.widget instanceof SliderWidget slider) {
            slider.moveLeft();
        } else if (row.widget instanceof RadioGroupWidget<?> radio) {
            radio.moveLeft();
        }
        refresh();
        syncAndPersist(row);
    }

    public void moveRight() {
        SettingsRow row = rows.get(selectedIndex);
        if (row.widget instanceof SliderWidget slider) {
            slider.moveRight();
        } else if (row.widget instanceof RadioGroupWidget<?> radio) {
            radio.moveRight();
        }
        refresh();
        syncAndPersist(row);
    }

    private void syncAndPersist(SettingsRow row) {
        switch (row.name) {
            case "Brightness" -> settingsStore.config().setBrightness(((SliderWidget) row.widget).getValue());
            case "Fullscreen" -> settingsStore.config().setFullscreen(String.valueOf(((RadioGroupWidget<?>) row.widget).getHighlightedOption()));
            case "Font" -> settingsStore.config().setFont(String.valueOf(((RadioGroupWidget<?>) row.widget).getHighlightedOption()));
            case "Theme" -> settingsStore.config().setTheme(String.valueOf(((RadioGroupWidget<?>) row.widget).getHighlightedOption()));
            case "Volume" -> settingsStore.config().setVolume(((SliderWidget) row.widget).getValue());
            default -> { return; }
        }
        settingsStore.persist();
        if ("Fullscreen".equals(row.name)) {
            onWindowModeChanged.accept(settingsStore.config().getFullscreen());
        }
    }

    public void confirm() {
        SettingsRow row = rows.get(selectedIndex);
        switch (row.name) {
            case "Keybinds" -> onBack.accept("keybinds");
            case "Open Game Folder" -> onOpenFolder.accept("game");
            case "Open Mod Folder" -> onOpenFolder.accept("mods");
            case "Go Back" -> back();
            case "About" -> {
                // Placeholder action
            }
            case "Reset to Defaults" -> resetConfirmationPopup.open();
            default -> {
                // Slider and radio rows act on Left/Right, not confirm
            }
        }
    }

    public void setBackTarget(String backTarget) {
        this.backTarget = backTarget;
    }

    public String getBackTarget() {
        return backTarget;
    }

    public void back() {
        onBack.accept(backTarget);
    }

    private void resetAllToDefaults() {
        settingsStore.config().resetToDefaults();
        initializeRows();
        refresh();
        settingsStore.persist();
        onWindowModeChanged.accept(settingsStore.config().getFullscreen());
    }

    public String getItemValue(String itemName) {
        for (SettingsRow row : rows) {
            if (row.name.equals(itemName)) {
                return row.displayValue();
            }
        }
        return "";
    }

    public int getSliderValue(String itemName) {
        for (SettingsRow row : rows) {
            if (row.name.equals(itemName) && row.widget instanceof SliderWidget slider) {
                return slider.getValue();
            }
        }
        return 0;
    }

    public String getRadioValue(String itemName) {
        for (SettingsRow row : rows) {
            if (row.name.equals(itemName) && row.widget instanceof RadioGroupWidget<?> radio) {
                return String.valueOf(radio.getHighlightedOption());
            }
        }
        return "";
    }

    private void refresh() {
        rowsPanel.removeAll();
        List<JLabel> labels = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            SettingsRow row = rows.get(i);
            String text = row.widget == null ? row.name : row.name + ": " + row.displayValue();
            JLabel label = new JLabel(text);
            label.setFont(ROW_FONT);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);
            label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            label.setForeground(i == selectedIndex ? WidgetTheme.selectedText() : WidgetTheme.normalText());
            label.setBackground(i == selectedIndex ? WidgetTheme.selectedHighlight() : WidgetTheme.background());
            label.setOpaque(true);
            labels.add(label);
            rowsPanel.add(label);
        }

        // Every row shares the widest row's width, matching RadioGroupWidget's vertical-mode
        // convention - otherwise each row shrink-wraps to its own text and the list reads as a
        // ragged block instead of a uniform menu.
        int maxWidth = 0;
        for (JLabel label : labels) {
            maxWidth = Math.max(maxWidth, label.getPreferredSize().width);
        }
        for (JLabel label : labels) {
            Dimension size = new Dimension(maxWidth, label.getPreferredSize().height);
            label.setMaximumSize(size);
            label.setPreferredSize(size);
        }

        revalidate();
        repaint();
        refreshHints();
    }

    private List<ControlsHintBarWidget.Hint> computeHints() {
        List<ControlsHintBarWidget.Hint> hints = new ArrayList<>();
        Object widget = rows.get(selectedIndex).widget;
        if (widget instanceof SliderWidget) {
            hints.add(new ControlsHintBarWidget.Hint("left", "Decrease"));
            hints.add(new ControlsHintBarWidget.Hint("right", "Increase"));
        } else if (widget instanceof RadioGroupWidget<?>) {
            hints.add(new ControlsHintBarWidget.Hint("left", "Previous"));
            hints.add(new ControlsHintBarWidget.Hint("right", "Next"));
        }
        hints.addAll(TAIL_HINTS);
        return hints;
    }

    @Override
    public void refreshHints() {
        hintBar.setHints(computeHints());
    }

    public List<String> getAllItemNames() {
        List<String> names = new ArrayList<>();
        for (SettingsRow row : rows) {
            names.add(row.name);
        }
        return names;
    }
}
