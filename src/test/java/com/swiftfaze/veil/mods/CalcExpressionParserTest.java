package com.swiftfaze.veil.mods;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalcExpressionParserTest {

    @Test
    void simpleAddition() {
        assertEquals(5.0, CalcExpressionParser.evaluate("2+3", 0));
    }

    @Test
    void operatorPrecedence() {
        assertEquals(14.0, CalcExpressionParser.evaluate("2+3*4", 0));
    }

    @Test
    void parentheses() {
        assertEquals(20.0, CalcExpressionParser.evaluate("(2+3)*4", 0));
    }

    @Test
    void levelVariable() {
        assertEquals(20.0, CalcExpressionParser.evaluate("level*2", 10));
    }

    @Test
    void decimals() {
        assertEquals(3.5, CalcExpressionParser.evaluate("1.5+2.0", 0));
    }

    @Test
    void unaryMinus() {
        assertEquals(-5.0, CalcExpressionParser.evaluate("-5", 0));
    }

    @Test
    void complexExpression() {
        assertEquals(9.5, CalcExpressionParser.evaluate("level*1.5+2", 5));
    }
}
