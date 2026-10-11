package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodexPanelTest {

    @Test
    void constructorInitializes() {
        CodexPanel panel = new CodexPanel(new ControlsHintBarWidget());

        assertEquals(CodexPanel.Category.ITEMS, panel.getSelectedCategory());
        assertEquals(0, panel.getEntryCount());
        assertTrue(panel.isEntryListFocused());
    }

    @Test
    void movingDownChangesTheSelectedEntry() {
        CodexPanel panel = new CodexPanel(new ControlsHintBarWidget());
        panel.showItems(List.of(itemNamed("Sword"), itemNamed("Shield")));
        assertEquals("Sword", panel.getSelectedEntryName());

        panel.onDown();

        assertEquals("Shield", panel.getSelectedEntryName());
    }

    @Test
    void movingRightFocusesDetails() {
        CodexPanel panel = new CodexPanel(new ControlsHintBarWidget());
        panel.showItems(List.of(itemNamed("Sword")));
        assertTrue(panel.isEntryListFocused());

        panel.onRight();

        assertTrue(panel.isFieldsTableFocused());
    }

    @Test
    void movingLeftFromDetailsRefocusesList() {
        CodexPanel panel = new CodexPanel(new ControlsHintBarWidget());
        panel.showItems(List.of(itemNamed("Sword")));
        panel.onRight();

        panel.onLeft();

        assertTrue(panel.isEntryListFocused());
    }

    @Test
    void tabNavigationCyclesThroughCategories() {
        CodexPanel panel = new CodexPanel(new ControlsHintBarWidget());
        assertEquals(CodexPanel.Category.ITEMS, panel.getSelectedCategory());

        panel.nextTab();
        assertEquals(CodexPanel.Category.TILES, panel.getSelectedCategory());

        panel.prevTab();
        assertEquals(CodexPanel.Category.ITEMS, panel.getSelectedCategory());
    }

    private static Item itemNamed(String name) {
        return new Item(
                name.toLowerCase(Locale.ROOT),
                name,
                new Item.ItemAttributes('!', "misc", "none", new Item.BaseDamage(0, 0), List.of())
        );
    }
}
