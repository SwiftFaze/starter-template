package com.swiftfaze.veil.entities.npc;

import com.swiftfaze.veil.entities.dialogue.Dialogue;
import org.jspecify.annotations.Nullable;

import java.awt.Color;

/**
 * An NPC as declared in a mod's {@code npcs/*.json}. Immutable registry content; each
 * world spawn creates its own {@link NonPlayerCharacter} from it.
 * {@code patrol} is null for a stationary NPC. {@code dialogue} is null for an NPC the
 * player cannot talk to; otherwise it is the dialogue the loader resolved from its id.
 */
public record NpcDefinition(String id, String name, char symbol, Color color, TileCoord spawn,
                            @Nullable Patrol patrol, @Nullable Dialogue dialogue) {
}
