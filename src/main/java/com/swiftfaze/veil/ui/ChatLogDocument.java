package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.entities.dialogue.Conversation;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Font;
import java.util.List;

/**
 * Builds the styled text of a dialogue chat log: one monospaced paragraph per line, the player's
 * lines in the accent colour and the NPC's in the normal text colour. Colours are read from
 * {@link WidgetTheme} each time a log is built, so a theme change shows on the next refresh.
 */
final class ChatLogDocument {

    private static final int LOG_FONT_SIZE = 16;
    private static final float LINE_SPACING = 4f;

    private ChatLogDocument() {
    }

    /** A new document holding {@code lines}, oldest first. */
    static StyledDocument build(List<Conversation.Line> lines) {
        StyledDocument document = new DefaultStyledDocument();
        try {
            for (Conversation.Line line : lines) {
                appendLine(document, line);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("the chat log could not be written", e);
        }
        return document;
    }

    private static void appendLine(StyledDocument document, Conversation.Line line) throws BadLocationException {
        int start = document.getLength();
        document.insertString(start, line.text() + "\n", lineStyle(line.fromPlayer()));
        document.setParagraphAttributes(start, line.text().length() + 1, lineSpacing(), false);
    }

    private static AttributeSet lineStyle(boolean fromPlayer) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setFontFamily(style, Font.MONOSPACED);
        StyleConstants.setFontSize(style, LOG_FONT_SIZE);
        StyleConstants.setForeground(style, fromPlayer ? WidgetTheme.accent() : WidgetTheme.normalText());
        return style;
    }

    /** 4px above and below each line, so neighbouring lines sit 8px apart. */
    private static AttributeSet lineSpacing() {
        SimpleAttributeSet spacing = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(spacing, LINE_SPACING);
        StyleConstants.setSpaceBelow(spacing, LINE_SPACING);
        return spacing;
    }
}
