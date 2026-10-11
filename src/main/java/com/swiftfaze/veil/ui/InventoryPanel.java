package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.PopupWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.border.Border;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class InventoryPanel extends PopupWidget {

    private static final List<ControlsHintBarWidget.Hint> LIST_HINTS_TAIL =
            List.of(new ControlsHintBarWidget.Hint("d", "Drop"), new ControlsHintBarWidget.Hint("escape", "Close"));

    private final ListWidget<Item> itemList;
    private final DetailsPaneWidget detailsPane;
    private final DropConfirmationPopup dropConfirmationPopup;
    private final ControlsHintBarWidget hintBar;

    public InventoryPanel(ControlsHintBarWidget hintBar) {
        this.hintBar = hintBar;
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0, WidgetTheme.border());
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        setBorder(BorderFactory.createCompoundBorder(bottomLine, padding));

        addContent(new HeaderWidget("Inventory"));

        itemList = new ListWidget<>(Item::getName);
        itemList.setWrapAround(false);
        itemList.setFocusTraversalKeysEnabled(false);
        detailsPane = new DetailsPaneWidget();
        itemList.setOnSelectionChange(detailsPane::showEntry);

        dropConfirmationPopup = new DropConfirmationPopup();
        dropConfirmationPopup.setOnDismiss(this::requestFocusInWindow);

        addContent(ListDetailLayoutUtility.buildBody(ListDetailLayoutUtility.buildScrollPane(itemList), detailsPane));
        bindDropKey();
        refreshHints();
    }

    public void showItems(List<Item> items) {
        itemList.setItems(items);
    }

    public int getSelectedIndex() {
        return itemList.getSelectedIndex();
    }

    public Item getSelectedItem() {
        return itemList.getSelectedItem();
    }

    public TableWidget<List<String>> getFieldsTable() {
        return getTableOrEmpty(0);
    }

    public TableWidget<List<String>> getEffectsTable() {
        return getTableOrEmpty(1);
    }

    private TableWidget<List<String>> getTableOrEmpty(int index) {
        TableWidget<List<String>> table = detailsPane.getTable(index);
        return table != null ? table : TableWidget.ofRows(List.of("Field", "Value"), List.of());
    }

    public DropConfirmationPopup getDropConfirmationPopup() {
        return dropConfirmationPopup;
    }

    public boolean isFieldsTableFocused() {
        return detailsPane.isTableFocused(0);
    }

    public boolean isEffectsTableFocused() {
        return detailsPane.isTableFocused(1);
    }

    public boolean isItemListFocused() {
        return !detailsPane.hasFocus();
    }

    @Override
    public void open() {
        super.open();
        refreshHints();
    }

    @Override
    protected void onUp() {
        if (detailsPane.hasFocus()) {
            detailsPane.moveUp();
        } else {
            itemList.moveUp();
        }
    }

    @Override
    protected void onDown() {
        if (detailsPane.hasFocus()) {
            detailsPane.moveDown();
        } else {
            itemList.moveDown();
        }
    }

    @Override
    protected void onLeft() {
        if (detailsPane.hasFocus()) {
            detailsPane.clearFocus();
            refreshHints();
        }
    }

    @Override
    protected void onRight() {
        if (!detailsPane.hasFocus()) {
            detailsPane.focusFirstTable();
            refreshHints();
        }
    }

    private void refreshHints() {
        List<ControlsHintBarWidget.Hint> hints = new ArrayList<>();
        hints.add(detailsPane.hasFocus()
                ? new ControlsHintBarWidget.Hint("left", "Back to list")
                : new ControlsHintBarWidget.Hint("right", "View details"));
        hints.addAll(LIST_HINTS_TAIL);
        hintBar.setHints(hints);
    }

    private void bindDropKey() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        inputMap.put(Keybindings.DROP_ITEM, Keybindings.ACTION_DROP_ITEM);
        actionMap.put(Keybindings.ACTION_DROP_ITEM, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Item selected = itemList.getSelectedItem();
                if (selected != null) {
                    dropConfirmationPopup.open(selected.getName());
                }
            }
        });
    }
}
