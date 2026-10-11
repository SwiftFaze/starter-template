package com.swiftfaze.veil.ui;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A shared details-pane widget that renders however many DetailTables an
 * Inspectable returns and routes Up/Down/Left/Right keyboard focus across them.
 * Replaces the hand-duplicated focus-routing logic in InventoryPanel and CodexPanel.
 * Package placement: com.swiftfaze.veil.ui (not .widget) — see ModuleDependencyTest
 * for ArchUnit constraints.
 */
public final class DetailsPaneWidget extends JPanel {

    private final JPanel detailsPanel;
    private final JScrollPane detailsScrollPane;
    private final List<TableWidget<List<String>>> tables = new ArrayList<>();
    private int focusedIndex = -1;

    /** A details pane beside a list (Codex, Inventory), with a divider against that list. */
    public DetailsPaneWidget() {
        this(ListDetailLayoutUtility.buildDetailsPanel());
    }

    private DetailsPaneWidget(JPanel detailsPanel) {
        this.detailsPanel = detailsPanel;
        detailsScrollPane = ListDetailLayoutUtility.buildScrollPane(detailsPanel);
        setOpaque(false);
        setLayout(new BorderLayout());
        add(detailsScrollPane, BorderLayout.CENTER);
        setFocusTraversalKeysEnabled(false);
    }

    /** A details pane shown on its own, with no list beside it and so no divider. */
    public static DetailsPaneWidget standalone() {
        return new DetailsPaneWidget(ListDetailLayoutUtility.buildStandaloneDetailsPanel());
    }

    public void showEntry(Inspectable entry) {
        detailsPanel.removeAll();
        tables.clear();
        focusedIndex = -1;

        if (entry == null) {
            detailsPanel.add(buildPlaceholderLabel("(no item selected)"));
        } else {
            addDetailTables(entry.getDetailTables());
        }

        detailsPanel.revalidate();
        detailsPanel.repaint();
        detailsScrollPane.getViewport().setViewPosition(new Point(0, 0));
    }

    @Override
    public boolean hasFocus() {
        return focusedIndex >= 0;
    }

    public void focusFirstTable() {
        if (tables.isEmpty()) {
            return;
        }
        focusedIndex = 0;
        applyHighlight();
        tables.get(0).moveToStart();
    }

    public void clearFocus() {
        focusedIndex = -1;
        applyHighlight();
    }

    public void moveUp() {
        if (!hasFocus()) {
            return;
        }
        if (tables.get(focusedIndex).isAtFirstRow() && focusedIndex > 0) {
            focusedIndex--;
            applyHighlight();
            tables.get(focusedIndex).moveToEnd();
        } else {
            tables.get(focusedIndex).moveUp();
        }
    }

    public void moveDown() {
        if (!hasFocus()) {
            return;
        }
        if (tables.get(focusedIndex).isAtLastRow() && focusedIndex < tables.size() - 1) {
            focusedIndex++;
            applyHighlight();
            tables.get(focusedIndex).moveToStart();
        } else {
            tables.get(focusedIndex).moveDown();
        }
    }

    public boolean isTableFocused(int index) {
        return focusedIndex == index;
    }

    public @Nullable TableWidget<List<String>> getTable(int index) {
        return index < 0 || index >= tables.size() ? null : tables.get(index);
    }

    public int getTableCount() {
        return tables.size();
    }

    public boolean isShowingPlaceholder() {
        return tables.isEmpty() && detailsPanel.getComponentCount() > 0;
    }

    private void addDetailTables(List<DetailTable> detailTables) {
        for (DetailTable detail : detailTables) {
            if (!detail.label().isBlank()) {
                detailsPanel.add(ListDetailLayoutUtility.makeSectionLabel(detail.label()));
            }
            TableWidget<List<String>> table = TableWidget.ofRows(detail.columnHeaders(), detail.rows());
            ListDetailLayoutUtility.configureDetailsTable(table);
            table.setFocusTraversalKeysEnabled(false);
            tables.add(table);
            detailsPanel.add(table);
        }
    }

    private void applyHighlight() {
        for (int i = 0; i < tables.size(); i++) {
            tables.get(i).setSelectable(i == focusedIndex);
        }
    }

    private static JLabel buildPlaceholderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(WidgetTheme.normalText());
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }
}
