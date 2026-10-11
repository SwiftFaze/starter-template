package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranscriptWidgetTest {

    @Test
    void resultTablePadsEachColumnToItsWidestValueAndSeparatesWithGap() {
        TranscriptWidget transcript = new TranscriptWidget();

        transcript.appendResultTable(
                List.of("id", "name"),
                List.of(List.of("1", "sword"), List.of("22", "axe")));

        Component[] lines = transcript.getComponents();
        assertEquals("id   name ", ((JLabel) lines[0]).getText());
        assertEquals("1    sword", ((JLabel) lines[1]).getText());
        assertEquals("22   axe  ", ((JLabel) lines[2]).getText());
    }
}
