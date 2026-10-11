package com.swiftfaze.veil.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraTest {

    @Test
    void centeringOffsetsByHalfTheViewport() {
        Camera camera = new Camera(10, 10);

        camera.centerOn(20, 20);

        assertEquals(15, camera.getX());
        assertEquals(15, camera.getY());
    }

    @Test
    void centeringFollowsTargetMinusHalfViewportForUnevenOffsets() {
        Camera camera = new Camera(10, 10);

        camera.centerOn(25, 13);

        assertEquals(20, camera.getX());
        assertEquals(8, camera.getY());
    }

    @Test
    void reCenteringReplacesThePreviousOffsetWithNoSmoothing() {
        Camera camera = new Camera(10, 10);
        camera.centerOn(20, 20);

        camera.centerOn(30, 30);

        assertEquals(25, camera.getX());
        assertEquals(25, camera.getY());
    }

    @Test
    void centeringNearAMapEdgeIsNotClampedToMapBounds() {
        Camera camera = new Camera(10, 10);

        camera.centerOn(2, 2);

        assertEquals(-3, camera.getX());
        assertEquals(-3, camera.getY());
    }
}
