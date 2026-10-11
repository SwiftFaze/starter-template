package com.swiftfaze.veil.render;

public class Camera {
    private static final int MIN_VIEWPORT_TILES = 5;

    private int x;
    private int y;
    private int viewportWidth;
    private int viewportHeight;

    public Camera(int viewportWidth, int viewportHeight) {
        this.viewportWidth = Math.max(MIN_VIEWPORT_TILES, viewportWidth);
        this.viewportHeight = Math.max(MIN_VIEWPORT_TILES, viewportHeight);
    }

    public void centerOn(int targetX, int targetY) {
        x = targetX - viewportWidth / 2;
        y = targetY - viewportHeight / 2;
    }

    public void resizeViewport(int newWidth, int newHeight) {
        this.viewportWidth = Math.max(MIN_VIEWPORT_TILES, newWidth);
        this.viewportHeight = Math.max(MIN_VIEWPORT_TILES, newHeight);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getViewportWidth() { return viewportWidth; }
    public int getViewportHeight() { return viewportHeight; }
}
