package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Optional;

/**
 * An item's full detail panel: an {@link InspectableDetailPanel} showing the field table plus
 * effects when present, with base-damage editing on top. A Base Damage row can be armed with
 * Enter and stepped with Left/Right, as in {@link PlayerDetailPanel}; each step goes through the
 * item's {@link DevConsoleFieldMutator}, so the command bar's floor and min-above-max rules apply
 * unchanged. This panel takes the keyboard focus, so it owns every key binding, Up/Down included.
 */
public final class ItemDetailPanel extends JPanel {

    private static final int FIELD_TABLE = 0;
    private static final String ONE_STEP = "1";

    private final InspectableDetailPanel details;
    private final DevConsoleFieldMutator fieldMutator;

    private Optional<ItemField> armedField = Optional.empty();

    public ItemDetailPanel(Item item, DevConsoleFieldMutator fieldMutator) {
        this.details = new InspectableDetailPanel(item.getName(), item);
        this.fieldMutator = fieldMutator;

        setBackground(WidgetTheme.background());
        setLayout(new BorderLayout());
        setFocusable(true);
        add(details, BorderLayout.CENTER);

        bindKeys();
    }

    public String title() {
        return details.title();
    }

    public int tableCount() {
        return details.tableCount();
    }

    public TableWidget<List<String>> table(int index) {
        return details.table(index);
    }

    private void bindKeys() {
        bind(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP, this::handleUp);
        bind(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN, this::handleDown);
        bind(Keybindings.MENU_LEFT, Keybindings.ACTION_MENU_LEFT, () -> step(DevConsoleMutationVerb.SUBTRACT));
        bind(Keybindings.MENU_RIGHT, Keybindings.ACTION_MENU_RIGHT, () -> step(DevConsoleMutationVerb.ADD));
        bind(Keybindings.MENU_CONFIRM, Keybindings.ACTION_MENU_CONFIRM, this::handleEnter);
        // MENU_CANCEL (Escape) is only bound while a row is armed - see PlayerDetailPanel.bindKeys()
        // for why an always-present binding would block DevConsolePanel's Escape back-navigation.
        getActionMap().put(Keybindings.ACTION_MENU_CANCEL, action(this::disarm));
    }

    private void bind(KeyStroke key, String actionName, Runnable handler) {
        getInputMap(WHEN_FOCUSED).put(key, actionName);
        getActionMap().put(actionName, action(handler));
    }

    private static AbstractAction action(Runnable handler) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handler.run();
            }
        };
    }

    private void handleUp() {
        if (armedField.isEmpty()) {
            details.moveUp();
        }
    }

    private void handleDown() {
        if (armedField.isEmpty()) {
            details.moveDown();
        }
    }

    private void handleEnter() {
        if (armedField.isPresent()) {
            disarm();
            return;
        }
        selectedDamageField().ifPresent(this::arm);
    }

    private Optional<ItemField> selectedDamageField() {
        if (!details.isTableFocused(FIELD_TABLE)) {
            return Optional.empty();
        }
        return ItemField.fromDisplayName(fieldTable().getSelectedRow().get(0));
    }

    private void step(DevConsoleMutationVerb verb) {
        armedField.ifPresent(field -> {
            if (fieldMutator.apply(verb, field.token(), ONE_STEP)
                    instanceof DevConsoleMutationResult.Success success) {
                TableWidget<List<String>> table = fieldTable();
                table.updateRow(table.getSelectedRowIndex(),
                        List.of(field.displayName(), String.valueOf(success.newValue())));
            }
        });
    }

    private void arm(ItemField field) {
        armedField = Optional.of(field);
        fieldTable().setSelectedRowAccentColor(WidgetTheme.validHighlight());
        fieldTable().setOtherRowsDimmed(true);
        getInputMap(WHEN_FOCUSED).put(Keybindings.MENU_CANCEL, Keybindings.ACTION_MENU_CANCEL);
    }

    private void disarm() {
        armedField = Optional.empty();
        fieldTable().setSelectedRowAccentColor(null);
        fieldTable().setOtherRowsDimmed(false);
        getInputMap(WHEN_FOCUSED).remove(Keybindings.MENU_CANCEL);
    }

    private TableWidget<List<String>> fieldTable() {
        return details.table(FIELD_TABLE);
    }
}
