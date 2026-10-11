package com.swiftfaze.veil.game;

/** A world entity that gets one chance to act each time the player successfully moves. */
@FunctionalInterface
public interface Tickable {

    void onTurn(int turn);
}
