package com.swiftfaze.veil.entities.npc;

import java.util.Locale;

/** How an NPC moves once it reaches the end of its waypoint list. */
public enum PatrolMode {
    /** A, B, C, B, A, B, ... */
    PING_PONG,
    /** A, B, C, A, B, C, ... (the last waypoint leads back to the first). */
    LOOP;

    /** Parses the {@code mode} value used in NPC JSON; {@code null} means the default, ping_pong. */
    public static PatrolMode fromJson(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "ping_pong" -> PING_PONG;
            case "loop" -> LOOP;
            default -> throw new IllegalArgumentException("unknown patrol mode: " + value);
        };
    }

    public String jsonName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
