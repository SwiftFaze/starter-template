package com.swiftfaze.veil.ui;

import org.jspecify.annotations.Nullable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.entities.player.classes.PlayerClass;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.PopupWidget;
import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.Tile;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * In-game reference overlay (X key): a tab switcher across Items/Tiles/Classes,
 * each tab a list+detail split mirroring InventoryPanel's own layout.
 */
public class CodexPanel extends PopupWidget {

    public enum Category {
        ITEMS("Items"), TILES("Tiles"), CLASSES("Classes");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final String NO_ENTRY_TEXT = "(no item selected)";
    private static final List<Category> CATEGORY_ORDER = List.of(Category.values());
    private static final List<ControlsHintBarWidget.Hint> TAB_HINTS_TAIL = List.of(
            new ControlsHintBarWidget.Hint("tab", "Next category"),
            new ControlsHintBarWidget.Hint("shift+tab", "Prev category"),
            new ControlsHintBarWidget.Hint("escape", "Close"));

    private final ListWidget<Inspectable> entryList;
    private final DetailsPaneWidget detailsPane;
    private final Map<Category, JLabel> tabLabels = new EnumMap<>(Category.class);
    private final ControlsHintBarWidget hintBar;

    private List<Inspectable> items = List.of();
    private List<Inspectable> tiles = List.of();
    private List<Inspectable> classes = List.of();
    private List<Inspectable> currentEntries = List.of();
    private Category selectedCategory = Category.ITEMS;

    public CodexPanel(ControlsHintBarWidget hintBar) {
        this.hintBar = hintBar;
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0, WidgetTheme.border());
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        setBorder(BorderFactory.createCompoundBorder(bottomLine, padding));

        addContent(new HeaderWidget("Codex"));
        addContent(buildTabRow());

        entryList = new ListWidget<>(Inspectable::getName);
        entryList.setWrapAround(false);
        detailsPane = new DetailsPaneWidget();
        entryList.setOnSelectionChange(detailsPane::showEntry);

        addContent(ListDetailLayoutUtility.buildBody(ListDetailLayoutUtility.buildScrollPane(entryList), detailsPane));
        bindTabKeys();
        disableFocusTraversalKeys();
        refreshHints();
    }

    /**
     * PopupWidget's own constructor already disables focus-traversal-keys on itself (see its
     * javadoc); entryList is the first focusable descendant Tab could otherwise land real
     * keyboard focus on, so it needs the same treatment for bindTabKeys()'s InputMap bindings
     * (and this popup's own onUp/onDown/onLeft/onRight routing) to keep seeing Tab and the
     * arrow keys instead of losing them to entryList's own WHEN_FOCUSED bindings.
     */
    private void disableFocusTraversalKeys() {
        entryList.setFocusTraversalKeysEnabled(false);
    }

    public void showItems(List<Item> items) {
        this.items = new ArrayList<>(items);
        if (selectedCategory == Category.ITEMS) {
            refreshEntries();
        }
    }

    public void showTiles(List<Tile> tiles) {
        this.tiles = new ArrayList<>(tiles);
        if (selectedCategory == Category.TILES) {
            refreshEntries();
        }
    }

    public void showClasses(List<PlayerClass> classes) {
        this.classes = new ArrayList<>(classes);
        if (selectedCategory == Category.CLASSES) {
            refreshEntries();
        }
    }

    @Override
    public void open() {
        selectedCategory = Category.ITEMS;
        refreshEntries();
        refreshTabHighlight();
        super.open();
        refreshHints();
    }

    public Category getSelectedCategory() {
        return selectedCategory;
    }

    public List<String> getTabLabels() {
        return List.of(Category.ITEMS.getLabel(), Category.TILES.getLabel(), Category.CLASSES.getLabel());
    }

    public int getEntryCount() {
        return currentEntries.size();
    }

    public @Nullable String getSelectedEntryName() {
        Inspectable selected = entryList.getSelectedItem();
        return selected == null ? null : selected.getName();
    }

    public boolean isShowingPlaceholder() {
        return detailsPane.isShowingPlaceholder();
    }

    public @Nullable String getDetailPlaceholderText() {
        return detailsPane.isShowingPlaceholder() ? NO_ENTRY_TEXT : null;
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

    public boolean isFieldsTableFocused() {
        return detailsPane.isTableFocused(0);
    }

    public boolean isEffectsTableFocused() {
        return detailsPane.isTableFocused(1);
    }

    public boolean isEntryListFocused() {
        return !detailsPane.hasFocus();
    }

    public void nextTab() {
        int index = CATEGORY_ORDER.indexOf(selectedCategory);
        selectCategory(CATEGORY_ORDER.get((index + 1) % CATEGORY_ORDER.size()));
    }

    public void prevTab() {
        int index = CATEGORY_ORDER.indexOf(selectedCategory);
        selectCategory(CATEGORY_ORDER.get((index - 1 + CATEGORY_ORDER.size()) % CATEGORY_ORDER.size()));
    }

    @Override
    protected void onUp() {
        if (detailsPane.hasFocus()) {
            detailsPane.moveUp();
        } else {
            entryList.moveUp();
        }
    }

    @Override
    protected void onDown() {
        if (detailsPane.hasFocus()) {
            detailsPane.moveDown();
        } else {
            entryList.moveDown();
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

    private void selectCategory(Category category) {
        selectedCategory = category;
        detailsPane.clearFocus();
        refreshTabHighlight();
        refreshEntries();
    }

    private void refreshEntries() {
        currentEntries = switch (selectedCategory) {
            case ITEMS -> items;
            case TILES -> tiles;
            case CLASSES -> classes;
        };
        entryList.setItems(currentEntries);
    }


    private void bindTabKeys() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();
        inputMap.put(Keybindings.NEXT_TAB, Keybindings.ACTION_NEXT_TAB);
        inputMap.put(Keybindings.PREV_TAB, Keybindings.ACTION_PREV_TAB);
        actionMap.put(Keybindings.ACTION_NEXT_TAB, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nextTab();
            }
        });
        actionMap.put(Keybindings.ACTION_PREV_TAB, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                prevTab();
            }
        });
    }

    private void refreshHints() {
        List<ControlsHintBarWidget.Hint> hints = new ArrayList<>();
        hints.add(detailsPane.hasFocus()
                ? new ControlsHintBarWidget.Hint("left", "Back to list")
                : new ControlsHintBarWidget.Hint("right", "View details"));
        hints.addAll(TAB_HINTS_TAIL);
        hintBar.setHints(hints);
    }

    private JPanel buildTabRow() {
        Category[] categories = Category.values();
        JPanel row = new JPanel(new GridLayout(1, categories.length));
        row.setBackground(WidgetTheme.background());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createLineBorder(WidgetTheme.border(), 1));
        for (int i = 0; i < categories.length; i++) {
            boolean isLast = i == categories.length - 1;
            JLabel label = new JLabel(categories[i].getLabel(), SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            Border padding = BorderFactory.createEmptyBorder(6, 8, 6, 8);
            label.setBorder(isLast ? padding : BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, WidgetTheme.border()), padding));
            tabLabels.put(categories[i], label);
            row.add(label);
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void refreshTabHighlight() {
        for (Category category : Category.values()) {
            WidgetTheme.applySelection(tabLabels.get(category), category == selectedCategory);
        }
    }

}
