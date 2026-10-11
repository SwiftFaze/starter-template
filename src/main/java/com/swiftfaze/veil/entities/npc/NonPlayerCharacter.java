package com.swiftfaze.veil.entities.npc;

import com.swiftfaze.veil.entities.dialogue.Dialogue;
import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * A live NPC in the world. Steps one tile along its patrol per turn; when the next tile is
 * blocked it waits and retries the same step. Never skips a waypoint.
 */
public final class NonPlayerCharacter implements DrawableAsciiEntity {

    private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 18);

    private final NpcDefinition definition;
    private final MoveRules rules;
    private int x;
    private int y;
    private int targetIndex = 1;
    private int direction = 1;

    /** Whether an NPC may step onto (x, y): walkable, and not occupied by the player or another NPC. */
    @FunctionalInterface
    public interface MoveRules {
        boolean canEnter(int x, int y);
    }

    public NonPlayerCharacter(NpcDefinition definition, MoveRules rules) {
        this.definition = definition;
        this.rules = rules;
        this.x = definition.spawn().x();
        this.y = definition.spawn().y();
    }

    /** Advances one tile toward the current waypoint, or does nothing if stationary. */
    public void onTurn(int turn) {
        Patrol patrol = definition.patrol();
        if (patrol == null) {
            return;
        }
        if (isAt(patrol.waypoints().get(targetIndex))) {
            advanceTarget(patrol);
        }
        stepToward(patrol.waypoints().get(targetIndex));
    }

    public String getId() {
        return definition.id();
    }

    public String getName() {
        return definition.name();
    }

    /** The dialogue the player talks to this NPC with, or null if it has none. */
    public @Nullable Dialogue getDialogue() {
        return definition.dialogue();
    }

    private boolean isAt(TileCoord tile) {
        return x == tile.x() && y == tile.y();
    }

    private void advanceTarget(Patrol patrol) {
        int count = patrol.waypoints().size();
        if (patrol.mode() == PatrolMode.LOOP) {
            targetIndex = (targetIndex + 1) % count;
            return;
        }
        int next = targetIndex + direction;
        if (next < 0 || next >= count) {
            direction = -direction;
            next = targetIndex + direction;
        }
        targetIndex = next;
    }

    private void stepToward(TileCoord target) {
        int nextX = x + Integer.signum(target.x() - x);
        int nextY = y + Integer.signum(target.y() - y);
        if (rules.canEnter(nextX, nextY)) {
            x = nextX;
            y = nextY;
        }
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public char getSymbol() {
        return definition.symbol();
    }

    @Override
    public Color getColor() {
        return definition.color();
    }

    @Override
    public void render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera) {
        g2d.setFont(FONT);
        g2d.setColor(definition.color());
        int screenX = (x - camera.getX()) * tileWidth;
        int screenY = (y - camera.getY()) * tileHeight + tileHeight;
        g2d.drawString(String.valueOf(definition.symbol()), screenX, screenY);
    }
}
