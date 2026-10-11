package com.swiftfaze.veil.ui.widget;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/**
 * A compact, fixed-size, centered popup variant suitable for smaller dialogs
 * (e.g. Yes/No confirmation). Unlike full-screen popups, this widget:
 * - Has a fixed preferred size (centered when laid out by FillLayout)
 * - Displays a title bar, divided from the body by a line
 * - Has no Close button (dismiss is via content-specific means, e.g. a radio choice)
 * - Reuses PopupWidget's core key-binding mechanism (Escape-to-dismiss, onUp/Down/Left/Right)
 */
public class CompactPopupWidget extends PopupWidget {
    private static final int POPUP_WIDTH = 400;
    private static final int POPUP_HEIGHT = 200;

    // Narrower than POPUP_WIDTH to clear the outer frame plus a body label's own padding
    // (see e.g. ResetConfirmationPopup/DropConfirmationPopup's question labels).
    private static final int BODY_TEXT_WIDTH = 340;

    private final JLabel titleLabel;

    public CompactPopupWidget(String title) {
        super();

        // Outer frame around the whole dialog, not just the title — without this the body
        // below the title bar has no border at all and reads as black-on-black against the
        // game view behind it.
        super.setBorder(BorderFactory.createLineBorder(WidgetTheme.border(), 2));

        // Add a title bar at the top, separated from the body by a divider line rather than
        // its own full box (the outer frame above already closes that box).
        titleLabel = new JLabel(title);
        titleLabel.setForeground(WidgetTheme.normalText());
        titleLabel.setBackground(WidgetTheme.background());
        titleLabel.setOpaque(true);
        titleLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, WidgetTheme.border()),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        add(titleLabel, BorderLayout.NORTH);
    }

    /**
     * Lets a subclass update the title after construction (e.g. to name the specific item a
     * confirmation is about) instead of being stuck with whatever title the constructor was
     * first given.
     */
    protected void setTitleText(String title) {
        titleLabel.setText(title);
    }

    /**
     * Builds a label-styled, word-wrapped, center-aligned body text component. A plain JLabel
     * never wraps on its own; HTML content in a JLabel promises wrapping via a CSS pixel width
     * but this JDK's HTML renderer doesn't actually honor that width when computing preferred
     * size (verified: it comes back *wider* than the unwrapped plain text, never constrained) —
     * so this uses a JTextPane instead, styled to read as a label (no border/caret/selection,
     * transparent so the popup's black background shows through, not editable or focusable).
     */
    protected static JTextPane createBodyLabel() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        pane.setBackground(null);
        pane.setForeground(WidgetTheme.normalText());
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        pane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        pane.setAlignmentX(Component.CENTER_ALIGNMENT);
        return pane;
    }

    /**
     * Sets a body label's text, center-aligns it, and fixes its size to BODY_TEXT_WIDTH wide by
     * however tall that many wrapped lines need — a JTextPane has no bounded maximumSize of its
     * own (a plain JComponent's defaults to unbounded), so without pinning both dimensions here
     * the popup's vertical BoxLayout content pane would stretch it to the pane's full width, and
     * its own centered alignment would have no narrower block left to center. The setSize() call
     * is what makes getPreferredSize() report the real wrapped height instead of an unwrapped
     * single-line guess — a JTextPane only wraps against a width it's actually been given, not
     * one just requested of it. Must be called again whenever the text changes, since a
     * different wrap (different line count) changes the required height.
     */
    protected static void setBodyText(JTextPane pane, String text) {
        setBodyText(pane, text, -1, -1);
    }

    /**
     * Same as {@link #setBodyText(JTextPane, String)}, but renders the [boldStart, boldEnd)
     * character range in bold — e.g. the specific item name inside an otherwise plain
     * confirmation sentence — instead of the whole line sharing one weight.
     */
    protected static void setBodyText(JTextPane pane, String text, int boldStart, int boldEnd) {
        pane.setText(text);

        StyledDocument doc = pane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        if (boldStart >= 0 && boldEnd > boldStart) {
            SimpleAttributeSet bold = new SimpleAttributeSet();
            StyleConstants.setBold(bold, true);
            doc.setCharacterAttributes(boldStart, boldEnd - boldStart, bold, false);
        }

        // Once set, getPreferredSize() returns exactly what setPreferredSize() was given,
        // regardless of new text — it stops asking the UI to recompute anything at all. Without
        // clearing a prior call's override first, re-measuring here would just read back that
        // stale (and possibly shorter, e.g. from an earlier one-line default) size instead of
        // this text's real wrapped height, silently clipping any line past it.
        pane.setPreferredSize(null);
        pane.setMaximumSize(null);
        pane.setSize(BODY_TEXT_WIDTH, Short.MAX_VALUE);
        Dimension fixed = new Dimension(BODY_TEXT_WIDTH, pane.getPreferredSize().height);
        pane.setPreferredSize(fixed);
        pane.setMaximumSize(fixed);
    }

    @Override
    public boolean isFullScreen() {
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(POPUP_WIDTH, POPUP_HEIGHT);
    }

    @Override
    public void open() {
        super.open();
        // Subclasses should override to focus their own content (e.g. a radio choice)
        // instead of the popup itself
    }
}
