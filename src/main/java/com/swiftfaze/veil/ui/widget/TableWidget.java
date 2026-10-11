package com.swiftfaze.veil.ui.widget;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.input.Keybindings;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A keyboard-navigable, full-width table with bordered grid cells (like a
 * terminal-rendered markdown table), an optional header row, and an optional
 * non-selectable mode for purely static/display data.
 */
public final class TableWidget<T> extends Widget {
    private final List<String> columnHeaders;
    private final List<Function<T, String>> columnRenderers;
    private final List<T> rows = new ArrayList<>();
    private final List<List<JLabel>> rowCells = new ArrayList<>();
    private JPanel headerPanel;
    private int selectedRowIndex = 0;
    private int selectedColumnIndex = 0;
    private boolean wrapAround = true;
    private boolean selectable = true;
    private Consumer<T> onConfirm = t -> {};
    private Color selectedRowAccentColor;
    private boolean otherRowsDimmed = false;

    public TableWidget(List<Function<T, String>> columnRenderers) {
        this(columnRenderers, List.of());
    }

    public TableWidget(List<Function<T, String>> columnRenderers, List<String> columnHeaders) {
        this.columnHeaders = columnHeaders;
        this.columnRenderers = columnRenderers;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);
        setBorder(BorderFactory.createMatteBorder(1, 1, 0, 0, WidgetTheme.border()));
        buildHeaderRow();
        bindKeys();
    }

    public static TableWidget<List<String>> ofRows(List<String> columnHeaders, List<List<String>> rows) {
        int columnCount = columnHeaders.isEmpty()
                ? (rows.isEmpty() ? 0 : rows.get(0).size())
                : columnHeaders.size();
        List<Function<List<String>, String>> renderers = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            int column = i;
            renderers.add(row -> row.get(column));
        }
        TableWidget<List<String>> table = new TableWidget<>(renderers, columnHeaders);
        table.setRows(rows);
        return table;
    }

    public void setRows(List<T> rows) {
        this.rows.clear();
        this.rows.addAll(rows);
        this.selectedRowIndex = 0;
        this.selectedColumnIndex = 0;
        refresh();
    }

    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /**
     * A non-selectable table renders every cell in NORMAL_TEXT, with no row-highlight
     * indication — for a table that isn't currently the keyboard-navigation target (either
     * purely static data, or momentarily not the active pane in a multi-table details view).
     */
    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        refreshHighlight();
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setOnConfirm(Consumer<T> onConfirm) {
        this.onConfirm = onConfirm;
    }

    /**
     * Paints an extra accent-colored outline around the selected row's cells, on top of
     * the normal selected-row highlight - for a consumer that needs to flag the selected
     * row as additionally "armed" for some other in-progress action (e.g. a popup open on
     * its behalf), distinct from just being the current cursor position. Null (the
     * default) paints no accent. Every cell always reserves this outline's thickness
     * regardless of whether it's currently painted, matching RadioGroupWidget's confirmed/
     * unconfirmed border convention - reserving it only when accented would change every
     * cell's insets between the two states and visibly resize the whole table on selection.
     */
    public void setSelectedRowAccentColor(Color color) {
        this.selectedRowAccentColor = color;
        refreshHighlight();
    }

    /**
     * When true, every row except the selected one renders in WidgetTheme.dimmedText()
     * instead of NORMAL_TEXT - pairs with setSelectedRowAccentBorder() to make the
     * accented row read as the only currently-active thing, like a modal dimming its
     * backdrop. False (the default) leaves every consumer's existing look unchanged.
     */
    public void setOtherRowsDimmed(boolean dimmed) {
        this.otherRowsDimmed = dimmed;
        refreshHighlight();
    }

    /**
     * Replaces one row's data and re-renders just its cells, preserving the current
     * selection - unlike setRows(), which always resets selection to the first row.
     * For a consumer whose row data can change in place (e.g. a live-editable value)
     * without the row set itself being rebuilt.
     */
    public void updateRow(int index, T newRow) {
        if (index < 0 || index >= rows.size()) {
            return;
        }
        rows.set(index, newRow);
        List<JLabel> cells = rowCells.get(index);
        for (int i = 0; i < columnRenderers.size(); i++) {
            cells.get(i).setText(columnRenderers.get(i).apply(newRow));
        }
        revalidate();
        repaint();
    }

    public T getSelectedRow() {
        return rows.isEmpty() ? null : rows.get(selectedRowIndex);
    }

    public int getSelectedRowIndex() {
        return selectedRowIndex;
    }

    public int getSelectedColumnIndex() {
        return selectedColumnIndex;
    }

    public int getRowCount() {
        return rows.size();
    }

    public List<T> getRows() {
        return List.copyOf(rows);
    }

    public List<String> getColumnHeaders() {
        return List.copyOf(columnHeaders);
    }

    public void moveToStart() {
        if (rows.isEmpty()) {
            return;
        }
        selectedRowIndex = 0;
        refreshHighlight();
    }

    public void moveToEnd() {
        if (rows.isEmpty()) {
            return;
        }
        selectedRowIndex = rows.size() - 1;
        refreshHighlight();
    }

    public boolean isAtFirstRow() {
        return selectedRowIndex == 0;
    }

    public boolean isAtLastRow() {
        return rows.isEmpty() || selectedRowIndex == rows.size() - 1;
    }

    public void moveUp() {
        if (rows.isEmpty()) {
            return;
        }
        selectedRowIndex = wrapAround
            ? (selectedRowIndex - 1 + rows.size()) % rows.size()
            : Math.max(0, selectedRowIndex - 1);
        refreshHighlight();
    }

    public void moveDown() {
        if (rows.isEmpty()) {
            return;
        }
        selectedRowIndex = wrapAround
            ? (selectedRowIndex + 1) % rows.size()
            : Math.min(rows.size() - 1, selectedRowIndex + 1);
        refreshHighlight();
    }

    public void moveLeft() {
        if (rows.isEmpty() || columnRenderers.isEmpty()) {
            return;
        }
        selectedColumnIndex = wrapAround
            ? (selectedColumnIndex - 1 + columnRenderers.size()) % columnRenderers.size()
            : Math.max(0, selectedColumnIndex - 1);
        refreshHighlight();
    }

    public void moveRight() {
        if (rows.isEmpty() || columnRenderers.isEmpty()) {
            return;
        }
        selectedColumnIndex = wrapAround
            ? (selectedColumnIndex + 1) % columnRenderers.size()
            : Math.min(columnRenderers.size() - 1, selectedColumnIndex + 1);
        refreshHighlight();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();
        inputMap.put(Keybindings.MENU_UP, "table-up");
        inputMap.put(Keybindings.MENU_DOWN, "table-down");
        inputMap.put(Keybindings.MENU_LEFT, "table-left");
        inputMap.put(Keybindings.MENU_RIGHT, "table-right");
        inputMap.put(Keybindings.MENU_CONFIRM, "table-confirm");
        actionMap.put("table-up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { moveUp(); } });
        actionMap.put("table-down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { moveDown(); } });
        actionMap.put("table-left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { moveLeft(); } });
        actionMap.put("table-right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { moveRight(); } });
        actionMap.put("table-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                T selected = getSelectedRow();
                if (selected != null) {
                    onConfirm.accept(selected);
                }
            }
        });
    }

    private void buildHeaderRow() {
        if (columnHeaders.isEmpty()) {
            return;
        }
        headerPanel = buildRowPanel(columnHeaders, /* isHeader= */ true);
        add(headerPanel);
    }

    private void refresh() {
        for (List<JLabel> cells : rowCells) {
            // Each row's panel is the shared parent of its cells; remove it once per row.
            remove(cells.get(0).getParent());
        }
        rowCells.clear();
        for (T row : rows) {
            List<String> cellText = new ArrayList<>();
            for (Function<T, String> renderer : columnRenderers) {
                cellText.add(renderer.apply(row));
            }
            JPanel rowPanel = buildRowPanel(cellText, /* isHeader= */ false);
            List<JLabel> cells = new ArrayList<>();
            for (var component : rowPanel.getComponents()) {
                cells.add((JLabel) component);
            }
            rowCells.add(cells);
            add(rowPanel);
        }
        refreshHighlight();
        revalidate();
        repaint();
    }

    private static JPanel buildRowPanel(List<String> cellText, boolean isHeader) {
        int columnCount = Math.max(1, cellText.size());
        JPanel rowPanel = new JPanel(new GridLayout(1, columnCount));
        rowPanel.setAlignmentX(LEFT_ALIGNMENT);
        rowPanel.setBackground(isHeader ? WidgetTheme.tableHeaderBackground() : WidgetTheme.background());
        for (String text : cellText) {
            rowPanel.add(buildCellLabel(text, isHeader));
        }
        // Computed after the cells are added — an empty panel's preferred height is ~0, which
        // would otherwise clamp every row (including the header) to zero visible height.
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowPanel.getPreferredSize().height));
        return rowPanel;
    }

    private static JLabel buildCellLabel(String text, boolean isHeader) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(isHeader ? WidgetTheme.tableHeaderBackground() : WidgetTheme.background());
        label.setForeground(WidgetTheme.normalText());
        label.setFont(new Font(Font.MONOSPACED, isHeader ? Font.BOLD : Font.PLAIN, 16));
        label.setBorder(new AccentableCellBorder(baseCellBorder(), null));
        return label;
    }

    private static Border baseCellBorder() {
        Border cellLine = BorderFactory.createMatteBorder(0, 0, 1, 1, WidgetTheme.border());
        Border padding = BorderFactory.createEmptyBorder(4, 8, 4, 8);
        return BorderFactory.createCompoundBorder(cellLine, padding);
    }

    private void refreshHighlight() {
        for (int i = 0; i < rowCells.size(); i++) {
            updateCellHighlight(i);
        }
        scrollToSelectedRow();
    }

    private void updateCellHighlight(int rowIndex) {
        boolean highlighted = selectable && rowIndex == selectedRowIndex;
        boolean accented = highlighted && selectedRowAccentColor != null;
        for (JLabel cell : rowCells.get(rowIndex)) {
            WidgetTheme.applySelection(cell, highlighted);
            cell.setBorder(new AccentableCellBorder(baseCellBorder(), accented ? selectedRowAccentColor : null));
            if (otherRowsDimmed && !highlighted) {
                cell.setForeground(WidgetTheme.dimmedText());
            }
        }
    }

    private void scrollToSelectedRow() {
        if (selectable && selectedRowIndex < rowCells.size() && !rowCells.isEmpty()) {
            // scrollRectToVisible scrolls the *minimum* distance needed to reveal exactly the
            // rectangle it's given. The header is a separate component sitting above row 0 that
            // it knows nothing about, so scrolling to reveal row 0 alone can (and did, in
            // practice) leave the header scrolled just out of view above it. Unioning the
            // header's bounds into the target whenever row 0 is selected forces the header along.
            Rectangle target = rowCells.get(selectedRowIndex).get(0).getParent().getBounds();
            if (selectedRowIndex == 0 && headerPanel != null) {
                target = target.union(headerPanel.getBounds());
            }
            scrollRectToVisible(target);
        }
    }

    /**
     * Paints an optional accent outline INSIDE the wrapped inner border's existing padding
     * instead of adding new space for it — insets are always exactly the inner border's own
     * insets, accented or not. An earlier version reserved extra thickness around every
     * cell so insets wouldn't change on selection, which did stop the whole table resizing
     * but shifted the inner border's grid lines inward by that same thickness, opening a
     * visible gap between adjacent rows/columns that used to sit flush. Drawing within the
     * existing padding avoids both problems at once — nothing about the layout ever changes.
     */
    private static class AccentableCellBorder extends AbstractBorder {
        private static final int OFFSET = 1;
        private static final int THICKNESS = 2;
        private final Border inner;
        private final Color accentColor;

        AccentableCellBorder(Border inner, @Nullable Color accentColor) {
            this.inner = inner;
            this.accentColor = accentColor;
        }

        @Override
        @SuppressWarnings("PMD.ExcessiveParameterList") // Required by Swing's Border interface
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            inner.paintBorder(c, g, x, y, width, height);
            if (accentColor != null) {
                g.setColor(accentColor);
                g.fillRect(x + OFFSET, y + OFFSET, width - 2 * OFFSET, THICKNESS);
                g.fillRect(x + OFFSET, y + height - OFFSET - THICKNESS, width - 2 * OFFSET, THICKNESS);
                g.fillRect(x + OFFSET, y + OFFSET, THICKNESS, height - 2 * OFFSET);
                g.fillRect(x + width - OFFSET - THICKNESS, y + OFFSET, THICKNESS, height - 2 * OFFSET);
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return inner.getBorderInsets(c);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            Insets result = getBorderInsets(c);
            insets.set(result.top, result.left, result.bottom, result.right);
            return insets;
        }
    }
}
