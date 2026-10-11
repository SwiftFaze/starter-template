package com.swiftfaze.veil.entities.npc;

import com.swiftfaze.veil.render.Camera;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NonPlayerCharacterTest {

    private static final int RENDER_TILE = 24;
    private static final int GLYPH_FONT_SIZE = 18;
    private static final TileCoord WAYPOINT_A = new TileCoord(3, 4);
    private static final TileCoord WAYPOINT_B = new TileCoord(5, 4);
    private static final TileCoord WAYPOINT_C = new TileCoord(5, 6);
    private static final TileCoord WAYPOINT_D = new TileCoord(3, 6);

    @Test
    void pingPongTurnsAroundAtTheEndOfItsRoute() {
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, List.of(), WAYPOINT_A, WAYPOINT_B);

        stepTurns(npc, 3);

        assertPosition(npc, 4, 4);
    }

    @Test
    void pingPongWithThreeWaypointsTurnsAtTheCornerThenPacesBack() {
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, List.of(), WAYPOINT_A, WAYPOINT_B, WAYPOINT_C);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 3);
        assertPosition(npc, 4, 4);
    }

    @Test
    void loopReturnsFromTheLastWaypointToTheFirst() {
        NonPlayerCharacter npc = patrolling(PatrolMode.LOOP, List.of(), WAYPOINT_A, WAYPOINT_B, WAYPOINT_C, WAYPOINT_D);

        stepTurns(npc, 4);
        assertPosition(npc, 5, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 6);

        stepTurns(npc, 2);
        assertPosition(npc, 3, 4);
    }

    @Test
    void blockedStepWaitsOnTheCurrentTileAndRetriesTheSameStep() {
        List<TileCoord> blocked = new ArrayList<>(List.of(new TileCoord(4, 4)));
        NonPlayerCharacter npc = patrolling(PatrolMode.PING_PONG, blocked, WAYPOINT_A, WAYPOINT_B);

        stepTurns(npc, 3);
        assertPosition(npc, 3, 4);

        blocked.clear();
        stepTurns(npc, 1);
        assertPosition(npc, 4, 4);
    }

    @Test
    void stationaryNpcNeverMoves() {
        NpcDefinition definition = new NpcDefinition("core:statue", "Statue", 'S', Color.GRAY, new TileCoord(2, 2), null, null);
        NonPlayerCharacter npc = new NonPlayerCharacter(definition, (x, y) -> true);

        stepTurns(npc, 3);

        assertPosition(npc, 2, 2);
    }

    @Test
    void rendersItsSymbolInItsColorAtItsTileRelativeToTheCamera() {
        NpcDefinition definition = new NpcDefinition("core:statue", "Statue", '#', Color.RED, new TileCoord(6, 5), null, null);
        NonPlayerCharacter npc = new NonPlayerCharacter(definition, (x, y) -> true);
        BufferedImage image = new BufferedImage(RENDER_TILE * 10, RENDER_TILE * 10, BufferedImage.TYPE_INT_ARGB);
        Camera camera = new Camera(10, 10);
        camera.centerOn(8, 7);
        Graphics2D graphics = image.createGraphics();

        npc.render(graphics, RENDER_TILE, RENDER_TILE, camera);
        graphics.dispose();

        assertTrue(paintedPixels(image, new Rectangle(3 * RENDER_TILE, 3 * RENDER_TILE, RENDER_TILE, RENDER_TILE)) > 0,
                "symbol should be drawn inside its own tile");
        assertEquals(0, paintedPixels(image, new Rectangle(0, 0, image.getWidth(), 3 * RENDER_TILE)),
                "nothing should be drawn above the NPC's tile row");
        assertEquals(Color.RED.getRGB(), firstPaintedColor(image));
    }

    @Test
    void rendersWithTheMonospacedGlyphFont() {
        NpcDefinition definition = new NpcDefinition("core:statue", "Statue", '#', Color.RED, new TileCoord(6, 5), null, null);
        NonPlayerCharacter npc = new NonPlayerCharacter(definition, (x, y) -> true);
        BufferedImage image = new BufferedImage(RENDER_TILE, RENDER_TILE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        npc.render(graphics, RENDER_TILE, RENDER_TILE, new Camera(10, 10));

        assertEquals(new Font(Font.MONOSPACED, Font.PLAIN, GLYPH_FONT_SIZE), graphics.getFont());
        graphics.dispose();
    }

    private static NonPlayerCharacter patrolling(PatrolMode mode, List<TileCoord> blocked, TileCoord... waypoints) {
        Patrol patrol = new Patrol(mode, List.of(waypoints));
        NpcDefinition definition = new NpcDefinition("core:test", "Test", 'T', Color.WHITE, waypoints[0], patrol, null);
        return new NonPlayerCharacter(definition, (x, y) -> !blocked.contains(new TileCoord(x, y)));
    }

    private static void stepTurns(NonPlayerCharacter npc, int turns) {
        for (int i = 0; i < turns; i++) {
            npc.onTurn(i + 1);
        }
    }

    private static void assertPosition(NonPlayerCharacter npc, int x, int y) {
        assertEquals(new TileCoord(x, y), new TileCoord(npc.getX(), npc.getY()));
    }

    private static int paintedPixels(BufferedImage image, Rectangle area) {
        int painted = 0;
        for (int px = area.x; px < area.x + area.width; px++) {
            for (int py = area.y; py < area.y + area.height; py++) {
                if (image.getRGB(px, py) != 0) {
                    painted++;
                }
            }
        }
        return painted;
    }

    private static int firstPaintedColor(BufferedImage image) {
        for (int px = 0; px < image.getWidth(); px++) {
            for (int py = 0; py < image.getHeight(); py++) {
                if (image.getRGB(px, py) != 0) {
                    return image.getRGB(px, py) | 0xFF000000;
                }
            }
        }
        return 0;
    }
}
