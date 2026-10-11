package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetailsPaneWidgetTest {

    private DetailsPaneWidget detailsPane;

    @BeforeEach
    void setUp() {
        detailsPane = new DetailsPaneWidget();
    }

    @Test
    void tablesAreCappedAtTheirRowsHeightSoTheyDoNotStretchIntoEmptySpace() {
        detailsPane.showEntry(createTwoTableEntry());

        TableWidget<List<String>> fieldTable = detailsPane.getTable(0);
        TableWidget<List<String>> effectsTable = detailsPane.getTable(1);

        assertEquals(fieldTable.getPreferredSize().height, fieldTable.getMaximumSize().height);
        assertEquals(effectsTable.getPreferredSize().height, effectsTable.getMaximumSize().height);
    }

    @Test
    void theDefaultPaneDrawsADividerAgainstTheListBesideIt() {
        CompoundBorder border = assertInstanceOf(CompoundBorder.class, detailsPanelBorder(detailsPane));

        assertInstanceOf(MatteBorder.class, border.getOutsideBorder());
    }

    @Test
    void aStandalonePaneHasNoDividerOrPaddingSoTablesSitFlushUnderTheHeader() {
        Border border = assertInstanceOf(EmptyBorder.class, detailsPanelBorder(DetailsPaneWidget.standalone()));

        assertEquals(new Insets(0, 0, 0, 0), border.getBorderInsets(null));
    }

    @Test
    void showEntryNullShowsPlaceholder() {
        detailsPane.showEntry(null);

        assertTrue(detailsPane.isShowingPlaceholder());
        assertEquals(0, detailsPane.getTableCount());
    }

    @Test
    void showEntrySingleTableEntry() {
        Inspectable entry = createSingleTableEntry();
        detailsPane.showEntry(entry);

        assertFalse(detailsPane.isShowingPlaceholder());
        assertEquals(1, detailsPane.getTableCount());
        assertNotNull(detailsPane.getTable(0));
        assertNull(detailsPane.getTable(1));
    }

    @Test
    void showEntryTwoTableEntry() {
        Inspectable entry = createTwoTableEntry();
        detailsPane.showEntry(entry);

        assertFalse(detailsPane.isShowingPlaceholder());
        assertEquals(2, detailsPane.getTableCount());
        assertNotNull(detailsPane.getTable(0));
        assertNotNull(detailsPane.getTable(1));
    }

    @Test
    void focusFirstTableWhenEmpty() {
        detailsPane.showEntry(null);
        detailsPane.focusFirstTable();

        assertFalse(detailsPane.hasFocus());
    }

    @Test
    void focusFirstTableWithOneTable() {
        detailsPane.showEntry(createSingleTableEntry());
        detailsPane.focusFirstTable();

        assertTrue(detailsPane.hasFocus());
        assertTrue(detailsPane.isTableFocused(0));
    }

    @Test
    void focusFirstTableWithTwoTables() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();

        assertTrue(detailsPane.hasFocus());
        assertTrue(detailsPane.isTableFocused(0));
        assertFalse(detailsPane.isTableFocused(1));
    }

    @Test
    void clearFocusAfterFocus() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();

        detailsPane.clearFocus();

        assertFalse(detailsPane.hasFocus());
    }

    @Test
    void moveDownFromFirstTableToSecond() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();

        detailsPane.moveDown();

        assertFalse(detailsPane.isTableFocused(0));
        assertTrue(detailsPane.isTableFocused(1));
    }

    @Test
    void moveUpFromSecondTableToFirst() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();
        detailsPane.moveDown();

        detailsPane.moveUp();

        assertTrue(detailsPane.isTableFocused(0));
    }

    @Test
    void moveDownOnLastRowOfLastTableStays() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();
        detailsPane.moveDown();

        detailsPane.moveDown();

        assertTrue(detailsPane.isTableFocused(1));
    }

    @Test
    void moveUpOnFirstRowOfFirstTableStays() {
        detailsPane.showEntry(createTwoTableEntry());
        detailsPane.focusFirstTable();

        detailsPane.moveUp();

        assertTrue(detailsPane.isTableFocused(0));
    }

    @Test
    void moveWhenNotFocusedIsNoOp() {
        detailsPane.showEntry(createTwoTableEntry());
        assertFalse(detailsPane.hasFocus());

        detailsPane.moveUp();
        detailsPane.moveDown();

        assertFalse(detailsPane.hasFocus());
    }

    @Test
    void getTableReturnsCorrectTable() {
        detailsPane.showEntry(createTwoTableEntry());

        assertEquals(1, detailsPane.getTable(0).getRowCount());
        assertEquals(2, detailsPane.getTable(1).getRowCount());
    }

    @Test
    void getTableOutOfBoundsReturnsNull() {
        detailsPane.showEntry(createTwoTableEntry());

        assertNull(detailsPane.getTable(-1));
        assertNull(detailsPane.getTable(2));
    }

    @Test
    void isTableFocusedReturnsFalseWhenNotFocused() {
        detailsPane.showEntry(createTwoTableEntry());

        assertFalse(detailsPane.isTableFocused(0));
        assertFalse(detailsPane.isTableFocused(1));
    }

    @Test
    void showEntryNullAfterDataClearsPlaceholder() {
        detailsPane.showEntry(createSingleTableEntry());

        detailsPane.showEntry(null);

        assertTrue(detailsPane.isShowingPlaceholder());
        assertEquals(0, detailsPane.getTableCount());
    }

    private static Inspectable createSingleTableEntry() {
        return new Inspectable() {
            @Override
            public String getId() {
                return "test:single";
            }

            @Override
            public String getName() {
                return "Single Table";
            }

            @Override
            public List<DetailTable> getDetailTables() {
                return List.of(
                        new DetailTable("", List.of("Field", "Value"),
                                List.of(List.of("Test", "Value")))
                );
            }
        };
    }

    private static Border detailsPanelBorder(DetailsPaneWidget pane) {
        JScrollPane scrollPane = (JScrollPane) pane.getComponent(0);
        return ((JComponent) scrollPane.getViewport().getView()).getBorder();
    }

    private static Inspectable createTwoTableEntry() {
        return new Inspectable() {
            @Override
            public String getId() {
                return "test:double";
            }

            @Override
            public String getName() {
                return "Two Tables";
            }

            @Override
            public List<DetailTable> getDetailTables() {
                return List.of(
                        new DetailTable("", List.of("Field", "Value"),
                                List.of(List.of("Test", "Value"))),
                        new DetailTable("Effects:", List.of("Type", "Stat"),
                                List.of(
                                        List.of("bonus", "strength"),
                                        List.of("penalty", "speed")
                                ))
                );
            }
        };
    }
}
