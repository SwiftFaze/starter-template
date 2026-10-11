package com.swiftfaze.veil.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Steps every registered {@link Tickable} once per turn. Iterates a snapshot,
 * so a tickable that registers another during its own tick cannot break the loop;
 * the new one first acts on the next turn.
 */
public final class TurnScheduler {

    private final List<Tickable> tickables = new ArrayList<>();
    private int turn;

    public void register(Tickable tickable) {
        tickables.add(tickable);
    }

    public void advance() {
        turn++;
        for (Tickable tickable : List.copyOf(tickables)) {
            tickable.onTurn(turn);
        }
    }

    public int getTurn() {
        return turn;
    }

    public int registeredCount() {
        return tickables.size();
    }

    /** Drops every registration but keeps the turn counter. */
    public void clearRegistrations() {
        tickables.clear();
    }

    public void reset() {
        clearRegistrations();
        turn = 0;
    }
}
