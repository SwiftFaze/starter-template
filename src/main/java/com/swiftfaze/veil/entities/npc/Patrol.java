package com.swiftfaze.veil.entities.npc;

import java.util.List;

/** An NPC's route: waypoints in order, plus how the route repeats. Validated by ModLoader. */
public record Patrol(PatrolMode mode, List<TileCoord> waypoints) {

    public Patrol {
        waypoints = List.copyOf(waypoints);
    }
}
