package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.ModLoader;
import com.swiftfaze.veil.mods.ModRegistry;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.ui.widget.TableWidget;
import org.junit.jupiter.api.Test;

import javax.swing.InputMap;
import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemDetailPanelTest {

    private static final String MAX_DAMAGE_ROW = "Base Damage (Max)";
    private static final String MIN_DAMAGE_ROW = "Base Damage (Min)";
    private static final int IRON_SWORD_MAX = 9;

    @Test
    void displaysItemNameInHeader() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = panelFor(ironSword);

        assertEquals("Iron Sword", panel.title());
    }

    @Test
    void exposesAtLeastOneDetailTable() {
        ModRegistry mods = ModLoader.load(Paths.get("mods"));
        Item ironSword = mods.getItem("core:iron_sword");

        ItemDetailPanel panel = panelFor(ironSword);

        assertTrue(panel.tableCount() >= 1);
    }

    @Test
    void menuDownNavigatesToNextRow() {
        ItemDetailPanel panel = ironSwordPanel();
        TableWidget<List<String>> table = panel.table(0);
        table.moveToStart();
        List<String> firstRow = table.getSelectedRow();

        press(panel, Keybindings.ACTION_MENU_DOWN);

        assertNotEquals(firstRow, table.getSelectedRow(), "Row should have changed after pressing down");
    }

    @Test
    void menuUpReturnsToThePreviousRow() {
        ItemDetailPanel panel = ironSwordPanel();
        TableWidget<List<String>> table = panel.table(0);
        table.moveToStart();
        List<String> firstRow = table.getSelectedRow();
        press(panel, Keybindings.ACTION_MENU_DOWN);

        press(panel, Keybindings.ACTION_MENU_UP);

        assertEquals(firstRow, table.getSelectedRow());
    }

    @Test
    void mapsMenuKeysToTheNavigationActionsWhenFocused() {
        ItemDetailPanel panel = ironSwordPanel();

        InputMap inputMap = panel.getInputMap(JComponent.WHEN_FOCUSED);

        assertEquals(Keybindings.ACTION_MENU_UP, inputMap.get(Keybindings.MENU_UP));
        assertEquals(Keybindings.ACTION_MENU_DOWN, inputMap.get(Keybindings.MENU_DOWN));
    }

    @Test
    void isFocusableSoKeyboardNavigationCanReachIt() {
        assertTrue(ironSwordPanel().isFocusable());
    }

    @Test
    void fillsItselfWithAnInspectableDetailPanelTitledWithTheItemName() {
        ItemDetailPanel panel = ironSwordPanel();
        InspectableDetailPanel details = assertInstanceOf(InspectableDetailPanel.class, panel.getComponent(0));

        assertInstanceOf(BorderLayout.class, panel.getLayout());
        assertEquals("Iron Sword", details.title());
    }

    @Test
    void usesTheThemeBackground() {
        assertEquals(WidgetTheme.background(), ironSwordPanel().getBackground());
    }

    @Test
    void rightOnAnArmedDamageRowRaisesItInPlace() {
        ItemDetailPanel panel = ironSwordPanel();
        selectRow(panel, MAX_DAMAGE_ROW);

        press(panel, Keybindings.ACTION_MENU_CONFIRM);
        press(panel, Keybindings.ACTION_MENU_RIGHT);

        assertEquals(List.of(MAX_DAMAGE_ROW, "10"), panel.table(0).getSelectedRow());
    }

    @Test
    void leftOnAnArmedDamageRowLowersItInPlace() {
        ItemDetailPanel panel = ironSwordPanel();
        selectRow(panel, MIN_DAMAGE_ROW);

        press(panel, Keybindings.ACTION_MENU_CONFIRM);
        press(panel, Keybindings.ACTION_MENU_LEFT);

        assertEquals(List.of(MIN_DAMAGE_ROW, "3"), panel.table(0).getSelectedRow());
    }

    @Test
    void leftAndRightDoNothingUntilARowIsArmed() {
        ItemDetailPanel panel = ironSwordPanel();
        selectRow(panel, MAX_DAMAGE_ROW);

        press(panel, Keybindings.ACTION_MENU_RIGHT);

        assertEquals(List.of(MAX_DAMAGE_ROW, String.valueOf(IRON_SWORD_MAX)), panel.table(0).getSelectedRow());
    }

    @Test
    void aRejectedStepLeavesTheRowUnchanged() {
        Item narrowSword = ironSword().withBaseDamage(IRON_SWORD_MAX, IRON_SWORD_MAX);
        ItemDetailPanel panel = panelFor(narrowSword);
        selectRow(panel, MAX_DAMAGE_ROW);

        press(panel, Keybindings.ACTION_MENU_CONFIRM);
        press(panel, Keybindings.ACTION_MENU_LEFT);

        assertEquals(List.of(MAX_DAMAGE_ROW, String.valueOf(IRON_SWORD_MAX)), panel.table(0).getSelectedRow());
    }

    @Test
    void escapeIsBoundOnlyWhileARowIsArmed() {
        ItemDetailPanel panel = ironSwordPanel();
        InputMap inputMap = panel.getInputMap(JComponent.WHEN_FOCUSED);
        selectRow(panel, MAX_DAMAGE_ROW);
        assertNull(inputMap.get(Keybindings.MENU_CANCEL));

        press(panel, Keybindings.ACTION_MENU_CONFIRM);
        assertEquals(Keybindings.ACTION_MENU_CANCEL, inputMap.get(Keybindings.MENU_CANCEL));

        press(panel, Keybindings.ACTION_MENU_CANCEL);
        assertNull(inputMap.get(Keybindings.MENU_CANCEL));
    }

    @Test
    void enterAgainDisarmsSoRightNoLongerSteps() {
        ItemDetailPanel panel = ironSwordPanel();
        selectRow(panel, MAX_DAMAGE_ROW);
        press(panel, Keybindings.ACTION_MENU_CONFIRM);

        press(panel, Keybindings.ACTION_MENU_CONFIRM);
        press(panel, Keybindings.ACTION_MENU_RIGHT);

        assertEquals(List.of(MAX_DAMAGE_ROW, String.valueOf(IRON_SWORD_MAX)), panel.table(0).getSelectedRow());
        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED).get(Keybindings.MENU_CANCEL));
    }

    @Test
    void enterOnANonDamageRowDoesNotArm() {
        ItemDetailPanel panel = ironSwordPanel();
        selectRow(panel, "Name");

        press(panel, Keybindings.ACTION_MENU_CONFIRM);

        assertNull(panel.getInputMap(JComponent.WHEN_FOCUSED).get(Keybindings.MENU_CANCEL));
    }

    private static void selectRow(ItemDetailPanel panel, String label) {
        TableWidget<List<String>> table = panel.table(0);
        table.moveToStart();
        while (!label.equals(table.getSelectedRow().get(0))) {
            table.moveDown();
        }
    }

    private static Item ironSword() {
        return ModLoader.load(Paths.get("mods")).getItem("core:iron_sword");
    }

    private static ItemDetailPanel panelFor(Item item) {
        AtomicReference<Item> current = new AtomicReference<>(item);
        return new ItemDetailPanel(item, new ItemFieldMutator(item.getBaseDamage(), current::get, current::set));
    }

    private static void press(ItemDetailPanel panel, String actionName) {
        panel.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null));
    }

    private static ItemDetailPanel ironSwordPanel() {
        return panelFor(ironSword());
    }
}
