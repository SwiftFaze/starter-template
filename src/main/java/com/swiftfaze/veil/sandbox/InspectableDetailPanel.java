package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * A dev-console panel for one {@link Inspectable}: a {@link HeaderWidget} title over its detail
 * tables in a standalone {@link DetailsPaneWidget}, with Up/Down moving through the rows. Both
 * children are left-aligned, as in {@link ClassDetailPanel} - BoxLayout offsets children whose
 * alignmentX differ, which pushed the header out of line with the tables below it.
 * {@link ItemDetailPanel} wraps one to add base-damage editing.
 */
public final class InspectableDetailPanel extends JPanel {

    private final String title;
    private final DetailsPaneWidget detailsPane;

    public InspectableDetailPanel(String title, Inspectable entry) {
        this.title = title;
        this.detailsPane = DetailsPaneWidget.standalone();
        this.detailsPane.setAlignmentX(LEFT_ALIGNMENT);
        this.detailsPane.showEntry(entry);
        this.detailsPane.focusFirstTable();

        setBackground(WidgetTheme.background());
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setFocusable(true);

        add(new HeaderWidget(title));
        add(detailsPane);

        bindKeys();
    }

    public String title() {
        return title;
    }

    public int tableCount() {
        return detailsPane.getTableCount();
    }

    public TableWidget<List<String>> table(int index) {
        return detailsPane.getTable(index);
    }

    public boolean isTableFocused(int index) {
        return detailsPane.isTableFocused(index);
    }

    public void moveUp() {
        detailsPane.moveUp();
    }

    public void moveDown() {
        detailsPane.moveDown();
    }

    private void bindKeys() {
        getInputMap(WHEN_FOCUSED).put(Keybindings.MENU_UP, Keybindings.ACTION_MENU_UP);
        getInputMap(WHEN_FOCUSED).put(Keybindings.MENU_DOWN, Keybindings.ACTION_MENU_DOWN);
        getActionMap().put(Keybindings.ACTION_MENU_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveUp();
            }
        });
        getActionMap().put(Keybindings.ACTION_MENU_DOWN, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                moveDown();
            }
        });
    }
}
