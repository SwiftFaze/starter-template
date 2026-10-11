package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.component.DetailTable;
import com.swiftfaze.veil.component.Inspectable;
import com.swiftfaze.veil.ui.DetailsPaneWidget;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import org.junit.jupiter.api.Test;

import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InspectableDetailPanelTest {

    private static final String TITLE = "Dirt";
    private static final List<String> FIRST_ROW = List.of("ID", "core:dirt");
    private static final List<String> SECOND_ROW = List.of("Walkable", "true");

    @Test
    void showsTheTitleInAHeaderAboveTheDetails() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertInstanceOf(HeaderWidget.class, panel.getComponent(0));
        assertInstanceOf(DetailsPaneWidget.class, panel.getComponent(1));
    }

    @Test
    void headerAndDetailsShareLeftAlignmentSoBoxLayoutKeepsThemInLine() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertEquals(Component.LEFT_ALIGNMENT, panel.getComponent(0).getAlignmentX());
        assertEquals(Component.LEFT_ALIGNMENT, panel.getComponent(1).getAlignmentX());
    }

    @Test
    void downAndUpMoveThroughTheRows() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        press(panel, Keybindings.ACTION_MENU_DOWN);
        assertEquals(SECOND_ROW, panel.table(0).getSelectedRow());

        press(panel, Keybindings.ACTION_MENU_UP);
        assertEquals(FIRST_ROW, panel.table(0).getSelectedRow());
    }

    @Test
    void mapsMenuUpAndDownWhenFocused() {
        InputMap inputMap = new InspectableDetailPanel(TITLE, entry()).getInputMap(JComponent.WHEN_FOCUSED);

        assertEquals(Keybindings.ACTION_MENU_UP, inputMap.get(Keybindings.MENU_UP));
        assertEquals(Keybindings.ACTION_MENU_DOWN, inputMap.get(Keybindings.MENU_DOWN));
    }

    @Test
    void isAFocusableThemedVerticalStack() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertTrue(panel.isFocusable());
        assertEquals(WidgetTheme.background(), panel.getBackground());
        assertInstanceOf(BoxLayout.class, panel.getLayout());
    }

    @Test
    void focusesOnlyTheFirstTable() {
        InspectableDetailPanel panel = new InspectableDetailPanel(TITLE, entry());

        assertTrue(panel.isTableFocused(0));
        assertFalse(panel.isTableFocused(1));
    }

    private static void press(InspectableDetailPanel panel, String actionName) {
        panel.getActionMap().get(actionName)
                .actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, null));
    }

    private static Inspectable entry() {
        return new Inspectable() {
            @Override
            public String getId() {
                return "core:dirt";
            }

            @Override
            public String getName() {
                return TITLE;
            }

            @Override
            public List<DetailTable> getDetailTables() {
                return List.of(
                        new DetailTable("", List.of("Field", "Value"), List.of(FIRST_ROW, SECOND_ROW)),
                        new DetailTable("Extra:", List.of("Field", "Value"), List.of(List.of("More", "x"))));
            }
        };
    }
}
