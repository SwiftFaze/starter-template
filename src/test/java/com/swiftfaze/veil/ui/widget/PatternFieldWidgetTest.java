package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatternFieldWidgetTest {
    private PatternFieldWidget patternField;

    @BeforeEach
    public void setUp() {
        patternField = new PatternFieldWidget("^[A-Za-z]+$");
    }

    @Test
    public void typingInputThatMatchesPatternShowsValidState() {
        patternField.typeCharacters("Rob");
        assertEquals("Rob", patternField.getInput());
        assertTrue(patternField.patternIsValid());
    }

    @Test
    public void typingInputThatFailsPatternShowsInvalidState() {
        patternField.typeCharacters("Rob1");
        assertEquals("Rob1", patternField.getInput());
        assertFalse(patternField.patternIsValid());
    }

    @Test
    public void emptyPatternFieldReflectsWhetherPatternMatchesEmptyString() {
        assertEquals("", patternField.getInput());
        assertFalse(patternField.patternIsValid());
    }

    @Test
    public void correctingInvalidInputBackToMatchReturnsToValidState() {
        patternField.typeCharacters("Rob1");
        assertFalse(patternField.patternIsValid());
        patternField.deleteLastCharacter();
        assertEquals("Rob", patternField.getInput());
        assertTrue(patternField.patternIsValid());
    }

    @Test
    public void deletingLastCharacterWorks() {
        patternField.typeCharacters("Test");
        assertEquals("Test", patternField.getInput());
        patternField.deleteLastCharacter();
        assertEquals("Tes", patternField.getInput());
    }

    @Test
    public void deletingFromEmptyFieldDoesNothing() {
        assertEquals("", patternField.getInput());
        patternField.deleteLastCharacter();
        assertEquals("", patternField.getInput());
    }
}
