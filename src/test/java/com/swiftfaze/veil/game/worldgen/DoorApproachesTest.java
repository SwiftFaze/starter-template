package com.swiftfaze.veil.game.worldgen;

import com.swiftfaze.veil.entities.buildings.Building;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoorApproachesTest {

    private static final int WORLD_X = 10;
    private static final int WORLD_Y = 20;

    @Test
    void bottomEdgeDoorOpensOntoTheTileBelow() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "SWS", "SDS");

        assertEquals(List.of(new Point(11, 23)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void topEdgeDoorOpensOntoTheTileAbove() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SDS", "SWS", "SSS");

        assertEquals(List.of(new Point(11, 19)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void leftEdgeDoorOpensOntoTheTileToTheLeft() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "DWS", "SSS");

        assertEquals(List.of(new Point(9, 21)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void rightEdgeDoorOpensOntoTheTileToTheRight() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "SWD", "SSS");

        assertEquals(List.of(new Point(13, 21)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void topLeftCornerDoorOpensOntoBothOutsideTiles() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "DSS", "SWS", "SSS");

        assertEquals(List.of(new Point(10, 19), new Point(9, 20)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void bottomRightCornerDoorOpensOntoBothOutsideTiles() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "SWS", "SSD");

        assertEquals(List.of(new Point(12, 23), new Point(13, 22)), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void interiorDoorHasNoApproach() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "SDS", "SSS");

        assertEquals(List.of(), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void buildingWithoutADoorHasNoApproach() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SSS", "SWS", "SSS");

        assertEquals(List.of(), DoorApproaches.outsideDoorsOf(house));
    }

    @Test
    void everyDoorContributesItsApproach() {
        Building house = WorldgenFixtures.building(WORLD_X, WORLD_Y, "SDS", "SWS", "SDS");

        assertEquals(List.of(new Point(11, 19), new Point(11, 23)), DoorApproaches.outsideDoorsOf(house));
    }
}
