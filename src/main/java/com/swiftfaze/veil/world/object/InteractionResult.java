package com.swiftfaze.veil.world.object;

public enum InteractionResult {
    /** The object changed state (a door opened or closed). */
    TOGGLED,
    /** The object refused to change state. */
    REFUSED,
    /** The object's contents should be shown to the player. */
    SHOW_CONTENTS
}
