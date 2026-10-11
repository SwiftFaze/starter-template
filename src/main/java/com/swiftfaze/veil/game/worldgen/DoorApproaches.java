package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import com.swiftfaze.veil.world.object.Door;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The world tiles directly outside a building's doors. A door is a {@link Door} object placed on
 * the building; a door on an edge row or column contributes the tile just beyond that edge, and a
 * corner door contributes both.
 */
final class DoorApproaches {

    private DoorApproaches() {
    }

    static List<Point> outsideDoorsOf(Building building) {
        List<Point> approaches = new ArrayList<>();
        for (Building.ObjectPlacement placement : building.getObjects()) {
            if (placement.object() instanceof Door) {
                addOutsideEdges(building, placement.x(), placement.y(), approaches);
            }
        }
        return approaches;
    }

    private static void addOutsideEdges(Building building, int x, int y, Collection<Point> approaches) {
        int worldX = building.getWorldX() + x;
        int worldY = building.getWorldY() + y;
        if (y == 0) {
            approaches.add(new Point(worldX, worldY - 1));
        }
        if (y == building.getHeight() - 1) {
            approaches.add(new Point(worldX, worldY + 1));
        }
        if (x == 0) {
            approaches.add(new Point(worldX - 1, worldY));
        }
        if (x == building.getWidth() - 1) {
            approaches.add(new Point(worldX + 1, worldY));
        }
    }
}
