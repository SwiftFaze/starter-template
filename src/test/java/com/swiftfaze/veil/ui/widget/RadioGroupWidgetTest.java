package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RadioGroupWidgetTest {
    private RadioGroupWidget<String> verticalRadio;
    private RadioGroupWidget<String> horizontalRadio;
    private List<String> confirmedOptions;

    @BeforeEach
    public void setUp() {
        confirmedOptions = new ArrayList<>();
        verticalRadio = new RadioGroupWidget<>(s -> s, false);
        verticalRadio.setOnConfirm(confirmedOptions::add);
        horizontalRadio = new RadioGroupWidget<>(s -> s, true);
        horizontalRadio.setOnConfirm(confirmedOptions::add);
    }

    @Test
    public void navigatingVerticalDownMovesHighlightedOptionToNext() {
        verticalRadio.setOptions(List.of("Warrior", "Mage", "Rogue"));
        assertEquals("Warrior", verticalRadio.getHighlightedOption());
        verticalRadio.moveDown();
        assertEquals("Mage", verticalRadio.getHighlightedOption());
    }

    @Test
    public void movingUpFromFirstOptionWrapsToLastOption() {
        verticalRadio.setOptions(List.of("Warrior", "Mage", "Rogue"));
        assertEquals("Warrior", verticalRadio.getHighlightedOption());
        verticalRadio.moveUp();
        assertEquals("Rogue", verticalRadio.getHighlightedOption());
    }

    @Test
    public void confirmingRadioGroupSelectsHighlightedOption() {
        verticalRadio.setOptions(List.of("Warrior", "Mage", "Rogue"));
        verticalRadio.moveDown();
        assertEquals("Mage", verticalRadio.getHighlightedOption());
        verticalRadio.getActionMap().get("radio-confirm").actionPerformed(null);
        assertEquals("Mage", verticalRadio.getSelectedOption());
        assertEquals(1, confirmedOptions.size());
        assertEquals("Mage", confirmedOptions.get(0));
    }

    @Test
    public void selectingNewOptionDeselectsPreviousOne() {
        verticalRadio.setOptions(List.of("Warrior", "Mage", "Rogue"));
        verticalRadio.selectOption(0);
        assertEquals("Warrior", verticalRadio.getSelectedOption());
        verticalRadio.moveDown();
        verticalRadio.getActionMap().get("radio-confirm").actionPerformed(null);
        assertEquals("Mage", verticalRadio.getSelectedOption());
        assertNotEquals("Warrior", verticalRadio.getSelectedOption());
    }

    @Test
    public void horizontalRadioNavigatesWithLeftRight() {
        horizontalRadio.setOptions(List.of("Windowed", "Fullscreen"));
        assertEquals("Windowed", horizontalRadio.getHighlightedOption());
        horizontalRadio.moveRight();
        assertEquals("Fullscreen", horizontalRadio.getHighlightedOption());
    }

    @Test
    public void emptyRadioGroupNavigationIsNoOp() {
        verticalRadio.setOptions(List.of());
        verticalRadio.moveUp();
        verticalRadio.moveDown();
        assertNull(verticalRadio.getHighlightedOption());
    }

    @Test
    public void emptyRadioGroupConfirmIsNoOp() {
        verticalRadio.setOptions(List.of());
        verticalRadio.getActionMap().get("radio-confirm").actionPerformed(null);
        assertEquals(0, confirmedOptions.size());
    }
}
