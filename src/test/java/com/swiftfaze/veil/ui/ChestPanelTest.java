package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.world.object.Chest;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestPanelTest {

    private static final Item POTION = new Item("core:health_potion", "Health Potion",
            new Item.ItemAttributes('!', "consumable", "none", new Item.BaseDamage(0, 0), List.of()));

    private static Chest chestWithOnePotionInTwoSlots() {
        return new Chest("test:chest", Color.YELLOW, 'C', new Chest.Contents(2, List.of(POTION)));
    }

    @Test
    void showingAChestListsItsItemsThenEmptySlots() {
        ChestPanel panel = new ChestPanel(hints -> { });

        panel.show(chestWithOnePotionInTwoSlots());

        assertEquals(List.of("Health Potion", "empty"), panel.slotLabels());
    }

    @Test
    void showingAChestPublishesTheCloseHint() {
        List<ControlsHintBarWidget.Hint> published = new ArrayList<>();
        ChestPanel panel = new ChestPanel(published::addAll);

        panel.show(chestWithOnePotionInTwoSlots());

        assertEquals(List.of(new ControlsHintBarWidget.Hint("escape", "Close")), published);
    }

    @Test
    void showingAChestOpensThePanel() {
        ChestPanel panel = new ChestPanel(hints -> { });

        panel.show(chestWithOnePotionInTwoSlots());

        assertTrue(panel.isVisible());
    }

    @Test
    void theFirstSlotStartsSelectedAndDownThenUpMovesTheSelection() {
        ChestPanel panel = new ChestPanel(hints -> { });
        panel.show(chestWithOnePotionInTwoSlots());
        ListWidget<?> list = findList(panel);

        assertEquals(0, list.getSelectedIndex());
        panel.onDown();
        assertEquals(1, list.getSelectedIndex());
        panel.onUp();
        assertEquals(0, list.getSelectedIndex());
    }

    @Test
    void theSelectionStopsAtTheLastSlotInsteadOfWrapping() {
        ChestPanel panel = new ChestPanel(hints -> { });
        panel.show(chestWithOnePotionInTwoSlots());
        ListWidget<?> list = findList(panel);

        panel.onDown();
        panel.onDown();

        assertEquals(1, list.getSelectedIndex());
    }

    private static ListWidget<?> findList(Container root) {
        return descendants(root)
                .filter(ListWidget.class::isInstance)
                .map(component -> (ListWidget<?>) component)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no ListWidget in the chest panel"));
    }

    private static Stream<Component> descendants(Container parent) {
        return Arrays.stream(parent.getComponents())
                .flatMap(child -> Stream.concat(Stream.of(child),
                        child instanceof Container container ? descendants(container) : Stream.empty()));
    }
}
