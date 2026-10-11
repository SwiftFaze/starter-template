package com.swiftfaze.veil.world.object;

import java.awt.Color;

public final class Door implements InteractableObject {

    private final Definition definition;
    private boolean open;

    public Door(String id, Color color, Glyphs glyphs, boolean open) {
        this.definition = new Definition(id, color, glyphs, open);
        this.open = open;
    }

    /** The glyph drawn while the door is open, and while it is closed. */
    public record Glyphs(char open, char closed) {
    }

    /** What a mod file defines; fixed for the door's lifetime, and enough to make a fresh copy. */
    private record Definition(String id, Color color, Glyphs glyphs, boolean initiallyOpen) {
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public String getId() {
        return definition.id();
    }

    @Override
    public char getSymbol() {
        return open ? definition.glyphs().open() : definition.glyphs().closed();
    }

    @Override
    public Color getColor() {
        return definition.color();
    }

    @Override
    public boolean isWalkable() {
        return open;
    }

    @Override
    public InteractionResult interact(boolean tileOccupied) {
        if (open && tileOccupied) {
            return InteractionResult.REFUSED;
        }
        open = !open;
        return InteractionResult.TOGGLED;
    }

    @Override
    public Door newInstance() {
        return new Door(definition.id(), definition.color(), definition.glyphs(), definition.initiallyOpen());
    }
}
