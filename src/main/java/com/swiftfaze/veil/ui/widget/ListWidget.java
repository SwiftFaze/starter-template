package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.input.Keybindings;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListWidget<T> extends Widget {

    private final List<T> items = new ArrayList<>();
    private final Function<T, String> itemRenderer;
    private int selectedIndex = 0;
    private boolean wrapAround = true;
    private Consumer<T> onConfirm = t -> {
    };
    private Consumer<T> onSelectionChange = t -> {
    };
    private final List<JLabel> labels = new ArrayList<>();

    public ListWidget(Function<T, String> itemRenderer) {
        this.itemRenderer = itemRenderer;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        bindKeys();
    }

    public void setItems(List<T> items) {
        this.items.clear();
        this.items.addAll(items);
        this.selectedIndex = 0;
        refresh();
    }

    public void setOnConfirm(Consumer<T> onConfirm) {
        this.onConfirm = onConfirm;
    }

    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    public void setOnSelectionChange(Consumer<T> onSelectionChange) {
        this.onSelectionChange = onSelectionChange;
    }

    public T getSelectedItem() {
        return items.isEmpty() ? null : items.get(selectedIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void moveUp() {
        if (items.isEmpty()) {
            return;
        }
        selectedIndex = wrapAround
                ? (selectedIndex - 1 + items.size()) % items.size()
                : Math.max(0, selectedIndex - 1);
        refreshHighlight();
    }

    public void moveDown() {
        if (items.isEmpty()) {
            return;
        }
        selectedIndex = wrapAround
                ? (selectedIndex + 1) % items.size()
                : Math.min(items.size() - 1, selectedIndex + 1);
        refreshHighlight();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(Keybindings.MENU_UP, "list-up");
        inputMap.put(Keybindings.MENU_DOWN, "list-down");
        inputMap.put(Keybindings.MENU_CONFIRM, "list-confirm");

        actionMap.put("list-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });

        actionMap.put("list-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });

        actionMap.put("list-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                T selected = getSelectedItem();
                if (selected != null) {
                    onConfirm.accept(selected);
                }
            }
        });
    }

    private void refresh() {
        removeAll();
        labels.clear();

        for (T item : items) {
            JLabel label = new JLabel(itemRenderer.apply(item));
            label.setOpaque(true);
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            label.setAlignmentX(LEFT_ALIGNMENT);
            label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            labels.add(label);
            add(label);
        }

        // Stretch each row to the widget's full width so the selected-row highlight fills the
        // whole row, not just the space behind its text - computed after every label has its
        // final text/border, since an empty label's preferred size would be wrong.
        for (JLabel label : labels) {
            label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        }

        refreshHighlight();
        revalidate();
        repaint();
    }

    private void refreshHighlight() {
        for (int i = 0; i < labels.size(); i++) {
            WidgetTheme.applySelection(labels.get(i), i == selectedIndex);
        }
        if (selectedIndex < labels.size()) {
            scrollRectToVisible(labels.get(selectedIndex).getBounds());
        }
        onSelectionChange.accept(getSelectedItem());
    }
}
