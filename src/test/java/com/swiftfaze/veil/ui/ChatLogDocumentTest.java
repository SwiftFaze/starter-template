package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.ui.widget.WidgetTheme;
import org.junit.jupiter.api.Test;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Font;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatLogDocumentTest {

    private static final String NPC_LINE = "Mira: Hi";
    private static final String PLAYER_LINE = "You: Bye";
    private static final int BODY_FONT_SIZE = 16;
    private static final float LINE_GAP = 4f;

    private static StyledDocument built() {
        return ChatLogDocument.build(List.of(
                new Conversation.Line(false, NPC_LINE), new Conversation.Line(true, PLAYER_LINE)));
    }

    private static AttributeSet attributesOfSecondLine(StyledDocument document) {
        return document.getCharacterElement(NPC_LINE.length() + 1).getAttributes();
    }

    @Test
    void eachLineIsItsOwnParagraphOldestFirst() throws BadLocationException {
        StyledDocument document = built();

        assertEquals(NPC_LINE + "\n" + PLAYER_LINE + "\n", document.getText(0, document.getLength()));
    }

    @Test
    void anEmptyLogBuildsAnEmptyDocument() {
        assertEquals(0, ChatLogDocument.build(List.of()).getLength());
    }

    @Test
    void linesAreBodySizeMonospaced() {
        AttributeSet attributes = built().getCharacterElement(0).getAttributes();

        assertEquals(Font.MONOSPACED, StyleConstants.getFontFamily(attributes));
        assertEquals(BODY_FONT_SIZE, StyleConstants.getFontSize(attributes));
    }

    @Test
    void theNpcLineUsesTheNormalTextColourAndThePlayerLineTheAccent() {
        StyledDocument document = built();

        assertEquals(WidgetTheme.normalText(), StyleConstants.getForeground(document.getCharacterElement(0).getAttributes()));
        assertEquals(WidgetTheme.accent(), StyleConstants.getForeground(attributesOfSecondLine(document)));
    }

    @Test
    void everyLineHasSpaceAboveAndBelow() {
        AttributeSet second = built().getParagraphElement(NPC_LINE.length() + 1).getAttributes();

        assertEquals(LINE_GAP, StyleConstants.getSpaceAbove(second));
        assertEquals(LINE_GAP, StyleConstants.getSpaceBelow(second));
    }
}
