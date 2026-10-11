package com.swiftfaze.veil.game;

import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.input.Keybindings;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;
import com.swiftfaze.veil.world.object.Door;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.swing.Action;
import java.awt.Color;
import java.awt.event.ActionEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GamePanelInteractTest {

    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);
    private static final int PLAYER_X = 5;
    private static final int PLAYER_Y = 5;
    private static final int SCENE_SIZE = 10;

    @ParameterizedTest
    @CsvSource({"0,-1", "1,0", "0,1", "-1,0"})
    void interactReachesADoorOnEachOrthogonalNeighbour(int dx, int dy) {
        WorldScene scene = floorScene();
        Door door = closedDoor();
        scene.placeObject(PLAYER_X + dx, PLAYER_Y + dy, door);
        GamePanel panel = new GamePanel(GameEventLog.noOp(), new Player(PLAYER_X, PLAYER_Y), scene);

        pressInteract(panel);

        assertTrue(door.isOpen());
    }

    @ParameterizedTest
    @CsvSource({"1,1", "-1,-1", "2,0", "0,-2"})
    void interactIgnoresDoorsOnDiagonalsAndBeyondTheNeighbours(int dx, int dy) {
        WorldScene scene = floorScene();
        Door door = closedDoor();
        scene.placeObject(PLAYER_X + dx, PLAYER_Y + dy, door);
        GamePanel panel = new GamePanel(GameEventLog.noOp(), new Player(PLAYER_X, PLAYER_Y), scene);

        pressInteract(panel);

        assertFalse(door.isOpen());
    }

    @Test
    void anOpenDoorWithNobodyInItClosesEvenThoughThePlayerIsStandingNearby() {
        WorldScene scene = floorScene();
        Door door = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), true);
        scene.placeObject(PLAYER_X + 1, PLAYER_Y, door);
        GamePanel panel = new GamePanel(GameEventLog.noOp(), new Player(PLAYER_X, PLAYER_Y), scene);

        pressInteract(panel);

        assertFalse(door.isOpen());
    }

    @Test
    void theScenesOwnOriginDoesNotCountAsSomeoneStandingInAnOpenDoor() {
        WorldScene scene = floorScene();
        Door door = new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), true);
        scene.placeObject(0, 0, door);
        GamePanel panel = new GamePanel(GameEventLog.noOp(), new Player(1, 0), scene);

        pressInteract(panel);

        assertFalse(door.isOpen());
    }

    @ParameterizedTest
    @CsvSource({"0,-1,1,0", "1,0,0,1", "0,1,-1,0", "0,-1,0,1", "1,0,-1,0"})
    void whenTwoNeighboursAreInteractableTheFirstInNorthEastSouthWestOrderIsTheOneUsed(
            int firstDx, int firstDy, int laterDx, int laterDy) {
        WorldScene scene = floorScene();
        Door first = closedDoor();
        Door later = closedDoor();
        scene.placeObject(PLAYER_X + firstDx, PLAYER_Y + firstDy, first);
        scene.placeObject(PLAYER_X + laterDx, PLAYER_Y + laterDy, later);
        GamePanel panel = new GamePanel(GameEventLog.noOp(), new Player(PLAYER_X, PLAYER_Y), scene);

        pressInteract(panel);

        assertTrue(first.isOpen());
        assertFalse(later.isOpen());
    }

    private static WorldScene floorScene() {
        WorldScene scene = new WorldScene(SCENE_SIZE, SCENE_SIZE) {
        };
        scene.fillAll(FLOOR);
        return scene;
    }

    private static Door closedDoor() {
        return new Door("test:door", Color.ORANGE, new Door.Glyphs('/', '+'), false);
    }

    private static void pressInteract(GamePanel panel) {
        Action action = panel.getActionMap().get(Keybindings.ACTION_INTERACT);
        action.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, Keybindings.ACTION_INTERACT));
    }
}
