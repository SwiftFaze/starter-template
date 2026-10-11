package com.swiftfaze.veil.ui.widget;

import javax.swing.JLabel;
import javax.swing.BoxLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An append-only, scrolling log view for the dev console: each typed command and its output
 * become new lines added to the bottom, never replacing what's already there - the command
 * itself is echoed as a dimmed line before its result. No timestamp or level word is shown;
 * a line's color is the only status signal (dimmed for an echoed command, normal for info,
 * green for success, red for error). A result set renders as plain, column-aligned text inline
 * in the flow (a "console.table"-style listing, not a bordered/selectable grid widget) -
 * columns padded to their widest value and separated by whitespace, with a colored header row.
 */
public class TranscriptWidget extends Widget {

    private static final Font LINE_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 16);
    private static final String COLUMN_GAP = "   ";

    public enum Level { COMMAND, INFO, SUCCESS, ERROR }

    public record TranscriptEntry(Level level, String text) {
    }

    private final List<TranscriptEntry> entries = new ArrayList<>();
    private List<List<String>> lastResultTable;

    public TranscriptWidget() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public void appendCommand(String text) {
        append(Level.COMMAND, text, WidgetTheme.dimmedText());
    }

    public void appendInfo(String text) {
        append(Level.INFO, text, WidgetTheme.normalText());
    }

    public void appendError(String text) {
        append(Level.ERROR, text, WidgetTheme.invalidHighlight());
    }

    public void appendSuccess(String text) {
        append(Level.SUCCESS, text, WidgetTheme.validHighlight());
    }

    public void appendResultTable(List<String> headers, List<List<String>> rows) {
        lastResultTable = rows;
        int[] columnWidths = columnWidths(headers, rows);
        add(fullWidth(buildTableLine(headers, columnWidths, WidgetTheme.tableHeaderText())));
        for (List<String> row : rows) {
            add(fullWidth(buildTableLine(row, columnWidths, WidgetTheme.normalText())));
        }
        scrollToBottom();
    }

    public List<TranscriptEntry> entries() {
        return Collections.unmodifiableList(entries);
    }

    public Optional<List<List<String>>> lastResultTable() {
        return Optional.ofNullable(lastResultTable);
    }

    private void append(Level level, String text, Color color) {
        entries.add(new TranscriptEntry(level, text));
        add(fullWidth(plainLabel(text, color)));
        scrollToBottom();
    }

    private static int[] columnWidths(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }
        return widths;
    }

    private static JLabel buildTableLine(List<String> cells, int[] columnWidths, Color color) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                line.append(COLUMN_GAP);
            }
            line.append(padded(cells.get(i), columnWidths[i]));
        }
        return plainLabel(line.toString(), color);
    }

    private static String padded(String text, int width) {
        return String.format(Locale.ROOT, "%-" + width + "s", text);
    }

    private static JLabel plainLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(LINE_FONT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Stretches a line's maximum width to fill whatever the transcript's container offers -
     * matching HeaderWidget/PatternFieldWidget's own "full width" convention - rather than
     * leaving it clamped to its own preferred (text-length) width.
     */
    private static <T extends Component> T fullWidth(T component) {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }

    private void scrollToBottom() {
        revalidate();
        scrollRectToVisible(new Rectangle(0, Math.max(0, getPreferredSize().height - 1), 1, 1));
    }
}
