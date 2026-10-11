package com.swiftfaze.veil.entities.player;

import com.swiftfaze.veil.render.Camera;
import com.swiftfaze.veil.render.DrawableAsciiEntity;
import com.swiftfaze.veil.world.WorldScene;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;


public class Player implements DrawableAsciiEntity {


    private int x;
    private int y;
    private static final char symbol = '◼';
    private static final Color color = Color.decode("#ef481f");
    private static final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 18);

    private final PlayerInfo playerInfo;

    public Player(int startX, int startY) {
        this.playerInfo = new PlayerInfo();
        this.x = startX;
        this.y = startY;
    }

    public void moveUp(WorldScene worldScene) {
        move(0, -1, worldScene);
    }

    public void moveDown(WorldScene worldScene) {
        move(0, 1, worldScene);
    }

    public void moveLeft(WorldScene worldScene) {
        move(-1, 0, worldScene);
    }

    public void moveRight(WorldScene worldScene) {
        move(1, 0, worldScene);
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    private void move(int dx, int dy, WorldScene worldScene) {
        int newX = x + dx;
        int newY = y + dy;

        if (worldScene.isWalkable(newX, newY)) {
            x = newX;
            y = newY;
        }
    }

    public PlayerInfo getPlayerInfo() {
        return playerInfo;
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
        return symbol;
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void render(Graphics2D g2d, int tileWidth, int tileHeight, Camera camera) {
        g2d.setFont(font);
        g2d.setColor(color);
        int screenX = (x - camera.getX()) * tileWidth;
        int screenY = (y - camera.getY()) * tileHeight + tileHeight;
        g2d.drawString(String.valueOf(symbol), screenX, screenY);
    }

    public Stats getStats() {
        return playerInfo.getStats();
    }
}
