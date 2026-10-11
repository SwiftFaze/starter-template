package com.swiftfaze.veil.ui.widget;

import org.jspecify.annotations.Nullable;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Material "outlined text field"-style widget: unfocused shows just a bottom
 * border with its label floating above; focused shows a full outline with
 * the label breaking the top edge (via {@link TitledBorder}, which already
 * implements exactly this "label interrupts the border line" look). Border
 * (and label) color tracks state: white while empty, red/green once there's
 * input, matching whether it fails or matches the pattern.
 * <p>
 * Backed by a real {@link JTextField} (styled to match, not a plain label)
 * rather than a hand-rolled StringBuilder + KeyListener, so cursor placement,
 * Left/Right movement, Home/End, Ctrl+A select-all, click-to-position, and
 * selection-replace-on-type all come from Swing's own well-tested text
 * component for free — a {@link DocumentFilter} is the only custom piece,
 * restricting what characters can actually land in the field.
 */
public class PatternFieldWidget extends Widget {
    private static final int UNFOCUSED_BORDER_WIDTH = 1;
    // Swing line/matte borders only take integer pixel widths, so 1px is as thin as either state
    // can get — focus is conveyed by the outline shape (full box vs. bottom-only) and color, not
    // by extra thickness.
    private static final int FOCUSED_BORDER_WIDTH = 1;
    private static final Font LABEL_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    // Fixed and generous, not derived from any child's own metrics — border width and whether a
    // TitledBorder's reserved label space is present both vary with state, and a tightly-computed
    // height left no slack for that, squeezing the content label's area toward zero.
    private static final int FIELD_HEIGHT = 40;
    private static final int LABELED_FIELD_HEIGHT = 56;
    // How far a Swing TitledBorder insets its wrapped outline from the component's own left/right
    // bounds to make room for the floating title - independent of, and smaller than, the extra
    // top-only space the title's own height needs (see getBorder() insets math in buildBorder()'s
    // caller). Not derivable from TitledBorder's own getBorderInsets(): that reports a larger
    // number on every edge (its own built-in safety margin for the title, not the outline's real
    // paint position), confirmed by comparing it against the actual rendered pixel column of the
    // outline. TitledBorder has no public API for this, so it's pinned here as a measured
    // constant of the JDK's own implementation rather than guessed from insets arithmetic.
    private static final int TITLED_BORDER_HORIZONTAL_EDGE_SPACING = 2;

    private final Pattern pattern;
    private final JTextField textField;
    private final String fieldLabel;
    private boolean hasFocus = false;
    private boolean validityColoringEnabled = true;
    private String placeholder;
    private Consumer<String> onInputChanged = input -> { };

    public PatternFieldWidget(String pattern) {
        this(pattern, null);
    }

    public PatternFieldWidget(String pattern, @Nullable String fieldLabel) {
        this.pattern = Pattern.compile(pattern);
        this.fieldLabel = fieldLabel;
        this.textField = new PlaceholderTextField();
        setFocusable(false); // the text field is the real focus target, not this outer panel

        textField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
        textField.setForeground(WidgetTheme.normalText());
        textField.setBackground(WidgetTheme.background());
        textField.setCaretColor(WidgetTheme.normalText());
        textField.setCaret(new BlockCaret());
        textField.setSelectionColor(WidgetTheme.selectedHighlight());
        textField.setSelectedTextColor(WidgetTheme.selectedText());
        textField.setBorder(BorderFactory.createEmptyBorder());
        ((AbstractDocument) textField.getDocument()).setDocumentFilter(new AllowedCharacterFilter());
        textField.getDocument().addDocumentListener(new PatternFieldDocumentListener());
        textField.addFocusListener(new PatternFieldFocusListener());
        bindEnterToNextField();

        setLayout(new BorderLayout());
        setAlignmentX(LEFT_ALIGNMENT);
        add(textField, BorderLayout.CENTER);
        updateAppearance();

        // Stretches to fill whatever width its container offers — matches every other widget's
        // "full width" treatment (ListWidget's rows, TableWidget's row panels).
        int height = fieldLabel != null ? LABELED_FIELD_HEIGHT : FIELD_HEIGHT;
        setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        setPreferredSize(new Dimension(200, height));
    }

    @Override
    public boolean requestFocusInWindow() {
        return textField.requestFocusInWindow();
    }

    /**
     * How far in from this field's own left and right edges its actual visible outlined box
     * sits, on each side - zero when there's no label (an untitled border paints flush with the
     * component's own bounds). See {@link #TITLED_BORDER_HORIZONTAL_EDGE_SPACING}.
     */
    public int getVisibleBoxHorizontalInset() {
        return fieldLabel != null ? TITLED_BORDER_HORIZONTAL_EDGE_SPACING : 0;
    }

    public String getInput() {
        return textField.getText();
    }

    public boolean patternIsValid() {
        return pattern.matcher(textField.getText()).matches();
    }

    public JTextField getTextField() {
        return textField;
    }

    /**
     * Gray hint text shown in place of the real value while the field is empty (e.g.
     * "Search by name, category, or mod...") — cleared automatically the moment any real
     * input is typed, same as a browser's native placeholder.
     */
    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        textField.repaint();
    }

    /**
     * Disables the red/green valid/invalid border and text coloring — for a consumer reusing
     * this widget's outlined-field look for free-text input that has no pattern to validate
     * against (e.g. a search box), where "invalid" has no meaning. Enabled by default.
     */
    public void setValidityColoringEnabled(boolean enabled) {
        this.validityColoringEnabled = enabled;
        updateAppearance();
    }

    public void setOnInputChanged(Consumer<String> onInputChanged) {
        this.onInputChanged = onInputChanged;
    }

    /**
     * Inserts at the current cursor position, replacing any active selection — same as real
     * typing. Existing callers that type into a fresh/empty field (the common case) see the same
     * append-at-the-end result as before, since the cursor naturally sits at the end there.
     */
    public void typeCharacters(String chars) {
        textField.replaceSelection(chars);
        // DefaultCaret doesn't reliably auto-advance past a programmatic replaceSelection() on a
        // component that's never been realized/shown (as in headless unit/Cucumber tests) - real
        // interactive typing (via the field's own native key handling once it has real focus in
        // a real window) doesn't need this, only this programmatic test-helper path does.
        textField.setCaretPosition(textField.getDocument().getLength());
    }

    /**
     * Deletes the selection if one is active, else the character immediately before the cursor —
     * same as a real Backspace press. Called "deleteLastCharacter" for its original meaning
     * (nothing had moved the cursor away from the end, so backspace-at-cursor and delete-the-
     * actual-last-character were the same operation); now that the cursor can move, it deletes at
     * the cursor, matching what Backspace does everywhere else.
     */
    public void deleteLastCharacter() {
        int selectionStart = textField.getSelectionStart();
        int selectionEnd = textField.getSelectionEnd();
        if (selectionStart != selectionEnd) {
            textField.replaceSelection("");
            return;
        }
        int caret = textField.getCaretPosition();
        if (caret == 0) {
            return;
        }
        try {
            textField.getDocument().remove(caret - 1, 1);
        } catch (BadLocationException ignored) {
            // caret - 1 is always a valid offset here (caret > 0, just checked)
        }
    }

    private static boolean isAppendable(char c) {
        // Enter (\n, \r) satisfies Character.isWhitespace() just like a space does, so without
        // this exclusion it could be inserted as a literal newline — a character no single-line
        // pattern ever matches. Real Enter presses never reach here (see bindEnterToNextField),
        // but the filter guards direct/programmatic insertion too.
        if (c == '\n' || c == '\r') {
            return false;
        }
        return Character.isLetterOrDigit(c) || Character.isWhitespace(c) || isPrintableSpecial(c);
    }

    private static boolean isPrintableSpecial(char c) {
        return c != '\t' && c >= 32 && c <= 126;
    }

    private void bindEnterToNextField() {
        // Enter moves to the next field, like Tab, rather than JTextField's default (fire an
        // ActionEvent, no focus change) - standard single-line-field behavior.
        textField.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "next-field");
        textField.getActionMap().put("next-field", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { textField.transferFocus(); }
        });
    }

    private void updateAppearance() {
        Color stateColor = stateColor();
        textField.setForeground(getInput().isEmpty() ? WidgetTheme.normalText() : stateColor);
        setBorder(buildBorder(stateColor));
        onInputChanged.accept(getInput());
    }

    private Color stateColor() {
        if (!validityColoringEnabled || getInput().isEmpty()) {
            return WidgetTheme.normalText();
        }
        return patternIsValid() ? WidgetTheme.validHighlight() : WidgetTheme.invalidHighlight();
    }

    private Border buildBorder(Color color) {
        // Unfocused: bottom line only. Focused: full outline. TitledBorder reserves the same
        // label space either way, but only visibly "breaks" a line that's actually drawn there —
        // so the label just floats above the field when unfocused (no top line to interrupt),
        // and sits on the top edge, breaking it, once focused. Exactly the Material "outlined
        // field" look, with no custom border-painting/gap-cutting logic needed.
        Border outline = hasFocus
                ? BorderFactory.createLineBorder(color, FOCUSED_BORDER_WIDTH)
                : BorderFactory.createMatteBorder(0, 0, UNFOCUSED_BORDER_WIDTH, 0, color);
        Border padded = BorderFactory.createCompoundBorder(outline, BorderFactory.createEmptyBorder(4, 8, 4, 8));
        if (fieldLabel == null) {
            return padded;
        }
        return BorderFactory.createTitledBorder(
                padded, fieldLabel, TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, LABEL_FONT, color);
    }

    /**
     * A plain {@link JTextField} that also paints gray placeholder text over itself while
     * empty — a minimal override rather than a separate overlaid label, since a label would
     * need its own positioning kept in sync with the field's insets/font.
     */
    private class PlaceholderTextField extends JTextField {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (placeholder == null || !getText().isEmpty()) {
                return;
            }
            FontMetrics metrics = g.getFontMetrics(getFont());
            int x = getInsets().left;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g.setColor(WidgetTheme.dimmedText());
            g.setFont(getFont());
            g.drawString(placeholder, x, y);
        }
    }

    /**
     * A solid block cursor (like a terminal/console) instead of Swing's default thin vertical
     * line — a different paint shape, otherwise using {@link DefaultCaret}'s own blink-timer
     * machinery as-is.
     */
    private static class BlockCaret extends DefaultCaret {
        private static final int BLINK_RATE_MS = 500;

        BlockCaret() {
            // The blink rate Swing normally applies comes from the look-and-feel installing its
            // *own* default caret onto a component; replacing that caret with this one (via
            // JTextField.setCaret) skips that step entirely, leaving DefaultCaret's own default
            // (0 - the blink timer never starts, so it just renders solid) unless set explicitly.
            setBlinkRate(BLINK_RATE_MS);
        }

        @Override
        public void paint(Graphics g) {
            if (!isVisible()) {
                return;
            }
            JTextComponent component = getComponent();
            if (component == null) {
                return;
            }
            try {
                int dot = getDot();
                Rectangle2D caretBounds = component.modelToView2D(dot);
                FontMetrics metrics = component.getFontMetrics(component.getFont());
                int x = (int) caretBounds.getX();
                int y = (int) caretBounds.getY();
                int charWidth = metrics.charWidth('M');

                g.setColor(component.getCaretColor());
                g.fillRect(x, y, charWidth, (int) caretBounds.getHeight());

                // A solid block would otherwise paint straight over whatever character sits at
                // the cursor position — redraw it on top, in the field's background color, so it
                // stays readable against the block (the usual terminal-cursor "invert" look).
                if (dot < component.getDocument().getLength()) {
                    String charUnderCursor = component.getDocument().getText(dot, 1);
                    g.setColor(component.getBackground());
                    g.setFont(component.getFont());
                    g.drawString(charUnderCursor, x, y + metrics.getAscent());
                }
            } catch (BadLocationException ignored) {
                // Nothing at this position to draw a cursor for.
            }
        }

        @Override
        protected synchronized void damage(Rectangle r) {
            if (r == null) {
                return;
            }
            JTextComponent component = getComponent();
            FontMetrics metrics = component.getFontMetrics(component.getFont());
            x = r.x;
            y = r.y;
            width = metrics.charWidth('M');
            height = r.height;
            repaint();
        }
    }

    private static final class AllowedCharacterFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            super.insertString(fb, offset, filtered(text), attr);
        }

        @Override
        @SuppressWarnings("PMD.ExcessiveParameterList") // Required by javax.swing.text.DocumentFilter's replace() signature
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            super.replace(fb, offset, length, filtered(text), attrs);
        }

        private static String filtered(String text) {
            if (text == null) {
                return "";
            }
            StringBuilder allowed = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (isAppendable(c)) {
                    allowed.append(c);
                }
            }
            return allowed.toString();
        }
    }

    private class PatternFieldDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) { updateAppearance(); }

        @Override
        public void removeUpdate(DocumentEvent e) { updateAppearance(); }

        @Override
        public void changedUpdate(DocumentEvent e) { updateAppearance(); }
    }

    private class PatternFieldFocusListener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) {
            hasFocus = true;
            updateAppearance();
        }

        @Override
        public void focusLost(FocusEvent e) {
            hasFocus = false;
            updateAppearance();
        }
    }
}
