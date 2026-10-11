package com.swiftfaze.veil.ui.widget;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Persistent bar docked at the bottom of the game window, rendering the
 * currently-valid key bindings pushed in by whichever screen currently has
 * focus. Each hint's key renders as a literal, reverse-video "keycap"
 * (background/foreground swapped from the theme's normal text colors) so it
 * reads like a terminal help bar (nano's status line), wrapping into a
 * compact grid instead of one ever-widening line.
 *
 * The bar also displays the application version at its right edge, using the
 * same font and a dimmed text color so it doesn't compete with the hints.
 */
public final class ControlsHintBarWidget extends JPanel {
    private static final Font HINT_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final int MAX_COLUMNS = 3;
    private static final int KEY_HORIZONTAL_PADDING = 4;
    private static final int CELL_GAP = 6;
    private static final int GRID_HGAP = 20;
    private static final int GRID_VGAP = 2;
    private static final int LABEL_PADDING_VERTICAL = 4;
    private static final int LABEL_PADDING_HORIZONTAL = 8;

    private List<Hint> hints = List.of();
    private final JPanel hintsPanel = new JPanel(new GridLayout(1, 1));
    private final JLabel versionLabel = new JLabel();

    /**
     * A single "key does action" pair. The widget owns turning {@code key}
     * ("up", "escape", "shift+tab") into its displayed keycap label -
     * callers pass the raw key identifier, not a formatted string.
     */
    public record Hint(String key, String action) {
    }

    public ControlsHintBarWidget() {
        setBackground(WidgetTheme.background());
        setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, WidgetTheme.border()));
        setLayout(new BorderLayout());

        hintsPanel.setBackground(WidgetTheme.background());
        add(hintsPanel, BorderLayout.CENTER);

        versionLabel.setForeground(WidgetTheme.dimmedText());
        versionLabel.setBackground(WidgetTheme.background());
        versionLabel.setOpaque(true);
        versionLabel.setFont(HINT_FONT);
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(
                LABEL_PADDING_VERTICAL, LABEL_PADDING_HORIZONTAL,
                LABEL_PADDING_VERTICAL, LABEL_PADDING_HORIZONTAL));
        add(versionLabel, BorderLayout.EAST);
    }

    public void setHints(List<Hint> newHints) {
        hints = new ArrayList<>(newHints);
        rebuild();
        revalidate();
        repaint();
    }

    public List<Hint> getHints() {
        return List.copyOf(hints);
    }

    public void setVersionText(String versionText) {
        versionLabel.setText(versionText);
    }

    private void rebuild() {
        hintsPanel.removeAll();
        if (hints.isEmpty()) {
            hintsPanel.setLayout(new GridLayout(1, 1));
            return;
        }
        int columns = Math.min(MAX_COLUMNS, hints.size());
        int rows = (int) Math.ceil(hints.size() / (double) columns);
        hintsPanel.setLayout(new GridLayout(rows, columns, GRID_HGAP, GRID_VGAP));

        int keyWidth = widestKeyWidth();
        FontMetrics metrics = getFontMetrics(HINT_FONT);
        for (Hint cell : columnMajorOrder(rows, columns)) {
            hintsPanel.add(buildCell(cell, keyWidth, metrics.getHeight()));
        }
    }

    private List<Hint> columnMajorOrder(int rows, int columns) {
        Hint[] cells = new Hint[rows * columns];
        for (int i = 0; i < hints.size(); i++) {
            int col = i / rows;
            int row = i % rows;
            cells[row * columns + col] = hints.get(i);
        }
        return Arrays.asList(cells);
    }

    private static JPanel buildCell(Hint hint, int keyWidth, int keyHeight) {
        JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, CELL_GAP, 0));
        cell.setBackground(WidgetTheme.background());
        if (hint == null) {
            return cell;
        }

        JLabel key = new JLabel(keycapText(hint.key()));
        key.setOpaque(true);
        key.setBackground(WidgetTheme.normalText());
        key.setForeground(WidgetTheme.background());
        key.setFont(HINT_FONT);
        key.setHorizontalAlignment(SwingConstants.LEFT);
        key.setPreferredSize(new Dimension(keyWidth, keyHeight));

        JLabel action = new JLabel(hint.action());
        action.setForeground(WidgetTheme.normalText());
        action.setFont(HINT_FONT);

        cell.add(key);
        cell.add(action);
        return cell;
    }

    private int widestKeyWidth() {
        FontMetrics metrics = getFontMetrics(HINT_FONT);
        int max = 0;
        for (Hint hint : hints) {
            max = Math.max(max, metrics.stringWidth(keycapText(hint.key())));
        }
        return max + KEY_HORIZONTAL_PADDING;
    }

    // Literal keycap label, capitalized the way it'd be printed on a
    // keyboard/legend - no glyph substitution. "escape"/"enter" are
    // special-cased since they're not just a capitalized first letter.
    private static String keycapText(String key) {
        return switch (key) {
            case "escape" -> "Esc";
            case "enter" -> "Enter";
            default -> Character.toUpperCase(key.charAt(0)) + key.substring(1);
        };
    }
}
