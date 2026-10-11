package com.swiftfaze.veil.ui;

import com.swiftfaze.veil.ui.widget.TableWidget;
import com.swiftfaze.veil.ui.widget.TerminalScrollBarUi;
import com.swiftfaze.veil.ui.widget.WidgetTheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Shared utilities for list/detail split-pane layouts (Codex, Inventory).
 * Extracts common UI building patterns to eliminate duplication between panels
 * implementing the same list-detail contract.
 */
public final class ListDetailLayoutUtility {
    private ListDetailLayoutUtility() {}

    /**
     * Builds a scrollpane with terminal-like scrollbar and transparent viewport.
     */
    public static JScrollPane buildScrollPane(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setUI(new TerminalScrollBarUi());
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        return scrollPane;
    }

    /**
     * Builds a two-column body pane (list on left, detail on right).
     * Reserves unbounded height so the pane expands to fill available vertical space.
     */
    public static JPanel buildBody(JComponent left, JComponent right) {
        JPanel body = new JPanel(new GridLayout(1, 2, 20, 0));
        body.setBackground(WidgetTheme.background());
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(left);
        body.add(right);
        return body;
    }

    /**
     * Builds a styled section label with the given text.
     */
    public static JLabel makeSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(WidgetTheme.normalText());
        label.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(10, 0, 4, 0));
        return label;
    }

    /**
     * Builds the styled details panel with standard borders and layout (shared by Codex and Inventory).
     */
    public static JPanel buildDetailsPanel() {
        Border detailsDivider = BorderFactory.createMatteBorder(0, 2, 0, 0, WidgetTheme.border());
        Border detailsPadding = BorderFactory.createEmptyBorder(4, 10, 0, 0);
        return buildDetailsPanel(BorderFactory.createCompoundBorder(detailsDivider, detailsPadding));
    }

    private static JPanel buildDetailsPanel(Border border) {
        JPanel detailsPanel = new JPanel();
        detailsPanel.setBackground(WidgetTheme.background());
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(border);
        return detailsPanel;
    }

    /**
     * Builds a details panel with no list divider and no padding, for a details pane shown on
     * its own under a header (the dev-console sandbox panels): the padding exists only to space
     * the tables off a divider, so without one the tables sit flush with the header above them.
     */
    public static JPanel buildStandaloneDetailsPanel() {
        return buildDetailsPanel(BorderFactory.createEmptyBorder());
    }

    /**
     * Configures a table widget with standard detail-pane styling (non-wrapping, non-selectable,
     * full width). Height is capped at the table's own rows: left uncapped, BoxLayout stretches
     * the table into the pane's spare height and its left border runs on below the last row.
     */
    public static <T> void configureDetailsTable(TableWidget<T> table) {
        table.setWrapAround(false);
        table.setSelectable(false);
        table.setAlignmentX(Component.LEFT_ALIGNMENT);
        table.setMaximumSize(new Dimension(Integer.MAX_VALUE, table.getPreferredSize().height));
    }
}
