package com.swiftfaze.veil.ui.widget;

import com.swiftfaze.veil.input.Keybindings;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class RadioGroupWidget<T> extends Widget {
    // Same "bottom border by default, full outline once committed" pattern as PatternFieldWidget
    // (there: unfocused/focused; here: unconfirmed/confirmed) — but unlike that widget, these
    // labels get a *fixed* width (see the vertical-alignment block in refresh() below), so the
    // two states must reserve identical insets regardless of what they actually paint - a plain
    // line border (4 sides) vs. a matte border (bottom only) don't, which visibly shifted the
    // label and squeezed its text down to an ellipsis whenever it flipped between them. A custom
    // Border fixes the insets at the line border's full thickness always, painting only the
    // bottom edge for the unconfirmed state and leaving the rest of that reserved space blank.
    private static final Border CONFIRMED_BORDER = new RadioOptionBorder(/* confirmed= */ true);
    private static final Border UNCONFIRMED_BORDER = new RadioOptionBorder(/* confirmed= */ false);

    private final Function<T, String> optionRenderer;
    private final boolean horizontal;
    private final List<T> options = new ArrayList<>();
    private final List<JLabel> labels = new ArrayList<>();
    private int highlightedIndex = 0;
    private int selectedIndex = -1;
    private boolean wrapAround = true;
    private boolean fillWidth = false;
    private Consumer<T> onConfirm = t -> {};

    public RadioGroupWidget(Function<T, String> optionRenderer, boolean horizontal) {
        this.optionRenderer = optionRenderer;
        this.horizontal = horizontal;
        setLayout(new BoxLayout(this, horizontal ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
        bindKeys();
    }

    public void setOptions(List<T> options) {
        this.options.clear();
        this.options.addAll(options);
        this.highlightedIndex = 0;
        this.selectedIndex = -1;
        refresh();
    }

    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /**
     * Stretches a horizontal group to fill its container's width, splitting that width evenly
     * across the options (e.g. a Yes/No footer spanning a dialog edge-to-edge) instead of each
     * option sizing to its own text. No effect on a vertical group. Re-applies immediately if
     * options are already set.
     */
    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
        refresh();
    }

    /**
     * Re-highlights the first option and clears any confirmed selection, without discarding the
     * option list — for a popup that reopens with the same fixed options each time (e.g. a
     * Yes/No confirmation) and shouldn't carry over the previous confirmation's green outline or
     * highlight into the next time it's shown.
     */
    public void resetSelection() {
        highlightedIndex = 0;
        selectedIndex = -1;
        refreshHighlight();
    }

    public void setOnConfirm(Consumer<T> onConfirm) {
        this.onConfirm = onConfirm;
    }

    public T getHighlightedOption() {
        return options.isEmpty() ? null : options.get(highlightedIndex);
    }

    public T getSelectedOption() {
        return selectedIndex < 0 || selectedIndex >= options.size() ? null : options.get(selectedIndex);
    }

    public void selectOption(int index) {
        if (index >= 0 && index < options.size()) {
            selectedIndex = index;
            refreshHighlight();
        }
    }

    /**
     * Sets both the highlighted and selected option to the same index — for restoring a
     * previously-confirmed choice (e.g. loaded from a settings file) as the initial display.
     * selectOption() alone only marks an option confirmed without moving the highlight/display
     * cursor to it, so a non-default restored value would be selected internally but still show
     * the first option on screen.
     */
    public void selectAndHighlightOption(int index) {
        if (index >= 0 && index < options.size()) {
            highlightedIndex = index;
            selectedIndex = index;
            refreshHighlight();
        }
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    /**
     * Without this, the group's maximumSize defaults to unbounded (no child label sets one for
     * the horizontal case), so a vertical BoxLayout parent — e.g. a compact popup's content pane
     * — stretches it to the parent's full width and packs the options against the left edge
     * instead of leaving room for the parent's own alignmentX to center them. A fillWidth group
     * is the deliberate exception: it's supposed to stretch to the parent's width, just evenly
     * split across options (see refresh()) rather than packed at one edge.
     */
    @Override
    public Dimension getMaximumSize() {
        if (fillWidth && horizontal) {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
        return getPreferredSize();
    }

    public void moveUp() {
        moveVertical(false);
    }

    public void moveDown() {
        moveVertical(true);
    }

    public void moveLeft() {
        moveHorizontal(false);
    }

    public void moveRight() {
        moveHorizontal(true);
    }

    public void moveVertical(boolean down) {
        if (options.isEmpty()) {
            return;
        }
        highlightedIndex = down
            ? (wrapAround
                ? (highlightedIndex + 1) % options.size()
                : Math.min(options.size() - 1, highlightedIndex + 1))
            : (wrapAround
                ? (highlightedIndex - 1 + options.size()) % options.size()
                : Math.max(0, highlightedIndex - 1));
        refreshHighlight();
    }

    public void moveHorizontal(boolean right) {
        if (options.isEmpty()) {
            return;
        }
        highlightedIndex = right
            ? (wrapAround
                ? (highlightedIndex + 1) % options.size()
                : Math.min(options.size() - 1, highlightedIndex + 1))
            : (wrapAround
                ? (highlightedIndex - 1 + options.size()) % options.size()
                : Math.max(0, highlightedIndex - 1));
        refreshHighlight();
    }

    private void bindKeys() {
        InputMap inputMap = getInputMap(WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        if (horizontal) {
            inputMap.put(Keybindings.MENU_LEFT, "radio-left");
            inputMap.put(Keybindings.MENU_RIGHT, "radio-right");
            actionMap.put("radio-left", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { moveHorizontal(false); } });
            actionMap.put("radio-right", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { moveHorizontal(true); } });
        } else {
            inputMap.put(Keybindings.MENU_UP, "radio-up");
            inputMap.put(Keybindings.MENU_DOWN, "radio-down");
            actionMap.put("radio-up", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { moveVertical(false); } });
            actionMap.put("radio-down", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { moveVertical(true); } });
        }

        inputMap.put(Keybindings.MENU_CONFIRM, "radio-confirm");
        actionMap.put("radio-confirm", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedIndex = highlightedIndex;
                // Without this, the confirmed border wasn't applied until whatever move happened
                // next indirectly triggered a refresh - setting selectedIndex alone doesn't
                // repaint anything.
                refreshHighlight();
                T option = getSelectedOption();
                if (option != null) {
                    onConfirm.accept(option);
                }
            }
        });
    }

    private void refresh() {
        removeAll();
        labels.clear();
        setLayout(buildLayout());
        createLabels();
        refreshHighlight();
        if (!horizontal) {
            uniformizeVerticalLabelWidths();
        }
        revalidate();
        repaint();
    }

    private LayoutManager buildLayout() {
        if (fillWidth && horizontal) {
            return new GridLayout(1, 0, 4, 0);
        }
        return new BoxLayout(this, horizontal ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS);
    }

    private void createLabels() {
        for (T option : options) {
            JLabel label = new JLabel(optionRenderer.apply(option));
            label.setOpaque(true);
            label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
            label.setHorizontalAlignment(horizontal ? SwingConstants.CENTER : SwingConstants.LEFT);
            label.setAlignmentX(LEFT_ALIGNMENT);
            labels.add(label);
            add(label);
        }
    }

    private void uniformizeVerticalLabelWidths() {
        int maxWidth = labels.stream().mapToInt(l -> l.getPreferredSize().width).max().orElse(0);
        for (JLabel label : labels) {
            label.setMaximumSize(new Dimension(maxWidth, label.getPreferredSize().height));
            label.setPreferredSize(new Dimension(maxWidth, label.getPreferredSize().height));
        }
    }

    private void refreshHighlight() {
        for (int i = 0; i < labels.size(); i++) {
            JLabel label = labels.get(i);
            WidgetTheme.applySelection(label, i == highlightedIndex);
            // The confirmed option (Enter pressed) gets a green border distinct from the
            // highlighted/cursor background above — they can be different options at once (you've
            // confirmed one, then arrowed elsewhere without confirming again).
            Border outline = i == selectedIndex ? CONFIRMED_BORDER : UNCONFIRMED_BORDER;
            label.setBorder(BorderFactory.createCompoundBorder(outline, BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        }
        if (highlightedIndex < labels.size()) {
            scrollRectToVisible(labels.get(highlightedIndex).getBounds());
        }
    }

    /**
     * Always reserves the same 2px insets on every side, whether it's currently painting a full
     * outline (confirmed) or just the bottom edge (unconfirmed) — see the field comment above on
     * why the insets can't be allowed to differ between the two states here.
     */
    private static class RadioOptionBorder extends AbstractBorder {
        private static final int THICKNESS = 2;
        private final boolean confirmed;

        RadioOptionBorder(boolean confirmed) {
            this.confirmed = confirmed;
        }

        @Override
        @SuppressWarnings("PMD.ExcessiveParameterList") // Required by Swing's Border interface
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(confirmed ? WidgetTheme.validHighlight() : WidgetTheme.border());
            g.fillRect(x, y + height - THICKNESS, width, THICKNESS); // bottom, always drawn
            if (confirmed) {
                g.fillRect(x, y, width, THICKNESS); // top
                g.fillRect(x, y, THICKNESS, height); // left
                g.fillRect(x + width - THICKNESS, y, THICKNESS, height); // right
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(THICKNESS, THICKNESS, THICKNESS, THICKNESS);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(THICKNESS, THICKNESS, THICKNESS, THICKNESS);
            return insets;
        }
    }
}
