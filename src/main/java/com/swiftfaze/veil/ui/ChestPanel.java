package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.items.Item;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget;
import com.swiftfaze.veil.ui.widget.HeaderWidget;
import com.swiftfaze.veil.ui.widget.ListWidget;
import com.swiftfaze.veil.ui.widget.PopupWidget;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import com.swiftfaze.veil.world.object.Chest;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Lists a chest's slots, one per row: the item's name, or "empty". Display only. */
public final class ChestPanel extends PopupWidget {

    private static final int PADDING = 10;
    private static final List<ControlsHintBarWidget.Hint> HINTS =
            List.of(new ControlsHintBarWidget.Hint("escape", "Close"));

    private final ListWidget<Optional<Item>> slotList;
    private final Consumer<List<ControlsHintBarWidget.Hint>> hintSink;
    private List<Optional<Item>> slots = List.of();

    /** @param hintSink receives the controls hints to show while the panel is open */
    public ChestPanel(Consumer<List<ControlsHintBarWidget.Hint>> hintSink) {
        this.hintSink = hintSink;
        Border bottomLine = BorderFactory.createMatteBorder(0, 0, 2, 0, WidgetTheme.border());
        Border padding = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING);
        setBorder(BorderFactory.createCompoundBorder(bottomLine, padding));

        addContent(new HeaderWidget("Chest"));

        slotList = new ListWidget<>(ChestPanel::label);
        slotList.setWrapAround(false);
        slotList.setFocusTraversalKeysEnabled(false);
        JScrollPane slotScroll = ListDetailLayoutUtility.buildScrollPane(slotList);
        // A BoxLayout column needs one alignment for all children; the header is LEFT_ALIGNMENT.
        slotScroll.setAlignmentX(LEFT_ALIGNMENT);
        addContent(slotScroll);

        // E closes the panel the same way Esc does. The popup's own binding wins over the
        // game's, so E never reaches the interact action while the panel is open.
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(Keybindings.INTERACT, "popup-dismiss");
    }

    public void show(Chest chest) {
        slots = chest.getSlots();
        slotList.setItems(slots);
        hintSink.accept(HINTS);
        open();
    }

    /** The text each slot shows, top slot first. */
    public List<String> slotLabels() {
        return slots.stream().map(ChestPanel::label).toList();
    }

    private static String label(Optional<Item> slot) {
        return slot.map(Item::getName).orElse("empty");
    }

    @Override
    protected void onUp() {
        slotList.moveUp();
    }

    @Override
    protected void onDown() {
        slotList.moveDown();
    }
}
