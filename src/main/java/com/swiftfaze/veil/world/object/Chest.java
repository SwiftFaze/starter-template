package com.swiftfaze.veil.world.object;

import com.swiftfaze.veil.entities.items.Item;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Chest implements InteractableObject {

    private final String id;
    private final Color color;
    private final char symbol;
    private final Contents contents;

    public Chest(String id, Color color, char symbol, Contents contents) {
        this.id = id;
        this.color = color;
        this.symbol = symbol;
        this.contents = contents;
    }

    /**
     * How many slots a chest has, and the items that start in its first slots, in order.
     *
     * @param items must not exceed {@code slotCount}
     */
    public record Contents(int slotCount, List<Item> items) {
        public Contents {
            if (items.size() > slotCount) {
                throw new IllegalArgumentException("chest has " + items.size() + " items but only " + slotCount + " slots");
            }
            items = List.copyOf(items);
        }
    }

    /** One entry per slot: the item in it, or empty. */
    public List<Optional<Item>> getSlots() {
        List<Optional<Item>> slots = new ArrayList<>(contents.slotCount());
        for (int i = 0; i < contents.slotCount(); i++) {
            slots.add(i < contents.items().size() ? Optional.of(contents.items().get(i)) : Optional.empty());
        }
        return List.copyOf(slots);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public char getSymbol() {
        return symbol;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public boolean isWalkable() {
        return false;
    }

    @Override
    public InteractionResult interact(boolean tileOccupied) {
        return InteractionResult.SHOW_CONTENTS;
    }

    @Override
    public Chest newInstance() {
        return new Chest(id, color, symbol, contents);
    }
}
