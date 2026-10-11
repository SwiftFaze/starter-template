package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SliderWidgetTest {
    private SliderWidget slider;

    @BeforeEach
    public void setUp() {
        slider = new SliderWidget(0, 10, 1, 5);
    }

    @Test
    public void movingRightIncreasesTheValueByOneStep() {
        assertEquals(5, slider.getValue());
        slider.moveRight();
        assertEquals(6, slider.getValue());
    }

    @Test
    public void movingLeftDecreasesTheValueByOneStep() {
        assertEquals(5, slider.getValue());
        slider.moveLeft();
        assertEquals(4, slider.getValue());
    }

    @Test
    public void movingRightAtTheMaximumDoesNotExceedTheMaximum() {
        slider = new SliderWidget(0, 10, 1, 10);
        assertEquals(10, slider.getValue());
        slider.moveRight();
        assertEquals(10, slider.getValue());
    }

    @Test
    public void movingLeftAtTheMinimumDoesNotGoBelowTheMinimum() {
        slider = new SliderWidget(0, 10, 1, 0);
        assertEquals(0, slider.getValue());
        slider.moveLeft();
        assertEquals(0, slider.getValue());
    }
}
