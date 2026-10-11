package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListWidgetTest {

    @Test
    void startsAtTheFirstItem() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        assertEquals(0, widget.getSelectedIndex());
    }

    @Test
    void moveDownAdvancesToTheNextItem() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        widget.moveDown();
        assertEquals(1, widget.getSelectedIndex());
    }

    @Test
    void moveUpFromTheFirstItemWrapsToTheLastItem() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        widget.moveUp();
        assertEquals(2, widget.getSelectedIndex());
    }

    @Test
    void moveDownFromTheLastItemWrapsToTheFirstItem() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        widget.moveDown();
        widget.moveDown();
        widget.moveDown();
        assertEquals(0, widget.getSelectedIndex());
    }

    @Test
    void moveUpFromTheFirstItemStaysThereWhenWrapAroundIsDisabled() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        widget.setWrapAround(false);
        widget.moveUp();
        assertEquals(0, widget.getSelectedIndex());
    }

    @Test
    void moveDownFromTheLastItemStaysThereWhenWrapAroundIsDisabled() {
        ListWidget<String> widget = new ListWidget<>(s -> s);
        widget.setItems(List.of("A", "B", "C"));
        widget.setWrapAround(false);
        widget.moveDown();
        widget.moveDown();
        widget.moveDown();
        assertEquals(2, widget.getSelectedIndex());
    }
}
