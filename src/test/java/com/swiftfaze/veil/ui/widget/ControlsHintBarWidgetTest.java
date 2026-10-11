package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.ui.widget.ControlsHintBarWidget.Hint;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlsHintBarWidgetTest {
    private static final String VERSION_TEXT = "v1.2.3";
    private static final int BAR_WIDTH = 800;
    private static final int BAR_HEIGHT = 60;
    private static final int HINT_FONT_SIZE = 14;
    private static final int VERTICAL_PADDING = 4;
    private static final int HORIZONTAL_PADDING = 8;

    @Test
    void startsWithNoHints() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        assertTrue(bar.getHints().isEmpty());
    }

    @Test
    void setHintsStoresExactContent() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        bar.setHints(List.of(new Hint("up", "Navigate"), new Hint("down", "Navigate")));
        assertEquals(List.of(new Hint("up", "Navigate"), new Hint("down", "Navigate")), bar.getHints());
    }

    @Test
    void setHintsReplacesPreviousContent() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        bar.setHints(List.of(new Hint("up", "Navigate")));
        bar.setHints(List.of(new Hint("enter", "Select")));
        assertEquals(List.of(new Hint("enter", "Select")), bar.getHints());
    }

    @Test
    void setVersionTextShowsTheTextInTheVersionLabel() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        bar.setVersionText(VERSION_TEXT);
        assertEquals(VERSION_TEXT, versionLabelOf(bar).getText());
    }

    @Test
    void versionLabelUsesDimmedTextOnTheBarBackground() {
        JLabel label = versionLabelOf(new ControlsHintBarWidget());
        assertEquals(WidgetTheme.dimmedText(), label.getForeground());
        assertEquals(WidgetTheme.background(), label.getBackground());
        assertTrue(label.isOpaque());
    }

    @Test
    void versionLabelUsesTheHintFontRightAndBottomAligned() {
        JLabel label = versionLabelOf(new ControlsHintBarWidget());
        assertEquals(new Font(Font.MONOSPACED, Font.PLAIN, HINT_FONT_SIZE), label.getFont());
        assertEquals(SwingConstants.RIGHT, label.getHorizontalAlignment());
        assertEquals(SwingConstants.BOTTOM, label.getVerticalAlignment());
    }

    @Test
    void versionLabelIsPadded() {
        JLabel label = versionLabelOf(new ControlsHintBarWidget());
        assertEquals(new Insets(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING, HORIZONTAL_PADDING),
                label.getInsets());
    }

    @Test
    void versionLabelSitsAtTheRightEdge() {
        ControlsHintBarWidget bar = laidOutBar();
        JLabel label = versionLabelOf(bar);
        assertEquals(BAR_WIDTH, label.getX() + label.getWidth());
    }

    @Test
    void hintsFillTheSpaceLeftOfTheVersionLabel() {
        ControlsHintBarWidget bar = laidOutBar();
        JPanel hintsPanel = hintsPanelOf(bar);
        assertEquals(0, hintsPanel.getX());
        assertEquals(versionLabelOf(bar).getX(), hintsPanel.getX() + hintsPanel.getWidth());
        assertEquals(WidgetTheme.background(), hintsPanel.getBackground());
    }

    private static ControlsHintBarWidget laidOutBar() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        bar.setVersionText(VERSION_TEXT);
        bar.setSize(BAR_WIDTH, BAR_HEIGHT);
        bar.doLayout();
        return bar;
    }

    @Test
    void versionTextSurvivesHintChanges() {
        ControlsHintBarWidget bar = new ControlsHintBarWidget();
        bar.setVersionText(VERSION_TEXT);
        bar.setHints(List.of(new Hint("up", "Navigate")));
        bar.setHints(List.of());
        assertEquals(VERSION_TEXT, versionLabelOf(bar).getText());
    }

    private static JLabel versionLabelOf(ControlsHintBarWidget bar) {
        return (JLabel) childOfType(bar, JLabel.class);
    }

    private static JPanel hintsPanelOf(ControlsHintBarWidget bar) {
        return (JPanel) childOfType(bar, JPanel.class);
    }

    private static Component childOfType(ControlsHintBarWidget bar, Class<? extends Component> type) {
        List<Component> matches = Arrays.stream(bar.getComponents()).filter(type::isInstance).toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }
}
