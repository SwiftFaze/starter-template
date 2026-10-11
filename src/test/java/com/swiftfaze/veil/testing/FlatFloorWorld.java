package com.swiftfaze.veil.testing;

import com.swiftfaze.veil.GameConst;
import com.swiftfaze.veil.entities.player.Player;
import com.swiftfaze.veil.game.GamePanel;
import com.swiftfaze.veil.game.event.GameEventLog;
import com.swiftfaze.veil.world.Tile;
import com.swiftfaze.veil.world.WorldScene;

import java.awt.Color;

/**
 * Test-only all-walkable map at the default game size. Movement tests use it so they don't
 * depend on generated terrain, which is random per new game.
 */
public final class FlatFloorWorld {

    private static final Tile FLOOR = new Tile("test:floor", '.', Color.GRAY, true);

    private FlatFloorWorld() {
    }

    public static WorldScene create() {
        WorldScene world = new WorldScene(GameConst.DEFAULT_MAP_WIDTH, GameConst.DEFAULT_MAP_HEIGHT) {
        };
        world.fillAll(FLOOR);
        return world;
    }

    /** A panel with the player at the map centre on {@link #create()}. */
    public static GamePanel panel() {
        Player player = new Player(GameConst.DEFAULT_PLAYER_START_X, GameConst.DEFAULT_PLAYER_START_Y);
        return new GamePanel(GameEventLog.noOp(), player, create());
    }
}
