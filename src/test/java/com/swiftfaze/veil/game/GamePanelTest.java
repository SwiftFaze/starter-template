package com.swiftfaze.veil.game;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.entities.npc.NonPlayerCharacter;
import com.swiftfaze.veil.entities.npc.NpcDefinition;
import com.swiftfaze.veil.entities.npc.Patrol;
import com.swiftfaze.veil.entities.npc.PatrolMode;
import com.swiftfaze.veil.entities.npc.TileCoord;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.game.worldgen.GeneratedWorldScene;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.mods.RegistryFixtures;
import com.swiftfaze.veil.testing.FlatFloorWorld;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePanelTest {
    private static final int PAINT_WIDTH = 300;
    private static final int PAINT_HEIGHT = 225;
    private static final int START_X = 5;
    private static final int START_Y = 5;
    /** A seed whose generated world leaves the tile above the spawn point walkable (asserted by the test that uses it). */
    private static final long OPEN_ABOVE_SPAWN_SEED = 1L;
    private static final long FIRST_HOUSES_SEED = 42L;
    private static final long SECOND_HOUSES_SEED = 43L;
    private static final int TILE_RIGHT_X = START_X + 1;
    private static final int ELSEWHERE = 8;
    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final Tile WALL = new Tile("test:wall", '#', Color.WHITE, false);

    @Test
    void constructorInitializes() {
        GamePanel panel = new GamePanel();

        assertPlayerOnSpawn(panel);
    }

    /**
     * Paints into an off-screen image, so it runs the same with or without a display.
     * Without it, paintComponent is only covered when some other test happens to show
     * a real window, and CI (headless) scores it at 0% coverage in the CRAP gate.
     */
    @Test
    void paintingRendersTheWorldIntoTheGraphics() {
        GamePanel panel = new GamePanel();
        panel.setSize(PAINT_WIDTH, PAINT_HEIGHT);
        BufferedImage image = new BufferedImage(PAINT_WIDTH, PAINT_HEIGHT, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        panel.paint(graphics);
        graphics.dispose();

        assertTrue(distinctColours(image) > 1, "painting should draw tiles and glyphs, not one flat fill");
    }

    private static long distinctColours(BufferedImage image) {
        return IntStream.range(0, image.getHeight())
                .flatMap(y -> IntStream.range(0, image.getWidth()).map(x -> image.getRGB(x, y)))
                .distinct()
                .count();
    }

    @Test
    void toggleInventoryActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void toggleInventory() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_INVENTORY);

        assertTrue(listenerCalled[0]);
    }

    @Test
    void startGameLoopWorks() {
        GamePanel panel = new GamePanel();

        assertDoesNotThrow(panel::startGameLoop);
    }

    @Test
    void heldWorldPausePreventsMovement() {
        GamePanel panel = FlatFloorWorld.panel();
        Player player = playerOf(panel);
        int originalX = player.getX();
        int originalY = player.getY();

        WorldPause worldPause = worldPauseOf(panel);
        worldPause.hold("inventory");
        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(originalX, player.getX());
        assertEquals(originalY, player.getY());
    }

    @Test
    void successfulMoveAdvancesTheTurnByOne() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        TurnScheduler scheduler = schedulerOf(panel);
        CountingTickable counter = new CountingTickable();
        scheduler.register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(1, scheduler.getTurn());
        assertEquals(1, counter.steps);
    }

    @Test
    void blockedMoveDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(WALL);
        TurnScheduler scheduler = schedulerOf(panel);
        CountingTickable counter = new CountingTickable();
        scheduler.register(counter);

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, counter.steps);
    }

    @Test
    void movementWhilePausedDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        Player player = playerOf(panel);
        TurnScheduler scheduler = schedulerOf(panel);
        WorldPause worldPause = worldPauseOf(panel);
        worldPause.hold("pause");

        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        assertEquals(START_X, player.getX());
        assertEquals(0, scheduler.getTurn());
    }

    @Test
    void repositioningThePlayerDirectlyDoesNotAdvanceTheTurn() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);

        Player player = playerOf(panel);
        TurnScheduler scheduler = schedulerOf(panel);

        player.setPosition(ELSEWHERE, ELSEWHERE);

        assertEquals(0, scheduler.getTurn());
    }

    @Test
    void togglePauseActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void togglePause() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_PAUSE);

        assertTrue(listenerCalled[0]);
    }

    @Test
    void resetStateRestoresDefaultPlayerPositionAndUnpauses() {
        GamePanel panel = new GamePanel();
        Player player = playerOf(panel);
        WorldPause worldPause = worldPauseOf(panel);
        player.setPosition(1, 1);
        worldPause.hold("pause");

        panel.resetState();

        assertPlayerOnSpawn(panel);
        assertFalse(panel.isPaused());
    }

    @Test
    void resetStateClearsTheTurnSchedulerAndItsRegistrations() {
        GamePanel panel = panelWithTileToTheRight(FLOOR);
        TurnScheduler scheduler = schedulerOf(panel);
        scheduler.register(new CountingTickable());
        fireAction(panel, Keybindings.ACTION_MOVE_RIGHT);

        panel.resetState();

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, scheduler.registeredCount());
    }

    @Test
    void movementStillWorksAfterResetState() {
        GeneratedWorldScene expectedWorld = worldFromSeed(OPEN_ABOVE_SPAWN_SEED);
        assertTrue(expectedWorld.isWalkable(expectedWorld.getSpawnX(), expectedWorld.getSpawnY() - 1),
                "fixture seed must leave the tile above the spawn open, or this test asserts nothing");
        GamePanel panel = new GamePanel(GameEventLog.noOp(), () -> OPEN_ABOVE_SPAWN_SEED);
        panel.resetState();
        Player player = playerOf(panel);
        int startY = player.getY();

        fireAction(panel, Keybindings.ACTION_MOVE_UP);

        assertEquals(startY - 1, player.getY());
    }

    @Test
    void regeneratingTheWorldNotifiesListenersOfTheMovedPlayer() {
        GamePanel panel = FlatFloorWorld.panel();
        Player[] notified = {null};
        panel.addGameListener(new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                notified[0] = player;
            }

            @Override
            public void toggleDevConsole() {
                // Not needed for this test
            }
        });

        panel.regenerateWorld(OPEN_ABOVE_SPAWN_SEED);

        assertEquals(playerOf(panel), notified[0]);
    }

    @Test
    void toggleDevConsoleActionInvokesListener() {
        GamePanel panel = new GamePanel();
        boolean[] listenerCalled = {false};
        GameListener listener = new GameListener() {
            @Override
            public void updatePlayer(Player player) {
                // Not needed for this test
            }

            @Override
            public void toggleDevConsole() {
                listenerCalled[0] = true;
            }
        };
        panel.addGameListener(listener);

        fireAction(panel, Keybindings.ACTION_TOGGLE_DEV_CONSOLE);

        assertTrue(listenerCalled[0]);
    }

    @Test
    void regeneratingTheWorldRespawnsNpcsAtTheirSpawnPoints() {
        GamePanel panel = panelWithPatrollingGuardAfterTwoTurns();

        panel.regenerateWorld(OPEN_ABOVE_SPAWN_SEED);

        NonPlayerCharacter guard = panel.getNpcs().get(0);
        assertEquals(1, panel.getNpcs().size());
        assertEquals(3, guard.getX());
        assertEquals(4, guard.getY());
    }

    @Test
    void regeneratingTheWorldLeavesOneSchedulerRegistrationPerNpc() {
        GamePanel panel = panelWithPatrollingGuardAfterTwoTurns();

        panel.regenerateWorld(OPEN_ABOVE_SPAWN_SEED);

        assertEquals(1, schedulerOf(panel).registeredCount());
        assertEquals(1, panel.getEntitiesToDraw().stream().filter(NonPlayerCharacter.class::isInstance).count());
    }

    @Test
    void regeneratingTheWorldDropsTheOldObjectsAndPlacesOnlyTheNewWorldsObjects() {
        GamePanel panel = FlatFloorWorld.panel();
        panel.regenerateWorld(FIRST_HOUSES_SEED);
        int objectsBefore = objectCount(panel.getScene());

        panel.regenerateWorld(SECOND_HOUSES_SEED);

        assertTrue(objectsBefore > 0, "the first world should have objects to drop");
        assertEquals(expectedObjectCount(SECOND_HOUSES_SEED), objectCount(panel.getScene()));
    }

    private static int objectCount(WorldScene scene) {
        int count = 0;
        for (int x = 0; x < scene.getWidth(); x++) {
            for (int y = 0; y < scene.getHeight(); y++) {
                if (scene.getObject(x, y) != null) {
                    count++;
                }
            }
        }
        return count;
    }

    /** The objects a generated world from {@code seed} should hold: every building placement's object. */
    private static int expectedObjectCount(long seed) {
        GeneratedWorldScene world = new GeneratedWorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT, seed);
        int expected = 0;
        for (Building building : world.getBuildings()) {
            expected += building.getObjects().size();
        }
        return expected;
    }

    /** A flat-floor panel with one patrolling guard that has walked from (3, 4) to (5, 4). */
    private static GamePanel panelWithPatrollingGuardAfterTwoTurns() {
        GamePanel panel = FlatFloorWorld.panel();
        NpcDefinition guard = new NpcDefinition("core:guard", "Guard", 'G', Color.RED, new TileCoord(3, 4),
                new Patrol(PatrolMode.PING_PONG, List.of(new TileCoord(3, 4), new TileCoord(6, 4))), null);
        panel.spawnNpcs(RegistryFixtures.withNpcs(List.of(guard)));
        schedulerOf(panel).advance();
        schedulerOf(panel).advance();
        assertEquals(5, panel.getNpcs().get(0).getX(), "fixture guard must have moved before the regen");
        return panel;
    }

    private static GeneratedWorldScene worldFromSeed(long seed) {
        return new GeneratedWorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT, seed);
    }

    private static void assertPlayerOnSpawn(GamePanel panel) {
        assertPlayerOnSpawn(panel::getPlayer, panel::getScene);
    }

    private static void assertPlayerOnSpawn(Supplier<Player> playerSource, Supplier<WorldScene> sceneSource) {
        Player player = playerSource.get();
        GeneratedWorldScene world = (GeneratedWorldScene) sceneSource.get();
        assertEquals(world.getSpawnX(), player.getX());
        assertEquals(world.getSpawnY(), player.getY());
    }

    /** A panel with the player at (START_X, START_Y) on floor, and {@code tileToTheRight} beside it. */
    private static GamePanel panelWithTileToTheRight(Tile tileToTheRight) {
        WorldScene scene = FlatFloorWorld.create();
        scene.fillRegion(new Rectangle(TILE_RIGHT_X, START_Y, 1, 1), tileToTheRight);
        return new GamePanel(GameEventLog.noOp(), new Player(START_X, START_Y), scene);
    }

    private static final class CountingTickable implements Tickable {
        int steps;

        @Override
        public void onTurn(int turn) {
            steps++;
        }
    }

    private static Player playerOf(GamePanel panel) {
        return panel.getPlayer();
    }

    private static TurnScheduler schedulerOf(GamePanel panel) {
        return panel.getTurnScheduler();
    }

    private static WorldPause worldPauseOf(GamePanel panel) {
        return panel.getWorldPause();
    }

    private static void fireAction(GamePanel panel, String actionName) {
        Action action = panel.getActionMap().get(actionName);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, actionName));
    }
}
