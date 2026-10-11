package com.swiftfaze.veil.world.object;

import java.awt.Color;

/**
 * A world object placed on a scene tile that the player can act on with the interact key.
 * Each object type is a Java class; its instance data (symbols, contents, initial state) comes
 * from mod JSON. Placed objects are mutable, so templates held by the registry must be copied
 * with {@link #newInstance()} before they are placed.
 */
public interface InteractableObject {

    String getId();

    char getSymbol();

    Color getColor();

    boolean isWalkable();

    /**
     * Acts on the player's interact key press.
     *
     * @param tileOccupied whether another entity stands on this object's tile
     * @return what the interaction did; the caller reports refusals and opens panels
     */
    InteractionResult interact(boolean tileOccupied);

    /** A fresh object in this definition's initial state, sharing no mutable state with this one. */
    InteractableObject newInstance();
}
