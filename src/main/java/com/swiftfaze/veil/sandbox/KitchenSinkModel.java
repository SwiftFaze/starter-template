package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.world.WorldScene;
import java.util.Optional;

/**
 * Model for the kitchen-sink preview: marker position, overlay state, and
 * the underlying scene. Kept separate so tests can assert on the model
 * without depending on pixel rendering.
 */
public class KitchenSinkModel {
    private final WorldScene scene;
    private int markerX;
    private int markerY;
    private boolean overlayOn;

    public KitchenSinkModel(WorldScene scene) {
        this.scene = scene;
        initializeMarkerPosition();
    }

    private void initializeMarkerPosition() {
        // Start on the first walkable tile that has a walkable neighbour, so the marker can move
        // at all. Falls back to the first walkable tile, then to the origin (empty registry).
        firstWalkable(true).or(() -> firstWalkable(false))
                .ifPresent(cell -> {
                    this.markerX = cell.x();
                    this.markerY = cell.y();
                });
    }

    private record Cell(int x, int y) {
    }

    /** The first walkable cell in row-major order, optionally only one with a walkable neighbour. */
    private Optional<Cell> firstWalkable(boolean needsWalkableNeighbour) {
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.isWalkable(x, y) && (!needsWalkableNeighbour || hasWalkableNeighbour(x, y))) {
                    return Optional.of(new Cell(x, y));
                }
            }
        }
        return Optional.empty();
    }

    private boolean hasWalkableNeighbour(int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && scene.isWalkable(x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void moveMarker(int dx, int dy) {
        int newX = markerX + dx;
        int newY = markerY + dy;
        if (scene.isWalkable(newX, newY)) {
            markerX = newX;
            markerY = newY;
        }
    }

    /**
     * Whether the scene holds at least one tile. An empty registry still yields a 1x1 scene
     * (WorldScene can't be 0x0), so size alone can't tell.
     */
    public boolean hasTiles() {
        for (int y = 0; y < scene.getHeight(); y++) {
            for (int x = 0; x < scene.getWidth(); x++) {
                if (scene.getTile(x, y) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public void toggleOverlay() {
        overlayOn = !overlayOn;
    }

    public WorldScene scene() {
        return scene;
    }

    public int getMarkerX() {
        return markerX;
    }

    public int getMarkerY() {
        return markerY;
    }

    public boolean isOverlayOn() {
        return overlayOn;
    }
}
