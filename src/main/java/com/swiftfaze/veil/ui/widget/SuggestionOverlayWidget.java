package com.swiftfaze.veil.ui.widget;

import org.jspecify.annotations.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A suggestion list anchored directly above an owner component (the dev console's command
 * field) - like an IDE/shell autocomplete popup. Lives as a persistent child of the owner
 * window's JLayeredPane (popup layer), repositioned/resized/repainted in place on every update
 * rather than torn down and recreated. An earlier version used a fresh javax.swing.Popup window
 * per keystroke; that visibly flashed blank on every update under rapid typing (a brand new
 * rendering surface always starts from its default clear color, and Popup exposes no way to
 * resize/reuse an already-shown one) - this design has no new surface to flash, since it's a
 * normal component in an already-painted window throughout.
 */
public class SuggestionOverlayWidget {

    private static final Font ROW_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 14);
    private static final int ROW_PADDING_V = 4;
    private static final int ROW_PADDING_H = 8;
    // Matches PatternFieldWidget's own outline thickness (both its focused and unfocused
    // border widths are 1px) so the overlay reads as part of the same command field, not a
    // visually heavier separate box.
    private static final int BORDER_WIDTH = 1;

    private List<String> candidates = List.of();
    private List<JLabel> rowLabels = new ArrayList<>();
    private int highlightedIndex;
    private JPanel content;
    private JLayeredPane layeredPane;
    private boolean isActive = false;

    public void show(List<String> newCandidates, JComponent owner) {
        show(newCandidates, owner, 0, 0);
    }

    /**
     * Shows the overlay anchored to {@code owner}, offset from its top edge.
     *
     * @param ownerTopOffset how far down from owner's own top edge its actual visible box
     *                       begins - 0 if owner's border paints flush with its own bounds,
     *                       which is the Swing default. A positive value places the overlay's
     *                       bottom edge that far down instead of at owner's own top edge - past
     *                       that point risks painting over whatever owner itself draws in that
     *                       reserved space (e.g. a floating title), since the overlay is an
     *                       opaque rectangle, not just a border line.
     */
    public void show(List<String> newCandidates, JComponent owner, int ownerTopOffset) {
        show(newCandidates, owner, ownerTopOffset, 0);
    }

    /**
     * Shows the overlay anchored to {@code owner}, offset from its top edge and inset horizontally.
     *
     * @param horizontalInset how far in from owner's own left and right edges its actual visible
     *                        box sits, on each side - 0 if owner's border paints flush with its
     *                        own bounds. Needed separately from ownerTopOffset because a Swing
     *                        TitledBorder (a floating field label) insets its wrapped border's
     *                        outline from the component's raw left/right bounds too, not just
     *                        from the top edge where the title itself lives - see
     *                        PatternFieldWidget.getVisibleBoxHorizontalInset().
     */
    public void show(List<String> newCandidates, JComponent owner, int ownerTopOffset, int horizontalInset) {
        this.candidates = List.copyOf(newCandidates);
        this.highlightedIndex = 0;
        this.isActive = true;
        // Row labels back highlight navigation (moveHighlightUp/Down) independently of whether
        // there's a real window to paint into - built unconditionally so a headless test (no
        // frame ever shown) can still exercise Up/Down without an out-of-bounds crash below.
        rowLabels = buildRowLabels();

        JLayeredPane pane = layeredPaneOf(owner);
        if (pane == null) {
            // Owner has no window ancestor yet (e.g. a headless test that never shows the
            // frame) - overlay state stays active for test assertions, nothing to paint yet.
            return;
        }
        attachTo(pane);
        content.removeAll();
        rowLabels.forEach(content::add);
        positionAbove(owner, pane, ownerTopOffset, horizontalInset);
        content.setVisible(true);
        content.revalidate();
        content.repaint();
    }

    public void hide() {
        if (content != null) {
            content.setVisible(false);
        }
        rowLabels = new ArrayList<>();
        isActive = false;
    }

    public boolean isShowing() {
        return isActive && !candidates.isEmpty();
    }

    public List<String> candidates() {
        return candidates;
    }

    public String highlighted() {
        return candidates.get(highlightedIndex);
    }

    public void moveHighlightDown() {
        setHighlightedIndex((highlightedIndex + 1) % candidates.size());
    }

    public void moveHighlightUp() {
        setHighlightedIndex((highlightedIndex - 1 + candidates.size()) % candidates.size());
    }

    private void setHighlightedIndex(int newIndex) {
        restyleRow(highlightedIndex, /* isHighlighted= */ false);
        highlightedIndex = newIndex;
        restyleRow(highlightedIndex, /* isHighlighted= */ true);
    }

    private void restyleRow(int index, boolean isHighlighted) {
        JLabel row = rowLabels.get(index);
        row.setBackground(isHighlighted ? WidgetTheme.selectedHighlight() : WidgetTheme.background());
        row.setForeground(isHighlighted ? WidgetTheme.selectedText() : WidgetTheme.normalText());
    }

    private static @Nullable JLayeredPane layeredPaneOf(JComponent owner) {
        Container ancestor = SwingUtilities.getAncestorOfClass(JLayeredPane.class, owner);
        return ancestor instanceof JLayeredPane pane ? pane : null;
    }

    private void attachTo(JLayeredPane pane) {
        if (content != null && Objects.equals(pane, layeredPane)) {
            return;
        }
        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(WidgetTheme.background());
        content.setBorder(BorderFactory.createLineBorder(WidgetTheme.windowBorder(), BORDER_WIDTH));
        this.layeredPane = pane;
        pane.add(content, JLayeredPane.POPUP_LAYER);
    }

    private List<JLabel> buildRowLabels() {
        List<JLabel> labels = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            labels.add(fullWidth(buildRow(candidates.get(i), i == highlightedIndex)));
        }
        return labels;
    }

    /**
     * Sized and positioned in one setBounds() call. A plain Swing border paints flush with its
     * own component's bounds - it never insets itself from them - so by default the visible
     * box's left, right, and bottom edges exactly match owner's own bounds, and width matches
     * owner's own on-screen width (same "full width" convention as TranscriptWidget's
     * fullWidth() helper, just applied to the whole popup instead of a transcript line).
     * ownerTopOffset and horizontalInset are the exceptions: owner may reserve extra space
     * around its own bounds - above the top edge for a floating label, and inset from the left
     * and right edges too, independent of that label (e.g. PatternFieldWidget's "Command"
     * TitledBorder does both) - which the caller must supply since only owner itself knows those
     * reserved amounts; deriving them externally via owner.getInsets() conflates that reserved
     * space with the border+padding around owner's own inner content, which are a different,
     * smaller number on every edge.
     */
    private void positionAbove(JComponent owner, JLayeredPane pane, int ownerTopOffset, int horizontalInset) {
        Point ownerOrigin = SwingUtilities.convertPoint(owner, 0, 0, pane);
        int width = owner.getWidth() - 2 * horizontalInset;
        int height = content.getPreferredSize().height;
        content.setBounds(ownerOrigin.x + horizontalInset, ownerOrigin.y + ownerTopOffset - height, width, height);
    }

    private static JLabel buildRow(String candidate, boolean isHighlighted) {
        JLabel label = new JLabel(candidate);
        label.setFont(ROW_FONT);
        label.setOpaque(true);
        label.setFocusable(false);
        label.setBackground(isHighlighted ? WidgetTheme.selectedHighlight() : WidgetTheme.background());
        label.setForeground(isHighlighted ? WidgetTheme.selectedText() : WidgetTheme.normalText());
        label.setBorder(BorderFactory.createEmptyBorder(ROW_PADDING_V, ROW_PADDING_H, ROW_PADDING_V, ROW_PADDING_H));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel fullWidth(JLabel label) {
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, label.getPreferredSize().height));
        return label;
    }
}
